package org.papiricoh.create_nuclearindustry.accelerator.physics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
