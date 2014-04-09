package com.github.nkzawa.socketio.client;

import com.github.nkzawa.emitter.Emitter;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CountDownLatch;

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
                JSONObject data = new JSONObject();
                data.put("test", true);
                socket.emit("ack", 5, data, new Ack() {
                    @Override
                    public void call(Object... args) {
                        JSONObject data = (JSONObject)args[1];
                        if ((Integer)args[0] == 5 && data.getBoolean("test")) {
                            latch.countDown();
                        }
                    }
                });
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
                        latch.countDown();
                    }
                });
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
}
