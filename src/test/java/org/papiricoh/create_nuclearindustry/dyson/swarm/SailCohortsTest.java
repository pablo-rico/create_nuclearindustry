package org.papiricoh.create_nuclearindustry.dyson.swarm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Sails counted by when they wear out. */
class SailCohortsTest {
    @Test
    void sailsWearOutInOrder() {
        SailCohorts c = new SailCohorts();
        c.add(5000, 3);
        c.add(2000, 2);
        c.add(9000, 1);
        assertEquals(6, c.total());
        assertEquals(0, c.expire(1000));
        assertEquals(2, c.expire(2400));
        assertEquals(4, c.total());
        assertEquals(3, c.expire(6000));
        assertEquals(1, c.total());
        assertEquals(1, c.expire(1_000_000));
        assertEquals(0, c.total());
        assertEquals(-1, c.nextExpiry());
    }

    @Test
    void sailsWithinAMinuteShareACohort() {
        SailCohorts c = new SailCohorts();
        for (int t = 0; t < 1200; t += 40) c.add(24000 + t + 1, 1);
        assertEquals(1, c.marks().length);
        assertEquals(30, c.total());
    }

    @Test
    void expiringBeforeCounts() {
        SailCohorts c = new SailCohorts();
        c.add(1200, 4);
        c.add(3600, 5);
        assertEquals(4, c.expiringBefore(2400));
        assertEquals(9, c.expiringBefore(100_000));
    }

    @Test
    void removeOldestTakesFromTheFront() {
        SailCohorts c = new SailCohorts();
        c.add(1200, 4);
        c.add(3600, 5);
        assertEquals(6, c.removeOldest(6));
        assertEquals(3, c.total());
        assertArrayEquals(new long[] {3600}, c.marks());
        assertEquals(3, c.removeOldest(100));
        assertEquals(0, c.total());
    }

    @Test
    void savedAndLoadedTheSame() {
        SailCohorts c = new SailCohorts();
        for (int i = 0; i < 40; i++) c.add(i * 5000L, i + 1);
        SailCohorts back = SailCohorts.of(c.marks(), c.counts());
        assertArrayEquals(c.marks(), back.marks());
        assertArrayEquals(c.counts(), back.counts());
        assertEquals(c.total(), back.total());
    }
}
