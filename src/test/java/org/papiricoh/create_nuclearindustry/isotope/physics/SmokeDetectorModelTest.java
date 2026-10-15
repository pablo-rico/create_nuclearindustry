package org.papiricoh.create_nuclearindustry.isotope.physics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.papiricoh.create_nuclearindustry.isotope.physics.SmokeDetectorModel.SmokeSource.CAMPFIRE;
import static org.papiricoh.create_nuclearindustry.isotope.physics.SmokeDetectorModel.SmokeSource.FIRE;
import static org.papiricoh.create_nuclearindustry.isotope.physics.SmokeDetectorModel.SmokeSource.LAVA;

/** The americium smoke detector: smoke from nearby fires, ion current drop and the redstone signal. */
class SmokeDetectorModelTest {
    private static final SmokeDetectorModel.Params P = SmokeDetectorModel.Params.DEFAULT;
    private static final double AM = 1.0;

    private static int signal(double smoke) {
        return SmokeDetectorModel.signal(P, smoke, AM);
    }

    @Test
    void cleanAirIsQuiet() {
        assertEquals(0, signal(0));
        assertEquals(0, SmokeDetectorModel.currentDrop(P, -1), 0.0);
        assertFalse(SmokeDetectorModel.alarm(P, 0, AM));
    }

    @Test
    void aFireAnywhereInRangeTripsIt() {
        for (int d = 0; d <= P.radius(); d++) {
            double smoke = SmokeDetectorModel.contribution(P, FIRE, d);
            assertTrue(signal(smoke) > 0, "fire at " + d);
        }
        assertEquals(0, SmokeDetectorModel.contribution(P, FIRE, P.radius() + 0.5), 0.0, "out of range");
        assertEquals(0, SmokeDetectorModel.contribution(P, FIRE, -1), 0.0);
        assertEquals(0, SmokeDetectorModel.contribution(P, FIRE, Double.NaN), 0.0);
    }

    @Test
    void closerFiresGiveAStrongerSignal() {
        int previous = 16;
        for (int d = 0; d <= P.radius(); d++) {
            int now = signal(SmokeDetectorModel.contribution(P, FIRE, d));
            assertTrue(now <= previous, "at " + d);
            previous = now;
        }
        assertTrue(signal(SmokeDetectorModel.contribution(P, FIRE, 1)) > signal(SmokeDetectorModel.contribution(P, FIRE, 5)));
    }

    @Test
    void lavaOnlyCountsCloseBy() {
        assertTrue(signal(SmokeDetectorModel.contribution(P, LAVA, 3)) > 0, "lava three blocks away");
        assertEquals(0, signal(SmokeDetectorModel.contribution(P, LAVA, 5)), "lava at the edge is not enough");
        assertTrue(signal(SmokeDetectorModel.contribution(P, CAMPFIRE, 4)) > 0, "a campfire's smoke");
    }

    @Test
    void manyFiresSaturateAtFifteen() {
        double smoke = 0;
        for (int i = 0; i < 40; i++) {
            smoke += SmokeDetectorModel.contribution(P, FIRE, 1);
        }
        assertEquals(15, signal(smoke));
        assertTrue(SmokeDetectorModel.currentDrop(P, smoke) < 1, "the current never quite stops");
    }

    @Test
    void halfDropAtTheHalfDropSmoke() {
        assertEquals(0.5, SmokeDetectorModel.currentDrop(P, P.halfDropSmoke()), 1e-12);
    }

    @Test
    void aDeadSourceGivesNothing() {
        double smoke = SmokeDetectorModel.contribution(P, FIRE, 0);
        assertEquals(0, SmokeDetectorModel.signal(P, smoke, 0.01));
        assertFalse(SmokeDetectorModel.alive(P, 0.01));
        // Americium-241 decays too slowly for that ever to happen in a game.
        assertTrue(SmokeDetectorModel.alive(P, IsotopeDecay.activity(1, Isotope.AMERICIUM_241.halfLifeDays(), 3650)));
    }

    @Test
    void signalIsAlwaysInRange() {
        for (double s = 0; s < 50; s += 0.37) {
            int signal = signal(s);
            assertTrue(signal >= 0 && signal <= 15);
        }
    }
}
