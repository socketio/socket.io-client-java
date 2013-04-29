package com.github.nkzawa.socketio.client;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.parser.Packet;
import com.github.nkzawa.socketio.parser.Parser;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class Socket extends Emitter {

    private static final Logger logger = Logger.getLogger("socket.io-client:socket");

    private static final Gson gson = new Gson();

    public static final String EVENT_CONNECT = "connect";
    public static final String EVENT_DISCONNECT = "disconnect";
    public static final String EVENT_MESSAGE = "message";
    public static final String EVENT_ERROR = "error";

    private static List<String> events = new ArrayList<String>() {{
        add(EVENT_CONNECT);
        add(EVENT_DISCONNECT);
        add(EVENT_ERROR);
    }};

    private boolean connected;
    private boolean disconnected = true;
    private AtomicInteger ids = new AtomicInteger();
    private String nsp;
    private Manager io;
    private Map<Integer, Ack> acks = new ConcurrentHashMap<Integer, Ack>();
    private Queue<On.Handle> subs;
    private final Queue<LinkedList<Object>> buffer = new ConcurrentLinkedQueue<LinkedList<Object>>();

    public Socket(Manager io, String nsp) {
        this.io = io;
        this.nsp = nsp;
    }

    public void open() {
        final Manager io = this.io;
        this.subs = new ConcurrentLinkedQueue<On.Handle>() {{
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
        if (this.io.readyState == Manager.OPEN) this.onopen();
        io.open();
    }

    public void connect() {
        this.open();
    }

    public Socket send(Object... args) {
        this.emit(EVENT_MESSAGE, args);
        return this;
    }

    @Override
    public Emitter emit(String event, Object... args) {
        if (events.contains(event)) {
            super.emit(event, args);
        } else {
            LinkedList<Object> _args = new LinkedList<Object>(Arrays.asList(args));
            if (_args.peekLast() instanceof Ack) {
                Ack ack = (Ack)_args.pollLast();
                return this.emit(event, _args.toArray(), ack);
            }

            _args.offerFirst(event);
            Packet packet = new Packet(Parser.EVENT, toJsonArray(_args));
            this.packet(packet);
        }

        return this;
    }

    /**
     * emit with an ack callback
     *
     * @param event
     * @param args
     * @param ack
     * @return
     */
    public Emitter emit(final String event, final Object[] args, Ack ack) {
        List<Object> _args = new ArrayList<Object>() {{
            add(event);
            addAll(Arrays.asList(args));
        }};
        Packet packet = new Packet(Parser.EVENT, toJsonArray(_args));

        int ids = this.ids.getAndIncrement();
        logger.info(String.format("emitting packet with ack id %d", ids));
        this.acks.put(ids, ack);
        packet.id = ids;

        this.packet(packet);

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
        logger.info("transport is open - connecting");

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
        logger.info(String.format("close (%s)", reason));
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
        logger.info(String.format("emitting event %s", args));

        if (packet.id >= 0) {
            logger.info("attaching ack callback to event");
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
                logger.info(String.format("sending ack %s", args));
                Packet packet = new Packet(Parser.ACK, gson.toJsonTree(args));
                packet.id = id;
                self.packet(packet);
            }
        };
    }

    private void onack(Packet packet) {
        logger.info(String.format("calling ack %s with %s", packet.id, packet.data));
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
        logger.info(String.format("server disconnect (%s)", this.nsp));
        this.destroy();
        this.onclose("io server disconnect");
    }

    private void destroy() {
        logger.info(String.format("destroying socket (%s)", this.nsp));

        for (On.Handle sub : this.subs) {
            sub.destroy();
        }

        this.io.destroy(this);
    }

    public Socket close() {
        if (!this.connected) return this;

        logger.info(String.format("performing disconnect (%s)", this.nsp));
        this.packet(new Packet(Parser.DISCONNECT));

        this.destroy();

        this.onclose("io client disconnect");
        return this;
    }

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
            data.add(v.isJsonPrimitive() ? gson.fromJson(v, Object.class) : v);
        }
        return data;
    }


    public static interface Ack {

        public void call(Object... args);
    }
}

