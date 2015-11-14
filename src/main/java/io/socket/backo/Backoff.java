package io.socket.backo;

public class Backoff {

    private final double jitterMin = 0.0; // FIX added min and max allowed values for jitter, see setJitter()
    private final double jitterMax = 0.5;

    private long ms = 100;
    private long min = ms; // FIX added min so we can ensure ms stays within its bounds
    private long max = 10000;
    private int factor = 2;
    private double jitter = 0.0;
    private int attempts = 0;

    public Backoff() {}

    public long duration() {
        long ms = this.ms * (long) Math.pow(this.factor, this.attempts++);
        if (jitter != 0.0) {
            double rand = Math.random();
            long deviation = (long) (2.0 * ms * jitter * (rand - 0.5)); // FIX simplified deviation as a negative or positive value, as mathematically defined
            ms = ms + deviation;
        }
        ms = Math.min(Math.max(ms, min), max); // FIX changed this to keep ms between min and max
        return ms;
    }

    public void reset() {
        this.attempts = 0;
    }

    public Backoff setMin(long min) {
        this.min = min; // FIX set min as well so we have updated bounds for the check in duration()
        this.ms = min;
        return this;
    }

    public Backoff setMax(long max) {
        this.max = max;
        return this;
    }

    public Backoff setFactor(int factor) {
        this.factor = factor;
        return this;
    }

    public Backoff setJitter(double jitter) {
        jitter = Math.min(Math.max(jitter, jitterMin), jitterMax); // FIX optional, but we may want to ensure jitter stays within certain bounds, otherwise we'll get wild values for deviation
        this.jitter = jitter;
        return this;
    }

    public int getAttempts() {
        return this.attempts;
    }
}
