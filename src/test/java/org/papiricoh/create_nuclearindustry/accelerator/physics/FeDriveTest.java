package org.papiricoh.create_nuclearindustry.accelerator.physics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Forge Energy standing in for rotation. */
class FeDriveTest {

    @Test
    void demandIsTheRotationItReplaces() {
        assertEquals(1024, FeDrive.demand(32, 128, 0.25), 1e-9, "a klystron: 32 SU/RPM at 128 RPM");
        assertEquals(0, FeDrive.demand(32, 128, 0), 1e-9);
    }

    @Test
    void receivedEnergyGivesItsShareOfDrive() {
        assertEquals(0.5, FeDrive.fromEnergy(512, 1024), 1e-9);
        assertEquals(1, FeDrive.fromEnergy(5000, 1024), 1e-9);
        assertEquals(0, FeDrive.fromEnergy(100, 0), 1e-9);
    }

    @Test
    void theBetterDriveWins() {
        assertEquals(0.8, FeDrive.drive(0.8, 0.5), 1e-9);
        assertEquals(1, FeDrive.drive(0, 1), 1e-9, "full FE with no rotation at all");
        assertEquals(0, FeDrive.drive(0, 0), 1e-9);
    }

    @Test
    void theBufferHoldsSomeTicksOfDemand() {
        assertEquals(20480, FeDrive.buffer(1024, 20));
        assertEquals(1, FeDrive.buffer(0, 20));
    }

    @Test
    void fairShareSplitsEvenlyAndPassesOnWhatAFullReceiverLeaves() {
        assertArrayEquals(new int[]{300, 300, 300}, FeDrive.fairShare(900, new int[]{1000, 1000, 1000}));
        assertArrayEquals(new int[]{100, 400, 400}, FeDrive.fairShare(900, new int[]{100, 1000, 1000}));
        assertArrayEquals(new int[]{100, 0, 200}, FeDrive.fairShare(900, new int[]{100, 0, 200}), "never more than they take");
        assertArrayEquals(new int[]{0, 0}, FeDrive.fairShare(0, new int[]{5, 5}));
        assertArrayEquals(new int[]{}, FeDrive.fairShare(100, new int[]{}));
    }

    @Test
    void fairShareHandsOutEveryLastUnit() {
        int[] given = FeDrive.fairShare(7, new int[]{10, 10, 10, 10, 10, 10});
        int sum = 0;
        for (int g : given) {
            sum += g;
            assertTrue(g == 1 || g == 2, "nobody gets more than one extra");
        }
        assertEquals(7, sum);
        int[] big = FeDrive.fairShare(Integer.MAX_VALUE, new int[]{Integer.MAX_VALUE, Integer.MAX_VALUE});
        assertEquals((long) Integer.MAX_VALUE, (long) big[0] + big[1], "no overflow");
    }
}
