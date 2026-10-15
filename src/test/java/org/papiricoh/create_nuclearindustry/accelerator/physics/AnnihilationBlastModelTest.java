package org.papiricoh.create_nuclearindustry.accelerator.physics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Blast radius, gamma pulse and energy of an antimatter annihilation. */
class AnnihilationBlastModelTest {
    private static final AnnihilationBlastModel.Params P = AnnihilationBlastModel.Params.DEFAULT;

    @Test
    void radiusFollowsTheCubeRootWithinItsLimits() {
        assertEquals(2 + 1.2 * Math.cbrt(4), AnnihilationBlastModel.radius(P, 4), 1e-9);
        assertEquals(3.905, AnnihilationBlastModel.radius(P, 4), 1e-3, "a vial");
        assertEquals(5.810, AnnihilationBlastModel.radius(P, 32), 1e-3, "a full small trap");
        assertEquals(3, AnnihilationBlastModel.radius(P, 0.01), 1e-9, "floor");
        assertEquals(8, AnnihilationBlastModel.radius(P, 256), 1e-9, "cap");
        assertEquals(8, AnnihilationBlastModel.radius(P, 2048), 1e-9, "cap");
        assertEquals(8, AnnihilationBlastModel.radius(P, 125), 1e-9, "exactly at the cap");
    }

    @Test
    void theRadiusNeverShrinksWithMoreCharge() {
        double last = 0;
        for (double ng = 0.0001; ng < 5000; ng *= 1.3) {
            double r = AnnihilationBlastModel.radius(P, ng);
            assertTrue(r >= last);
            last = r;
        }
    }

    @Test
    void tinyChargesFizzle() {
        assertFalse(AnnihilationBlastModel.explodes(P, 0));
        assertFalse(AnnihilationBlastModel.explodes(P, 1e-6));
        assertTrue(AnnihilationBlastModel.explodes(P, 1e-3));
        assertEquals(0, AnnihilationBlastModel.radius(P, 0), 1e-9);
        assertEquals(0, AnnihilationBlastModel.radius(P, -5), 1e-9);
        assertEquals(0, AnnihilationBlastModel.centreDose(P, 1e-6), 1e-9);
        assertEquals(0, AnnihilationBlastModel.dose(P, 1e-6, 0), 1e-9);
    }

    @Test
    void thePulseReachesTwiceTheBlast() {
        assertEquals(16, AnnihilationBlastModel.pulseRadius(P, 2048), 1e-9);
        assertEquals(2 * AnnihilationBlastModel.radius(P, 4), AnnihilationBlastModel.pulseRadius(P, 4), 1e-9);
    }

    @Test
    void centreDoseGrowsWithTheChargeUpToItsCap() {
        assertEquals(100, AnnihilationBlastModel.centreDose(P, 4), 1e-9, "a vial: nausea");
        assertEquals(800, AnnihilationBlastModel.centreDose(P, 32), 1e-9, "a small trap: sickness");
        assertEquals(1000, AnnihilationBlastModel.centreDose(P, 2048), 1e-9);
    }

    @Test
    void doseFallsWithDistanceAndStopsAtThePulse() {
        double r = AnnihilationBlastModel.radius(P, 32);
        assertEquals(800, AnnihilationBlastModel.dose(P, 32, 0), 1e-9);
        assertEquals(400, AnnihilationBlastModel.dose(P, 32, r), 1e-9, "half at the blast edge");
        assertEquals(160, AnnihilationBlastModel.dose(P, 32, 2 * r), 1e-6, "a fifth at the pulse edge");
        assertEquals(0, AnnihilationBlastModel.dose(P, 32, 2 * r + 0.01), 1e-9);
        assertEquals(800, AnnihilationBlastModel.dose(P, 32, -3), 1e-9, "negative distance is the centre");
        double last = Double.MAX_VALUE;
        for (double d = 0; d <= 2 * r; d += 0.25) {
            double dose = AnnihilationBlastModel.dose(P, 32, d);
            assertTrue(dose <= last);
            last = dose;
        }
    }

    @Test
    void negligibleDosesAreDropped() {
        AnnihilationBlastModel.Params weak = new AnnihilationBlastModel.Params(2, 1.2, 3, 8, 2, 0.1, 1000, 1e-4);
        assertEquals(0, AnnihilationBlastModel.dose(weak, 4, 7), 1e-9);
    }

    @Test
    void aNanogramIsAboutFortyGramsOfTnt() {
        assertEquals(179.75, AnnihilationBlastModel.KJ_PER_NANOGRAM, 0.01);
        assertEquals(719.0, AnnihilationBlastModel.energyKj(4), 0.1);
        assertEquals(0.043, AnnihilationBlastModel.tntKg(1), 0.001);
        assertEquals(0, AnnihilationBlastModel.energyKj(-1), 1e-9);
    }
}
