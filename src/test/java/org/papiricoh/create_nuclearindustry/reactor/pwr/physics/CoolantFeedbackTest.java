package org.papiricoh.create_nuclearindustry.reactor.pwr.physics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoolantFeedbackTest {
    private static double feedback(double voids, double coverage, double temperature) {
        return CoolantFeedback.multiplier(voids, coverage, temperature, 307.5, 0.6, 0.002);
    }

    @Test
    void neutralAtNormalConditions() {
        assertEquals(1.0, feedback(0, 1, 307.5), 1.0e-12);
    }

    @Test
    void steamVoidsAndAnUncoveredCoreHoldThePowerBack() {
        assertEquals(1.0 - 0.6 * 0.5, feedback(0.5, 1, 307.5), 1.0e-12);
        assertEquals(1.0 - 0.6 * 0.5, feedback(0, 0.5, 307.5), 1.0e-12);
        assertEquals(1.0 - 0.6 * 0.75, feedback(0.5, 0.5, 307.5), 1.0e-12);
    }

    @Test
    void hotterWaterLowersAndColderWaterRaisesIt() {
        assertEquals(0.98, feedback(0, 1, 317.5), 1.0e-12);
        assertEquals(1.02, feedback(0, 1, 297.5), 1.0e-12);
    }

    @Test
    void staysInsideItsBounds() {
        assertEquals(CoolantFeedback.MIN, CoolantFeedback.multiplier(1, 0, 600, 307.5, 1.0, 0.01), 0.0);
        assertEquals(CoolantFeedback.MAX, feedback(0, 1, 0), 0.0);
        assertTrue(feedback(-1, 2, 307.5) <= 1.0, "out-of-range inputs are clamped");
    }
}
