package com.github.nkzawa.socketio.parser;

import com.github.nkzawa.emitter.Emitter;
import com.google.gson.*;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class Parser {

    private static final Logger logger = Logger.getLogger(Parser.class.getName());

    private static final Gson gson = new Gson();
    private static final JsonParser json = new JsonParser();

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

    public static int protocol = 3;

    /**
     * Packet types.
     */
    public static String[] types = new String[] {
        "CONNECT",
        "DISCONNECT",
        "EVENT",
        "BINARY_EVENT",
        "ACK",
        "ERROR",
    };


    private Parser() {}

    private static Packet error() {
        return new Packet(ERROR, new JsonPrimitive("parser error"));
    }


    public static class Encoder {

        public Encoder() {}

        public void encode(Packet obj, Callback callback) {
            logger.fine(String.format("encoding packet %s", obj));

            if (BINARY_EVENT == obj.type || ACK == obj.type) {
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

            if (BINARY_EVENT == obj.type || ACK == obj.type) {
                str.append(obj.attachments);
                str.append("-");
            }

            if (obj.nsp != null && !obj.nsp.isEmpty() && !"/".equals(obj.nsp)) {
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
                str.append(gson.toJson(obj.data));
            }

            logger.fine(String.format("encoded %s as %s", obj, str));
            return str.toString();
        }

        private void encodeAsBinary(Packet obj, Callback callback) {
            // TODO
        }

        public interface Callback {

            public void call(String[] data);
        }
    }

    public static class Decoder extends Emitter {

        public static String EVENT_DECODED = "decoded";

        private BinaryReconstructor reconstructor;

        public Decoder() {
            this.reconstructor = null;
        }

        public void add(String obj) {
            Packet packet = decodeString(obj);
            if (BINARY_EVENT == packet.type || ACK == packet.type) {
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

        public static Packet decodeString(String str) {
            Packet p = new Packet();
            int i = 0;

            p.type = Character.getNumericValue(str.charAt(0));
            if (p.type < 0 || p.type > types.length - 1) return error();

            if (BINARY_EVENT == p.type || ACK == p.type) {
                StringBuilder attachments = new StringBuilder();
                while (str.charAt(++i) != '-') {
                    attachments.append(str.charAt(i));
                }
                p.attachments = Integer.parseInt(attachments.toString());
            }

            if (str.length() > i + 1 && '/' == str.charAt(i + 1)) {
                StringBuilder nsp = new StringBuilder();
                while (true) {
                    ++i;
                    char c = str.charAt(i);
                    if (',' == c) break;
                    nsp.append(c);
                    if (i + 1 == str.length()) break;
                }
                p.nsp = nsp.toString();
            } else {
                p.nsp = "/";
            }

            Character next;
            try {
                next = str.charAt(i + 1);
            } catch (IndexOutOfBoundsException e) {
                next = Character.UNASSIGNED;
            }
            if (Character.UNASSIGNED != next && Character.getNumericValue(next) > -1) {
                StringBuilder id = new StringBuilder();
                while (true) {
                    ++i;
                    char c = str.charAt(i);
                    if (Character.getNumericValue(c) < 0) {
                        --i;
                        break;
                    }
                    id.append(c);
                    if (i + 1 == str.length()) break;
                }
                p.id = Integer.parseInt(id.toString());
            }

            try {
                str.charAt(++i);
                p.data = json.parse(str.substring(i));
            } catch (IndexOutOfBoundsException e) {
                // do nothing
            } catch (JsonParseException e) {
                return error();
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


    private static class BinaryReconstructor {

        public Packet reconPack;

        private List<byte[]> buffers;

        BinaryReconstructor(Packet packet) {
            this.reconPack = packet;
            this.buffers = new ArrayList<byte[]>();
        }

        public Packet takeBinaryData(byte[] binData) {
            this.buffers.add(binData);
            if (this.buffers.size() == this.reconPack.attachments) {
                // TODO:
            }
            return null;
        }

        public void finishReconstruction () {
            this.reconPack = null;
            this.buffers = new ArrayList<byte[]>();
        }
    }
}


