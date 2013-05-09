package com.github.nkzawa.socketio.client;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.engineio.client.EventThread;
import com.github.nkzawa.socketio.parser.Packet;
import com.github.nkzawa.socketio.parser.Parser;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.util.*;
import java.util.logging.Logger;

/**
 * The socket class for Socket.IO Client.
 */
public class Socket extends Emitter {

    private static final Logger logger = Logger.getLogger(Socket.class.getName());

    private static final Gson gson = new Gson();

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

    private static List<String> events = new ArrayList<String>() {{
        add(EVENT_CONNECT);
        add(EVENT_DISCONNECT);
        add(EVENT_ERROR);
    }};

    private boolean connected;
    private boolean disconnected = true;
    private int ids;
    private String nsp;
    private Manager io;
    private Map<Integer, Ack> acks = new HashMap<Integer, Ack>();
    private Queue<On.Handle> subs;
    private final Queue<LinkedList<Object>> buffer = new LinkedList<LinkedList<Object>>();


    public Socket(Manager io, String nsp) {
        this.io = io;
        this.nsp = nsp;
    }

    /**
     * Connects the socket.
     */
    public void open() {
        EventThread.exec(new Runnable() {
            @Override
            public void run() {
                final Manager io = Socket.this.io;
                Socket.this.subs = new LinkedList<On.Handle>() {{
                    add(On.on(io, Manager.EVENT_OPEN, new Listener() {
                        @Override
                        public void call(Object... objects) {
                            Socket.this.onopen();
                        }
                    }));
                    add(On.on(io, Manager.EVENT_ERROR, new Listener() {
                        @Override
                        public void call(Object... objects) {
                            Socket.this.onerror(objects.length > 0 ? (Exception) objects[0] : null);
                        }
                    }));
                }};
                if (Socket.this.io.readyState == Manager.ReadyState.OPEN) Socket.this.onopen();
                io.open();
            }
        });
    }

    /**
     * Connects the socket.
     */
    public void connect() {
        this.open();
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
                if (events.contains(event)) {
                    Socket.super.emit(event, args);
                } else {
                    LinkedList<Object> _args = new LinkedList<Object>(Arrays.asList(args));
                    if (_args.peekLast() instanceof Ack) {
                        Ack ack = (Ack)_args.pollLast();
                        Socket.this.emit(event, _args.toArray(), ack);
                        return;
                    }

                    _args.offerFirst(event);
                    Packet packet = new Packet(Parser.EVENT, toJsonArray(_args));
                    Socket.this.packet(packet);
                }
            }
        });
        return this;
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
                    addAll(Arrays.asList(args));
                }};
                Packet packet = new Packet(Parser.EVENT, toJsonArray(_args));

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

        Manager io = this.io;
        this.subs.add(On.on(io, Manager.EVENT_PACKET, new Listener() {
            @Override
            public void call(Object... objects) {
                Socket.this.onpacket((Packet)objects[0]);
            }
        }));
        this.subs.add(On.on(io, Manager.EVENT_CLOSE, new Listener() {
            @Override
            public void call(Object... objects) {
                String reason = objects.length > 0 ? (String) objects[0] : null;
                Socket.this.onclose(reason);
            }
        }));
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

            case Parser.ACK:
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

    private void onevent(Packet packet) {
        LinkedList<Object> args = new LinkedList<Object>(fromJsonArray(packet.data.getAsJsonArray()));
        logger.fine(String.format("emitting event %s", args));

        if (packet.id >= 0) {
            logger.fine("attaching ack callback to event");
            args.offerLast(this.ack(packet.id));
        }

        if (this.connected) {
            String event = (String)args.pollFirst();
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
                Packet packet = new Packet(Parser.ACK, gson.toJsonTree(args));
                packet.id = id;
                self.packet(packet);
            }
        };
    }

    private void onack(Packet packet) {
        logger.fine(String.format("calling ack %s with %s", packet.id, packet.data));
        Ack fn = this.acks.remove(packet.id);
        fn.call(fromJsonArray(packet.data.getAsJsonArray()).toArray());
    }

    private void onconnect() {
        this.connected = true;
        this.disconnected = false;
        this.emit(EVENT_CONNECT);
        this.emitBuffered();
    }

    private void emitBuffered() {
        synchronized (this.buffer) {
            LinkedList<Object> data;
            while ((data = this.buffer.poll()) != null) {
                String event = (String)data.pollFirst();
                super.emit(event, data.toArray());
            }
        }
    }

    private void ondisconnect() {
        logger.fine(String.format("server disconnect (%s)", this.nsp));
        this.destroy();
        this.onclose("io server disconnect");
    }

    private void destroy() {
        logger.fine(String.format("destroying socket (%s)", this.nsp));

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

    private static JsonArray toJsonArray(List<Object> list) {
        JsonArray data = new JsonArray();
        for (Object v : list) {
            data.add(v instanceof JsonElement ? (JsonElement)v : gson.toJsonTree(v));
        }
        return data;
    }

    private static List<Object> fromJsonArray(JsonArray array) {
        List<Object> data = new ArrayList<Object>();
        for (JsonElement v : array) {
            data.add(v.isJsonPrimitive() || v.isJsonNull() ? gson.fromJson(v, Object.class) : v);
        }
        return data;
    }
}

