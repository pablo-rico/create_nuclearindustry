package org.papiricoh.create_nuclearindustry.civildefense;

import org.junit.jupiter.api.Test;
import org.papiricoh.create_nuclearindustry.civildefense.blast.BlastShell;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The blast wave's per-tick geometry: the ring walk visits exactly the columns (and in the order)
 * of the old square loop, and far fewer of them at large radii.
 */
class BlastShellTest {

    /** The columns the old loop over the whole square kept, in its order. */
    private static List<long[]> squareLoop(double previous, double radius) {
        List<long[]> columns = new ArrayList<>();
        int outer = (int) Math.floor(radius);
        double inner2 = previous * previous;
        double outer2 = radius * radius;
        for (int dx = -outer; dx <= outer; dx++) {
            for (int dz = -outer; dz <= outer; dz++) {
                double d2 = (double) dx * dx + (double) dz * dz;
                if (d2 > outer2 || (d2 <= inner2 && previous > 0)) {
                    continue;
                }
                columns.add(new long[]{dx, dz});
            }
        }
        return columns;
    }

    private static List<long[]> ringWalk(double previous, double radius) {
        List<long[]> columns = new ArrayList<>();
        BlastShell.ringColumns(previous, radius, (dx, dz) -> {
            columns.add(new long[]{dx, dz});
            return true;
        });
        return columns;
    }

    private static void assertSameColumns(double previous, double radius) {
        List<long[]> expected = squareLoop(previous, radius);
        List<long[]> actual = ringWalk(previous, radius);
        assertEquals(expected.size(), actual.size(), "column count for " + previous + " -> " + radius);
        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i)[0], actual.get(i)[0], "dx of column " + i + " for " + previous + " -> " + radius);
            assertEquals(expected.get(i)[1], actual.get(i)[1], "dz of column " + i + " for " + previous + " -> " + radius);
        }
    }

    @Test
    void ringWalkMatchesTheSquareLoop() {
        assertSameColumns(0, 8);
        assertSameColumns(0, 0.5);
        assertSameColumns(8, 16);
        assertSameColumns(16, 24);
        assertSameColumns(5, 5.0001);
        assertSameColumns(120, 128);
        assertSameColumns(248, 256);
        assertSameColumns(7.3, 15.3);
        Random random = new Random(42);
        for (int i = 0; i < 200; i++) {
            double previous = random.nextDouble() * 60;
            double radius = previous + random.nextDouble() * 10;
            assertSameColumns(random.nextBoolean() ? previous : Math.floor(previous), radius);
        }
    }

    @Test
    void ringWalkStopsWhenAsked() {
        int[] seen = {0};
        int visited = BlastShell.ringColumns(10, 20, (dx, dz) -> ++seen[0] < 5);
        assertEquals(5, visited);
        assertEquals(5, seen[0]);
    }

    @Test
    void ringWalkLooksAtFarFewerColumnsAtLargeRadii() {
        // A wave 8 blocks a tick at 256 blocks: the old loop went over the 513 x 513 square.
        int square = 513 * 513;
        int ring = BlastShell.ringColumns(248, 256, (dx, dz) -> true);
        assertTrue(ring < square / 20, "the ring holds " + ring + " columns, the square " + square);
    }
}
