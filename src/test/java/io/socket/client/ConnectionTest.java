package io.socket.client;

import io.socket.emitter.Emitter;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class ConnectionTest extends Connection {

    private Socket socket;

    @Test(timeout = TIMEOUT)
    public void connectToLocalhost() throws InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<>();
        socket = client();
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... objects) {
                socket.emit("echo");
                socket.on("echoBack", new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        values.offer("done");
                    }
                });
            }
        });
        socket.connect();
        values.take();
        socket.close();
    }

    @Test(timeout = TIMEOUT)
    public void startTwoConnectionsWithSamePath() throws InterruptedException {
        Socket s1 = client("/");
        Socket s2 = client("/");

        assertThat(s1.io(), not(equalTo(s2.io())));
        s1.close();
        s2.close();
    }

    @Test(timeout = TIMEOUT)
    public void startTwoConnectionsWithSamePathAndDifferentQuerystrings() throws InterruptedException {
        Socket s1 = client("/?woot");
        Socket s2 = client("/");

        assertThat(s1.io(), not(equalTo(s2.io())));
        s1.close();
        s2.close();
    }

    @Test(timeout = TIMEOUT)
    public void workWithAcks() throws InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<>();
        socket = client();
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... objects) {
                socket.on("ack", new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        Ack fn = (Ack) args[0];
                        JSONObject data = new JSONObject();
                        try {
                            data.put("test", true);
                        } catch (JSONException e) {
                            throw new AssertionError(e);
                        }
                        fn.call(5, data);
                    }
                });
                socket.on("ackBack", new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        JSONObject data = (JSONObject)args[1];
                        try {
                            if ((Integer)args[0] == 5 && data.getBoolean("test")) {
                                values.offer("done");
                            }
                        } catch (JSONException e) {
                            throw new AssertionError(e);
                        }
                    }
                });
                socket.emit("callAck");
            }
        });
        socket.connect();
        values.take();
        socket.close();
    }

    @Test(timeout = TIMEOUT)
    public void receiveDateWithAck() throws InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<>();

        socket = client();
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... objects) {
                try {
                    socket.emit("getAckDate", new JSONObject("{test: true}"), new Ack() {
                        @Override
                        public void call(Object... args) {
                            values.offer(args[0]);
                        }
                    });
                } catch (JSONException e) {
                    throw new AssertionError(e);
                }
            }
        });
        socket.connect();
        assertThat(values.take(), instanceOf(String.class));
        socket.close();
    }

    @Test(timeout = TIMEOUT)
    public void sendBinaryAck() throws InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<>();
        final byte[] buf = "huehue".getBytes(Charset.forName("UTF-8"));

        socket = client();
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... objects) {
                socket.emit("callAckBinary");
                socket.on("ack", new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        Ack fn = (Ack) args[0];
                        fn.call(buf);
                    }
                });

                socket.on("ackBack", new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        byte[] data = (byte[]) args[0];
                        values.offer(data);
                    }
                });
            }
        });
        socket.connect();
        Assert.assertArrayEquals(buf, (byte[]) values.take());
        socket.close();
    }

    @Test(timeout = TIMEOUT)
    public void receiveBinaryDataWithAck() throws InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<>();
        final byte[] buf = "huehue".getBytes(Charset.forName("UTF-8"));

        socket = client();
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... objects) {
                socket.emit("getAckBinary", "", new Ack() {

                    @Override
                    public void call(Object... args) {
                        values.offer(args[0]);
                    }
                });
            }
        });
        socket.connect();
        Assert.assertArrayEquals(buf, (byte[]) values.take());
        socket.close();
    }

    @Test(timeout = TIMEOUT)
    public void workWithFalse() throws InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<>();
        socket = client();
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... objects) {
                socket.emit("echo", false);
                socket.on("echoBack", new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        values.offer(args[0]);
                    }
                });
            }
        });
        socket.connect();
        assertThat((Boolean) values.take(), is(false));
        socket.close();
    }

    @Test(timeout = TIMEOUT)
    public void receiveUTF8MultibyteCharacters() throws InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<>();
        final String[] correct = new String[]{
                "てすと",
                "Я Б Г Д Ж Й",
                "Ä ä Ü ü ß",
                "utf8 — string",
                "utf8 — string"
        };

        socket = client();
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... objects) {
                socket.on("echoBack", new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        values.offer(args[0]);
                    }
                });
                for (String data : correct) {
                    socket.emit("echo", data);
                }
            }
        });
        socket.connect();
        for (String expected : correct) {
            assertThat((String) values.take(), is(expected));
        }
        socket.close();
    }

    @Test(timeout = TIMEOUT)
    public void connectToNamespaceAfterConnectionEstablished() throws InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<>();
        final Manager manager = new Manager(uri());
        socket = manager.socket("/");
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... objects) {
                final Socket foo = manager.socket("/foo");
                foo.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        foo.close();
                        socket.close();
                        manager.close();
                        values.offer("done");
                    }
                });
                foo.open();
            }
        });
        socket.open();
        values.take();
    }

    @Test(timeout = TIMEOUT)
    public void connectToNamespaceAfterConnectionGetsClosed() throws InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<>();
        final Manager manager = new Manager(uri());
        socket = manager.socket("/");
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... objects) {
                socket.close();
            }
        }).on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... objects) {
                final Socket foo = manager.socket("/foo");
                foo.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        foo.close();
                        manager.close();
                        values.offer("done");
                    }
                });
                foo.open();
            }
        });
        socket.open();
        values.take();
    }

    @Test(timeout = TIMEOUT)
    public void reconnectByDefault() throws InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<>();
        socket = client();
        socket.io().on(Manager.EVENT_RECONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... objects) {
                socket.close();
                values.offer("done");
            }
        });
        socket.open();
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                socket.io().engine.close();
            }
        }, 500);
        values.take();
    }

    @Test(timeout = TIMEOUT)
    public void reconnectManually() throws InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<>();
        socket = client();
        socket.once(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                socket.disconnect();
            }
        }).once(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                socket.once(Socket.EVENT_CONNECT, new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        socket.disconnect();
                        values.offer("done");
                    }
                });
                socket.connect();
            }
        });
        socket.connect();
        values.take();
    }

    @Test(timeout = TIMEOUT)
    public void reconnectAutomaticallyAfterReconnectingManually() throws InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<>();
        socket = client();
        socket.once(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                socket.disconnect();
            }
        }).once(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                socket.io().on(Manager.EVENT_RECONNECT, new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        socket.disconnect();
                        values.offer("done");
                    }
                });
                socket.connect();
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        socket.io().engine.close();
                    }
                }, 500);
            }
        });
        socket.connect();
        values.take();
    }

    @Test(timeout = 14000)
    public void attemptReconnectsAfterAFailedReconnect() throws InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<>();
        IO.Options opts = createOptions();
        opts.reconnection = true;
        opts.timeout = 0;
        opts.reconnectionAttempts = 2;
        opts.reconnectionDelay = 10;
        final Manager manager = new Manager(uri(), opts);
        socket = manager.socket("/timeout");
        manager.once(Manager.EVENT_RECONNECT_FAILED, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                final int[] reconnects = new int[]{0};
                Emitter.Listener reconnectCb = new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        reconnects[0]++;
                    }
                };

                manager.on(Manager.EVENT_RECONNECT_ATTEMPT, reconnectCb);
                manager.on(Manager.EVENT_RECONNECT_FAILED, new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        values.offer(reconnects[0]);
                    }
                });
                socket.connect();
            }
        });
        socket.connect();
        assertThat((Integer) values.take(), is(2));
        socket.close();
        manager.close();
    }

    @Test(timeout = TIMEOUT)
    public void reconnectDelayShouldIncreaseEveryTime() throws InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<>();
        IO.Options opts = createOptions();
        opts.reconnection = true;
        opts.timeout = 0;
        opts.reconnectionAttempts = 3;
        opts.reconnectionDelay = 100;
        opts.randomizationFactor = 0.2;
        final Manager manager = new Manager(uri(), opts);
        socket = manager.socket("/timeout");

        final int[] reconnects = new int[]{0};
        final boolean[] increasingDelay = new boolean[]{true};
        final long[] startTime = new long[]{0};
        final long[] prevDelay = new long[]{0};

        manager.on(Manager.EVENT_ERROR, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                startTime[0] = new Date().getTime();
            }
        });
        manager.on(Manager.EVENT_RECONNECT_ATTEMPT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                reconnects[0]++;
                long currentTime = new Date().getTime();
                long delay = currentTime - startTime[0];
                if (delay <= prevDelay[0]) {
                    increasingDelay[0] = false;
                }
                prevDelay[0] = delay;
            }
        });
        manager.on(Manager.EVENT_RECONNECT_FAILED, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                values.offer(true);
            }
        });

        socket.connect();
        values.take();
        assertThat(reconnects[0], is(3));
        assertThat(increasingDelay[0], is(true));
        socket.close();
        manager.close();
    }

    @Test(timeout = TIMEOUT)
    public void notReconnectWhenForceClosed() throws URISyntaxException, InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<>();
        IO.Options opts = createOptions();
        opts.timeout = 0;
        opts.reconnectionDelay = 10;
        socket = IO.socket(uri() + "/invalid", opts);
        socket.io().on(Manager.EVENT_ERROR, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                socket.io().on(Manager.EVENT_RECONNECT_ATTEMPT, new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        values.offer(false);
                    }
                });
                socket.disconnect();
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        values.offer(true);
                    }
                }, 500);
            }
        });
        socket.connect();
        assertThat((Boolean) values.take(), is(true));
    }

    @Test(timeout = TIMEOUT)
    public void stopReconnectingWhenForceClosed() throws URISyntaxException, InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<>();
        IO.Options opts = createOptions();
        opts.timeout = 0;
        opts.reconnectionDelay = 10;
        socket = IO.socket(uri() + "/invalid", opts);
        socket.io().once(Manager.EVENT_RECONNECT_ATTEMPT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                socket.io().on(Manager.EVENT_RECONNECT_ATTEMPT, new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        values.offer(false);
                    }
                });
                socket.disconnect();
                // set a timer to let reconnection possibly fire
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        values.offer(true);
                    }
                }, 500);
            }
        });
        socket.connect();
        assertThat((Boolean) values.take(), is(true));
    }

    @Test(timeout = TIMEOUT)
    public void reconnectAfterStoppingReconnection() throws InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<>();
        IO.Options opts = createOptions();
        opts.forceNew = true;
        opts.timeout = 0;
        opts.reconnectionDelay = 10;
        socket = client("/invalid", opts);
        socket.io().once(Manager.EVENT_RECONNECT_ATTEMPT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                socket.io().once(Manager.EVENT_RECONNECT_ATTEMPT, new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        values.offer("done");
                    }
                });
                socket.disconnect();
                socket.connect();
            }
        });
        socket.connect();
        values.take();
        socket.disconnect();
    }

    @Test(timeout = TIMEOUT)
    public void stopReconnectingOnASocketAndKeepToReconnectOnAnother() throws InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<>();
        final Manager manager = new Manager(uri());
        final Socket socket1 = manager.socket("/");
        final Socket socket2 = manager.socket("/asd");

        manager.on(Manager.EVENT_RECONNECT_ATTEMPT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                socket1.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        values.offer(false);
                    }
                });
                socket2.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        new Timer().schedule(new TimerTask() {
                            @Override
                            public void run() {
                                socket2.disconnect();
                                manager.close();
                                values.offer(true);
                            }
                        }, 500);
                    }
                });
                socket1.disconnect();
            }
        });

        socket1.connect();
        socket2.connect();

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                manager.engine.close();
            }
        }, 1000);

        assertThat((Boolean) values.take(), is(true));
    }

    @Test(timeout = TIMEOUT)
    public void connectWhileDisconnectingAnotherSocket() throws InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<>();

        final Manager manager = new Manager(uri());
        final Socket socket1 = manager.socket("/foo");
        socket1.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                final Socket socket2 = manager.socket("/asd");
                socket2.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        values.offer("done");
                        socket2.disconnect();
                    }
                });
                socket2.open();
                socket1.disconnect();
            }
        });

        socket1.open();
        values.take();
        manager.close();
    }

    @Test(timeout = TIMEOUT)
    public void tryToReconnectTwiceAndFailWithIncorrectAddress() throws InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<>();
        IO.Options opts = new IO.Options();
        opts.reconnection = true;
        opts.reconnectionAttempts = 2;
        opts.reconnectionDelay = 10;
        final Manager manager = new Manager(URI.create("http://localhost:3940"), opts);
        socket = manager.socket("/asd");
        final int[] reconnects = new int[]{0};
        Emitter.Listener cb = new Emitter.Listener() {
            @Override
            public void call(Object... objects) {
                reconnects[0]++;
            }
        };

        manager.on(Manager.EVENT_RECONNECT_ATTEMPT, cb);

        manager.on(Manager.EVENT_RECONNECT_FAILED, new Emitter.Listener() {
            @Override
            public void call(Object... objects) {
                values.offer(reconnects[0]);
            }
        });

        socket.open();
        assertThat((Integer) values.take(), is(2));
        socket.close();
        manager.close();
    }

    @Test(timeout = TIMEOUT)
    public void tryToReconnectTwiceAndFailWithImmediateTimeout() throws InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<>();
        IO.Options opts = new IO.Options();
        opts.reconnection = true;
        opts.timeout = 0;
        opts.reconnectionAttempts = 2;
        opts.reconnectionDelay = 10;
        final Manager manager = new Manager(uri(), opts);

        final int[] reconnects = new int[]{0};
        Emitter.Listener reconnectCb = new Emitter.Listener() {
            @Override
            public void call(Object... objects) {
                reconnects[0]++;
            }
        };

        manager.on(Manager.EVENT_RECONNECT_ATTEMPT, reconnectCb);
        manager.on(Manager.EVENT_RECONNECT_FAILED, new Emitter.Listener() {
            @Override
            public void call(Object... objects) {
                socket.close();
                manager.close();
                values.offer(reconnects[0]);
            }
        });

        socket = manager.socket("/timeout");
        socket.open();
        assertThat((Integer) values.take(), is(2));
    }

    @Test(timeout = TIMEOUT)
    public void notTryToReconnectWithIncorrectPortWhenReconnectionDisabled() throws InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<>();
        IO.Options opts = new IO.Options();
        opts.reconnection = false;
        final Manager manager = new Manager(URI.create("http://localhost:9823"), opts);
        Emitter.Listener cb = new Emitter.Listener() {
            @Override
            public void call(Object... objects) {
                socket.close();
                throw new RuntimeException();
            }
        };
        manager.on(Manager.EVENT_RECONNECT_ATTEMPT, cb);
        manager.on(Manager.EVENT_ERROR, new Emitter.Listener() {
            @Override
            public void call(Object... objects) {
                Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        socket.close();
                        manager.close();
                        values.offer("done");
                    }
                }, 1000);
            }
        });

        socket = manager.socket("/invalid");
        socket.open();
        values.take();
    }

    @Test(timeout = TIMEOUT)
    public void fireReconnectEventsOnSocket() throws InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<>();

        Manager.Options opts = new Manager.Options();
        opts.reconnection = true;
        opts.timeout = 0;
        opts.reconnectionAttempts = 2;
        opts.reconnectionDelay = 10;
        final Manager manager = new Manager(uri(), opts);
        socket = manager.socket("/timeout_socket");

        final int[] reconnects = new int[]{0};
        Emitter.Listener reconnectCb = new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                reconnects[0]++;
                values.offer(args[0]);
            }
        };

        manager.on(Manager.EVENT_RECONNECT_ATTEMPT, reconnectCb);
        manager.on(Manager.EVENT_RECONNECT_FAILED, new Emitter.Listener() {
            @Override
            public void call(Object... objects) {
                socket.close();
                manager.close();
                values.offer(reconnects[0]);
            }
        });
        socket.open();
        assertThat((Integer) values.take(), is(reconnects[0]));
        assertThat((Integer) values.take(), is(2));
    }

    @Test(timeout = TIMEOUT)
    public void fireReconnectingWithAttemptsNumberWhenReconnectingTwice() throws InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<>();

        Manager.Options opts = new Manager.Options();
        opts.reconnection = true;
        opts.timeout = 0;
        opts.reconnectionAttempts = 2;
        opts.reconnectionDelay = 10;
        final Manager manager = new Manager(uri(), opts);
        socket = manager.socket("/timeout_socket");

        final int[] reconnects = new int[]{0};
        Emitter.Listener reconnectCb = new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                reconnects[0]++;
                values.offer(args[0]);
            }
        };

        manager.on(Manager.EVENT_RECONNECT_ATTEMPT, reconnectCb);
        manager.on(Manager.EVENT_RECONNECT_FAILED, new Emitter.Listener() {
            @Override
            public void call(Object... objects) {
                socket.close();
                manager.close();
                values.offer(reconnects[0]);
            }
        });
        socket.open();
        assertThat((Integer) values.take(), is(reconnects[0]));
        assertThat((Integer) values.take(), is(2));
    }

    @Test(timeout = TIMEOUT)
    public void emitDateAsString() throws InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<>();
        socket = client();
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... objects) {
                socket.emit("echo", new Date());
                socket.on("echoBack", new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        socket.close();
                        values.offer(args[0]);
                    }
                });
            }
        });
        socket.connect();
        assertThat(values.take(), instanceOf(String.class));
    }

    @Test(timeout = TIMEOUT)
    public void emitDateInObject() throws InterruptedException, JSONException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<>();
        socket = client();
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... objects) {
                JSONObject data = new JSONObject();
                try {
                    data.put("date", new Date());
                } catch (JSONException e) {
                    throw new AssertionError(e);
                }
                socket.emit("echo", data);
                socket.on("echoBack", new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        values.offer(args[0]);
                    }
                });
            }
        });
        socket.connect();
        Object data = values.take();
        assertThat(data, instanceOf(JSONObject.class));
        assertThat(((JSONObject) data).get("date"), instanceOf(String.class));
        socket.close();
    }


    @Test(timeout = TIMEOUT)
    public void sendAndGetBinaryData() throws InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<>();
        final byte[] buf = "asdfasdf".getBytes(Charset.forName("UTF-8"));
        socket = client();
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                socket.emit("echo", buf);
                socket.on("echoBack", new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        values.offer(args[0]);
                    }
                });
            }
        });
        socket.open();
        assertThat((byte[]) values.take(), is(buf));
        socket.close();
    }

    @Test(timeout = TIMEOUT)
    public void sendBinaryDataMixedWithJson() throws InterruptedException, JSONException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<>();
        final byte[] buf = "howdy".getBytes(Charset.forName("UTF-8"));
        socket = client();
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                JSONObject data = new JSONObject();
                try {
                    data.put("hello", "lol");
                    data.put("message", buf);
                    data.put("goodbye", "gotcha");
                } catch (JSONException e) {
                    throw new AssertionError(e);
                }
                socket.emit("echo", data);
                socket.on("echoBack", new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        values.offer(args[0]);
                    }
                });
            }
        });
        socket.open();
        JSONObject a = (JSONObject) values.take();
        assertThat(a.getString("hello"), is("lol"));
        assertThat((byte[]) a.get("message"), is(buf));
        assertThat(a.getString("goodbye"), is("gotcha"));
        socket.close();
    }

    @Test(timeout = TIMEOUT)
    public void sendEventsWithByteArraysInTheCorrectOrder() throws Exception {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<>();
        final byte[] buf = "abuff1".getBytes(Charset.forName("UTF-8"));
        socket = client();
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                socket.emit("echo", buf);
                socket.emit("echo", "please arrive second");

                socket.on("echoBack", new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        values.offer(args[0]);
                    }
                });
            }
        });
        socket.open();
        assertThat((byte[]) values.take(), is(buf));
        assertThat((String) values.take(), is("please arrive second"));
        socket.close();
    }

    @Test(timeout = TIMEOUT)
    public void shouldReceiveBufferedEventAfterReconnect() throws InterruptedException {
        final BlockingQueue<String> events = new LinkedBlockingQueue<>();

        socket = client();
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                if (!socket.isRecovered()) {
                    events.offer("first-connect");
                    // Tell server to start buffering scenario
                    socket.emit("startBufferTest");

                    // Disconnect engine after 1 second
                    Timer timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            socket.io().engine.close();
                        }
                    }, 1000);
                } else {
                    // Reconnected with recovery
                    events.offer("reconnected");
                }
            }
        });

        socket.on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                events.offer("disconnected");
                // Reconnect after 2 seconds
                Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        socket.connect();
                    }
                }, 2000);
            }
        });

        socket.on("message", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                if (args[0] instanceof JSONObject) {
                    JSONObject data = (JSONObject) args[0];
                    String text = data.optString("text");
                    events.offer(text);
                }
            }
        });

        socket.connect();

        // Verify sequence
        assertEquals("first-connect", events.poll(TIMEOUT, TimeUnit.MILLISECONDS));
        assertEquals("disconnected", events.poll(TIMEOUT, TimeUnit.MILLISECONDS));
        assertEquals("buffered-message", events.poll(TIMEOUT, TimeUnit.MILLISECONDS));
        assertEquals("reconnected", events.poll(TIMEOUT, TimeUnit.MILLISECONDS));

        assertTrue("Should have recovered session", socket.isRecovered());
        assertNotNull("Should have tracked offset", socket.getLastOffset());
    }
}
