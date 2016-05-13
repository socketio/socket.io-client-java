package io.socket.client;

import io.socket.emitter.Emitter;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;


/**
 * This tests are exactly same as {@link ConnectionTest} one
 * except creating IO.Options.
 *
 * @author junbong
 */
public class SocketOptionBuilderTest extends ConnectionTest {
    private Socket socket;


    @Override
    @Test(timeout = TIMEOUT)
    public void attemptReconnectsAfterAFailedReconnect() throws URISyntaxException, InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<>();

        final IO.Options opts = SocketOptionBuilder.builder()
                                                   .setForceNew(true)
                                                   .setReconnection(true)
                                                   .setTimeout(0)
                                                   .setReconnectionAttempts(2)
                                                   .setReconnectionDelay(10)
                                                   .build();

        final Manager manager = new Manager(new URI(uri()), opts);
        socket = manager.socket("/timeout");
        socket.once(Socket.EVENT_RECONNECT_FAILED, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                final int[] reconnects = new int[]{0};
                Emitter.Listener reconnectCb = new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        reconnects[0]++;
                    }
                };

                manager.on(Manager.EVENT_RECONNECT_ATTEMPT, reconnectCb);
                manager.on(Manager.EVENT_RECONNECT_FAILED, new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        values.offer(reconnects[0]);
                    }
                });
                socket.connect();
            }
        });
        socket.connect();
        assertThat((Integer) values.take(), is(2));
        socket.close();
        manager.close();
    }


    @Override
    @Test(timeout = TIMEOUT)
    public void reconnectDelayShouldIncreaseEveryTime() throws URISyntaxException, InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<Object>();
        IO.Options opts = SocketOptionBuilder.builder()
                                             .setForceNew(true)
                                             .setReconnection(true)
                                             .setTimeout(0)
                                             .setReconnectionAttempts(3)
                                             .setReconnectionDelay(100)
                                             .setRandomizationFactor(0.2)
                                             .build();

        final Manager manager = new Manager(new URI(uri()), opts);
        socket = manager.socket("/timeout");

        final int[] reconnects = new int[] {0};
        final boolean[] increasingDelay = new boolean[] {true};
        final long[] startTime = new long[] {0};
        final long[] prevDelay = new long[] {0};

        socket.on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                startTime[0] = new Date().getTime();
            }
        });
        socket.on(Socket.EVENT_RECONNECT_ATTEMPT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                reconnects[0]++;
                long currentTime = new Date().getTime();
                long delay = currentTime - startTime[0];
                if (delay <= prevDelay[0]) {
                    increasingDelay[0] = false;
                }
                prevDelay[0] = delay;
            }
        });
        socket.on(Socket.EVENT_RECONNECT_FAILED, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                values.offer(true);
            }
        });

        socket.connect();
        values.take();
        assertThat(reconnects[0], is(3));
        assertThat(increasingDelay[0], is(true));
        socket.close();
        manager.close();
    }


    @Override
    @Test(timeout = TIMEOUT)
    public void notReconnectWhenForceClosed() throws URISyntaxException, InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<Object>();
        IO.Options opts = SocketOptionBuilder.builder()
                                             .setForceNew(true)
                                             .setTimeout(0)
                                             .setReconnectionDelay(10)
                                             .build();

        socket = IO.socket(uri() + "/invalid", opts);
        socket.on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                socket.on(Socket.EVENT_RECONNECT_ATTEMPT, new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        values.offer(false);
                    }
                });
                socket.disconnect();
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        values.offer(true);
                    }
                }, 500);
            }
        });
        socket.connect();
        assertThat((Boolean)values.take(), is(true));
    }


    @Override
    @Test(timeout = TIMEOUT)
    public void stopReconnectingWhenForceClosed() throws URISyntaxException, InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<Object>();
        IO.Options opts = SocketOptionBuilder.builder()
                                             .setForceNew(true)
                                             .setTimeout(0)
                                             .setReconnectionDelay(10)
                                             .build();

        socket = IO.socket(uri() + "/invalid", opts);
        socket.once(Socket.EVENT_RECONNECT_ATTEMPT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                socket.on(Socket.EVENT_RECONNECT_ATTEMPT, new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        values.offer(false);
                    }
                });
                socket.disconnect();
                // set a timer to let reconnection possibly fire
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        values.offer(true);
                    }
                }, 500);
            }
        });
        socket.connect();
        assertThat((Boolean) values.take(), is(true));
    }


    @Override
    @Test(timeout = TIMEOUT)
    public void reconnectAfterStoppingReconnection() throws URISyntaxException, InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<Object>();
        IO.Options opts = SocketOptionBuilder.builder()
                                             .setForceNew(true)
                                             .setTimeout(0)
                                             .setReconnectionDelay(10)
                                             .build();

        socket = client("/invalid", opts);
        socket.once(Socket.EVENT_RECONNECT_ATTEMPT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                socket.once(Socket.EVENT_RECONNECT_ATTEMPT, new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        values.offer("done");
                    }
                });
                socket.disconnect();
                socket.connect();
            }
        });
        socket.connect();
        values.take();
        socket.disconnect();
    }


    @Override
    @Test(timeout = TIMEOUT)
    public void tryToReconnectTwiceAndFailWithIncorrectAddress() throws URISyntaxException, InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<Object>();
        IO.Options opts = SocketOptionBuilder.builder()
                                             .setForceNew(true)
                                             .setReconnection(true)
                                             .setReconnectionAttempts(2)
                                             .setReconnectionDelay(10)
                                             .build();

        final Manager manager = new Manager(new URI("http://localhost:3940"), opts);
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
        manager.close();
    }


    @Override
    @Test(timeout = TIMEOUT)
    public void tryToReconnectTwiceAndFailWithImmediateTimeout() throws URISyntaxException, InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<Object>();
        IO.Options opts = SocketOptionBuilder.builder()
                                             .setForceNew(true)
                                             .setTimeout(0)
                                             .setReconnection(true)
                                             .setReconnectionAttempts(2)
                                             .setReconnectionDelay(10)
                                             .build();

        final Manager manager = new Manager(new URI(uri()), opts);

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
                socket.close();
                manager.close();
                values.offer(reconnects[0]);
            }
        });

        socket = manager.socket("/timeout");
        socket.open();
        assertThat((Integer)values.take(), is(2));
    }


    @Override
    @Test(timeout = TIMEOUT)
    public void notTryToReconnectWithIncorrectPortWhenReconnectionDisabled() throws URISyntaxException, InterruptedException {
        final BlockingQueue<Object> values = new LinkedBlockingQueue<Object>();
        IO.Options opts = SocketOptionBuilder.builder()
                                             .setForceNew(true)
                                             .setReconnection(false)
                                             .build();

        final Manager manager = new Manager(new URI("http://localhost:9823"), opts);
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
                        manager.close();
                        values.offer("done");
                    }
                }, 1000);
            }
        });

        socket = manager.socket("/invalid");
        socket.open();
        values.take();
    }
}
