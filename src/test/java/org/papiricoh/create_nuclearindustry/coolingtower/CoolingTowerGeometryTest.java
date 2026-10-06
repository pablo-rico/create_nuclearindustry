package org.papiricoh.create_nuclearindustry.coolingtower;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoolingTowerGeometryTest {
    private static final int[][] NEIGHBOURS = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};

    /** Every interior cell touches only interior or wall, except upward out of the open top. */
    @Test
    void everyProfileIsWatertight() {
        for (int rb = CoolingTowerGeometry.MIN_BASE_RADIUS; rb <= CoolingTowerGeometry.MAX_BASE_RADIUS; rb++) {
            for (int h = CoolingTowerGeometry.minHeight(rb); h <= CoolingTowerGeometry.maxHeight(rb); h++) {
                CoolingTowerGeometry g = CoolingTowerGeometry.of(rb, h);
                for (int y = 1; y < h; y++) {
                    for (int dx = -rb; dx <= rb; dx++) {
                        for (int dz = -rb; dz <= rb; dz++) {
                            if (g.classify(dx, y, dz) != CoolingTowerGeometry.Cell.INTERIOR) {
                                continue;
                            }
                            for (int[] n : NEIGHBOURS) {
                                if (y == h - 1 && n[1] == 1) {
                                    continue;
                                }
                                CoolingTowerGeometry.Cell next = g.classify(dx + n[0], y + n[1], dz + n[2]);
                                assertTrue(next != CoolingTowerGeometry.Cell.OUTSIDE,
                                        "leak at rb=" + rb + " h=" + h + " (" + dx + "," + y + "," + dz + ")");
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    void profileNarrowsToTheWaistAndFlaresBack() {
        for (int rb = CoolingTowerGeometry.MIN_BASE_RADIUS; rb <= CoolingTowerGeometry.MAX_BASE_RADIUS; rb++) {
            for (int h = CoolingTowerGeometry.minHeight(rb); h <= CoolingTowerGeometry.maxHeight(rb); h++) {
                CoolingTowerGeometry g = CoolingTowerGeometry.of(rb, h);
                int waist = CoolingTowerGeometry.waistRadius(rb);
                assertEquals(rb, g.radius(0), "base radius");
                assertEquals(waist, g.radius(CoolingTowerGeometry.waistHeight(h)), "waist radius");
                int top = g.radius(h - 1);
                assertTrue(top >= waist && top <= waist + 2, "top radius " + top + " for rb=" + rb + " h=" + h);
                for (int y = 1; y < h; y++) {
                    assertTrue(Math.abs(g.radius(y) - g.radius(y - 1)) <= 1, "radius jumps at y=" + y);
                    assertTrue(g.radius(y) >= 2, "interior too narrow");
                }
            }
        }
    }

    @Test
    void smallestTowerHasTheExpectedProfile() {
        CoolingTowerGeometry g = CoolingTowerGeometry.of(4, 12);
        int[] radii = new int[12];
        for (int y = 0; y < 12; y++) {
            radii[y] = g.radius(y);
        }
        assertArrayEquals(new int[]{4, 4, 3, 3, 3, 2, 2, 2, 2, 2, 2, 2}, radii);
        assertEquals(48, g.passiveCapacity(1.0));
    }

    @Test
    void floorIsFullAndTopIsOpen() {
        CoolingTowerGeometry g = CoolingTowerGeometry.of(6, 20);
        assertEquals(CoolingTowerGeometry.Cell.FLOOR, g.classify(0, 0, 0));
        assertEquals(CoolingTowerGeometry.Cell.FLOOR, g.classify(6, 0, 0));
        assertEquals(CoolingTowerGeometry.Cell.OUTSIDE, g.classify(7, 0, 0));
        assertEquals(CoolingTowerGeometry.Cell.INTERIOR, g.classify(0, 19, 0));
        assertEquals(CoolingTowerGeometry.Cell.OUTSIDE, g.classify(0, 20, 0));
    }

    @Test
    void sizeLimitsAreEnforced() {
        assertFalse(CoolingTowerGeometry.isValid(3, 12));
        assertFalse(CoolingTowerGeometry.isValid(4, 11));
        assertFalse(CoolingTowerGeometry.isValid(4, 21));
        assertTrue(CoolingTowerGeometry.isValid(16, 64));
        assertThrows(IllegalArgumentException.class, () -> CoolingTowerGeometry.of(17, 40));
    }
}
