package com.github.nkzawa.socketio.client;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.net.URISyntaxException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

@RunWith(JUnit4.class)
public class SocketTest extends Connection {

    private Socket socket;

    @Test(timeout = TIMEOUT)
    public void shouldHaveAnAccessibleSocketIdEqualToTheEngineIOSocketId() throws URISyntaxException, InterruptedException {
        final BlockingQueue<Optional> values = new LinkedBlockingQueue<Optional>();
        socket = client();
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... objects) {
                values.offer(Optional.ofNullable(socket.id()));
            }
        });
        socket.connect();

        @SuppressWarnings("unchecked")
        Optional<String> id = values.take();
        assertThat(id.isPresent(), is(true));
        assertThat(id.get(), is(socket.io().engine.id()));
        socket.disconnect();
    }

    @Test(timeout = TIMEOUT)
    public void clearsSocketIdUponDisconnection() throws URISyntaxException, InterruptedException {
        final BlockingQueue<Optional> values = new LinkedBlockingQueue<Optional>();
        socket = client();
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... objects) {
                socket.on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        values.offer(Optional.ofNullable(socket.id()));
                    }
                });

                socket.disconnect();
            }
        });
        socket.connect();
        @SuppressWarnings("unchecked")
        Optional<String> id = values.take();
        assertThat(id.isPresent(), is(false));
    }

    @Test(timeout = TIMEOUT)
    public void shouldChangeSocketIdUponReconnection() throws URISyntaxException, InterruptedException {
        final BlockingQueue<Optional> values = new LinkedBlockingQueue<Optional>();
        socket = client();
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... objects) {
                values.offer(Optional.ofNullable(socket.id()));

                socket.on(Socket.EVENT_RECONNECT_ATTEMPT, new Emitter.Listener() {
                    @Override
                    public void call(Object... objects) {
                        values.offer(Optional.ofNullable(socket.id()));
                    }
                });

                socket.on(Socket.EVENT_RECONNECT, new Emitter.Listener() {
                    @Override
                    public void call(Object... objects) {
                        values.offer(Optional.ofNullable(socket.id()));
                    }
                });

                socket.io().engine.close();
            }
        });
        socket.connect();
        @SuppressWarnings("unchecked")
        Optional<String> id1 = values.take();

        @SuppressWarnings("unchecked")
        Optional<String> id2 = values.take();
        assertThat(id2.isPresent(), is(false));

        @SuppressWarnings("unchecked")
        Optional<String> id3 = values.take();
        assertThat(id3.get(), is(not(id1.get())));

        socket.disconnect();
    }
}
