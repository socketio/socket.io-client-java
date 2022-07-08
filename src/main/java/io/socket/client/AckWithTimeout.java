package io.socket.client;

import java.util.Timer;
import java.util.TimerTask;

public abstract class AckWithTimeout implements Ack {
    private final long timeout;
    private final Timer timer = new Timer();

    /**
     *
     * @param timeout delay in milliseconds
     */
    public AckWithTimeout(long timeout) {
        this.timeout = timeout;
    }

    @Override
    public final void call(Object... args) {
        this.timer.cancel();
        this.onSuccess(args);
    }

    public final void schedule(TimerTask task) {
        this.timer.schedule(task, this.timeout);
    }

    public final void cancelTimer() {
        this.timer.cancel();
    }

    public abstract void onSuccess(Object... args);
    public abstract void onTimeout();

}
