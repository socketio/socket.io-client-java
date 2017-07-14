package io.socket.client;

import io.socket.backo.Backoff;
import io.socket.emitter.Emitter;
import io.socket.parser.IOParser;
import io.socket.parser.Packet;
import io.socket.parser.Parser;
import io.socket.thread.EventThread;
import okhttp3.Call;
import okhttp3.WebSocket;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manager class represents a connection to a given Socket.IO server.
 */
public class Manager extends Emitter {

    private static final Logger logger = Logger.getLogger(Manager.class.getName());

    /*package*/ enum ReadyState {
        CLOSED, OPENING, OPEN
    }

    /**
     * Called on a successful connection.
     */
    public static final String EVENT_OPEN = "open";

    /**
     * Called on a disconnection.
     */
    public static final String EVENT_CLOSE = "close";

    public static final String EVENT_PACKET = "packet";
    public static final String EVENT_ERROR = "error";

    /**
     * Called on a connection error.
     */
    public static final String EVENT_CONNECT_ERROR = "connect_error";

    /**
     * Called on a connection timeout.
     */
    public static final String EVENT_CONNECT_TIMEOUT = "connect_timeout";

    /**
     * Called on a successful reconnection.
     */
    public static final String EVENT_RECONNECT = "reconnect";

    /**
     * Called on a reconnection attempt error.
     */
    public static final String EVENT_RECONNECT_ERROR = "reconnect_error";

    public static final String EVENT_RECONNECT_FAILED = "reconnect_failed";

    public static final String EVENT_RECONNECT_ATTEMPT = "reconnect_attempt";

    public static final String EVENT_RECONNECTING = "reconnecting";

    public static final String EVENT_PING = "ping";

    public static final String EVENT_PONG = "pong";

    /**
     * Called when a new transport is created. (experimental)
     */
    public static final String EVENT_TRANSPORT = Engine.EVENT_TRANSPORT;

    /*package*/ static WebSocket.Factory defaultWebSocketFactory;
    /*package*/ static Call.Factory defaultCallFactory;

    /*package*/ ReadyState readyState;

    private boolean _reconnection;
    private boolean skipReconnect;
    private boolean reconnecting;
    private boolean encoding;
    private int _reconnectionAttempts;
    private long _reconnectionDelay;
    private long _reconnectionDelayMax;
    private double _randomizationFactor;
    private Backoff backoff;
    private long _timeout;
    private Set<Socket> connecting = new HashSet<Socket>();
    private Date lastPing;
    private URI uri;
    private List<Packet> packetBuffer;
    private Queue<On.Handle> subs;
    private Options opts;
    /*package*/ io.socket.engineio.client.Socket engine;
    private Parser.Encoder encoder;
    private Parser.Decoder decoder;

    /**
     * This HashMap can be accessed from outside of EventThread.
     */
    /*package*/ ConcurrentHashMap<String, Socket> nsps;


    public Manager() {
        this(null, null);
    }

    public Manager(URI uri) {
        this(uri, null);
    }

    public Manager(Options opts) {
        this(null, opts);
    }

    public Manager(URI uri, Options opts) {
        if (opts == null) {
            opts = new Options();
        }
        if (opts.path == null) {
            opts.path = "/socket.io";
        }
        if (opts.webSocketFactory == null) {
            opts.webSocketFactory = defaultWebSocketFactory;
        }
        if (opts.callFactory == null) {
            opts.callFactory = defaultCallFactory;
        }
        this.opts = opts;
        this.nsps = new ConcurrentHashMap<String, Socket>();
        this.subs = new LinkedList<On.Handle>();
        this.reconnection(opts.reconnection);
        this.reconnectionAttempts(opts.reconnectionAttempts != 0 ? opts.reconnectionAttempts : Integer.MAX_VALUE);
        this.reconnectionDelay(opts.reconnectionDelay != 0 ? opts.reconnectionDelay : 1000);
        this.reconnectionDelayMax(opts.reconnectionDelayMax != 0 ? opts.reconnectionDelayMax : 5000);
        this.randomizationFactor(opts.randomizationFactor != 0.0 ? opts.randomizationFactor : 0.5);
        this.backoff = new Backoff()
                .setMin(this.reconnectionDelay())
                .setMax(this.reconnectionDelayMax())
                .setJitter(this.randomizationFactor());
        this.timeout(opts.timeout);
        this.readyState = ReadyState.CLOSED;
        this.uri = uri;
        this.encoding = false;
        this.packetBuffer = new ArrayList<Packet>();
        this.encoder = opts.encoder != null ? opts.encoder : new IOParser.Encoder();
        this.decoder = opts.decoder != null ? opts.decoder : new IOParser.Decoder();
    }

