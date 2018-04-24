package io.socket.client;

import io.socket.emitter.Emitter;
import io.socket.engineio.client.Transport;
import io.socket.engineio.client.transports.Polling;
import io.socket.engineio.client.transports.WebSocket;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class ServerConnectionTest extends Connection {

    private Socket socket;
    private Socket socket2;

    @Test(timeout = TIMEOUT)
    public void openAndClose() throws URISyntaxException, InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<Object>();

        socket = client();
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                values.offer(args);
                socket.disconnect();
            }
        }).on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                values.offer(args);
            }
        });
        socket.connect();

        assertThat(((Object[])values.take()).length, is(0));
        Object[] args = (Object[] )values.take();
        assertThat(args.length, is(1));
        assertThat(args[0], is(instanceOf(String.class)));
    }

    @Test(timeout = TIMEOUT)
    public void message() throws URISyntaxException, InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<Object>();

        socket = client();
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... objects) {
                socket.send("foo", "bar");
            }
        }).on(Socket.EVENT_MESSAGE, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                values.offer(args);
            }
        });
        socket.connect();

        assertThat((Object[])values.take(), is(new Object[] {"hello client"}));
        assertThat((Object[])values.take(), is(new Object[] {"foo", "bar"}));
        socket.disconnect();
    }

    @Test(timeout = TIMEOUT)
    public void event() throws Exception {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<Object>();

        final JSONObject obj = new JSONObject();
        obj.put("foo", 1);

        socket = client();
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... objects) {
                socket.emit("echo", obj, null, "bar");
            }
        }).on("echoBack", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                values.offer(args);
            }
        });
        socket.connect();

        Object[] args = (Object[])values.take();
        assertThat(args.length, is(3));
        assertThat(args[0].toString(), is(obj.toString()));
        assertThat(args[1], is(nullValue()));
        assertThat((String)args[2], is("bar"));
        socket.disconnect();
    }

    @Test(timeout = TIMEOUT)
    public void ack() throws Exception {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<Object>();

        final JSONObject obj = new JSONObject();
        obj.put("foo", 1);

        socket = client();
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... objects) {
                socket.emit("ack", new Object[] {obj, "bar"}, new Ack() {
                    @Override
                    public void call(Object... args) {
                        values.offer(args);
                    }
                });
            }
        });
        socket.connect();

        Object[] args = (Object[])values.take();
        assertThat(args.length, is(2));
        assertThat(args[0].toString(), is(obj.toString()));
        assertThat((String)args[1], is("bar"));
        socket.disconnect();
    }

    /**
     * TestCase:
     * -> Server has not implemented the `ackNotImplemented`
     * -> server will never answer to client
     * -> client decides that the callback for him is useful if received in 2 seconds
     * -> client will receive NoAck as response after 2 seconds, because server did not respond
     */
    @Test(timeout = TIMEOUT)
    public void noack() throws Exception {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<Object>();

        final JSONObject obj = new JSONObject();
        obj.put("foo", 1);

        socket = client();
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... objects) {

                socket.emit("ackNotImplemented", new Object[]{obj, "bar"}, new AckWithTimeOut(2000) {
                    @Override
                    public void call(Object... args) {
                        values.offer(args);
                    }
                });

            }
        });

        socket.connect();

        Object[] args = (Object[]) values.take();
        assertTrue(args[0] instanceof NoAck);
        socket.disconnect();
    }

    /**
     * TestCase:
     * -> Server has not implemented the `ackNotImplemented`
     * -> server will never answer to client
     * -> client decides that the callback for him is useful if received in 1 second
     * -> Client got disconnected, because of a network issue
     * -> keeps sending emits, which will be put in the buffer to be sent after the reconnection
     * -> onReconnection he expects the results of all his emits otherwise a NoAck after 1 second
     * <p>
     * NOTE:
     * If we don't have the ack timeout, this test and all other emit-tests which expect a callback will fail for TIMEOUT
     */
    @Test(timeout = TIMEOUT)
    public void noackWithReconnect() throws Exception {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<Object>();

        // disconnect after 2 seconds
        simulateReconnection(2000);

        socket = client();
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... objects) {

                // make 4 emits delayed one from the other, and each will have a timeout of 1 second
                for (int i = 0; i < 4; i++) {

                    final Timer emitTimer = new Timer();
                    final int pos = i;
                    emitTimer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            socket.emit("ackNotImplemented", "emit number " + pos, new AckWithTimeOut(1000) {
                                @Override
                                public void call(Object... args) {
                                    values.offer(args);
                                }
                            });
                        }
                    }, i * 1000);

                }
            }

        });

        socket.connect();
        // make sure we have received 4 NoAcks
        assertTrue(((Object[]) values.take())[0] instanceof NoAck);
        assertTrue(((Object[]) values.take())[0] instanceof NoAck);
        assertTrue(((Object[]) values.take())[0] instanceof NoAck);
        assertTrue(((Object[]) values.take())[0] instanceof NoAck);

        socket.disconnect();
    }

    /**
     * Simulate the socket reconnection
     *
     * @param after milliseconds, when to start the reconnection
     */
    private void simulateReconnection(long after) {
        final Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                // simulate reconnect...
                socket.io().engine.close();
            }
        }, after);
    }

    @Test(timeout = TIMEOUT)
    public void ackWithoutArgs() throws URISyntaxException, InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<Object>();

        socket = client();
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... objects) {
                socket.emit("ack", null, new Ack() {
                    @Override
                    public void call(Object... args) {
                        values.offer(args.length);
                    }
                });
            }
        });
        socket.connect();

        assertThat((Integer)values.take(), is(0));
        socket.disconnect();
    }

    @Test(timeout = TIMEOUT)
    public void ackWithoutArgsFromClient() throws URISyntaxException, InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<Object>();

        socket = client();
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... objects) {
                socket.on("ack", new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        values.offer(args);
                        Ack ack = (Ack)args[0];
                        ack.call();
                    }
                }).on("ackBack", new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        values.offer(args);
                        socket.disconnect();
                    }
                });
                socket.emit("callAck");
            }
        });
        socket.connect();

        Object[] args = (Object[])values.take();
        assertThat(args.length, is(1));
        assertThat(args[0], is(instanceOf(Ack.class)));
        args = (Object[])values.take();
        assertThat(args.length, is(0));
        socket.disconnect();
    }

    @Test(timeout = TIMEOUT)
    public void closeEngineConnection() throws URISyntaxException, InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<Object>();

        socket = client();
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                socket.io().engine.on(io.socket.engineio.client.Socket.EVENT_CLOSE, new Emitter.Listener() {
                    @Override
                    public void call(Object... objects) {
                        values.offer("done");
                    }
                });
                socket.disconnect();
            }
        });
        socket.connect();
        values.take();
    }

    @Test(timeout = TIMEOUT)
    public void broadcast() throws URISyntaxException, InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<Object>();

        socket = client();
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... objects) {
                try {
                    socket2 = client();
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }

                socket2.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
                    @Override
                    public void call(Object... objects) {
                        socket2.emit("broadcast", "hi");
                    }
                });
                socket2.connect();
            }
        }).on("broadcastBack", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                values.offer(args);
            }
        });
        socket.connect();

        Object[] args = (Object[])values.take();
        assertThat(args.length, is(1));
        assertThat((String)args[0], is("hi"));
        socket.disconnect();
        socket2.disconnect();
    }

    @Test(timeout = TIMEOUT)
    public void room() throws URISyntaxException, InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<Object>();

        socket = client();
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... objects) {
                socket.emit("room", "hi");
            }
        }).on("roomBack", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                values.offer(args);
            }
        });
        socket.connect();

        Object[] args = (Object[])values.take();
        assertThat(args.length, is(1));
        assertThat((String)args[0], is("hi"));
        socket.disconnect();
    }

    @Test(timeout = TIMEOUT)
    public void pollingHeaders() throws URISyntaxException, InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<Object>();

        IO.Options opts = createOptions();
        opts.transports = new String[] {Polling.NAME};
        socket = client(opts);
        socket.io().on(Manager.EVENT_TRANSPORT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Transport transport = (Transport)args[0];
                transport.on(Transport.EVENT_REQUEST_HEADERS, new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        @SuppressWarnings("unchecked")
                        Map<String, List<String>> headers = (Map<String, List<String>>)args[0];
                        headers.put("X-SocketIO", Arrays.asList("hi"));
                    }
                }).on(Transport.EVENT_RESPONSE_HEADERS, new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        @SuppressWarnings("unchecked")
                        Map<String, List<String>> headers = (Map<String, List<String>>)args[0];
                        List<String> value = headers.get("X-SocketIO");
                        values.offer(value != null ? value.get(0) : "");
                    }
                });
            }
        });
        socket.open();

        assertThat((String)values.take(), is("hi"));
        socket.close();
    }

    @Test(timeout = TIMEOUT)
    public void websocketHandshakeHeaders() throws URISyntaxException, InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<Object>();

        IO.Options opts = createOptions();
        opts.transports = new String[] {WebSocket.NAME};
        socket = client(opts);
        socket.io().on(Manager.EVENT_TRANSPORT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Transport transport = (Transport)args[0];
                transport.on(Transport.EVENT_REQUEST_HEADERS, new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        @SuppressWarnings("unchecked")
                        Map<String, List<String>> headers = (Map<String, List<String>>)args[0];
                        headers.put("X-SocketIO", Arrays.asList("hi"));
                    }
                }).on(Transport.EVENT_RESPONSE_HEADERS, new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        @SuppressWarnings("unchecked")
                        Map<String, List<String>> headers = (Map<String, List<String>>)args[0];
                        List<String> value = headers.get("X-SocketIO");
                        values.offer(value != null ? value.get(0) : "");
                    }
                });
            }
        });
        socket.open();

        assertThat((String)values.take(), is("hi"));
        socket.close();
    }

    @Test(timeout = TIMEOUT)
    public void disconnectFromServer() throws URISyntaxException, InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<Object>();

        socket = client();
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                socket.emit("requestDisconnect");
            }
        }).on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                values.offer("disconnected");
            }
        });
        socket.connect();
        assertThat((String)values.take(), is("disconnected"));
    }
}
