package org.papiricoh.create_nuclearindustry.civildefense;

import org.junit.jupiter.api.Test;
import org.papiricoh.create_nuclearindustry.civildefense.shelter.ShelterCertification;
import org.papiricoh.create_nuclearindustry.civildefense.shelter.ShelterCertification.Status;
import org.papiricoh.create_nuclearindustry.multiblock.SealedVolume;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShelterCertificationTest {

    private static Status sealed(double resistance, double transmission) {
        return ShelterCertification.evaluate(SealedVolume.State.SEALED, 27, 4096, resistance, transmission, 1.5, 0.9);
    }

    @Test
    void aSealedRoomWithSolidWallsIsCertified() {
        assertEquals(Status.CERTIFIED, sealed(6, 0.9));
        assertTrue(sealed(6, (float) 0.9).certified(), "a transmission read back from a float still passes");
    }

    @Test
    void weakOrLeakyWallsFail() {
        assertEquals(Status.WEAK_WALL, sealed(0.3, 1.0), "glass fails on strength first");
        assertEquals(Status.LEAKY_WALL, sealed(6, 1.0), "a sturdy block that does not shield");
        assertFalse(sealed(1.4, 0.2).certified());
    }

    @Test
    void openOrOversizedRoomsAreTold() {
        assertEquals(Status.OPEN, ShelterCertification.evaluate(SealedVolume.State.LEAKED, 30, 4096, 6, 0.9, 1.5, 0.9));
        assertEquals(Status.TOO_BIG, ShelterCertification.evaluate(SealedVolume.State.LEAKED, 4097, 4096, 6, 0.9, 1.5, 0.9));
        assertEquals(Status.SCANNING, ShelterCertification.evaluate(SealedVolume.State.FILLING, 10, 4096, 6, 0.9, 1.5, 0.9));
    }

    @Test
    void worksWithTheFloodFill() {
        // A 3x3x3 room inside a 5x5x5 shell, with and without a hole in it.
        assertEquals(Status.CERTIFIED, survey(-9));
        assertEquals(Status.OPEN, survey(2));
    }

    private static Status survey(int holeZ) {
        SealedVolume.World world = new SealedVolume.World() {
            public boolean known(int x, int y, int z) {
                // Just past the shell the world is unloaded, so air that gets out is lost at once.
                return x >= -1 && x <= 5 && y >= -1 && y <= 5 && z >= -1 && z <= 5;
            }

            public boolean passable(int x, int y, int z) {
                boolean shell = (x == 0 || x == 4 || y == 0 || y == 4 || z == 0 || z == 4)
                        && x >= 0 && x <= 4 && y >= 0 && y <= 4 && z >= 0 && z <= 4;
                return !shell || (x == 0 && y == 2 && z == holeZ);
            }

            public double wallRating(int x, int y, int z) {
                return 6;
            }
        };
        SealedVolume room = SealedVolume.fill(world, 2, 2, 2, 4096);
        return ShelterCertification.evaluate(room.state(), room.volume(), 4096, room.minRating(), 0.9, 1.5, 0.9);
    }
}