    private void emitAll(String event, Object... args) {
        this.emit(event, args);
        for (Socket socket : this.nsps.values()) {
            socket.emit(event, args);
        }
    }

    /**
     * Update `socket.id` of all sockets
     */
    private void updateSocketIds() {
        for (Map.Entry<String, Socket> entry : this.nsps.entrySet()) {
            String nsp = entry.getKey();
            Socket socket = entry.getValue();
            socket.id = this.generateId(nsp);
        }
    }

    private String generateId(String nsp) {
        return ("/".equals(nsp) ? "" : (nsp + "#")) + this.engine.id();
    }

    public boolean reconnection() {
        return this._reconnection;
    }

    public Manager reconnection(boolean v) {
        this._reconnection = v;
        return this;
    }

    public int reconnectionAttempts() {
        return this._reconnectionAttempts;
    }

    public Manager reconnectionAttempts(int v) {
        this._reconnectionAttempts = v;
        return this;
    }

    public final long reconnectionDelay() {
        return this._reconnectionDelay;
    }

    public Manager reconnectionDelay(long v) {
        this._reconnectionDelay = v;
        if (this.backoff != null) {
            this.backoff.setMin(v);
        }
        return this;
    }

    public final double randomizationFactor() {
        return this._randomizationFactor;
    }

    public Manager randomizationFactor(double v) {
        this._randomizationFactor = v;
        if (this.backoff != null) {
            this.backoff.setJitter(v);
        }
        return this;
    }

    public final long reconnectionDelayMax() {
        return this._reconnectionDelayMax;
    }

    public Manager reconnectionDelayMax(long v) {
        this._reconnectionDelayMax = v;
        if (this.backoff != null) {
            this.backoff.setMax(v);
        }
        return this;
    }

    public long timeout() {
        return this._timeout;
    }

    public Manager timeout(long v) {
        this._timeout = v;
        return this;
    }

    private void maybeReconnectOnOpen() {
        // Only try to reconnect if it's the first time we're connecting
        if (!this.reconnecting && this._reconnection && this.backoff.getAttempts() == 0) {
            this.reconnect();
        }
    }

    public Manager open(){
        return open(null);
    }

    /**
     * Connects the client.
     *
     * @param fn callback.
     * @return a reference to this object.
     */
    public Manager open(final OpenCallback fn) {
        EventThread.exec(new Runnable() {
            @Override
            public void run() {
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine(String.format("readyState %s", Manager.this.readyState));
                }
                if (Manager.this.readyState == ReadyState.OPEN || Manager.this.readyState == ReadyState.OPENING) return;

                if (logger.isLoggable(Level.FINE)) {
                    logger.fine(String.format("opening %s", Manager.this.uri));
                }
                Manager.this.engine = new Engine(Manager.this.uri, Manager.this.opts);
                final io.socket.engineio.client.Socket socket = Manager.this.engine;
                final Manager self = Manager.this;
                Manager.this.readyState = ReadyState.OPENING;
                Manager.this.skipReconnect = false;

                // propagate transport event.
                socket.on(Engine.EVENT_TRANSPORT, new Listener() {
                    @Override
                    public void call(Object... args) {
                        self.emit(Manager.EVENT_TRANSPORT, args);
                    }
                });

                final On.Handle openSub = On.on(socket, Engine.EVENT_OPEN, new Listener() {
                    @Override
                    public void call(Object... objects) {
                        self.onopen();
                        if (fn != null) fn.call(null);
                    }
                });

                On.Handle errorSub = On.on(socket, Engine.EVENT_ERROR, new Listener() {
                    @Override
                    public void call(Object... objects) {
                        Object data = objects.length > 0 ? objects[0] : null;
                        logger.fine("connect_error");
                        self.cleanup();
                        self.readyState = ReadyState.CLOSED;
                        self.emitAll(EVENT_CONNECT_ERROR, data);
                        if (fn != null) {
                            Exception err = new SocketIOException("Connection error",
                                    data instanceof Exception ? (Exception) data : null);
                            fn.call(err);
                        } else {
                            // Only do this if there is no fn to handle the error
                            self.maybeReconnectOnOpen();
                        }
                    }
                });

                if (Manager.this._timeout >= 0) {
                    final long timeout = Manager.this._timeout;
                    logger.fine(String.format("connection attempt will timeout after %d", timeout));

                    final Timer timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            EventThread.exec(new Runnable() {
                                @Override
                                public void run() {
                                    logger.fine(String.format("connect attempt timed out after %d", timeout));
                                    openSub.destroy();
                                    socket.close();
                                    socket.emit(Engine.EVENT_ERROR, new SocketIOException("timeout"));
                                    self.emitAll(EVENT_CONNECT_TIMEOUT, timeout);
                                }
                            });
                        }
                    }, timeout);

                    Manager.this.subs.add(new On.Handle() {
                        @Override
                        public void destroy() {
                            timer.cancel();
                        }
                    });
                }

