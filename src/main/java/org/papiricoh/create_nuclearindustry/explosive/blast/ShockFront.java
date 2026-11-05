package org.papiricoh.create_nuclearindustry.explosive.blast;

/**
 * When each part of a detonation reaches a listener {@code distance} blocks away. The shock front and the boom that rides it
 * travel at the blast wave's speed for as far as the wave carries; past that only sound goes on, at the speed of sound. The
 * ground carries a faint tremor ahead of both. One model for the server's wave, the client's boom and the shake, so they can
 * never drift apart. No game classes, so it is tested on its own.
 */
public final class ShockFront {
    /** The speed of sound, blocks per tick (343 m/s). */
    public static final double SOUND_SPEED = 17.15;
    /** The ground wave, blocks per tick: rock carries it several times faster than air. */
    public static final double SEISMIC_SPEED = 60;

    private ShockFront() {}

    /**
     * Ticks until the shock front reaches {@code distance} when it moves at {@code waveSpeed} blocks per tick and dies out at
     * {@code waveRadius}; past that the boom goes on as plain sound.
     */
    public static double arrivalTicks(double distance, double waveSpeed, double waveRadius) {
        double d = Math.max(0, distance);
        double speed = Math.max(1e-3, waveSpeed);
        double reach = Math.max(0, waveRadius);
        if (d <= reach) {
            return d / speed;
        }
        return reach / speed + (d - reach) / SOUND_SPEED;
    }

    /** Ticks until the shock front reaches {@code distance}, a wave without an end. */
    public static double arrivalTicks(double distance, double waveSpeed) {
        return Math.max(0, distance) / Math.max(1e-3, waveSpeed);
    }

    /** Ticks until the ground's tremor reaches {@code distance}. */
    public static double seismicArrivalTicks(double distance) {
        return Math.max(0, distance) / SEISMIC_SPEED;
    }

    /** Ticks until plain sound, never a shock, reaches {@code distance}. */
    public static double soundArrivalTicks(double distance) {
        return Math.max(0, distance) / SOUND_SPEED;
    }
}
