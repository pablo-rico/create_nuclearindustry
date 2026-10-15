package org.papiricoh.create_nuclearindustry.isotope.physics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Cobalt-59 in the reactor's irradiation channel: fluence, saturation and finishing a target. */
class NeutronActivationTest {
    private static final NeutronActivation.Params P = NeutronActivation.Params.DEFAULT;
    private static final double RATED = 1000.0;

    @Test
    void fluenceIsNeutronLevelTimesTicks() {
        assertEquals(5000, NeutronActivation.fluenceAfter(0, 1000, 5), 1e-9);
        assertEquals(7000, NeutronActivation.fluenceAfter(5000, 400, 5), 1e-9);
        assertEquals(5000, NeutronActivation.fluenceAfter(5000, -10, 5), 1e-9, "no negative flux");
        assertEquals(5000, NeutronActivation.fluenceAfter(5000, 1000, -5), 1e-9);
        assertEquals(0, NeutronActivation.fluenceAfter(-5, 0, 1), 1e-9);
    }

    @Test
    void activationSaturates() {
        assertEquals(0, NeutronActivation.activated(P, 0), 1e-12);
        assertEquals(1 - Math.exp(-1), NeutronActivation.activated(P, P.fluenceScale()), 1e-12);
        double previousGain = Double.MAX_VALUE;
        double previous = 0;
        for (int i = 1; i <= 10; i++) {
            double now = NeutronActivation.activated(P, i * P.fluenceScale() / 2);
            assertTrue(now > previous && now < 1);
            assertTrue(now - previous < previousGain, "diminishing returns");
            previousGain = now - previous;
            previous = now;
        }
    }

    @Test
    void aTargetTakesAQuarterDayAtRatedPower() {
        assertEquals(6.0e6, NeutronActivation.requiredFluence(P), 1e-3);
        assertEquals(6000, NeutronActivation.ticksLeft(P, 0, RATED));
        assertEquals(3000, NeutronActivation.ticksLeft(P, 0, 2 * RATED), "twice the power, half the time");
        assertEquals(Long.MAX_VALUE, NeutronActivation.ticksLeft(P, 0, 0), "a shut-down reactor activates nothing");
        assertEquals(0, NeutronActivation.ticksLeft(P, 7e6, 0));
    }

    @Test
    void steppingTickByTickFinishesOnTime() {
        double fluence = 0;
        int ticks = 0;
        while (!NeutronActivation.done(P, fluence)) {
            fluence = NeutronActivation.fluenceAfter(fluence, RATED, 1);
            ticks++;
            assertTrue(ticks <= 6001);
        }
        assertEquals(6000, ticks);
        assertEquals(1.0, NeutronActivation.outputActivity(P, fluence), 1e-6, "a finished target is nominal");
        assertEquals(0.8, NeutronActivation.activated(P, fluence), 1e-6);
    }

    @Test
    void progressIsLinearInFluence() {
        double need = NeutronActivation.requiredFluence(P);
        assertEquals(0, NeutronActivation.progress(P, 0), 1e-12);
        assertEquals(0.5, NeutronActivation.progress(P, need / 2), 1e-12);
        assertEquals(1, NeutronActivation.progress(P, 2 * need), 1e-12, "capped");
        assertFalse(NeutronActivation.done(P, need * 0.99));
        assertTrue(NeutronActivation.done(P, need));
    }

    @Test
    void overshootIsBoundedAndCarried() {
        double need = NeutronActivation.requiredFluence(P);
        double over = NeutronActivation.outputActivity(P, need * 10);
        assertTrue(over > 1 && over <= 1 / P.targetActivity() + 1e-12, "" + over);
        assertEquals(500, NeutronActivation.carry(P, need + 500), 1e-6);
        assertEquals(1234, NeutronActivation.carry(P, 1234), 1e-9, "unfinished keeps its fluence");
    }

    @Test
    void degenerateParams() {
        NeutronActivation.Params instant = new NeutronActivation.Params(0, 0.8);
        assertEquals(0, NeutronActivation.requiredFluence(instant), 1e-12);
        assertTrue(NeutronActivation.done(instant, 0));
        assertEquals(1, NeutronActivation.progress(instant, 0), 1e-12);
        assertEquals(1, NeutronActivation.activated(instant, 1), 1e-12);
        NeutronActivation.Params noTarget = new NeutronActivation.Params(1e6, 0);
        assertEquals(1, NeutronActivation.outputActivity(noTarget, 0), 1e-12);
        NeutronActivation.Params full = new NeutronActivation.Params(1e6, 1.0);
        assertTrue(Double.isFinite(NeutronActivation.requiredFluence(full)), "a target of 100 % is clamped, not infinite");
    }
}
