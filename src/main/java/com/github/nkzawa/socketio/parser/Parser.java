package com.github.nkzawa.socketio.parser;

import com.github.nkzawa.emitter.Emitter;
import org.json.JSONException;
import org.json.JSONTokener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class Parser {

    private static final Logger logger = Logger.getLogger(Parser.class.getName());

    /**
     * Packet type `connect`.
     */
    public static final int CONNECT = 0;

    /**
     * Packet type `disconnect`.
     */
    public static final int DISCONNECT = 1;

    /**
     * Packet type `event`.
     */
    public static final int EVENT = 2;

    /**
     * Packet type `ack`.
     */
    public static final int ACK = 3;

    /**
     * Packet type `error`.
     */
    public static final int ERROR = 4;

    /**
     * Packet type `binary event`.
     */
    public static final int BINARY_EVENT = 5;

    /**
     * Packet type `binary ack`.
     */
    public static final int BINARY_ACK = 6;

    public static int protocol = 4;

    /**
     * Packet types.
     */
    public static String[] types = new String[] {
        "CONNECT",
        "DISCONNECT",
        "EVENT",
        "ACK",
        "ERROR",
        "BINARY_EVENT",
        "BINARY_ACK"
    };


    private Parser() {}

    private static Packet<String> error() {
        return new Packet<String>(ERROR, "parser error");
    }


    public static class Encoder {

        public Encoder() {}

        public void encode(Packet obj, Callback callback) {
            logger.fine(String.format("encoding packet %s", obj));

            if (BINARY_EVENT == obj.type || BINARY_ACK == obj.type) {
                encodeAsBinary(obj, callback);
            } else {
                String encoding = encodeAsString(obj);
                callback.call(new String[] {encoding});
            }
        }

        private String encodeAsString(Packet obj) {
            StringBuilder str = new StringBuilder();
            boolean nsp = false;

            str.append(obj.type);

            if (BINARY_EVENT == obj.type || BINARY_ACK == obj.type) {
                str.append(obj.attachments);
                str.append("-");
            }

            if (obj.nsp != null && obj.nsp.length() != 0 && !"/".equals(obj.nsp)) {
                nsp = true;
                str.append(obj.nsp);
            }

            if (obj.id >= 0) {
                if (nsp) {
                    str.append(",");
                    nsp = false;
                }
                str.append(obj.id);
            }

            if (obj.data != null) {
                if (nsp) str.append(",");
                str.append(obj.data);
            }

            logger.fine(String.format("encoded %s as %s", obj, str));
            return str.toString();
        }

        private void encodeAsBinary(Packet obj, Callback callback) {
            Binary.DeconstructedPacket deconstruction = Binary.deconstructPacket(obj);
            String pack = encodeAsString(deconstruction.packet);
            List<Object> buffers = new ArrayList<Object>(Arrays.asList(deconstruction.buffers));

            buffers.add(0, pack);
            callback.call(buffers.toArray());
        }

        public interface Callback {

            public void call(Object[] data);
        }
    }

    public static class Decoder extends Emitter {

        public static String EVENT_DECODED = "decoded";

        /*package*/ BinaryReconstructor reconstructor;

        public Decoder() {
            this.reconstructor = null;
        }

        public void add(String obj) {
            Packet packet = decodeString(obj);
            if (BINARY_EVENT == packet.type || BINARY_ACK == packet.type) {
                this.reconstructor = new BinaryReconstructor(packet);

                if (this.reconstructor.reconPack.attachments == 0) {
                    this.emit(EVENT_DECODED, packet);
                }
            } else {
                this.emit(EVENT_DECODED, packet);
            }
        }

        public void add(byte[] obj) {
            if (this.reconstructor == null) {
                throw new RuntimeException("got binary data when not reconstructing a packet");
            } else {
                Packet packet = this.reconstructor.takeBinaryData(obj);
                if (packet != null) {
                    this.reconstructor = null;
                    this.emit(EVENT_DECODED, packet);
                }
            }
        }

        private static Packet decodeString(String str) {
            Packet p = new Packet();
            int i = 0;
            int length = str.length();

            p.type = Character.getNumericValue(str.charAt(0));
            if (p.type < 0 || p.type > types.length - 1) return error();

            if (BINARY_EVENT == p.type || BINARY_ACK == p.type) {
                if (!str.contains("-") || length <= i + 1) return error();
                StringBuilder attachments = new StringBuilder();
                while (str.charAt(++i) != '-') {
                    attachments.append(str.charAt(i));
                }
                p.attachments = Integer.parseInt(attachments.toString());
            }

            if (length > i + 1 && '/' == str.charAt(i + 1)) {
                StringBuilder nsp = new StringBuilder();
                while (true) {
                    ++i;
                    char c = str.charAt(i);
                    if (',' == c) break;
                    nsp.append(c);
                    if (i + 1 == length) break;
                }
                p.nsp = nsp.toString();
            } else {
                p.nsp = "/";
            }

            if (length > i + 1){
                Character next = str.charAt(i + 1);
                if (Character.getNumericValue(next) > -1) {
                    StringBuilder id = new StringBuilder();
                    while (true) {
                        ++i;
                        char c = str.charAt(i);
                        if (Character.getNumericValue(c) < 0) {
                            --i;
                            break;
                        }
                        id.append(c);
                        if (i + 1 == length) break;
                    }
                    try {
                        p.id = Integer.parseInt(id.toString());
                    } catch (NumberFormatException e){
                        return error();
                    }
                }
            }

            if (length > i + 1){
                try {
                    str.charAt(++i);
                    p.data = new JSONTokener(str.substring(i)).nextValue();
                } catch (JSONException e) {
                    return error();
                }
            }

            logger.fine(String.format("decoded %s as %s", str, p));
            return p;
        }

        public void destroy() {
            if (this.reconstructor != null) {
                this.reconstructor.finishReconstruction();
            }
        }
    }


    /*package*/ static class BinaryReconstructor {

        public Packet reconPack;

        /*package*/ List<byte[]> buffers;

        BinaryReconstructor(Packet packet) {
            this.reconPack = packet;
            this.buffers = new ArrayList<byte[]>();
        }

        public Packet takeBinaryData(byte[] binData) {
            this.buffers.add(binData);
            if (this.buffers.size() == this.reconPack.attachments) {
                Packet packet = Binary.reconstructPacket(this.reconPack,
                        this.buffers.toArray(new byte[this.buffers.size()][]));
                this.finishReconstruction();
                return packet;
            }
            return null;
        }

        public void finishReconstruction () {
            this.reconPack = null;
            this.buffers = new ArrayList<byte[]>();
        }
    }
}


