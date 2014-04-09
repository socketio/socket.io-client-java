package com.github.nkzawa.socketio.client;

import com.github.nkzawa.emitter.Emitter;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.net.URISyntaxException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

@RunWith(JUnit4.class)
public class ServerConnectionTest extends Connection {

    private Socket socket;

    @Test(timeout = TIMEOUT)
    public void openAndClose() throws URISyntaxException, InterruptedException {
        final BlockingQueue<String> events = new LinkedBlockingQueue<String>();

        socket = client();
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... objects) {
                System.out.println("connect:");
                events.offer("connect");
            }
        }).on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... objects) {
                System.out.println("disconnect:");
                events.offer("disconnect");
            }
        });
        socket.connect();

        assertThat(events.take(), is("connect"));
        socket.disconnect();
        assertThat(events.take(), is("disconnect"));
    }

    @Test(timeout = TIMEOUT)
    public void message() throws URISyntaxException, InterruptedException {
        final BlockingQueue<Object[]> events = new LinkedBlockingQueue<Object[]>();

        socket = client();
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... objects) {
                System.out.println("connect:");
                socket.send("foo", "bar");
            }
        }).on(Socket.EVENT_MESSAGE, new Emitter.Listener() {
            @Override
            public void call(Object... objects) {
                System.out.println(String.format(
                        objects.length > 1 ? "message: %s, %s" : "message: %s", objects));
                events.offer(objects);
            }
        });
        socket.connect();

        assertThat(events.take(), is(new Object[] {"hello client"}));
        assertThat(events.take(), is(new Object[] {"foo", "bar"}));
        socket.disconnect();
    }

    @Test(timeout = TIMEOUT)
    public void event() throws URISyntaxException, InterruptedException {
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
    public void ack() throws URISyntaxException, InterruptedException {
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
        final BlockingQueue<Object[]> events = new LinkedBlockingQueue<Object[]>();

        socket = client();
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... objects) {
                socket.emit("ack", null, new Ack() {
                    @Override
                    public void call(Object... args) {
                        System.out.println("ack: " + args);
                        events.offer(args);
                    }
                });
            }
        });
        socket.connect();

        assertThat(events.take(), is(new Object[] {}));
        socket.disconnect();
    }
}
