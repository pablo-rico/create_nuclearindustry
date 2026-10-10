package org.papiricoh.create_nuclearindustry.multiblock;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SealedVolumeTest {

    /** A hollow box of walls from 0 to size-1 on every axis, rating 6 except one weaker cell. */
    private static SealedVolume.World box(int size, int holeX, int holeY, int holeZ) {
        return new SealedVolume.World() {
            public boolean known(int x, int y, int z) {
                return y >= -64 && y < 320;
            }

            public boolean passable(int x, int y, int z) {
                boolean shell = x == 0 || y == 0 || z == 0 || x == size - 1 || y == size - 1 || z == size - 1;
                boolean outside = x < 0 || y < 0 || z < 0 || x >= size || y >= size || z >= size;
                return outside || !shell || (x == holeX && y == holeY && z == holeZ);
            }

            public double wallRating(int x, int y, int z) {
                return x == 0 && y == 2 && z == 2 ? 1.5 : 6.0;
            }
        };
    }

    @Test
    void aClosedBoxIsSealedWithItsInteriorVolume() {
        SealedVolume volume = SealedVolume.fill(box(5, -9, -9, -9), 2, 2, 2, 1000);
        assertEquals(SealedVolume.State.SEALED, volume.state());
        assertEquals(27, volume.volume());
        assertEquals(54, volume.wallCount());
        assertEquals(1.5, volume.minRating(), 1e-9);
        long weakest = volume.weakestWall();
        assertEquals(0, SealedVolume.unpackX(weakest));
        assertEquals(2, SealedVolume.unpackY(weakest));
        assertEquals(2, SealedVolume.unpackZ(weakest));
    }

    @Test
    void aHoleLetsTheFillOutUntilTheCap() {
        SealedVolume volume = SealedVolume.fill(box(5, 4, 2, 2), 2, 2, 2, 500);
        assertEquals(SealedVolume.State.LEAKED, volume.state());
    }

    @Test
    void anUnknownCellLeaks() {
        SealedVolume.World world = new SealedVolume.World() {
            public boolean known(int x, int y, int z) {
                return x < 3;
            }

            public boolean passable(int x, int y, int z) {
                return true;
            }

            public double wallRating(int x, int y, int z) {
                return 1;
            }
        };
        assertEquals(SealedVolume.State.LEAKED, SealedVolume.fill(world, 0, 0, 0, 10_000).state());
    }

    @Test
    void stepsSpreadTheFillOverSeveralCalls() {
        SealedVolume volume = new SealedVolume(5, 5, 5, 10_000);
        SealedVolume.World world = box(12, -9, -9, -9);
        int steps = 0;
        while (volume.step(world, 50) == SealedVolume.State.FILLING) {
            steps++;
        }
        assertTrue(steps > 5, "a 1000-cell room takes several 50-cell steps");
        assertEquals(SealedVolume.State.SEALED, volume.state());
        assertEquals(1000, volume.volume());
        assertTrue(volume.contains(1, 1, 1));
        assertFalse(volume.contains(0, 1, 1));
        assertTrue(volume.isWall(0, 1, 1));
    }

    @Test
    void packingRoundTripsNegativeCoordinates() {
        long packed = SealedVolume.pack(-1234, -60, 98765);
        assertEquals(-1234, SealedVolume.unpackX(packed));
        assertEquals(-60, SealedVolume.unpackY(packed));
        assertEquals(98765, SealedVolume.unpackZ(packed));
    }
}
