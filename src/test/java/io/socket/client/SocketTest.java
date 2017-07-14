package io.socket.client;

import io.socket.emitter.Emitter;
import io.socket.util.Optional;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.net.URISyntaxException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertThat;

@RunWith(JUnit4.class)
public class SocketTest extends Connection {

    private Socket socket;

    @Test(timeout = TIMEOUT)
    public void shouldHaveAnAccessibleSocketIdEqualToServerSideSocketId() throws URISyntaxException, InterruptedException {
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
    public void shouldHaveAnAccessibleSocketIdEqualToServerSideSocketIdOnCustomNamespace() throws URISyntaxException, InterruptedException {
        final BlockingQueue<Optional> values = new LinkedBlockingQueue<Optional>();
        socket = client("/foo");
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
        assertThat(id.get(), is("/foo#" + socket.io().engine.id()));
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
    public void doesNotFireConnectErrorIfWeForceDisconnectInOpeningState() throws URISyntaxException, InterruptedException {
        final BlockingQueue<Optional> values = new LinkedBlockingQueue<Optional>();
        IO.Options opts = new IO.Options();
        opts.timeout = 100;
        socket = client(opts);
        socket.on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                values.offer(Optional.of(new Error("Unexpected")));
            }
        });
        socket.connect();
        socket.disconnect();

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                values.offer(Optional.empty());
            }
        }, 300);

        @SuppressWarnings("unchecked")
        Optional<Error> err = values.take();
        if (err.isPresent()) throw err.get();
    }

    @Test(timeout = TIMEOUT)
    public void pingAndPongWithLatency() throws URISyntaxException, InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<Object>();
        socket = client();
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... objects) {
                final boolean[] pinged = new boolean[] { false };
                socket.once(Socket.EVENT_PING, new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        pinged[0] = true;
                    }
                });
                socket.once(Socket.EVENT_PONG, new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        long ms = (long)args[0];
                        values.offer(pinged[0]);
                        values.offer(ms);
                    }
                });
            }
        });
        socket.connect();

        @SuppressWarnings("unchecked")
        boolean pinged = (boolean)values.take();
        assertThat(pinged, is(true));

        @SuppressWarnings("unchecked")
        long ms = (long)values.take();
        assertThat(ms, greaterThan((long)0));

        socket.disconnect();
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

    @Test(timeout = TIMEOUT)
    public void shouldAcceptAQueryStringOnDefaultNamespace() throws URISyntaxException, InterruptedException, JSONException {
        final BlockingQueue<Optional> values = new LinkedBlockingQueue<Optional>();

        socket = client("/?c=d");
        socket.emit("getHandshake", new Ack() {
            @Override
            public void call(Object... args) {
                JSONObject handshake = (JSONObject)args[0];
                values.offer(Optional.ofNullable(handshake));
            }
        });
        socket.connect();

        @SuppressWarnings("unchecked")
        Optional<JSONObject> handshake = values.take();
        assertThat(handshake.get().getJSONObject("query").getString("c"), is("d"));

        socket.disconnect();
    }

    @Test(timeout = TIMEOUT)
    public void shouldAcceptAQueryString() throws URISyntaxException, InterruptedException, JSONException {
        final BlockingQueue<Optional> values = new LinkedBlockingQueue<Optional>();

        socket = client("/abc?b=c&d=e");
        socket.on("handshake", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                JSONObject handshake = (JSONObject)args[0];
                values.offer(Optional.ofNullable(handshake));
            }
        });
        socket.connect();

        @SuppressWarnings("unchecked")
        Optional<JSONObject> handshake = values.take();
        JSONObject query = handshake.get().getJSONObject("query");
        assertThat(query.getString("b"), is("c"));
        assertThat(query.getString("d"), is("e"));

        socket.disconnect();
    }
}
