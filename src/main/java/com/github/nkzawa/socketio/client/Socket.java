package com.github.nkzawa.socketio.client;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.hasbinarydata.HasBinaryData;
import com.github.nkzawa.socketio.parser.Packet;
import com.github.nkzawa.socketio.parser.Parser;
import com.github.nkzawa.thread.EventThread;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;
import java.util.logging.Logger;

/**
 * The socket class for Socket.IO Client.
 */
public class Socket extends Emitter {

    private static final Logger logger = Logger.getLogger(Socket.class.getName());

    /**
     * Called on a connection.
     */
    public static final String EVENT_CONNECT = "connect";

    /**
     * Called on a disconnection.
     */
    public static final String EVENT_DISCONNECT = "disconnect";

    /**
     * Called on a connection error.
     *
     * <p>Parameters:</p>
     * <ul>
     *   <li>(Exception) error data.</li>
     * </ul>
     */
    public static final String EVENT_ERROR = "error";

    public static final String EVENT_MESSAGE = "message";

    private static Map<String, Integer> events = new HashMap<String, Integer>() {{
        put(EVENT_CONNECT, 1);
        put(EVENT_DISCONNECT, 1);
        put(EVENT_ERROR, 1);
    }};

    private static boolean jsonArrayNativeRemove;

    {
        try {
            jsonArrayNativeRemove = JSONArray.class.getMethod("remove", new Class[] {
                int.class
            }) != null;
        } catch (NoSuchMethodException e) {
            jsonArrayNativeRemove = false;
        }
    }

    private boolean connected;
    private boolean disconnected = true;
    private int ids;
    private String nsp;
    /*package*/ Manager io;
    private Map<Integer, Ack> acks = new HashMap<Integer, Ack>();
    private Queue<On.Handle> subs;
    private final Queue<List<Object>> buffer = new LinkedList<List<Object>>();

    public Socket(Manager io, String nsp) {
        this.io = io;
        this.nsp = nsp;
    }

    /**
     * Connects the socket.
     */
    public Socket open() {
        EventThread.exec(new Runnable() {
            @Override
            public void run() {
                if (Socket.this.connected) return;
                final Manager io = Socket.this.io;
                io.open();
                Socket.this.subs = new LinkedList<On.Handle>() {{
                    add(On.on(io, Manager.EVENT_OPEN, new Listener() {
                        @Override
                        public void call(Object... args) {
                            Socket.this.onopen();
                        }
                    }));
                    add(On.on(io, Manager.EVENT_ERROR, new Listener() {
                        @Override
                        public void call(Object... args) {
                            Socket.this.onerror(args.length > 0 ? (Exception) args[0] : null);
                        }
                    }));
                    add(On.on(io, Manager.EVENT_PACKET, new Listener() {
                        @Override
                        public void call(Object... args) {
                            Socket.this.onpacket((Packet) args[0]);
                        }
                    }));
                    add(On.on(io, Manager.EVENT_CLOSE, new Listener() {
                        @Override
                        public void call(Object... args) {
                            Socket.this.onclose(args.length > 0 ? (String) args[0] : null);
                        }
                    }));
                }};
                if (Manager.ReadyState.OPEN == Socket.this.io.readyState) Socket.this.onopen();
            }
        });
        return this;
    }

    /**
     * Connects the socket.
     */
    public Socket connect() {
        return this.open();
    }

    /**
     * Send messages.
     *
     * @param args data to send.
     * @return a reference to this object.
     */
    public Socket send(final Object... args) {
        EventThread.exec(new Runnable() {
            @Override
            public void run() {
                Socket.this.emit(EVENT_MESSAGE, args);
            }
        });
        return this;
    }

    /**
     * Emits an event. When you pass {@link Ack} at the last argument, then the acknowledge is done.
     *
     * @param event an event name.
     * @param args data to send.
     * @return a reference to this object.
     */
    @Override
    public Emitter emit(final String event, final Object... args) {
        EventThread.exec(new Runnable() {
            @Override
            public void run() {
                if (events.containsKey(event)) {
                    Socket.super.emit(event, args);
                    return;
                }

                List<Object> _args = new ArrayList<Object>(args.length + 1);
                _args.add(event);
                _args.addAll(Arrays.asList(args));

                JSONArray jsonArgs = new JSONArray();
                for (Object arg : _args) {
                    jsonArgs.put(arg);
                }
                int parserType = Parser.EVENT;
                if (HasBinaryData.hasBinary(jsonArgs)) { parserType = Parser.BINARY_EVENT; }
                Packet<JSONArray> packet = new Packet<JSONArray>(parserType, jsonArgs);

                if (_args.get(_args.size() - 1) instanceof Ack) {
                    logger.fine(String.format("emitting packet with ack id %d", Socket.this.ids));
                    Socket.this.acks.put(Socket.this.ids, (Ack)_args.remove(_args.size() - 1));
                    if (jsonArrayNativeRemove) {
                        jsonArgs.remove(jsonArgs.length() - 1);
                    } else {
                        jsonArgs = remove(jsonArgs, jsonArgs.length() - 1);
                        packet.data = jsonArgs;
                    }
                    packet.id = Socket.this.ids++;
                }

                Socket.this.packet(packet);
            }
        });
        return this;
    }

