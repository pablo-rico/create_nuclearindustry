package org.papiricoh.create_nuclearindustry.missile.cluster;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Where a cluster warhead's bomblets come down. */
class ClusterPatternTest {

    /** Where a bomblet thrown at {@code v} from {@code height} lands, relative to under the opening, by stepping its fall. */
    private static double[] landing(double vx, double vy, double vz, double height) {
        double x = 0, y = height, z = 0;
        for (int t = 0; t < 10000 && y > 0; t++) {
            x += vx;
            y += vy;
            z += vz;
            vx *= ClusterPattern.DRAG;
            vy = vy * ClusterPattern.DRAG - ClusterPattern.GRAVITY;
            vz *= ClusterPattern.DRAG;
        }
        return new double[]{x, z};
    }

    @Test
    void theBombletsCoverTheFootprint() {
        double[] v = ClusterPattern.velocities(48, 64, 40, 0, -2, 0, 7);
        assertEquals(48 * 3, v.length);
        double furthest = 0;
        int inside = 0;
        for (int i = 0; i < 48; i++) {
            double[] at = landing(v[3 * i], v[3 * i + 1], v[3 * i + 2], 64);
            double r = Math.hypot(at[0], at[1]);
            furthest = Math.max(furthest, r);
            if (r <= 45) inside++;
        }
        assertTrue(furthest > 20, "they spread out: " + furthest);
        assertTrue(inside >= 44, "and nearly all come down within the footprint: " + inside);
    }

    @Test
    void oneOpeningAlwaysScattersTheSameWay() {
        assertArrayEquals(ClusterPattern.velocities(12, 64, 40, 1, -2, 0, 99), ClusterPattern.velocities(12, 64, 40, 1, -2, 0, 99));
        assertEquals(0, ClusterPattern.velocities(0, 64, 40, 0, 0, 0, 1).length);
    }

    @Test
    void aHigherOpeningFallsLonger() {
        assertTrue(ClusterPattern.fallTicks(128, 0) > ClusterPattern.fallTicks(32, 0));
        assertEquals(0, ClusterPattern.fallTicks(0, 0), 1e-9);
    }
}
