package com.github.nkzawa.socketio.client;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.engineio.client.Transport;
import com.github.nkzawa.engineio.client.transports.Polling;
import com.github.nkzawa.engineio.client.transports.WebSocket;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

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
                socket.io().engine.on(com.github.nkzawa.engineio.client.Socket.EVENT_CLOSE, new Emitter.Listener() {
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
                        Map<String, String> headers = (Map<String, String>)args[0];
                        headers.put("X-SocketIO", "hi");
                    }
                }).on(Transport.EVENT_RESPONSE_HEADERS, new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        @SuppressWarnings("unchecked")
                        Map<String, String> headers = (Map<String, String>)args[0];
                        String value = headers.get("X-SocketIO");
                        values.offer(value != null ? value : "");
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
                        Map<String, String> headers = (Map<String, String>)args[0];
                        headers.put("X-SocketIO", "hi");
                    }
                }).on(Transport.EVENT_RESPONSE_HEADERS, new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        @SuppressWarnings("unchecked")
                        Map<String, String> headers = (Map<String, String>)args[0];
                        String value = headers.get("X-SocketIO");
                        values.offer(value != null ? value : "");
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
