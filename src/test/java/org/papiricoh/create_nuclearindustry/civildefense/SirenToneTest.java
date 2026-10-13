package org.papiricoh.create_nuclearindustry.civildefense;

import org.junit.jupiter.api.Test;
import org.papiricoh.create_nuclearindustry.civildefense.siren.SirenTone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SirenToneTest {

    @Test
    void wailRisesThenFallsWithinTheEngineRange() {
        int period = SirenTone.ALERT_PERIOD;
        float previous = SirenTone.wail(0, period);
        assertEquals(SirenTone.LOW_PITCH, previous, 1e-6, "starts low");
        float peak = previous;
        for (int tick = 1; tick < period; tick++) {
            float pitch = SirenTone.wail(tick, period);
            assertTrue(pitch >= 0.5f && pitch <= 2.0f, "the sound engine clamps to 0.5..2, got " + pitch);
            if (tick <= period * 0.6) {
                assertTrue(pitch >= previous, "rising at tick " + tick);
            } else {
                assertTrue(pitch <= previous, "falling at tick " + tick);
            }
            peak = Math.max(peak, pitch);
            previous = pitch;
        }
        assertEquals(SirenTone.HIGH_PITCH, peak, 1e-3, "reaches the top");
        assertEquals(SirenTone.wail(10, period), SirenTone.wail(10 + period, period), 1e-6, "repeats");
    }

    @Test
    void crankPitchRisesWithSpeed() {
        assertTrue(SirenTone.crank(64) > SirenTone.crank(32));
        assertEquals(SirenTone.crank(-128), SirenTone.crank(128), "either direction");
        assertEquals(SirenTone.HIGH_PITCH, SirenTone.crank(1000), 1e-6, "flat past 256 RPM");
    }
}
