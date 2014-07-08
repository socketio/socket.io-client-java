package com.github.nkzawa.socketio.client;

import com.github.nkzawa.emitter.Emitter;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(JUnit4.class)
public class ConnectionTest extends Connection {

    private Socket socket;

    @Test(timeout = TIMEOUT)
    public void connectToLocalhost() throws URISyntaxException, InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        socket = client();
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... objects) {
                socket.emit("echo");
                socket.on("echoBack", new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        socket.close();
                        latch.countDown();
                    }
                });
            }
        });
        socket.connect();
        latch.await();
    }

    @Test(timeout = TIMEOUT)
    public void workWithAcks() throws URISyntaxException, InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        socket = client();
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... objects) {
                socket.emit("callAck");
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
                                socket.close();
                                latch.countDown();
                            }
                        } catch (JSONException e) {
                            throw new AssertionError(e);
                        }
                    }
                });
            }
        });
        socket.connect();
        latch.await();
    }

    @Test(timeout = TIMEOUT)
    public void receiveDateWithAck() throws URISyntaxException, InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        socket = client();
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... objects) {
                try {
                    socket.emit("getAckDate", new JSONObject("{test: true}"), new Ack() {
                        @Override
                        public void call(Object... args) {
                            assertThat(args[0], instanceOf(String.class));
                            socket.close();
                            latch.countDown();
                        }
                    });
                } catch (JSONException e) {
                    throw new AssertionError(e);
                }
            }
        });
        socket.connect();
        latch.await();
    }

    @Test(timeout = TIMEOUT)
    public void workWithFalse() throws URISyntaxException, InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        socket = client();
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... objects) {
                socket.emit("echo", false);
                socket.on("echoBack", new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        assertThat((Boolean)args[0], is(false));
                        socket.close();
                        latch.countDown();
                    }
                });
            }
        });
        socket.connect();
        latch.await();
    }

    @Test(timeout = TIMEOUT)
    public void receiveUTF8MultibyteCharacters() throws URISyntaxException, InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final String[] correct = new String[] {
            "てすと",
            "Я Б Г Д Ж Й",
            "Ä ä Ü ü ß",
            "utf8 — string",
            "utf8 — string"
        };

        socket = client();
        final int[] i = new int[] {0};
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... objects) {
                socket.on("echoBack", new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        assertThat((String)args[0], is(correct[i[0]]));
                        i[0]++;
                        if (i[0] == correct.length) {
                            socket.close();
                            latch.countDown();
                        }
                    }
                });
                for (String data : correct) {
                    socket.emit("echo", data);
                }
            }
        });
        socket.connect();
        latch.await();
    }

    @Test(timeout = TIMEOUT)
    public void connectToNamespaceAfterConnectionEstablished() throws URISyntaxException, InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final Manager manager = new Manager(new URI(uri()));
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
                        latch.countDown();
                    }
                });
                foo.open();
            }
        });
        socket.open();
        latch.await();
    }

    @Test(timeout = TIMEOUT)
    public void reconnectByDefault() throws URISyntaxException, InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        socket = IO.socket(uri());
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... objects) {
                socket.io.engine.close();
                socket.io.on(Manager.EVENT_RECONNECT, new Emitter.Listener() {
                    @Override
                    public void call(Object... objects) {
                        socket.close();
                        latch.countDown();
                    }
                });
            }
        });
        socket.open();
        latch.await();
    }

    @Test(timeout = TIMEOUT)
    public void tryToReconnectTwiceAndFailWithIncorrectAddress() throws URISyntaxException, InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        IO.Options opts = new IO.Options();
        opts.reconnection = true;
        opts.reconnectionAttempts = 2;
        opts.reconnectionDelay = 10;
        Manager manager = new Manager(new URI("http://localhost:3940"), opts);
        socket = manager.socket("/asd");
        final int[] reconnects = new int[] {0};
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
                assertThat(reconnects[0], is(2));
                socket.close();
                latch.countDown();
            }
        });

        socket.open();
        latch.await();
    }

    @Test(timeout = TIMEOUT)
    public void tryToReconnectTwiceAndFailWithImmediateTimeout() throws URISyntaxException, InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        IO.Options opts = new IO.Options();
        opts.reconnection = true;
        opts.timeout = 0;
        opts.reconnectionAttempts = 2;
        opts.reconnectionDelay = 10;
        Manager manager = new Manager(new URI(uri()), opts);

        final int[] reconnects = new int[] {0};
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
                assertThat(reconnects[0], is(2));
                socket.close();
                latch.countDown();
            }
        });

        socket = manager.socket("/timeout");
        socket.open();
        latch.await();
    }

    @Test(timeout = TIMEOUT)
    public void notTryToReconnectWithIncorrectPortWhenReconnectionDisabled() throws URISyntaxException, InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        IO.Options opts = new IO.Options();
        opts.reconnection = false;
        Manager manager = new Manager(new URI("http://localhost:9823"), opts);
        Emitter.Listener cb = new Emitter.Listener() {
            @Override
            public void call(Object... objects) {
                socket.close();
                throw new RuntimeException();
            }
        };
        manager.on(Manager.EVENT_RECONNECT_ATTEMPT, cb);
        manager.on(Manager.EVENT_CONNECT_ERROR, new Emitter.Listener() {
            @Override
            public void call(Object... objects) {
                Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        socket.close();
                        latch.countDown();
                    }
                }, 1000);
            }
        });

        socket = manager.socket("/invalid");
        socket.open();
        latch.await();
    }

    @Test(timeout = TIMEOUT)
    public void emitDateAsString() throws URISyntaxException, InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        socket = client();
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... objects) {
                socket.emit("echo", new Date());
                socket.on("echoBack", new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        assertThat(args[0], instanceOf(String.class));
                        socket.close();
                        latch.countDown();
                    }
                });
            }
        });
        socket.connect();
        latch.await();
    }

    @Test(timeout = TIMEOUT)
    public void emitDateInObject() throws URISyntaxException, InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
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
                        assertThat(args[0], instanceOf(JSONObject.class));
                        try {
                            assertThat(((JSONObject)args[0]).get("date"), instanceOf(String.class));
                        } catch (JSONException e) {
                            throw new AssertionError(e);
                        }
                        socket.close();
                        latch.countDown();
                    }
                });
            }
        });
        socket.connect();
        latch.await();
    }

    @Test(timeout = TIMEOUT)
    public void sendAndGetBinaryData() throws URISyntaxException, InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        socket = client();
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                final byte[] buf = "asdfasdf".getBytes(Charset.forName("UTF-8"));
                socket.emit("echo", buf);
                socket.on("echoBack", new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        assertThat(args[0], instanceOf(byte[].class));
                        assertThat((byte[])args[0], is(buf));
                        socket.close();
                        latch.countDown();
                    }
                });
            }
        });
        socket.open();
        latch.await();
    }

    @Test(timeout = TIMEOUT)
    public void sendBinaryDataMixedWithJson() throws URISyntaxException, InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        socket = client();
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                final byte[] buf = "howdy".getBytes(Charset.forName("UTF-8"));
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
                        JSONObject a = (JSONObject)args[0];
                        try {
                            assertThat(a.getString("hello"), is("lol"));
                            assertThat((byte[])a.get("message"), is(buf));
                            assertThat(a.getString("goodbye"), is("gotcha"));
                        } catch (JSONException e) {
                            throw new AssertionError(e);
                        }
                        socket.close();
                        latch.countDown();
                    }
                });
            }
        });
        socket.open();
        latch.await();
    }

    @Test(timeout = TIMEOUT)
    public void sendEventsWithByteArraysInTheCorrectOrder() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        socket = client();
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                final byte[] buf = "abuff1".getBytes(Charset.forName("UTF-8"));
                socket.emit("echo", buf);
                socket.emit("echo", "please arrive second");

                final boolean[] receivedAbuff1 = new boolean[] {false};
                socket.on("echoBack", new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        Object data = args[0];
                        if (data instanceof byte[]) {
                            assertThat((byte[])data, is(buf));
                            receivedAbuff1[0] = true;
                            return;
                        }

                        assertThat((String)data, is("please arrive second"));
                        assertThat(receivedAbuff1[0], is(true));
                        socket.close();
                        latch.countDown();
                    }
                });
            }
        });
        socket.open();
        latch.await();
    }
}
