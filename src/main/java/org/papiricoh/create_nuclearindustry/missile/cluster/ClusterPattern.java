package org.papiricoh.create_nuclearindustry.missile.cluster;

import java.util.SplittableRandom;

/**
 * Where a cluster warhead's bomblets go: each is thrown out of the opening casing at its own angle and speed, so they fall over
 * a disc round the warhead's path, denser towards its middle, as a real dispenser's pattern is. Seeded, so one opening always
 * scatters the same way. No game classes, so it is tested on its own.
 */
public final class ClusterPattern {
    /** Gravity on a falling bomblet, blocks per tick squared (a thrown item's). */
    public static final double GRAVITY = 0.03;
    /** What the air takes off a bomblet's speed each tick. */
    public static final double DRAG = 0.99;

    private ClusterPattern() {}

    /**
     * The velocities, blocks per tick, of {@code count} bomblets thrown out at {@code openHeight} blocks above the ground so they
     * land within about {@code footprint} blocks of the point under the warhead, the warhead itself going {@code vx, vy, vz}.
     * Three numbers per bomblet.
     */
    public static double[] velocities(int count, double openHeight, double footprint, double vx, double vy, double vz, long seed) {
        double[] out = new double[Math.max(0, count) * 3];
        if (count <= 0) {
            return out;
        }
        double fall = fallTicks(openHeight, Math.min(0, vy));
        SplittableRandom random = new SplittableRandom(seed);
        double golden = Math.PI * (3 - Math.sqrt(5));
        for (int i = 0; i < count; i++) {
            // A sunflower spread, jittered: even cover, denser in the middle by the square root.
            double reach = footprint * Math.sqrt((i + 0.5) / count) * (0.75 + 0.25 * random.nextDouble());
            double angle = i * golden + random.nextDouble() * 0.6;
            double speed = reach / Math.max(1, fall);
            // Most of the warhead's own motion is shed as the casing opens; a little carries the pattern on.
            out[3 * i] = Math.cos(angle) * speed + 0.15 * vx;
            out[3 * i + 1] = Math.min(0, vy) * 0.2 + (random.nextDouble() - 0.5) * 0.1;
            out[3 * i + 2] = Math.sin(angle) * speed + 0.15 * vz;
        }
        return out;
    }

    /** Ticks a bomblet takes to fall {@code height} blocks starting at {@code vy}, gravity only. */
    public static double fallTicks(double height, double vy) {
        double h = Math.max(0, height);
        double v = -Math.min(0, vy) * 0.2;
        return (-v + Math.sqrt(v * v + 2 * GRAVITY * h)) / GRAVITY;
    }
}
