package io.socket.client;

import io.socket.emitter.Emitter;
import io.socket.parser.Packet;
import io.socket.parser.Parser;
import io.socket.thread.EventThread;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
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
    public static final String EVENT_CONNECT_ERROR = "connect_error";

    static final String EVENT_MESSAGE = "message";

    protected static Map<String, Integer> RESERVED_EVENTS = new HashMap<String, Integer>() {{
        put(EVENT_CONNECT, 1);
        put(EVENT_CONNECT_ERROR, 1);
        put(EVENT_DISCONNECT, 1);
        // used on the server-side
        put("disconnecting", 1);
        put("newListener", 1);
        put("removeListener", 1);
    }};

    /*package*/ String id;

    private volatile boolean connected;
    private int ids;
    private final String nsp;
    private final Manager io;
    private final JSONObject auth;

    private String _pid = null;
    private String _lastOffset = null;
    private boolean recovered = false;

    private final Map<Integer, Ack> acks = new ConcurrentHashMap<>();
    private Queue<On.Handle> subs;
    private final Queue<Packet<JSONArray>> receiveBuffer = new ConcurrentLinkedQueue<>();
    private final Queue<Packet<JSONArray>> sendBuffer = new ConcurrentLinkedQueue<>();

    private final ConcurrentLinkedQueue<Listener> onAnyIncomingListeners = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Listener> onAnyOutgoingListeners = new ConcurrentLinkedQueue<>();

    public Socket(Manager io, String nsp, Manager.Options opts) {
        this.io = io;
        this.nsp = nsp;
        this.auth = opts != null ? opts.auth : null;
    }

    private void subEvents() {
        if (this.subs != null) return;

        final Manager io = Socket.this.io;
        Socket.this.subs = new LinkedList<On.Handle>() {{
            add(On.on(io, Manager.EVENT_OPEN, new Listener() {
                @Override
                public void call(Object... args) {
                    Socket.this.onopen();
                }
            }));
            add(On.on(io, Manager.EVENT_PACKET, new Listener() {
                @Override
                public void call(Object... args) {
                    Socket.this.onpacket((Packet<?>) args[0]);
                }
            }));
            add(On.on(io, Manager.EVENT_ERROR, new Listener() {
                @Override
                public void call(Object... args) {
                    if (!Socket.this.connected) {
                        Socket.super.emit(EVENT_CONNECT_ERROR, args[0]);
                    }
                }
            }));
            add(On.on(io, Manager.EVENT_CLOSE, new Listener() {
                @Override
                public void call(Object... args) {
                    Socket.this.onclose(args.length > 0 ? (String) args[0] : null);
                }
            }));
        }};
    }

    public boolean isActive() {
        return this.subs != null;
    }

    /**
     * Connects the socket.
     */
    public Socket open() {
        EventThread.exec(new Runnable() {
            @Override
            public void run() {
                if (Socket.this.connected || Socket.this.io.isReconnecting()) return;

                Socket.this.subEvents();
                Socket.this.io.open(); // ensure open
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
     * @param args  data to send.
     * @return a reference to this object.
     */
    @Override
    public Emitter emit(final String event, final Object... args) {
        if (RESERVED_EVENTS.containsKey(event)) {
            throw new RuntimeException("'" + event + "' is a reserved event name");
        }

        EventThread.exec(new Runnable() {
            @Override
            public void run() {
                Ack ack;
                Object[] _args;
                int lastIndex = args.length - 1;

                if (args.length > 0 && args[lastIndex] instanceof Ack) {
                    _args = new Object[lastIndex];
                    for (int i = 0; i < lastIndex; i++) {
                        _args[i] = args[i];
                    }
                    ack = (Ack) args[lastIndex];
                } else {
                    _args = args;
                    ack = null;
                }

                Socket.this.emit(event, _args, ack);
            }
        });
        return this;
    }

    /**
     * Emits an event with an acknowledge.
     *
     * @param event an event name
     * @param args  data to send.
     * @param ack   the acknowledgement to be called
     * @return a reference to this object.
     */
    public Emitter emit(final String event, final Object[] args, final Ack ack) {
        EventThread.exec(new Runnable() {
            @Override
            public void run() {
                JSONArray jsonArgs = new JSONArray();
                jsonArgs.put(event);

                if (args != null) {
                    for (Object arg : args) {
                        jsonArgs.put(arg);
                    }
                }

                Packet<JSONArray> packet = new Packet<>(Parser.EVENT, jsonArgs);

                if (ack != null) {
                    final int ackId = Socket.this.ids;

                    logger.fine(String.format("emitting packet with ack id %d", ackId));

                    if (ack instanceof AckWithTimeout) {
                        final AckWithTimeout ackWithTimeout = (AckWithTimeout) ack;
                        ackWithTimeout.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                // remove the ack from the map (to prevent an actual acknowledgement)
                                acks.remove(ackId);

                                // remove the packet from the buffer (if applicable)
                                Iterator<Packet<JSONArray>> iterator = sendBuffer.iterator();
                                while (iterator.hasNext()) {
                                    if (iterator.next().id == ackId) {
                                        iterator.remove();
                                    }
                                }

                                ackWithTimeout.onTimeout();
                            }
                        });
                    }

                    Socket.this.acks.put(ackId, ack);
                    packet.id = ids++;
                }

                if (Socket.this.connected) {
                    Socket.this.packet(packet);
                } else {
                    Socket.this.sendBuffer.add(packet);
                }
            }
        });
        return this;
    }

    private void packet(Packet packet) {
        if (packet.type == Parser.EVENT) {
            if (!onAnyOutgoingListeners.isEmpty()) {
                Object[] argsAsArray = toArray((JSONArray) packet.data);
                for (Listener listener : onAnyOutgoingListeners) {
                    listener.call(argsAsArray);
                }
            }
        }
        packet.nsp = this.nsp;
        this.io.packet(packet);
    }

    private void onopen() {
        logger.fine("transport is open - connecting");

        JSONObject auth = this.auth;

        if (_pid != null && _lastOffset != null) {
            // Reconnection - add recovery data
            try {
                if (auth == null) {
                    auth = new JSONObject();
                }
                auth.put("pid", _pid);
                auth.put("offset", String.valueOf(_lastOffset));
            } catch (JSONException e) {
                logger.warning(e.getMessage());
            }
        }

        if (auth != null) {
            this.packet(new Packet<>(Parser.CONNECT, auth));
        } else {
            this.packet(new Packet<>(Parser.CONNECT));
        }
    }

    private void onclose(String reason) {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine(String.format("close (%s)", reason));
        }
        this.connected = false;
        this.id = null;
        super.emit(EVENT_DISCONNECT, reason);
        this.clearAcks();
    }

    /**
     * Clears the acknowledgement handlers upon disconnection, since the client will never receive an acknowledgement from
     * the server.
     */
    private void clearAcks() {
        for (Ack ack : this.acks.values()) {
            if (ack instanceof AckWithTimeout) {
                ((AckWithTimeout) ack).onTimeout();
            }
            // note: basic Ack objects have no way to report an error, so they are simply ignored here
        }
        this.acks.clear();
    }

    private void onpacket(Packet<?> packet) {
        if (!this.nsp.equals(packet.nsp)) return;

        switch (packet.type) {
            case Parser.CONNECT: {
                if (packet.data instanceof JSONObject) {
                    JSONObject data = (JSONObject) packet.data;
                    try {
                        if (data.has("pid") && data.has("sid")) {
                            this.onconnect(data.getString("sid"), data.getString("pid"));
                            return;
                        } else if (data.has("sid")) {
                            this.onconnect(data.getString("sid"));
                            return;
                        }
                    } catch (JSONException e) {
                        logger.warning(e.getMessage());
                    }
                } else {
                    super.emit(EVENT_CONNECT_ERROR, new SocketIOException("It seems you are trying to reach a Socket.IO server in v2.x with a v3.x client, which is not possible"));
                }
                break;
            }

            case Parser.EVENT:
            case Parser.BINARY_EVENT: {
                @SuppressWarnings("unchecked")
                Packet<JSONArray> p = (Packet<JSONArray>) packet;
                this.onevent(p);
                break;
            }

            case Parser.ACK:
            case Parser.BINARY_ACK: {
                @SuppressWarnings("unchecked")
                Packet<JSONArray> p = (Packet<JSONArray>) packet;
                this.onack(p);
                break;
            }

            case Parser.DISCONNECT:
                this.ondisconnect();
                break;

            case Parser.CONNECT_ERROR:
                this.destroy();
                super.emit(EVENT_CONNECT_ERROR, packet.data);
                break;
        }
    }

    private void onevent(Packet<JSONArray> packet) {
        List<Object> args = new ArrayList<>(Arrays.asList(toArray(packet.data)));
        if (logger.isLoggable(Level.FINE)) {
            logger.fine(String.format("emitting event %s", args));
        }

        if (packet.id >= 0) {
            logger.fine("attaching ack callback to event");
            args.add(this.ack(packet.id));
        }

        if (this.connected) {
            if (args.isEmpty()) return;
            if (!this.onAnyIncomingListeners.isEmpty()) {
                Object[] argsAsArray = args.toArray();
                for (Listener listener : this.onAnyIncomingListeners) {
                    listener.call(argsAsArray);
                }
            }
            String event = args.remove(0).toString();
            super.emit(event, args.toArray());
            // Update offset if present
            if (this._pid != null && args.get(args.size() - 1) instanceof String) {
                _lastOffset = (String) args.get(args.size() - 1);
            }
        } else {
            this.receiveBuffer.add(packet);
        }
    }

    private Ack ack(final int id) {
        final Socket self = this;
        final boolean[] sent = new boolean[] {false};
        return new Ack() {
            @Override
            public void call(final Object... args) {
                EventThread.exec(new Runnable() {
                    @Override
                    public void run() {
                        if (sent[0]) return;
                        sent[0] = true;
                        if (logger.isLoggable(Level.FINE)) {
                            logger.fine(String.format("sending ack %s", args.length != 0 ? args : null));
                        }

                        JSONArray jsonArgs = new JSONArray();
                        for (Object arg : args) {
                            jsonArgs.put(arg);
                        }

                        Packet<JSONArray> packet = new Packet<>(Parser.ACK, jsonArgs);
                        packet.id = id;
                        self.packet(packet);
                    }
                });
            }
        };
    }

    private void onack(Packet<JSONArray> packet) {
        Ack fn = this.acks.remove(packet.id);
        if (fn != null) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine(String.format("calling ack %s with %s", packet.id, packet.data));
            }
            fn.call(toArray(packet.data));
        } else {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine(String.format("bad ack %s", packet.id));
            }
        }
    }

    private void onconnect(String sid) {
        this.connected = true;
        this.id = sid;
        this.recovered = false;
        this.emitBuffered();
        super.emit(EVENT_CONNECT);
    }

    private void onconnect(String sid, String pid) {
        this.connected = true;
        this.id = sid;
        this.recovered = pid != null && pid.equals(_pid);
        this._pid = pid;
        this.emitBuffered();
        super.emit(EVENT_CONNECT);
    }

    private void emitBuffered() {
        Packet<JSONArray> data;
        while ((data = this.receiveBuffer.poll()) != null) {
            onevent(data);
        }
        this.receiveBuffer.clear();

        Packet<JSONArray> packet;
        while ((packet = this.sendBuffer.poll()) != null) {
            this.packet(packet);
        }
        this.sendBuffer.clear();
    }

    private void ondisconnect() {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine(String.format("server disconnect (%s)", this.nsp));
        }
        recovered = false;
        this.destroy();
        this.onclose("io server disconnect");
    }

    private void destroy() {
        if (this.subs != null) {
            // clean subscriptions to avoid reconnection
            for (On.Handle sub : this.subs) {
                sub.destroy();
            }
            this.subs = null;
        }

        this.io.destroy();
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
                if (Socket.this.connected) {
                    if (logger.isLoggable(Level.FINE)) {
                        logger.fine(String.format("performing disconnect (%s)", Socket.this.nsp));
                    }
                    Socket.this.packet(new Packet(Parser.DISCONNECT));
                }

                Socket.this.destroy();

                if (Socket.this.connected) {
                    Socket.this.onclose("io client disconnect");
                }
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

    public Manager io() {
        return this.io;
    }

    public boolean connected() {
        return this.connected;
    }

    /**
     * A property on the socket instance that is equal to the underlying engine.io socket id.
     * <p>
     * The value is present once the socket has connected, is removed when the socket disconnects and is updated if the socket reconnects.
     *
     * @return a socket id
     */
    public String id() {
        return this.id;
    }

    private static Object[] toArray(JSONArray array) {
        int length = array.length();
        Object[] data = new Object[length];
        for (int i = 0; i < length; i++) {
            Object v;
            try {
                v = array.get(i);
            } catch (JSONException e) {
                logger.log(Level.WARNING, "An error occured while retrieving data from JSONArray", e);
                v = null;
            }
            data[i] = JSONObject.NULL.equals(v) ? null : v;
        }
        return data;
    }

    public Socket onAnyIncoming(Listener fn) {
        this.onAnyIncomingListeners.add(fn);
        return this;
    }

    public Socket offAnyIncoming() {
        this.onAnyIncomingListeners.clear();
        return this;
    }

    public Socket offAnyIncoming(Listener fn) {
        Iterator<Listener> it = this.onAnyIncomingListeners.iterator();
        while (it.hasNext()) {
            Listener listener = it.next();
            if (listener == fn) {
                it.remove();
                break;
            }
        }
        return this;
    }

    public Socket onAnyOutgoing(Listener fn) {
        this.onAnyOutgoingListeners.add(fn);
        return this;
    }

    public Socket offAnyOutgoing() {
        this.onAnyOutgoingListeners.clear();
        return this;
    }

    public Socket offAnyOutgoing(Listener fn) {
        Iterator<Listener> it = this.onAnyOutgoingListeners.iterator();
        while (it.hasNext()) {
            Listener listener = it.next();
            if (listener == fn) {
                it.remove();
                break;
            }
        }
        return this;
    }

    public String getLastOffset() {
        return this._lastOffset;
    }

    public boolean isRecovered() {
        return recovered;
    }
}

