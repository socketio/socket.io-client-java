package com.github.nkzawa.socketio.client;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.engineio.client.EventThread;
import com.github.nkzawa.socketio.parser.Packet;
import com.github.nkzawa.socketio.parser.Parser;

import java.net.URI;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.*;
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

    /*package*/ ReadyState readyState = ReadyState.CLOSED;

    private boolean _reconnection;
    private boolean skipReconnect;
    private boolean reconnecting;
    private int _reconnectionAttempts;
    private long _reconnectionDelay;
    private long _reconnectionDelayMax;
    private long _timeout;
    private int connected;
    private int attempts;
    private Queue<On.Handle> subs = new LinkedList<On.Handle>();
    private com.github.nkzawa.engineio.client.Socket engine;

    /**
     * This HashMap can be accessed from outside of EventThread.
     */
    private ConcurrentHashMap<String, Socket> nsps = new ConcurrentHashMap<String, Socket>();

    private ScheduledExecutorService timeoutScheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledExecutorService reconnectScheduler = Executors.newSingleThreadScheduledExecutor();


    public Manager(URI uri, IO.Options opts) {
        opts = initOptions(opts);
        this.engine = new Engine(uri, opts);
    }

    public Manager(com.github.nkzawa.engineio.client.Socket socket, IO.Options opts) {
        opts = initOptions(opts);
        this.engine = socket;
    }

    private IO.Options initOptions(IO.Options opts) {
        if (opts == null) {
            opts = new IO.Options();
        }
        if (opts.path == null) {
            opts.path = "/socket.io";
        }
        this.reconnection(opts.reconnection);
        this.reconnectionAttempts(opts.reconnectionAttempts != 0 ? opts.reconnectionAttempts : Integer.MAX_VALUE);
        this.reconnectionDelay(opts.reconnectionDelay != 0 ? opts.reconnectionDelay : 1000);
        this.reconnectionDelayMax(opts.reconnectionDelayMax != 0 ? opts.reconnectionDelayMax : 5000);
        this.timeout(opts.timeout < 0 ? 10000 : opts.timeout);
        return opts;
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

    public long reconnectionDelay() {
        return this._reconnectionDelay;
    }

    public Manager reconnectionDelay(long v) {
        this._reconnectionDelay = v;
        return this;
    }

    public long reconnectionDelayMax() {
        return this._reconnectionDelayMax;
    }

    public Manager reconnectionDelayMax(long v) {
        this._reconnectionDelayMax = v;
        return this;
    }

    public long timeout() {
        return this._timeout;
    }

    public Manager timeout(long v) {
        this._timeout = v;
        return this;
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
                if (Manager.this.readyState == ReadyState.OPEN) return;

                final com.github.nkzawa.engineio.client.Socket socket = Manager.this.engine;
                final Manager self = Manager.this;

                Manager.this.readyState = ReadyState.OPENING;

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
                        self.cleanup();
                        self.emit(EVENT_CONNECT_ERROR, data);
                        if (fn != null) {
                            Exception err = new SocketIOException("Connection error",
                                    data instanceof Exception ? (Exception)data : null);
                            fn.call(err);
                        }
                    }
                });

                if (Manager.this._timeout >= 0) {
                    final long timeout = Manager.this._timeout;
                    logger.fine(String.format("connection attempt will timeout after %d", timeout));

                    final Future timer = timeoutScheduler.schedule(new Runnable() {
                        @Override
                        public void run() {
                            EventThread.exec(new Runnable() {
                                @Override
                                public void run() {
                                    logger.fine(String.format("connect attempt timed out after %d", timeout));
                                    openSub.destroy();
                                    socket.close();
                                    socket.emit(Engine.EVENT_ERROR, new SocketIOException("timeout"));
                                    self.emit(EVENT_CONNECT_TIMEOUT, timeout);
                                }
                            });
                        }
                    }, timeout, TimeUnit.MILLISECONDS);

                    On.Handle timeSub = new On.Handle() {
                        @Override
                        public void destroy() {
                            timer.cancel(false);
                        }
                    };

                    Manager.this.subs.add(timeSub);
                }

                Manager.this.subs.add(openSub);
                Manager.this.subs.add(errorSub);

                Manager.this.engine.open();
            }
        });
        return this;
    }

    private void onopen() {
        this.cleanup();

        this.readyState = ReadyState.OPEN;
        this.emit(EVENT_OPEN);

        final com.github.nkzawa.engineio.client.Socket socket = this.engine;
        this.subs.add(On.on(socket, Engine.EVENT_DATA, new Listener() {
            @Override
            public void call(Object... objects) {
                Manager.this.ondata((String)objects[0]);
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
                Manager.this.onclose();
            }
        }));
    }

    private void ondata(String data) {
        this.emit(EVENT_PACKET, Parser.decode(data));
    }

    private void onerror(Exception err) {
        this.emit(EVENT_ERROR, err);
    }

    /**
     * Initializes {@link Socket} instances for each namespaces.
     *
     * @param nsp namespace.
     * @return a socket instance for the namespace.
     */
    public Socket socket(String nsp) {
        Socket socket = this.nsps.get(nsp);
        if (socket == null) {
            socket = new Socket(this, nsp);
            Socket _socket = this.nsps.putIfAbsent(nsp, socket);
            if (_socket != null) {
                socket = _socket;
            } else {
                final Manager self = this;
                socket.on(Socket.EVENT_CONNECT, new Listener() {
                    @Override
                    public void call(Object... objects) {
                        self.connected++;
                    }
                });
            }
        }
        return socket;
    }

    /*package*/ void destroy(Socket socket) {
        this.connected--;
        if (connected == 0) {
            this.close();
        }
    }

    /*package*/ void packet(Packet packet) {
        logger.fine(String.format("writing packet %s", packet));
        this.engine.write(Parser.encode(packet));
    }

    private void cleanup() {
        On.Handle sub;
        while ((sub = this.subs.poll()) != null) sub.destroy();
    }

    private void close() {
        this.skipReconnect = true;
        this.cleanup();
        this.readyState = ReadyState.CLOSED;
        this.engine.close();
    }

    private void onclose() {
        this.cleanup();
        this.readyState = ReadyState.CLOSED;
        if (this._reconnection && !this.skipReconnect) {
            this.reconnect();
        }
    }

    private void reconnect() {
        final Manager self = this;
        this.attempts++;

        if (attempts > this._reconnectionAttempts) {
            this.emit(EVENT_RECONNECT_FAILED);
            this.reconnecting = false;
        } else {
            long delay = this.attempts * this.reconnectionDelay();
            delay = Math.min(delay, this.reconnectionDelayMax());
            logger.fine(String.format("will wait %dms before reconnect attempt", delay));

            this.reconnecting = true;
            final Future timer = this.reconnectScheduler.schedule(new Runnable() {
                @Override
                public void run() {
                    EventThread.exec(new Runnable() {
                        @Override
                        public void run() {
                            logger.fine("attempting reconnect");
                            self.open(new OpenCallback() {
                                @Override
                                public void call(Exception err) {
                                    if (err != null) {
                                        logger.fine("reconnect attempt error");
                                        self.reconnect();
                                        self.emit(EVENT_RECONNECT_ERROR, err);
                                    } else {
                                        logger.fine("reconnect success");
                                        self.onreconnect();
                                    }
                                }
                            });
                        }
                    });
                }
            }, delay, TimeUnit.MILLISECONDS);

            this.subs.add(new On.Handle() {
                @Override
                public void destroy() {
                    timer.cancel(false);
                }
            });
        }
    }

    private void onreconnect() {
        int attempts = this.attempts;
        this.attempts = 0;
        this.reconnecting = false;
        this.emit(EVENT_RECONNECT, attempts);
    }


    public static interface OpenCallback {

        public void call(Exception err);
    }
}
