package com.github.nkzawa.socketio.client;

import com.github.nkzawa.emitter.Emitter;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.net.URISyntaxException;
import java.util.concurrent.Semaphore;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

@RunWith(JUnit4.class)
public class ServerConnectionTest extends Connection {

    private Socket socket;

    @Test(timeout = TIMEOUT)
    public void openAndClose() throws URISyntaxException, InterruptedException {
        final Semaphore semaphore = new Semaphore(0);

        socket = client();
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... objects) {
                socket.disconnect();
            }
        }).on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... objects) {
                semaphore.release();
            }
        });
        socket.connect();
        semaphore.acquire();
    }

    @Test(timeout = TIMEOUT)
    public void message() throws URISyntaxException, InterruptedException {
        final Semaphore semaphore = new Semaphore(0);
        final int[] count = new int[] {0};

        socket = client();
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... objects) {
                socket.send("foo", "bar");
            }
        }).on(Socket.EVENT_MESSAGE, new Emitter.Listener() {
            @Override
            public void call(Object... objects) {
                switch (count[0]++) {
                case 0:
                    assertThat(objects, is(new Object[] {"hello client"}));
                    break;
                case 1:
                    assertThat(objects, is(new Object[] {"foo", "bar"}));
                    socket.disconnect();
                    semaphore.release();
                    break;
                }
            }
        });
        socket.connect();
        semaphore.acquire();
    }

    @Test(timeout = TIMEOUT)
    public void event() throws Exception {
        final Semaphore semaphore = new Semaphore(0);

        final JSONObject obj = new JSONObject();
        obj.put("foo", 1);

        socket = client();
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... objects) {
                System.out.println("connect:");
                socket.emit("echo", obj, null, "bar");
            }
        }).on("echoBack", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                System.out.println(String.format("echoBack: %s, %s, %s", args));
                assertThat(args.length, is(3));
                assertThat(args[0].toString(), is(obj.toString()));
                assertThat(args[1], is(nullValue()));
                assertThat((String)args[2], is("bar"));
                socket.disconnect();
                semaphore.release();
            }
        });
        socket.connect();
        semaphore.acquire();
    }

    @Test(timeout = TIMEOUT)
    public void ack() throws Exception {
        final Semaphore semaphore = new Semaphore(0);

        final JSONObject obj = new JSONObject();
        obj.put("foo", 1);

        socket = client();
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... objects) {
                System.out.println("connect:");
                socket.emit("ack", new Object[] {obj, "bar"}, new Ack() {
                    @Override
                    public void call(Object... args) {
                        System.out.println(String.format("ack: %s, %s", args));
                        assertThat(args.length, is(2));
                        assertThat(args[0].toString(), is(obj.toString()));
                        assertThat((String)args[1], is("bar"));
                        socket.disconnect();
                        semaphore.release();
                    }
                });
            }
        });
        socket.connect();
        semaphore.acquire();
    }

    @Test(timeout = TIMEOUT)
    public void ackWithoutArgs() throws URISyntaxException, InterruptedException {
        final Semaphore semaphore = new Semaphore(0);

        socket = client();
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... objects) {
                socket.emit("ack", null, new Ack() {
                    @Override
                    public void call(Object... args) {
                        assertThat(args, is(new Object[] {}));
                        socket.disconnect();
                        semaphore.release();
                    }
                });
            }
        });
        socket.connect();
        semaphore.acquire();
    }
}
