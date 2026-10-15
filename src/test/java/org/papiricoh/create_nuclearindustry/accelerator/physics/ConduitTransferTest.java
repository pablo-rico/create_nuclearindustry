package org.papiricoh.create_nuclearindustry.accelerator.physics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The magnetic conduit's power demand and how it moves antimatter. */
class ConduitTransferTest {
    private static final ConduitTransfer.Params P = ConduitTransfer.Params.DEFAULT;

    @Test
    void everySegmentNeedsPower() {
        assertEquals(40, ConduitTransfer.demand(P, 10));
        assertEquals(0, ConduitTransfer.demand(P, -1));
        assertTrue(ConduitTransfer.flows(P, 10, true));
        assertFalse(ConduitTransfer.flows(P, 10, false), "one dark segment stops it all");
        assertFalse(ConduitTransfer.flows(P, 0, true));
        assertFalse(ConduitTransfer.flows(P, P.maxSegments() + 1, true), "too long");
    }

    @Test
    void moveTakesTheLeastAndSkipsCrumbs() {
        assertEquals(0.5, ConduitTransfer.move(0.5, 10, 10), 1e-12);
        assertEquals(0.2, ConduitTransfer.move(0.5, 0.2, 10), 1e-12);
        assertEquals(0.1, ConduitTransfer.move(0.5, 10, 0.1), 1e-12);
        assertEquals(0, ConduitTransfer.move(0.5, 1e-9, 10), 1e-12);
        assertEquals(0, ConduitTransfer.move(0.5, -1, 10), 1e-12);
        assertEquals(0, ConduitTransfer.move(-1, 10, 10), 1e-12);
    }

    @Test
    void planSharesBySupplyAndRoom() {
        ConduitTransfer.Plan plan = ConduitTransfer.plan(0.5, new double[]{3, 1}, new double[]{10, 30});
        assertEquals(0.5, plan.total(), 1e-12);
        assertArrayEquals(new double[]{0.375, 0.125}, plan.extract(), 1e-12);
        assertArrayEquals(new double[]{0.125, 0.375}, plan.insert(), 1e-12);
    }

    @Test
    void planConservesAndNeverOverfills() {
        ConduitTransfer.Plan plan = ConduitTransfer.plan(5, new double[]{2, 2}, new double[]{0.3, 0, -1});
        assertEquals(0.3, plan.total(), 1e-12);
        assertEquals(plan.total(), plan.extract()[0] + plan.extract()[1], 1e-12);
        assertArrayEquals(new double[]{0.3, 0, 0}, plan.insert(), 1e-12);
    }

    @Test
    void planWithNothingToMove() {
        ConduitTransfer.Plan plan = ConduitTransfer.plan(0.5, new double[]{0}, new double[]{10});
        assertEquals(0, plan.total(), 1e-12);
        assertArrayEquals(new double[]{0}, plan.extract(), 1e-12);
        assertEquals(0, ConduitTransfer.plan(0.5, new double[0], new double[0]).total(), 1e-12);
    }

    @Test
    void balanceMovesTowardsAnEqualFill() {
        // A full small trap and an empty medium one: mean fill 32 / 288.
        double[] delta = ConduitTransfer.balance(0.5, new double[]{32, 0}, new double[]{32, 256});
        assertArrayEquals(new double[]{-0.5, 0.5}, delta, 1e-12);
    }

    @Test
    void balanceConservesAndStopsAtTheMean() {
        double[] stored = {32, 0, 10};
        double[] capacity = {32, 256, 32};
        for (int i = 0; i < 10000; i++) {
            double[] delta = ConduitTransfer.balance(0.5, stored, capacity);
            double sum = 0;
            for (int j = 0; j < stored.length; j++) {
                stored[j] += delta[j];
                sum += delta[j];
                assertTrue(stored[j] >= -1e-9 && stored[j] <= capacity[j] + 1e-9);
            }
            assertEquals(0, sum, 1e-9);
        }
        double mean = 42.0 / 320.0;
        for (int j = 0; j < stored.length; j++) {
            assertEquals(mean, stored[j] / capacity[j], 1e-6);
        }
        assertArrayEquals(new double[3], ConduitTransfer.balance(0.5, stored, capacity), 1e-12, "settled");
    }

    @Test
    void balanceWithoutCapacityDoesNothing() {
        assertArrayEquals(new double[]{0, 0}, ConduitTransfer.balance(0.5, new double[]{1, 0}, new double[]{0, 0}), 1e-12);
    }
}