    private static JSONArray remove(JSONArray a, int pos) {
        JSONArray na = new JSONArray();
        try {
            for (int i = 0; i < a.length(); i++){
                if (i != pos) {
                    na.put(a.get(i));
                }
            }
        } catch (Exception e) {
            throw new JSONException(e);
        }
        return na;
    }

    /**
     * Emits an event with an acknowledge.
     *
     * @param event an event name
     * @param args data to send.
     * @param ack the acknowledgement to be called
     * @return a reference to this object.
     */
    public Emitter emit(final String event, final Object[] args, final Ack ack) {
        EventThread.exec(new Runnable() {
            @Override
            public void run() {
                List<Object> _args = new ArrayList<Object>() {{
                    add(event);
                    if (args != null) {
                        addAll(Arrays.asList(args));
                    }
                }};
                Packet<JSONArray> packet = new Packet<JSONArray>(Parser.EVENT, new JSONArray(_args));

                logger.fine(String.format("emitting packet with ack id %d", ids));
                Socket.this.acks.put(ids, ack);
                packet.id = ids++;

                Socket.this.packet(packet);
            }
        });
        return this;
    }

    private void packet(Packet packet) {
        packet.nsp = this.nsp;
        this.io.packet(packet);
    }

    private void onerror(Exception err) {
        this.emit(EVENT_ERROR, err);
    }

    private void onopen() {
        logger.fine("transport is open - connecting");

        if (!"/".equals(this.nsp)) {
            this.packet(new Packet(Parser.CONNECT));
        }
    }

    private void onclose(String reason) {
        logger.fine(String.format("close (%s)", reason));
        this.connected = false;
        this.disconnected = true;
        this.emit(EVENT_DISCONNECT, reason);
    }

    private void onpacket(Packet packet) {
        if (!this.nsp.equals(packet.nsp)) return;

        switch (packet.type) {
            case Parser.CONNECT:
                this.onconnect();
                break;

            case Parser.EVENT:
                this.onevent(packet);
                break;

            case Parser.BINARY_EVENT:
                this.onevent(packet);
                break;

            case Parser.ACK:
                this.onack(packet);
                break;

            case Parser.BINARY_ACK:
                this.onack(packet);
                break;

            case Parser.DISCONNECT:
                this.ondisconnect();
                break;

            case Parser.ERROR:
                this.emit(EVENT_ERROR, packet.data);
                break;
        }
    }

    private void onevent(Packet<JSONArray> packet) {
        List<Object> args = new ArrayList<Object>(Arrays.asList(toArray(packet.data)));
        logger.fine(String.format("emitting event %s", args));

        if (packet.id >= 0) {
            logger.fine("attaching ack callback to event");
            args.add(this.ack(packet.id));
        }

        if (this.connected) {
            String event = (String)args.remove(0);
            super.emit(event, args.toArray());
        } else {
            this.buffer.add(args);
        }
    }

    private Ack ack(final int id) {
        final Socket self = this;
        final boolean[] sent = new boolean[] {false};
        return new Ack() {
            @Override
            public synchronized void call(Object... args) {
                if (sent[0]) return;
                sent[0] = true;
                logger.fine(String.format("sending ack %s", args));

                int type = HasBinaryData.hasBinary(args) ? Parser.BINARY_ACK : Parser.ACK;
                Packet<JSONArray> packet = new Packet<JSONArray>(type, new JSONArray(Arrays.asList(args)));
                packet.id = id;
                self.packet(packet);
            }
        };
    }

    private void onack(Packet<JSONArray> packet) {
        logger.fine(String.format("calling ack %s with %s", packet.id, packet.data));
        Ack fn = this.acks.remove(packet.id);
        fn.call(toArray(packet.data));
    }

    private void onconnect() {
        this.connected = true;
        this.disconnected = false;
        this.emit(EVENT_CONNECT);
        this.emitBuffered();
    }

    private void emitBuffered() {
        List<Object> data;
        while ((data = this.buffer.poll()) != null) {
            String event = (String)data.get(0);
            super.emit(event, data.toArray());
        }
    }

    private void ondisconnect() {
        logger.fine(String.format("server disconnect (%s)", this.nsp));
        this.destroy();
        this.onclose("io server disconnect");
    }

    private void destroy() {
        for (On.Handle sub : this.subs) {
            sub.destroy();
        }

        this.io.destroy(this);
    }

    /**
     * Disconnects the socket.
     *
     * @return a reference to this object.
     */
    public Socket close() {
        EventThread.exec(new Runnable() {
            @Override
            public void run() {
                if (!Socket.this.connected) return;

                logger.fine(String.format("performing disconnect (%s)", Socket.this.nsp));
                Socket.this.packet(new Packet(Parser.DISCONNECT));

                Socket.this.destroy();

                Socket.this.onclose("io client disconnect");
            }
        });
        return this;
    }

    /**
     * Disconnects the socket.
     *
     * @return a reference to this object.
     */
    public Socket disconnect() {
        return this.close();
    }

    private static Object[] toArray(JSONArray array) {
        int length = array.length();
        Object[] data = new Object[length];
        for (int i = 0; i < length; i++) {
            Object v = array.get(i);
            data[i] = v == JSONObject.NULL ? null : v;
        }
        return data;
    }
}

