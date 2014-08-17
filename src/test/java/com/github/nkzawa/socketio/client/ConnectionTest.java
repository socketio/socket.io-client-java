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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(JUnit4.class)
public class ConnectionTest extends Connection {

    private Socket socket;

    @Test(timeout = TIMEOUT)
    public void connectToLocalhost() throws URISyntaxException, InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<Object>();
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
    public void workWithAcks() throws URISyntaxException, InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<Object>();
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
                                values.offer("done");
                            }
                        } catch (JSONException e) {
                            throw new AssertionError(e);
                        }
                    }
                });
            }
        });
        socket.connect();
        values.take();
        socket.close();
    }

    @Test(timeout = TIMEOUT)
    public void receiveDateWithAck() throws URISyntaxException, InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<Object>();
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
    public void workWithFalse() throws URISyntaxException, InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<Object>();
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
        assertThat((Boolean)values.take(), is(false));
        socket.close();
    }

    @Test(timeout = TIMEOUT)
    public void receiveUTF8MultibyteCharacters() throws URISyntaxException, InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<Object>();
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
            assertThat((String)values.take(), is(expected));
        }
        socket.close();
    }

    @Test(timeout = TIMEOUT)
    public void connectToNamespaceAfterConnectionEstablished() throws URISyntaxException, InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<Object>();
        final Manager manager = new Manager(new URI(uri()));
        socket = manager.socket("/");
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... objects) {
                final Socket foo = manager.socket("/foo");
                foo.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        values.offer("done");
                        foo.close();
                    }
                });
                foo.open();
            }
        });
        socket.open();
        values.take();
        socket.close();
    }

    @Test(timeout = TIMEOUT)
    public void reconnectByDefault() throws URISyntaxException, InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<Object>();
        socket = client();
        socket.io().on(Manager.EVENT_RECONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... objects) {
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
        socket.close();
    }

    @Test(timeout = TIMEOUT)
    public void reconnectEventFireInSocket() throws URISyntaxException, InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<Object>();
        socket = client();
        socket.on(Socket.EVENT_RECONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... objects) {
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
        socket.close();
    }

    @Test(timeout = TIMEOUT)
    public void tryToReconnectTwiceAndFailWithIncorrectAddress() throws URISyntaxException, InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<Object>();
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
                values.offer(reconnects[0]);
            }
        });

        socket.open();
        assertThat((Integer)values.take(), is(2));
        socket.close();
    }

    @Test(timeout = TIMEOUT)
    public void tryToReconnectTwiceAndFailWithImmediateTimeout() throws URISyntaxException, InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<Object>();
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
                values.offer(reconnects[0]);
            }
        });

        socket = manager.socket("/timeout");
        socket.open();
        assertThat((Integer)values.take(), is(2));
        socket.close();
    }

    @Test(timeout = TIMEOUT)
    public void notTryToReconnectWithIncorrectPortWhenReconnectionDisabled() throws URISyntaxException, InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<Object>();
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
                        values.offer("done");
                    }
                }, 1000);
            }
        });

        socket = manager.socket("/invalid");
        socket.open();
        values.take();
        socket.close();
    }

    @Test(timeout = TIMEOUT)
    public void fireReconnectEventsOnSocket() throws URISyntaxException, InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<Object>();

        Manager.Options opts = new Manager.Options();
        opts.reconnection = true;
        opts.timeout = 0;
        opts.reconnectionAttempts = 2;
        opts.reconnectionDelay = 10;
        Manager manager = new Manager(new URI(uri()), opts);
        socket = manager.socket("/timeout_socket");

        final int[] reconnects = new int[] {0};
        Emitter.Listener reconnectCb = new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                reconnects[0]++;
                values.offer(args[0]);
            }
        };

        socket.on(Socket.EVENT_RECONNECT_ATTEMPT, reconnectCb);
        socket.on(Socket.EVENT_RECONNECT_FAILED, new Emitter.Listener() {
            @Override
            public void call(Object... objects) {
                values.offer(reconnects[0]);
            }
        });
        socket.open();
        assertThat((Integer)values.take(), is(reconnects[0]));
        assertThat((Integer)values.take(), is(2));
        socket.close();
    }

    @Test(timeout = TIMEOUT)
    public void fireReconnectingWithAttemptsNumberWhenReconnectingTwice() throws URISyntaxException, InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<Object>();

        Manager.Options opts = new Manager.Options();
        opts.reconnection = true;
        opts.timeout = 0;
        opts.reconnectionAttempts = 2;
        opts.reconnectionDelay = 10;
        Manager manager = new Manager(new URI(uri()), opts);
        socket = manager.socket("/timeout_socket");

        final int[] reconnects = new int[] {0};
        Emitter.Listener reconnectCb = new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                reconnects[0]++;
                values.offer(args[0]);
            }
        };

        socket.on(Socket.EVENT_RECONNECTING, reconnectCb);
        socket.on(Socket.EVENT_RECONNECT_FAILED, new Emitter.Listener() {
            @Override
            public void call(Object... objects) {
                values.offer(reconnects[0]);
            }
        });
        socket.open();
        assertThat((Integer)values.take(), is(reconnects[0]));
        assertThat((Integer)values.take(), is(2));
        socket.close();
    }

    @Test(timeout = TIMEOUT)
    public void emitDateAsString() throws URISyntaxException, InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<Object>();
        socket = client();
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... objects) {
                socket.emit("echo", new Date());
                socket.on("echoBack", new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        values.offer(args[0]);
                        socket.close();
                    }
                });
            }
        });
        socket.connect();
        assertThat(values.take(), instanceOf(String.class));
        socket.close();
    }

    @Test(timeout = TIMEOUT)
    public void emitDateInObject() throws URISyntaxException, InterruptedException, JSONException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<Object>();
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
        assertThat(((JSONObject)data).get("date"), instanceOf(String.class));
        socket.close();
    }

    @Test(timeout = TIMEOUT)
    public void sendAndGetBinaryData() throws URISyntaxException, InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<Object>();
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
        assertThat((byte[])values.take(), is(buf));
        socket.close();
    }

    @Test(timeout = TIMEOUT)
    public void sendBinaryDataMixedWithJson() throws URISyntaxException, InterruptedException, JSONException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<Object>();
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
        JSONObject a = (JSONObject)values.take();
        assertThat(a.getString("hello"), is("lol"));
        assertThat((byte[])a.get("message"), is(buf));
        assertThat(a.getString("goodbye"), is("gotcha"));
        socket.close();
    }

    @Test(timeout = TIMEOUT)
    public void sendEventsWithByteArraysInTheCorrectOrder() throws Exception {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<Object>();
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
        assertThat((byte[])values.take(), is(buf));
        assertThat((String)values.take(), is("please arrive second"));
        socket.close();
    }
}
