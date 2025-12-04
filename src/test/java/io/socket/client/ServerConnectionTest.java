package io.socket.client;

import io.socket.emitter.Emitter;
import io.socket.engineio.client.Transport;
import io.socket.engineio.client.transports.Polling;
import io.socket.engineio.client.transports.WebSocket;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Arrays;
import java.util.List;
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
    public void openAndClose() throws InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<>();

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

        assertThat(((Object[]) values.take()).length, is(0));
        Object[] args = (Object[]) values.take();
        assertThat(args.length, is(1));
        assertThat(args[0], is(instanceOf(String.class)));
    }

    @Test(timeout = TIMEOUT)
    public void message() throws InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<>();

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

        assertThat(stripOffset((Object[]) values.take()), is(new Object[]{"hello client"}));
        assertThat(stripOffset((Object[]) values.take()), is(new Object[]{"foo", "bar"}));
        socket.disconnect();
    }

    @Test(timeout = TIMEOUT)
    public void event() throws Exception {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<>();

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

        Object[] args = (Object[]) values.take();
        args = stripOffset(args);
        assertThat(args.length, is(3));
        assertThat(args[0].toString(), is(obj.toString()));
        assertThat(args[1], is(nullValue()));
        assertThat((String) args[2], is("bar"));
        socket.disconnect();
    }

    @Test(timeout = TIMEOUT)
    public void ack() throws Exception {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<>();

        final JSONObject obj = new JSONObject();
        obj.put("foo", 1);

        socket = client();
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... objects) {
                socket.emit("ack", new Object[]{obj, "bar"}, new Ack() {
                    @Override
                    public void call(Object... args) {
                        values.offer(args);
                    }
                });
            }
        });
        socket.connect();

        Object[] args = (Object[]) values.take();
        assertThat(args.length, is(2));
        assertThat(args[0].toString(), is(obj.toString()));
        assertThat((String) args[1], is("bar"));
        socket.disconnect();
    }

    @Test(timeout = TIMEOUT)
    public void ackWithoutArgs() throws InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<>();

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

        assertThat((Integer) values.take(), is(0));
        socket.disconnect();
    }

    @Test(timeout = TIMEOUT)
    public void ackWithoutArgsFromClient() throws InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<>();

        socket = client();
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... objects) {
                socket.on("ack", new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        values.offer(args);
                        Ack ack = (Ack) args[0];
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

        Object[] args = (Object[]) values.take();
        args = stripOffset(args);
        assertThat(args.length, is(1));
        assertThat(args[0], is(instanceOf(Ack.class)));
        args = (Object[]) values.take();
        args = stripOffset(args);
        assertThat(args.length, is(0));
        socket.disconnect();
    }

    @Test(timeout = TIMEOUT)
    public void closeEngineConnection() throws InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<>();

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
    public void broadcast() throws InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<>();

        socket = client();
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... objects) {
                socket2 = client();

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

        Object[] args = (Object[]) values.take();
        args = stripOffset(args);
        assertThat(args.length, is(1));
        assertThat((String) args[0], is("hi"));
        socket.disconnect();
        socket2.disconnect();
    }

    @Test(timeout = TIMEOUT)
    public void room() throws InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<>();

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

        Object[] args = (Object[]) values.take();
        args = stripOffset(args);
        assertThat(args.length, is(1));
        assertThat((String) args[0], is("hi"));
        socket.disconnect();
    }

    @Test(timeout = TIMEOUT)
    public void pollingHeaders() throws InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<>();

        IO.Options opts = createOptions();
        opts.transports = new String[]{Polling.NAME};
        socket = client(opts);
        socket.io().on(Manager.EVENT_TRANSPORT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Transport transport = (Transport) args[0];
                transport.on(Transport.EVENT_REQUEST_HEADERS, new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        @SuppressWarnings("unchecked")
                        Map<String, List<String>> headers = (Map<String, List<String>>) args[0];
                        headers.put("X-SocketIO", Arrays.asList("hi"));
                    }
                }).on(Transport.EVENT_RESPONSE_HEADERS, new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        @SuppressWarnings("unchecked")
                        Map<String, List<String>> headers = (Map<String, List<String>>) args[0];
                        List<String> value = headers.get("X-SocketIO");
                        values.offer(value != null ? value.get(0) : "");
                    }
                });
            }
        });
        socket.open();

        assertThat((String) values.take(), is("hi"));
        socket.close();
    }

    @Test(timeout = TIMEOUT)
    public void websocketHandshakeHeaders() throws InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<>();

        IO.Options opts = createOptions();
        opts.transports = new String[]{WebSocket.NAME};
        socket = client(opts);
        socket.io().on(Manager.EVENT_TRANSPORT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Transport transport = (Transport) args[0];
                transport.on(Transport.EVENT_REQUEST_HEADERS, new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        @SuppressWarnings("unchecked")
                        Map<String, List<String>> headers = (Map<String, List<String>>) args[0];
                        headers.put("X-SocketIO", Arrays.asList("hi"));
                    }
                }).on(Transport.EVENT_RESPONSE_HEADERS, new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        @SuppressWarnings("unchecked")
                        Map<String, List<String>> headers = (Map<String, List<String>>) args[0];
                        List<String> value = headers.get("X-SocketIO");
                        values.offer(value != null ? value.get(0) : "");
                    }
                });
            }
        });
        socket.open();

        assertThat((String) values.take(), is("hi"));
        socket.close();
    }

    @Test(timeout = TIMEOUT)
    public void disconnectFromServer() throws InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<>();

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
        assertThat((String) values.take(), is("disconnected"));
    }

    private Object[] stripOffset(Object[] args) {
        return args.length > 0 && args[args.length - 1] instanceof String ? Arrays.copyOf(args, args.length - 1) : args;
    }
}
