package org.papiricoh.create_nuclearindustry.civildefense;

import org.junit.jupiter.api.Test;
import org.papiricoh.create_nuclearindustry.civildefense.blastdoor.BlastDoorTravel;
import org.papiricoh.create_nuclearindustry.civildefense.blastdoor.BlastDoorTravel.Cell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlastDoorTravelTest {

    @Test
    void fasterShaftsMoveTheDoorFaster() {
        double slow = BlastDoorTravel.rotationRate(16, 2.0);
        double fast = BlastDoorTravel.rotationRate(128, 2.0);
        assertEquals(fast, slow * 8, 1e-12);
        assertEquals(BlastDoorTravel.rotationRate(-64, 2.0), BlastDoorTravel.rotationRate(64, 2.0), "direction does not change speed");
        assertEquals(64 / 60.0 / 20.0 * 2.0, BlastDoorTravel.rotationRate(64, 2.0), 1e-12);
        assertEquals(0.1, BlastDoorTravel.redstoneRate(10), 1e-12);
        assertEquals(1.0, BlastDoorTravel.redstoneRate(0), "at least a tick per block");
    }

    @Test
    void stepStaysWithinTheDoor() {
        assertEquals(3.0, BlastDoorTravel.step(2.9, 3, 0.5, true));
        assertEquals(0.0, BlastDoorTravel.step(0.2, 3, 0.5, false));
        assertEquals(1.5, BlastDoorTravel.step(1.0, 3, 0.5, true), 1e-12);
    }

    @Test
    void shutDoorIsClosedEverywhere() {
        for (int column = 0; column < 4; column++) {
            assertEquals(Cell.CLOSED, BlastDoorTravel.cell(column, 4, 0));
        }
    }

    @Test
    void columnsFarFromThePocketOpenFirst() {
        int width = 3;
        // Half a block in: everything is covered by the moving panel.
        assertEquals(Cell.MOVING, BlastDoorTravel.cell(2, width, 0.5));
        // One block in: the far column is clear.
        assertEquals(Cell.OPEN, BlastDoorTravel.cell(2, width, 1.0));
        assertEquals(Cell.MOVING, BlastDoorTravel.cell(1, width, 1.0));
        assertEquals(Cell.MOVING, BlastDoorTravel.cell(0, width, 1.0));
        // Fully retracted: all open.
        for (int column = 0; column < width; column++) {
            assertEquals(Cell.OPEN, BlastDoorTravel.cell(column, width, width));
        }
    }

    @Test
    void comparatorFollowsTheOpening() {
        assertEquals(0, BlastDoorTravel.comparatorSignal(0, 2));
        assertEquals(15, BlastDoorTravel.comparatorSignal(2, 2));
        assertTrue(BlastDoorTravel.comparatorSignal(0.01, 4) >= 1, "any opening is a signal");
        assertEquals(8, BlastDoorTravel.comparatorSignal(2, 4));
    }
}