                Manager.this.subs.add(openSub);
                Manager.this.subs.add(errorSub);

                Manager.this.engine.open();
            }
        });
        return this;
    }

    private void onopen() {
        logger.fine("open");

        this.cleanup();

        this.readyState = ReadyState.OPEN;
        this.emit(EVENT_OPEN);

        final io.socket.engineio.client.Socket socket = this.engine;
        this.subs.add(On.on(socket, Engine.EVENT_DATA, new Listener() {
            @Override
            public void call(Object... objects) {
                Object data = objects[0];
                if (data instanceof String) {
                    Manager.this.ondata((String)data);
                } else if (data instanceof byte[]) {
                    Manager.this.ondata((byte[])data);
                }
            }
        }));
        this.subs.add(On.on(socket, Engine.EVENT_PING, new Listener() {
            @Override
            public void call(Object... objects) {
                Manager.this.onping();
            }
        }));
        this.subs.add(On.on(socket, Engine.EVENT_PONG, new Listener() {
            @Override
            public void call(Object... objects) {
                Manager.this.onpong();
            }
        }));
        this.subs.add(On.on(socket, Engine.EVENT_ERROR, new Listener() {
            @Override
            public void call(Object... objects) {
                Manager.this.onerror((Exception)objects[0]);
            }
        }));
        this.subs.add(On.on(socket, Engine.EVENT_CLOSE, new Listener() {
            @Override
            public void call(Object... objects) {
                Manager.this.onclose((String)objects[0]);
            }
        }));
        this.decoder.onDecoded(new Parser.Decoder.Callback() {
            @Override
            public void call (Packet packet) {
                Manager.this.ondecoded(packet);
            }
        });
    }

    private void onping() {
        this.lastPing = new Date();
        this.emitAll(EVENT_PING);
    }

    private void onpong() {
        this.emitAll(EVENT_PONG,
                null != this.lastPing ? new Date().getTime() - this.lastPing.getTime() : 0);
    }

    private void ondata(String data) {
        this.decoder.add(data);
    }

    private void ondata(byte[] data) {
        this.decoder.add(data);
    }

    private void ondecoded(Packet packet) {
        this.emit(EVENT_PACKET, packet);
    }

    private void onerror(Exception err) {
        logger.log(Level.FINE, "error", err);
        this.emitAll(EVENT_ERROR, err);
    }

    /**
     * Initializes {@link Socket} instances for each namespaces.
     *
     * @param nsp namespace.
     * @param opts options.
     * @return a socket instance for the namespace.
     */
    public Socket socket(final String nsp, Options opts) {
        Socket socket = this.nsps.get(nsp);
        if (socket == null) {
            socket = new Socket(this, nsp, opts);
            Socket _socket = this.nsps.putIfAbsent(nsp, socket);
            if (_socket != null) {
                socket = _socket;
            } else {
                final Manager self = this;
                final Socket s = socket;
                socket.on(Socket.EVENT_CONNECTING, new Listener() {
                    @Override
                    public void call(Object... args) {
                        self.connecting.add(s);
                    }
                });
                socket.on(Socket.EVENT_CONNECT, new Listener() {
                    @Override
                    public void call(Object... objects) {
                        s.id = self.generateId(nsp);
                    }
                });
            }
        }
        return socket;
    }

    public Socket socket(String nsp) {
        return socket(nsp, null);
    }

    /*package*/ void destroy(Socket socket) {
        this.connecting.remove(socket);
        if (!this.connecting.isEmpty()) return;

        this.close();
    }

    /*package*/ void packet(Packet packet) {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine(String.format("writing packet %s", packet));
        }
        final Manager self = this;

        if (packet.query != null && !packet.query.isEmpty() && packet.type == Parser.CONNECT) {
            packet.nsp += "?" + packet.query;
        }

        if (!self.encoding) {
            self.encoding = true;
            this.encoder.encode(packet, new Parser.Encoder.Callback() {
                @Override
                public void call(Object[] encodedPackets) {
                    for (Object packet : encodedPackets) {
                        if (packet instanceof String) {
                            self.engine.write((String)packet);
                        } else if (packet instanceof byte[]) {
                            self.engine.write((byte[])packet);
                        }
                    }
                    self.encoding = false;
                    self.processPacketQueue();
                }
            });
        } else {
            self.packetBuffer.add(packet);
        }
    }

    private void processPacketQueue() {
        if (!this.packetBuffer.isEmpty() && !this.encoding) {
            Packet pack = this.packetBuffer.remove(0);
            this.packet(pack);
        }
    }

    private void cleanup() {
        logger.fine("cleanup");

        On.Handle sub;
        while ((sub = this.subs.poll()) != null) sub.destroy();
        this.decoder.onDecoded(null);

        this.packetBuffer.clear();
        this.encoding = false;
        this.lastPing = null;

        this.decoder.destroy();
    }

    /*package*/ void close() {
        logger.fine("disconnect");
        this.skipReconnect = true;
        this.reconnecting = false;
        if (this.readyState != ReadyState.OPEN) {
            // `onclose` will not fire because
            // an open event never happened
            this.cleanup();
        }
        this.backoff.reset();
        this.readyState = ReadyState.CLOSED;
        if (this.engine != null) {
            this.engine.close();
        }
    }

    private void onclose(String reason) {
        logger.fine("onclose");
        this.cleanup();
        this.backoff.reset();
        this.readyState = ReadyState.CLOSED;
        this.emit(EVENT_CLOSE, reason);

        if (this._reconnection && !this.skipReconnect) {
            this.reconnect();
        }
    }

    private void reconnect() {
        if (this.reconnecting || this.skipReconnect) return;

        final Manager self = this;

        if (this.backoff.getAttempts() >= this._reconnectionAttempts) {
            logger.fine("reconnect failed");
            this.backoff.reset();
            this.emitAll(EVENT_RECONNECT_FAILED);
            this.reconnecting = false;
        } else {
            long delay = this.backoff.duration();
            logger.fine(String.format("will wait %dms before reconnect attempt", delay));

            this.reconnecting = true;
            final Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    EventThread.exec(new Runnable() {
                        @Override
                        public void run() {
                            if (self.skipReconnect) return;

                            logger.fine("attempting reconnect");
                            int attempts = self.backoff.getAttempts();
                            self.emitAll(EVENT_RECONNECT_ATTEMPT, attempts);
                            self.emitAll(EVENT_RECONNECTING, attempts);

                            // check again for the case socket closed in above events
                            if (self.skipReconnect) return;

                            self.open(new OpenCallback() {
                                @Override
                                public void call(Exception err) {
                                    if (err != null) {
                                        logger.fine("reconnect attempt error");
                                        self.reconnecting = false;
                                        self.reconnect();
                                        self.emitAll(EVENT_RECONNECT_ERROR, err);
                                    } else {
                                        logger.fine("reconnect success");
                                        self.onreconnect();
                                    }
                                }
                            });
                        }
                    });
                }
            }, delay);

            this.subs.add(new On.Handle() {
                @Override
                public void destroy() {
                    timer.cancel();
                }
            });
        }
    }

    private void onreconnect() {
        int attempts = this.backoff.getAttempts();
        this.reconnecting = false;
        this.backoff.reset();
        this.updateSocketIds();
        this.emitAll(EVENT_RECONNECT, attempts);
    }


    public static interface OpenCallback {

        public void call(Exception err);
    }


    private static class Engine extends io.socket.engineio.client.Socket {

        Engine(URI uri, Options opts) {
            super(uri, opts);
        }
    }

    public static class Options extends io.socket.engineio.client.Socket.Options {

        public boolean reconnection = true;
        public int reconnectionAttempts;
        public long reconnectionDelay;
        public long reconnectionDelayMax;
        public double randomizationFactor;
        public Parser.Encoder encoder;
        public Parser.Decoder decoder;

        /**
         * Connection timeout (ms). Set -1 to disable.
         */
        public long timeout = 20000;
    }
}
