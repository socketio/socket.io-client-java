package com.github.nkzawa.backo;

public class Backoff {

    private long ms = 100;
    private long max = 10000;
    private int factor = 2;
    private double jitter = 0.0;
    private int attempts = 0;

    public Backoff() {}

    public long duration() {
        long ms = this.ms * (long) Math.pow(this.factor, this.attempts++);
        if (jitter != 0.0) {
            double rand = Math.random();
            int deviation = (int) Math.floor(rand * this.jitter * ms);
            ms = (((int) Math.floor(rand * 10)) & 1) == 0 ? ms - deviation : ms + deviation;
        }
        if (ms < this.ms) {
            // overflow happened
            ms = Long.MAX_VALUE;
        }
        return Math.min(ms, this.max);
    }

    public void reset() {
        this.attempts = 0;
    }

    public Backoff setMin(long min) {
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
        this.jitter = jitter;
        return this;
    }

    public int getAttempts() {
        return this.attempts;
    }
}
