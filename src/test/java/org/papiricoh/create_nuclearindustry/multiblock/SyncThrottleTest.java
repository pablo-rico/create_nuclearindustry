package org.papiricoh.create_nuclearindustry.multiblock;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * When a controller updates its clients: on a visible change or after the forced interval, never on
 * drift a player could not see. A controller at rest used to send an update every ten ticks, twenty
 * in 200 ticks; with a forced interval of 100 it sends two.
 */
class SyncThrottleTest {
    private static final double[] STEPS = {80, 0, 0.5};
    private static final double[] NONE = {0, 0, 0};

    @Test
    void theFirstCheckSends() {
        SyncThrottle throttle = new SyncThrottle(STEPS, NONE);
        assertTrue(throttle.due(0, new double[]{0, 0, 0}, 100));
    }

    @Test
    void driftBelowTheStepsWaitsForTheForcedUpdate() {
        SyncThrottle throttle = new SyncThrottle(STEPS, NONE);
        double[] values = {4000, 1, 10};
        throttle.sent(0, values);
        int sent = 1;
        for (long tick = 10; tick <= 200; tick += 10) {
            // 79 mB of a 8000 mB tank and 0.4 of a reading: nothing a player would see.
            double[] now = {4000 + tick % 79, 1, 10 + (tick % 20 == 0 ? 0.4 : 0.0)};
            if (throttle.due(tick, now, 100)) {
                throttle.sent(tick, now);
                sent++;
            }
        }
        assertTrue(sent == 3, "the first update and one forced every 100 ticks, got " + sent);
    }

    @Test
    void aVisibleStepSendsAtOnce() {
        SyncThrottle throttle = new SyncThrottle(STEPS, NONE);
        throttle.sent(0, new double[]{4000, 1, 10});
        assertTrue(throttle.due(10, new double[]{4080, 1, 10}, 100), "1% of the tank");
        assertTrue(throttle.due(10, new double[]{4000, 2, 10}, 100), "a flag or phase, on any change");
        assertTrue(throttle.due(10, new double[]{4000, 1, 10.5}, 100), "half a unit of a reading");
        assertFalse(throttle.due(10, new double[]{4079, 1, 10.49}, 100));
    }

    @Test
    void anInvalidatedThrottleSendsWhateverTheValues() {
        SyncThrottle throttle = new SyncThrottle(STEPS, NONE);
        double[] values = {1, 1, 1};
        throttle.sent(0, values);
        assertFalse(throttle.due(10, values, 100));
        throttle.invalidate();
        assertTrue(throttle.due(10, values, 100));
    }

    @Test
    void relativeStepsScaleWithTheValue() {
        assertFalse(SyncThrottle.visiblyDifferent(1000, 1004, 0.001, 0.005), "0.4% of 1000");
        assertTrue(SyncThrottle.visiblyDifferent(1000, 1006, 0.001, 0.005), "0.6% of 1000");
        assertFalse(SyncThrottle.visiblyDifferent(0.0001, 0.0005, 0.001, 0.005), "below the minimum step");
        assertTrue(SyncThrottle.visiblyDifferent(0, 1, 0, 0), "a flag");
        assertTrue(SyncThrottle.visiblyDifferent(Double.NaN, 1, 0, 0));
        assertFalse(SyncThrottle.visiblyDifferent(Double.NaN, Double.NaN, 0, 0));
        assertTrue(SyncThrottle.visiblyDifferent(5, Double.POSITIVE_INFINITY, 0.001, 0.005), "a gain gone to ignition");
    }

    enum Phase { OFF, ON }

    record Readings(boolean formed, Phase phase, int cells, double temperature, int tank) {}

    @Test
    void recordsAreReadComponentByComponent() {
        assertArrayEquals(new double[]{1, 1, 12, 300.5, 4000},
                SyncThrottle.valuesOf(new Readings(true, Phase.ON, 12, 300.5, 4000)));
        SyncThrottle throttle = SyncThrottle.forRecord(Readings.class, 0.005, 1.0e-3, Map.of("tank", 80.0));
        throttle.sent(0, SyncThrottle.valuesOf(new Readings(true, Phase.ON, 12, 300, 4000)));
        assertFalse(throttle.due(10, SyncThrottle.valuesOf(new Readings(true, Phase.ON, 12, 301, 4079)), 100),
                "0.3% of the temperature and under 1% of the tank");
        assertTrue(throttle.due(10, SyncThrottle.valuesOf(new Readings(true, Phase.ON, 13, 300, 4000)), 100),
                "a whole number counts on any change");
        assertTrue(throttle.due(10, SyncThrottle.valuesOf(new Readings(true, Phase.OFF, 12, 300, 4000)), 100),
                "so does an enum");
        assertTrue(throttle.due(10, SyncThrottle.valuesOf(new Readings(true, Phase.ON, 12, 302, 4000)), 100),
                "and 0.7% of the temperature");
    }
}
