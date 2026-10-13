package org.papiricoh.create_nuclearindustry.reactor.control;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The column-indexed rod scan gives what the old one did: a set of filled cells, then each channel
 * counted by walking that set once per channel.
 */
class ControlRodGridTest {
    private static final double MIN_OVERLAP = 0.01;

    @Test
    void countsEachCellOnceAndSplitsPerChannelInOrder() {
        ControlRodGrid grid = new ControlRodGrid(new int[] {3, 5, 7}, new int[] {1, 1, 4}, 10, 4);
        assertTrue(grid.fill(grid.column(5, 1), 10));
        assertTrue(grid.fill(grid.column(5, 1), 11));
        assertFalse(grid.fill(grid.column(5, 1), 11), "a cell counts once");
        assertTrue(grid.fill(grid.column(7, 4), 13));
        assertFalse(grid.fill(grid.column(7, 4), 14), "above the channel");
        assertFalse(grid.fill(grid.column(7, 4), 9), "below the channel");
        assertEquals(-1, grid.column(4, 1), "not a channel");
        assertFalse(grid.fill(-1, 10));
        assertEquals(3, grid.filledCells());
        assertArrayEquals(new double[] {0.0, 0.5, 0.25}, grid.channelInsertions(), 1e-12);
    }

    @Test
    void channelsSharingAColumnShareItsCount() {
        ControlRodGrid grid = new ControlRodGrid(new int[] {2, 2}, new int[] {6, 6}, 0, 2);
        grid.fill(grid.column(2, 6), 0);
        assertEquals(1, grid.filledCells());
        assertArrayEquals(new double[] {0.5, 0.5}, grid.channelInsertions(), 1e-12);
    }

    @Test
    void negativeCoordinatesAreKeptApart() {
        ControlRodGrid grid = new ControlRodGrid(new int[] {-1, 0, -1}, new int[] {0, -1, -1}, -64, 3);
        assertEquals(0, grid.column(-1, 0));
        assertEquals(1, grid.column(0, -1));
        assertEquals(2, grid.column(-1, -1));
        assertEquals(-1, grid.column(0, 0));
    }

    /** Every cell a span overlaps by the least that counts lies between firstCell and lastCell. */
    @Test
    void candidateCellsCoverEveryOverlap() {
        Random random = new Random(7);
        for (int n = 0; n < 20_000; n++) {
            double min = (random.nextDouble() - 0.5) * 40;
            double max = min + random.nextDouble() * 3;
            if (random.nextInt(4) == 0) {
                min = Math.floor(min);
                max = min + 1;
            }
            for (int c = (int) Math.floor(min) - 3; c <= (int) Math.ceil(max) + 3; c++) {
                if (overlap(min, max, c, c + 1.0) >= MIN_OVERLAP) {
                    assertTrue(c >= ControlRodGrid.firstCell(min) && c <= ControlRodGrid.lastCell(max),
                            "cell " + c + " of [" + min + ", " + max + "]");
                }
            }
        }
    }

