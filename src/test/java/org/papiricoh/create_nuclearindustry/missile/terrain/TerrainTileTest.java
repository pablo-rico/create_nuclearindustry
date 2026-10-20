package org.papiricoh.create_nuclearindustry.missile.terrain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The terrain grid: keys, levels of detail up to an intercontinental flight's, ranges and the sample layout. */
class TerrainTileTest {

    @Test
    void keysRoundTripAtEveryLevelOfDetail() {
        for (int lod = 0; lod < TerrainTile.LODS; lod++) {
            for (int[] t : new int[][]{{0, 0}, {-1, 5}, {123456, -98765}, {-500000, -500000}}) {
                long key = TerrainTile.key(lod, t[0], t[1]);
                assertEquals(lod, TerrainTile.lod(key));
                assertEquals(t[0], TerrainTile.tx(key));
                assertEquals(t[1], TerrainTile.tz(key));
            }
        }
    }

    @Test
    void tilesGrowFourfoldPerLevel() {
        assertEquals(512, TerrainTile.span(0));
        assertEquals(2048, TerrainTile.span(1));
        assertEquals(8192, TerrainTile.span(2));
        assertEquals(32768, TerrainTile.span(3));
        assertEquals(-1, TerrainTile.tileOf(0, -1));
        assertEquals(0, TerrainTile.tileOf(0, 511));
        assertEquals(1, TerrainTile.tileOf(0, 512));
        assertEquals(-8192, TerrainTile.origin(2, -1));
    }

    @Test
    void theLevelOfDetailFollowsTheZoom() {
        assertEquals(0, TerrainTile.pickLod(1));
        assertEquals(1, TerrainTile.pickLod(8));
        assertEquals(2, TerrainTile.pickLod(32));
        assertEquals(3, TerrainTile.pickLod(200));
    }

    @Test
    void requestsStayWithinRange() {
        assertTrue(TerrainTile.inRange(TerrainTile.key(0, 0, 0), 100, 100, 1000));
        assertFalse(TerrainTile.inRange(TerrainTile.key(0, 100, 0), 0, 0, 1000), "51 km off");
        assertTrue(TerrainTile.inRange(TerrainTile.key(3, 0, 0), 30000, 0, TerrainTile.RANGE), "a big tile reaches far");
        assertFalse(TerrainTile.inRange(TerrainTile.key(0, 0, 0) | (7L << 60), 0, 0, 1000), "an unknown level of detail");
    }

    @Test
    void coveringListsEveryTileOfTheBox() {
        long[] keys = TerrainTile.covering(0, -10, -10, 600, 10);
        assertArrayEquals(new long[]{TerrainTile.key(0, -1, -1), TerrainTile.key(0, 0, -1), TerrainTile.key(0, 1, -1),
                TerrainTile.key(0, -1, 0), TerrainTile.key(0, 0, 0), TerrainTile.key(0, 1, 0)}, keys);
    }

    @Test
    void samplesKeepHeightAndSurface() {
        byte[] data = new byte[TerrainTile.BYTES];
        TerrainTile.set(data, 3, 64, -59, TerrainTile.surface(TerrainKind.SNOW, true, true));
        TerrainTile.set(data, 64, 0, 319, TerrainTile.surface(TerrainKind.DEEP_OCEAN, false, false));
        assertEquals(-59, TerrainTile.height(data, 3, 64));
        assertEquals(319, TerrainTile.height(data, 64, 0));
        int surface = TerrainTile.surface(data, 3, 64);
        assertEquals(TerrainKind.SNOW, TerrainKind.of(surface & TerrainTile.KIND_MASK));
        assertTrue((surface & TerrainTile.WATER) != 0 && (surface & TerrainTile.REAL) != 0);
        assertEquals(TerrainKind.OTHER, TerrainKind.of(99));
    }
}
