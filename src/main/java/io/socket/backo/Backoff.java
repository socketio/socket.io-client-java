package io.socket.backo;

import java.math.BigDecimal;
import java.math.BigInteger;

public class Backoff {

    private long ms = 100;
    private long max = 10000;
    private int factor = 2;
    private double jitter;
    private int attempts;

    public Backoff() {}

    public long duration() {
        BigInteger ms = BigInteger.valueOf(this.ms)
                .multiply(BigInteger.valueOf(this.factor).pow(this.attempts++));
        if (jitter != 0.0) {
            double rand = Math.random();
            BigInteger deviation = BigDecimal.valueOf(rand)
                    .multiply(BigDecimal.valueOf(jitter))
                    .multiply(new BigDecimal(ms)).toBigInteger();
            ms = (((int) Math.floor(rand * 10)) & 1) == 0 ? ms.subtract(deviation) : ms.add(deviation);
        }
        return ms.min(BigInteger.valueOf(this.max)).longValue();
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
