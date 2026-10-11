package org.papiricoh.create_nuclearindustry.multiblock;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A controller checks its readings record every ten ticks: the components are read through accessors
 * looked up once per record class into a row the throttle reuses, and the update that follows a check
 * remembers the row the check read instead of reading the record again.
 */
class SyncThrottleRecordTest {
    enum Mode { IDLE, RUN }

    record Readings(boolean on, Mode mode, int count, double level, float ratio, long ticks) {}

    @Test
    void aReusedRowGetsTheSameNumbers() {
        double[] row = new double[6];
        double[] filled = SyncThrottle.valuesOf(new Readings(true, Mode.RUN, 3, 2.5, 0.25f, 40L), row);
        assertSame(row, filled, "a row of the right length is filled in place");
        assertArrayEquals(new double[]{1, 1, 3, 2.5, 0.25, 40}, filled);
        assertArrayEquals(filled, SyncThrottle.valuesOf(new Readings(true, Mode.RUN, 3, 2.5, 0.25f, 40L)));
        double[] other = SyncThrottle.valuesOf(new Readings(false, Mode.IDLE, 0, 0, 0, 0), new double[2]);
        assertArrayEquals(new double[6], other, "a row of the wrong length is replaced");
    }

    @Test
    void theUpdateAfterACheckRemembersTheCheckedValues() {
        SyncThrottle throttle = SyncThrottle.forRecord(Readings.class, 0.005, 1.0e-3, Map.of());
        Readings first = new Readings(true, Mode.RUN, 3, 100, 0.5f, 0);
        assertTrue(throttle.due(0, first, 100), "never sent");
        throttle.sent(0, () -> first);
        assertFalse(throttle.due(10, new Readings(true, Mode.RUN, 3, 100.2, 0.5f, 0), 100), "0.2% is not visible");
        assertTrue(throttle.due(20, new Readings(true, Mode.RUN, 3, 101, 0.5f, 0), 100), "1% is");
        // The update follows the check in the same tick: it is the 101 just checked that the clients now show.
        throttle.sent(20, () -> {
            throw new AssertionError("the row the check read this tick is used");
        });
        assertFalse(throttle.due(30, new Readings(true, Mode.RUN, 3, 101.2, 0.5f, 0), 100));
        // An update with no check this tick reads the record it is given.
        throttle.sent(35, () -> new Readings(true, Mode.RUN, 4, 101, 0.5f, 0));
        assertFalse(throttle.due(40, new Readings(true, Mode.RUN, 4, 101, 0.5f, 0), 100), "the count it sent is the one now");
        assertTrue(throttle.sends() == 3, "three updates recorded, got " + throttle.sends());
    }

    /**
     * Delta updates: a broadcast moves the "every client has this" mark only when the last broadcast's
     * packet may have gone out (a packet was made since) and in a later tick; otherwise both go out in
     * one packet, which must still carry what the first would have.
     */
    @Test
    void broadcastsSharingOnePacketCountAsOne() {
        assertFalse(SyncThrottle.startsNewBroadcast(false, 100, 120), "no packet made since: nobody watches, or not out yet");
        assertFalse(SyncThrottle.startsNewBroadcast(true, 120, 120), "the same tick: one packet for both");
        assertTrue(SyncThrottle.startsNewBroadcast(true, 100, 120));
        assertTrue(SyncThrottle.startsNewBroadcast(true, Long.MIN_VALUE, 0), "the first broadcast after a packet");
    }

    @Test
    void manyChecksKeepAgreeingWithAFreshRead() {
        SyncThrottle throttle = SyncThrottle.forRecord(Readings.class, 0.005, 1.0e-3, Map.of());
        SyncThrottle reference = SyncThrottle.forRecord(Readings.class, 0.005, 1.0e-3, Map.of());
        for (long tick = 0; tick < 2000; tick += 10) {
            Readings now = new Readings(tick % 300 < 150, tick % 500 < 250 ? Mode.RUN : Mode.IDLE, (int) (tick / 400),
                    1000 + Math.sin(tick / 50.0) * 20, (float) (tick % 70) / 70f, 7L);
            boolean due = throttle.due(tick, now, 100);
            boolean expected = reference.due(tick, SyncThrottle.valuesOf(now), 100);
            assertTrue(due == expected, "tick " + tick + ": the reused row decides as a fresh read does");
            if (due) {
                throttle.sent(tick, () -> now);
                reference.sent(tick, SyncThrottle.valuesOf(now));
            }
        }
    }
}
