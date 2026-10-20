package org.papiricoh.create_nuclearindustry.missile.physics;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Misses are a round normal spread whose median radius is the CEP, drawn the same way every time from a seed. */
class AccuracyModelTest {

    @Test
    void halfTheShotsLandWithinTheCep() {
        double cep = 40;
        int shots = 10_000;
        double[] radii = new double[shots];
        double sumX = 0, sumZ = 0;
        for (int i = 0; i < shots; i++) {
            V3 miss = AccuracyModel.miss(cep, 0x5eedL * 31 + i);
            assertEquals(0, miss.y(), "misses are horizontal");
            radii[i] = Math.hypot(miss.x(), miss.z());
            sumX += miss.x();
            sumZ += miss.z();
        }
        Arrays.sort(radii);
        double median = (radii[shots / 2 - 1] + radii[shots / 2]) / 2;
        assertEquals(cep, median, cep * 0.04, "the median miss is the CEP");
        assertTrue(Math.abs(sumX / shots) < cep * 0.05 && Math.abs(sumZ / shots) < cep * 0.05, "with no bias to any side");
    }

    @Test
    void aSeedAlwaysMissesTheSameWay() {
        assertEquals(AccuracyModel.miss(25, 1234L), AccuracyModel.miss(25, 1234L));
        assertTrue(!AccuracyModel.miss(25, 1234L).equals(AccuracyModel.miss(25, 1235L)), "neighbouring seeds miss differently");
    }

    @Test
    void perfectGuidanceNeverMisses() {
        assertEquals(V3.ZERO, AccuracyModel.miss(0, 99L));
        assertEquals(V3.ZERO, AccuracyModel.miss(-3, 99L));
    }

    @Test
    void theMissScalesWithTheCep() {
        V3 small = AccuracyModel.miss(10, 77L);
        V3 large = AccuracyModel.miss(100, 77L);
        assertEquals(small.x() * 10, large.x(), 1e-9);
        assertEquals(small.z() * 10, large.z(), 1e-9);
    }
}
