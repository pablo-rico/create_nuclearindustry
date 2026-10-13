package org.papiricoh.create_nuclearindustry.civildefense.blast;

/**
 * Geometry of the shell a blast wave sweeps in one tick, from radius {@code inner} to {@code outer}
 * around a centre, without game classes so it can be tested on its own. The wave used to look at
 * every column of the square around it for fragile blocks; at a radius of 256 blocks that is a
 * 513x513 square of columns for a ring a few blocks wide. This gives only the ring's columns.
 */
public final class BlastShell {
    private BlastShell() {}

    /** Receives the columns of a ring, as offsets from the centre column. */
    @FunctionalInterface
    public interface ColumnVisitor {
        /** Returns false to stop the walk. */
        boolean visit(int dx, int dz);
    }

    /**
     * Visits the columns at horizontal offsets with {@code inner² < dx² + dz² <= outer²} (every one
     * within {@code outer} when {@code inner} is 0), in the order of a loop over dx then dz, both
     * ascending, over the whole square: the same columns, in the same order, without the ones
     * inside the inner circle or outside the outer one.
     *
     * @return the number of columns visited
     */
    public static int ringColumns(double inner, double outer, ColumnVisitor visitor) {
        int reach = (int) Math.floor(outer);
        double inner2 = inner * inner;
        double outer2 = outer * outer;
        int visited = 0;
        for (int dx = -reach; dx <= reach; dx++) {
            double rest = outer2 - (double) dx * dx;
            if (rest < 0) {
                continue;
            }
            int zOut = widest(rest);
            // Columns with dz² <= inner² - dx² are inside the inner circle (only once the wave has left the centre).
            int zIn = inner > 0 && inner2 - (double) dx * dx >= 0 ? widest(inner2 - (double) dx * dx) : -1;
            int firstEnd = zIn < 0 ? zOut : Math.min(zOut, -zIn - 1);
            for (int dz = -zOut; dz <= firstEnd; dz++) {
                visited++;
                if (!visitor.visit(dx, dz)) {
                    return visited;
                }
            }
            if (zIn >= 0) {
                for (int dz = Math.max(zIn + 1, -zOut); dz <= zOut; dz++) {
                    visited++;
                    if (!visitor.visit(dx, dz)) {
                        return visited;
                    }
                }
            }
        }
        return visited;
    }

    /** The largest whole number {@code n >= 0} with {@code n * n <= limit}, exact despite rounding. */
    static int widest(double limit) {
        int n = (int) Math.floor(Math.sqrt(limit));
        while (n > 0 && (double) n * n > limit) {
            n--;
        }
        while ((double) (n + 1) * (n + 1) <= limit) {
            n++;
        }
        return n;
    }
}