    /**
     * Random channels, standing rods and moving rod boxes: the grid with candidate cells finds the
     * same totals and per-channel insertions as the old scan over every channel cell.
     */
    @Test
    void matchesTheOldScanOnRandomCores() {
        Random random = new Random(42);
        for (int round = 0; round < 300; round++) {
            int bottomY = random.nextInt(20) - 10;
            int topY = bottomY + 3 + random.nextInt(9);
            int height = topY - bottomY - 1;
            List<int[]> channels = new ArrayList<>();
            Set<Long> seen = new HashSet<>();
            int channelCount = 1 + random.nextInt(12);
            while (channels.size() < channelCount) {
                int x = random.nextInt(7) - 3;
                int z = random.nextInt(7) - 3;
                if (seen.add(((long) x << 32) | (z & 0xFFFFFFFFL))) {
                    channels.add(new int[] {x, z});
                }
            }
            Set<List<Integer>> standing = new HashSet<>();
            for (int[] channel : channels) {
                for (int y = bottomY + 1; y < topY; y++) {
                    if (random.nextInt(3) == 0) {
                        standing.add(List.of(channel[0], y, channel[1]));
                    }
                }
            }
            List<double[]> rods = new ArrayList<>();
            for (int r = random.nextInt(10); r > 0; r--) {
                double x = random.nextInt(9) - 4 + (random.nextBoolean() ? 0 : random.nextDouble() * 0.04 - 0.02);
                double y = bottomY - 2 + random.nextDouble() * (height + 4);
                double z = random.nextInt(9) - 4;
                rods.add(new double[] {x, y, z, x + 1, y + 1, z + 1});
            }

            // the old scan
            Set<List<Integer>> inserted = new HashSet<>();
            int oldStatic = 0;
            for (int[] channel : channels) {
                for (int y = bottomY + 1; y < topY; y++) {
                    if (standing.contains(List.of(channel[0], y, channel[1])) && inserted.add(List.of(channel[0], y, channel[1]))) {
                        oldStatic++;
                    }
                }
            }
            int oldMoving = 0;
            for (double[] rod : rods) {
                for (int[] channel : channels) {
                    for (int y = bottomY + 1; y < topY; y++) {
                        if (overlapsCell(rod, channel[0], y, channel[1]) && inserted.add(List.of(channel[0], y, channel[1]))) {
                            oldMoving++;
                        }
                    }
                }
            }
            double[] oldChannels = new double[channels.size()];
            for (int i = 0; i < channels.size(); i++) {
                int inChannel = 0;
                for (List<Integer> pos : inserted) {
                    if (pos.get(0) == channels.get(i)[0] && pos.get(2) == channels.get(i)[1]) {
                        inChannel++;
                    }
                }
                oldChannels[i] = Math.min(1.0, inChannel / (double) height);
            }

            // the grid, as ControlRodTracker runs it
            int[] xs = channels.stream().mapToInt(c -> c[0]).toArray();
            int[] zs = channels.stream().mapToInt(c -> c[1]).toArray();
            ControlRodGrid grid = new ControlRodGrid(xs, zs, bottomY + 1, height);
            int newStatic = 0;
            for (int c = 0; c < xs.length; c++) {
                for (int y = grid.firstY(); y <= grid.lastY(); y++) {
                    if (standing.contains(List.of(xs[c], y, zs[c])) && grid.fill(grid.column(xs[c], zs[c]), y)) {
                        newStatic++;
                    }
                }
            }
            int newMoving = 0;
            for (double[] rod : rods) {
                int minY = Math.max(grid.firstY(), ControlRodGrid.firstCell(rod[1]));
                int maxY = Math.min(grid.lastY(), ControlRodGrid.lastCell(rod[4]));
                for (int x = ControlRodGrid.firstCell(rod[0]); x <= ControlRodGrid.lastCell(rod[3]); x++) {
                    for (int z = ControlRodGrid.firstCell(rod[2]); z <= ControlRodGrid.lastCell(rod[5]); z++) {
                        int column = grid.column(x, z);
                        for (int y = minY; column >= 0 && y <= maxY; y++) {
                            if (overlapsCell(rod, x, y, z) && grid.fill(column, y)) {
                                newMoving++;
                            }
                        }
                    }
                }
            }

            assertEquals(oldStatic, newStatic, "standing segments, round " + round);
            assertEquals(oldMoving, newMoving, "moving segments, round " + round);
            assertEquals(inserted.size(), grid.filledCells(), "inserted segments, round " + round);
            assertArrayEquals(oldChannels, grid.channelInsertions(), 1e-12, "per channel, round " + round);
        }
    }

    private static boolean overlapsCell(double[] box, int x, int y, int z) {
        return overlap(box[0], box[3], x, x + 1.0) >= MIN_OVERLAP
                && overlap(box[1], box[4], y, y + 1.0) >= MIN_OVERLAP
                && overlap(box[2], box[5], z, z + 1.0) >= MIN_OVERLAP;
    }

    private static double overlap(double minA, double maxA, double minB, double maxB) {
        return Math.max(0.0, Math.min(maxA, maxB) - Math.max(minA, minB));
    }
}
