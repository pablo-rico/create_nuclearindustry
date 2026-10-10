package org.papiricoh.create_nuclearindustry.multiblock;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/**
 * Flood fill of the air around a starting cell, to tell whether it is closed off: a containment
 * building, a shelter. Walls are every cell the fill cannot pass; the fill <em>leaks</em> when it
 * grows past its cap or reaches a cell the world cannot answer for (unloaded, out of height).
 * It runs a bounded number of cells per {@link #step} so a big volume spreads over several ticks.
 * No game classes, so it can be unit tested; positions are packed with {@link #pack}.
 */
public final class SealedVolume {

    /** What the fill sees of the world. */
    public interface World {
        /** Whether the cell is loaded and inside the build height. */
        boolean known(int x, int y, int z);

        /** Whether gas passes the cell (air, open doors, anything without a full collision box). */
        boolean passable(int x, int y, int z);

        /** How much pressure a wall cell holds, in bar; only asked for cells that are not passable. */
        double wallRating(int x, int y, int z);
    }

    public enum State {
        /** Still filling: call {@link #step} again. */
        FILLING,
        /** Closed off: every wall is known. */
        SEALED,
        /** Open to the outside, or bigger than the cap. */
        LEAKED
    }

    private static final int[][] NEIGHBOURS = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};

    private final int maxVolume;
    private final Set<Long> inside = new HashSet<>();
    private final Set<Long> walls = new HashSet<>();
    private final ArrayDeque<Long> frontier = new ArrayDeque<>();
    private State state = State.FILLING;
    private double minRating = Double.POSITIVE_INFINITY;
    private long weakestWall;
    private int minX, minY, minZ, maxX, maxY, maxZ;

    public SealedVolume(int x, int y, int z, int maxVolume) {
        this.maxVolume = maxVolume;
        long start = pack(x, y, z);
        inside.add(start);
        frontier.add(start);
        minX = maxX = x;
        minY = maxY = y;
        minZ = maxZ = z;
    }

    /** Fills a whole volume at once; for small ones and tests. */
    public static SealedVolume fill(World world, int x, int y, int z, int maxVolume) {
        SealedVolume volume = new SealedVolume(x, y, z, maxVolume);
        while (volume.step(world, Integer.MAX_VALUE) == State.FILLING) {
            // keeps going until sealed or leaked
        }
        return volume;
    }

    /** Visits up to {@code budget} cells of the frontier. */
    public State step(World world, int budget) {
        if (state != State.FILLING) {
            return state;
        }
        int x0 = unpackX(frontier.peekFirst()), y0 = unpackY(frontier.peekFirst()), z0 = unpackZ(frontier.peekFirst());
        if (inside.size() == 1 && !world.passable(x0, y0, z0)) {
            // Starting inside a wall: nothing to fill.
            state = State.LEAKED;
            return state;
        }
        for (int visited = 0; visited < budget && !frontier.isEmpty(); visited++) {
            long cell = frontier.pollFirst();
            int x = unpackX(cell), y = unpackY(cell), z = unpackZ(cell);
            for (int[] d : NEIGHBOURS) {
                int nx = x + d[0], ny = y + d[1], nz = z + d[2];
                long next = pack(nx, ny, nz);
                if (inside.contains(next) || walls.contains(next)) {
                    continue;
                }
                if (!world.known(nx, ny, nz)) {
                    state = State.LEAKED;
                    return state;
                }
                if (world.passable(nx, ny, nz)) {
                    inside.add(next);
                    if (inside.size() > maxVolume) {
                        state = State.LEAKED;
                        return state;
                    }
                    frontier.addLast(next);
                    minX = Math.min(minX, nx);
                    minY = Math.min(minY, ny);
                    minZ = Math.min(minZ, nz);
                    maxX = Math.max(maxX, nx);
                    maxY = Math.max(maxY, ny);
                    maxZ = Math.max(maxZ, nz);
                } else {
                    walls.add(next);
                    double rating = world.wallRating(nx, ny, nz);
                    if (rating < minRating) {
                        minRating = rating;
                        weakestWall = next;
                    }
                }
            }
        }
        if (frontier.isEmpty()) {
            state = State.SEALED;
        }
        return state;
    }

    public State state() {
        return state;
    }

    /** Cells of gas inside. */
    public int volume() {
        return inside.size();
    }

    /** Wall cells touching the inside. */
    public int wallCount() {
        return walls.size();
    }

    /** Pressure the weakest wall holds, bar; infinite before any wall is found. */
    public double minRating() {
        return minRating;
    }

    /** Packed position of the weakest wall; only meaningful once a wall is found. */
    public long weakestWall() {
        return weakestWall;
    }

    public boolean contains(int x, int y, int z) {
        return inside.contains(pack(x, y, z));
    }

    public boolean isWall(int x, int y, int z) {
        return walls.contains(pack(x, y, z));
    }

    public Set<Long> wallCells() {
        return java.util.Collections.unmodifiableSet(walls);
    }

    public int minX() { return minX; }
    public int minY() { return minY; }
    public int minZ() { return minZ; }
    public int maxX() { return maxX; }
    public int maxY() { return maxY; }
    public int maxZ() { return maxZ; }

    // Same layout as BlockPos.asLong: 26 bits x, 26 bits z, 12 bits y.
    public static long pack(int x, int y, int z) {
        return ((long) x & 0x3FFFFFFL) << 38 | ((long) z & 0x3FFFFFFL) << 12 | ((long) y & 0xFFFL);
    }

    public static int unpackX(long packed) {
        return (int) (packed >> 38);
    }

    public static int unpackY(long packed) {
        return (int) (packed << 52 >> 52);
    }

    public static int unpackZ(long packed) {
        return (int) (packed << 26 >> 38);
    }
}
