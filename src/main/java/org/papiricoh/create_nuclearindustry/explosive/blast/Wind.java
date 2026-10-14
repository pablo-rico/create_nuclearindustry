package org.papiricoh.create_nuclearindustry.explosive.blast;

/**
 * Which way the wind blows in a world, for where fallout drifts: a direction drawn from the world's seed
 * for each day, turning smoothly from one day's to the next's. The same for every player and every
 * restart. No game classes.
 */
public final class Wind {
    public static final long DAY = 24000;

    private Wind() {}

    /** The direction the wind blows towards, radians (0 along +x, π/2 along +z). */
    public static double angle(long seed, long gameTime) {
        long day = Math.floorDiv(gameTime, DAY);
        double t = Math.floorMod(gameTime, DAY) / (double) DAY;
        double from = dayAngle(seed, day);
        double to = dayAngle(seed, day + 1);
        double turn = Math.atan2(Math.sin(to - from), Math.cos(to - from));
        double smooth = t * t * (3 - 2 * t);
        return from + turn * smooth;
    }

    private static double dayAngle(long seed, long day) {
        return BlastHash.unit((int) seed ^ (int) (seed >>> 32), (int) day, 97) * 2 * Math.PI;
    }
}
