package org.papiricoh.create_nuclearindustry.missile.holo;

import org.junit.jupiter.api.Test;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

class SalvoPlanTest {
    @Test void simultaneousImpactsWaitForTheLongestFlight() {
        assertArrayEquals(new int[]{100, 0, 60}, SalvoPlan.delays(new double[]{100, 200, 140}, 0, 100));
    }

    @Test void sequencePreservesRequestedImpactOrder() {
        assertArrayEquals(new int[]{80, 0, 100}, SalvoPlan.delays(new double[]{100, 200, 120}, 20, 100));
        assertArrayEquals(new int[]{0, 60, 100}, SalvoPlan.delays(new double[]{200, 160, 140}, 20, 100));
    }

    @Test void limitsApplyAfterRounding() {
        assertArrayEquals(new int[]{0, 100}, SalvoPlan.delays(new double[]{200, 100.4}, 0, 100));
        assertNull(SalvoPlan.delays(new double[]{200, 99.4}, 0, 100));
        assertNull(SalvoPlan.delays(new double[]{100, 100}, 101, 100));
    }

    @Test void rejectsInvalidTimesAndArguments() {
        for (double bad : new double[]{Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, -1})
            assertNull(SalvoPlan.delays(new double[]{10, bad}, 0, 120));
        assertNull(SalvoPlan.delays(new double[]{10}, -1, 120));
        assertNull(SalvoPlan.delays(new double[]{10}, 0, -1));
        assertArrayEquals(new int[0], SalvoPlan.delays(new double[0], 0, 0));
        assertArrayEquals(new int[]{0}, SalvoPlan.delays(new double[]{10.6}, 0, 0));
    }

    @Test void roundedImpactsStayWithinHalfATickOfTheirSlot() {
        Random random = new Random(404);
        for (int trial = 0; trial < 2000; trial++) {
            int count = 1 + random.nextInt(16), spacing = random.nextInt(601);
            double[] flights = random.doubles(count, 1, 3000).toArray();
            int[] delays = SalvoPlan.delays(flights, spacing, 12000);
            assertNotNull(delays);
            double start = 0;
            for (int i = 0; i < count; i++) start = Math.max(start, flights[i] - i * spacing);
            for (int i = 0; i < count; i++) {
                assertTrue(delays[i] >= 0);
                assertEquals(start + i * spacing, flights[i] + delays[i], 0.50000001);
            }
        }
    }
}
