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
}
