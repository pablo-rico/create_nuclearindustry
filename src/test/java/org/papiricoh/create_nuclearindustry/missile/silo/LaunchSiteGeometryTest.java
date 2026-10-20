package org.papiricoh.create_nuclearindustry.missile.silo;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class LaunchSiteGeometryTest {
    private static final int[][] FACINGS = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}};

    @Test
    void candidatesCoverEverySideWithLegacyFirst() {
        for (int[] facing : FACINGS) {
            int[][] candidates = LaunchSiteGeometry.candidates(facing[0], facing[1]);
            assertEquals(12, candidates.length);
            assertArrayEquals(new int[]{-2 * facing[0], -2 * facing[1]}, candidates[0]);
            Set<String> actual = new HashSet<>();
            for (int[] c : candidates) {
                assertEquals(2, c.length);
                assertEquals(2, Math.max(Math.abs(c[0]), Math.abs(c[1])));
                assertTrue(Math.min(Math.abs(c[0]), Math.abs(c[1])) <= 1);
                assertTrue(actual.add(c[0] + ":" + c[1]), "unique candidate");
            }
            for (int[] side : FACINGS) for (int lateral = -1; lateral <= 1; lateral++) {
                int x = 2 * side[0] - lateral * side[1], z = 2 * side[1] + lateral * side[0];
                assertTrue(actual.contains(x + ":" + z));
            }
        }
    }

    @Test
    void eachSideTriesItsCentreBeforeItsCorners() {
        for (int[] facing : FACINGS) {
            int[][] candidates = LaunchSiteGeometry.candidates(facing[0], facing[1]);
            for (int i = 0; i < 12; i += 3) {
                int x = candidates[i][0], z = candidates[i][1];
                assertTrue(x == 0 || z == 0);
                assertArrayEquals(new int[]{x + z / 2, z - x / 2}, candidates[i + 1]);
                assertArrayEquals(new int[]{x - z / 2, z + x / 2}, candidates[i + 2]);
            }
        }
    }

    @Test
    void portsCoverBaseUndersideAndWallRingOnly() {
        for (int maxRocket : new int[]{1, 5, 17}) {
            for (int y = -3; y <= maxRocket + 3; y++) for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) {
                boolean base = y == 0 && x >= -2 && x <= 2 && z >= -2 && z <= 2;
                boolean under = y == -1 && x >= -1 && x <= 1 && z >= -1 && z <= 1;
                boolean ring = y >= 1 && y <= maxRocket + 1 && (Math.abs(x) == 2 && Math.abs(z) <= 2 || Math.abs(z) == 2 && Math.abs(x) <= 2);
                assertEquals(base || under || ring, LaunchSiteGeometry.portOffset(x, y, z, maxRocket), x + "," + y + "," + z);
            }
        }
    }

    @Test
    void clearanceIsEightCellsPerLayerAndExcludesColumn() {
        for (int height : new int[]{0, 1, 5, 17}) {
            int count = 0;
            for (int y = -1; y <= height + 1; y++) for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) {
                boolean expected = y > 0 && y <= height && x >= -1 && x <= 1 && z >= -1 && z <= 1 && (x != 0 || z != 0);
                assertEquals(expected, LaunchSiteGeometry.clearanceOffset(x, y, z, height));
                if (expected) count++;
            }
            assertEquals(8 * height, count);
        }
    }

    @Test
    void basePartsNumberTheNinePadsRowByRowFromTheNorthWest() {
        assertEquals(0, LaunchSiteGeometry.part(-1, -1));
        assertEquals(1, LaunchSiteGeometry.part(0, -1));
        assertEquals(4, LaunchSiteGeometry.part(0, 0));
        assertEquals(5, LaunchSiteGeometry.part(1, 0));
        assertEquals(8, LaunchSiteGeometry.part(1, 1));
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++)
            if (Math.abs(x) == 2 || Math.abs(z) == 2) assertEquals(-1, LaunchSiteGeometry.part(x, z), x + "," + z);
    }
}
