package io.socket.backo;

import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;

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
        for (int i = 0; i < 10; i++) {
            Backoff b = new Backoff();
            b.setMin(100);
            b.setMax(10000);
            b.setJitter(0.5);

            // repeats to make it overflow (a long can have 2 ** 63 - 1)
            for (int j = 0; j < 100; j++) {
                BigInteger ms = BigInteger.valueOf(100).multiply(BigInteger.valueOf(2).pow(j));
                BigInteger deviation = new BigDecimal(ms).multiply(BigDecimal.valueOf(0.5)).toBigInteger();
                BigInteger duration = BigInteger.valueOf(b.duration());

                BigInteger min = ms.subtract(deviation).min(BigInteger.valueOf(10000));
                BigInteger max = ms.add(deviation).min(BigInteger.valueOf(10001));
                assertTrue(min + " <= " + duration + " < " + max,
                        min.compareTo(duration) <= 0 && max.compareTo(duration) == 1);
            }
        }
    }
}
