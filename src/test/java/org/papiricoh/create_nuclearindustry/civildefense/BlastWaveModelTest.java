package org.papiricoh.create_nuclearindustry.civildefense;

import org.junit.jupiter.api.Test;
import org.papiricoh.create_nuclearindustry.civildefense.blast.BlastWaveModel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlastWaveModelTest {

    @Test
    void matchesKinneyGrahamReferencePoints() {
        // Kinney & Graham's table: about 10 bar at Z = 1 m/kg^(1/3), about 0.1 bar at Z = 10.
        assertEquals(1009, BlastWaveModel.overpressureAtScaled(1.0), 15);
        assertEquals(9.98, BlastWaveModel.overpressureAtScaled(10.0), 0.2);
        assertEquals(0, BlastWaveModel.overpressureAtScaled(Double.POSITIVE_INFINITY));
    }

    @Test
    void overpressureFallsWithDistance() {
        double yield = 1000;
        double previous = Double.POSITIVE_INFINITY;
        for (double r = 1; r <= 300; r *= 1.5) {
            double p = BlastWaveModel.overpressure(r, yield);
            assertTrue(p < previous, "overpressure should fall with distance, at r=" + r);
            assertTrue(p > 0, "and stay positive");
            previous = p;
        }
    }

    @Test
    void scalesWithTheCubeRootOfTheYield() {
        // Eight times the yield gives the same pressure twice as far away.
        double near = BlastWaveModel.overpressure(20, 500);
        double far = BlastWaveModel.overpressure(40, 4000);
        assertEquals(near, far, near * 1e-9);
        assertEquals(20 / Math.cbrt(500), BlastWaveModel.scaledDistance(20, 500), 1e-12);
        assertTrue(BlastWaveModel.overpressure(50, 4000) > BlastWaveModel.overpressure(50, 500), "a bigger blast hits harder at the same range");
    }

    @Test
    void noYieldNoWave() {
        assertEquals(0, BlastWaveModel.overpressure(10, 0));
        assertEquals(0, BlastWaveModel.yield(-5, 250));
        assertEquals(250 * 160, BlastWaveModel.yield(160, 250));
    }

    @Test
    void groundZeroIsClampedNotInfinite() {
        double centre = BlastWaveModel.overpressure(0, 1000);
        assertTrue(Double.isFinite(centre) && centre > BlastWaveModel.overpressure(1, 1000));
    }

    @Test
    void blocksLetLessThroughTheStrongerTheyAre() {
        assertEquals(1.0, BlastWaveModel.blockTransmission(0, 6));
        assertEquals(Math.exp(-1), BlastWaveModel.blockTransmission(6, 6), 1e-12);
        assertTrue(BlastWaveModel.blockTransmission(1200, 6) < 1e-6, "obsidian stops the wave");
        double threeStone = Math.pow(BlastWaveModel.blockTransmission(6, 6), 3);
        assertTrue(threeStone < 0.06, "three blocks of stone let about 5% through, got " + threeStone);
    }

    @Test
    void damageAndKnockbackGrowAboveTheThreshold() {
        assertEquals(0, BlastWaveModel.damage(4, 5, 0.5));
        assertEquals(5, BlastWaveModel.damage(15, 5, 0.5), 1e-12);
        assertEquals(0.2, BlastWaveModel.knockback(10, 0.02), 1e-12);
        assertEquals(BlastWaveModel.MAX_KNOCKBACK, BlastWaveModel.knockback(1.0e6, 0.02));
        assertEquals(0, BlastWaveModel.knockback(-3, 0.02));
    }

    @Test
    void reachIsFourCraterRadiiUpToTheCap() {
        assertEquals(200, BlastWaveModel.maxRadius(50, 256));
        assertEquals(256, BlastWaveModel.maxRadius(250, 256));
        assertEquals(0, BlastWaveModel.maxRadius(80, 0));
    }
}
