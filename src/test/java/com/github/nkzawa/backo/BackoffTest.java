package com.github.nkzawa.backo;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class BackoffTest {

    @Test
    public void durationShouldIncreaseTheBackoff() {
        Backoff b = new Backoff();

        assertTrue(100 == b.duration());
        assertTrue(200 == b.duration());
        assertTrue(400 == b.duration());
        assertTrue(800 == b.duration());

        b.reset();
        assertTrue(100 == b.duration());
        assertTrue(200 == b.duration());
    }

    @Test
    public void durationOverflow() {
        Backoff b = new Backoff();
        b.setMin(100);
        b.setMax(10000);
        b.setJitter(1.0);

        for (int i = 0; i < 100; i++) {
            long duration = b.duration();
            assertTrue(100 <= duration && duration <= 10000);
        }
    }
}
