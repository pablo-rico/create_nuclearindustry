package org.papiricoh.create_nuclearindustry.missile.fx;

import java.util.Arrays;

/**
 * What the rocket effects may spend so a sky full of launches never drags a client down: a cap on the particles alive at
 * once shared fairly between the flights that want them, a cap on how many flights draw a trail (the nearest), and the detail
 * of each flight by its distance. Free of game classes, for JUnit.
 */
public final class FxBudget {
    /** Rocket particles alive at once, at most. */
    public static final int MAX_PARTICLES = 2000;
    /** Flights drawing a smoke trail at once, at most (the nearest). */
    public static final int MAX_TRAILS = 16;
    /** Within this many blocks a flight is drawn in full: every plume layer, smoke and embers. */
    public static final double NEAR = 192;
    /** Within this many, a simpler plume and a thinner smoke; further, only its glow, its outline and its trail. */
    public static final double MID = 768;

    /** How much of a flight is drawn. */
    public enum Lod {
        NEAR, MID, FAR;

        public boolean particles() {
            return this != FAR;
        }
    }

    private FxBudget() {
    }

    public static Lod lod(double distance) {
        return distance <= NEAR ? Lod.NEAR : distance <= MID ? Lod.MID : Lod.FAR;
    }

    /**
     * How many particles one of {@code wanting} flights may add this tick when it would like {@code wanted}, with {@code alive}
     * already alive: an even share of what is left under the cap, and never past it.
     */
    public static int quota(int wanted, int alive, int wanting) {
        int left = MAX_PARTICLES - alive;
        if (left <= 0 || wanted <= 0) return 0;
        int share = Math.max(1, left / Math.max(1, wanting));
        return Math.min(wanted, share);
    }

    /**
     * Which of the flights at {@code distances} draw a trail: the {@link #MAX_TRAILS} nearest. Returns a mask by index.
     */
    public static boolean[] trailed(double[] distances) {
        boolean[] keep = new boolean[distances.length];
        if (distances.length <= MAX_TRAILS) {
            Arrays.fill(keep, true);
            return keep;
        }
        Integer[] order = new Integer[distances.length];
        for (int i = 0; i < order.length; i++) order[i] = i;
        Arrays.sort(order, (a, b) -> Double.compare(distances[a], distances[b]));
        for (int i = 0; i < MAX_TRAILS; i++) keep[order[i]] = true;
        return keep;
    }

    /**
     * How far from the camera something at {@code distance} blocks is drawn, so it is never cut off by the far plane at
     * {@code limit}: anything nearer stays put, anything further is pulled in along its line of sight to {@code limit}. Seen
     * from the camera it is the same size in the same place; only its depth changes.
     */
    public static double pullIn(double distance, double limit) {
        return distance <= limit || distance <= 0 ? 1 : limit / distance;
    }
}
