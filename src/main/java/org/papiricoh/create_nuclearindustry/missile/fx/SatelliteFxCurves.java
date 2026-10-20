package org.papiricoh.create_nuclearindustry.missile.fx;

import org.papiricoh.create_nuclearindustry.missile.physics.V3;

/**
 * How the satellites' moments play out over time: the fairing peeling open and tumbling away at orbit insertion, the satellite
 * let go, the glittering cloud of a satellite shot down and the fireball of one burning up as it comes down. Times are ticks
 * since the moment; lengths blocks. Most is worked out from the age alone, so the effects need no state to step and draw the
 * same at any frame rate. Free of game classes, for JUnit.
 */
public final class SatelliteFxCurves {
    /** Ticks after insertion the fairing's bolts fire and its halves start to swing open. */
    public static final int FAIRING_DELAY = 6;
    /** Ticks the halves take to swing open on their hinges before they come off them and tumble free. */
    public static final int PEEL_TICKS = 12;
    /** Degrees a tick a half turns once free. */
    public static final double TUMBLE_RATE = 7;
    /** How fast the halves part sideways once free, blocks a tick, and how fast they fall (blocks a tick, each tick). */
    public static final double FAIRING_SPEED = 0.14, FAIRING_GRAVITY = 0.012;
    /** Ticks the halves are followed, falling, before they are let go of. */
    public static final int FAIRING_LIFE = 200;
    /** Ticks after insertion the satellite is let go, pushed out of the open fairing by its springs. */
    public static final int RELEASE_DELAY = FAIRING_DELAY + PEEL_TICKS / 2;
    /** How fast the springs push it out, blocks a tick. */
    public static final double RELEASE_SPEED = 0.05;
    /** Ticks the satellite is shown drifting on, the last {@link #SATELLITE_FADE} of them fading out. */
    public static final int SATELLITE_LIFE = 240, SATELLITE_FADE = 80;

    /** A shot-down satellite's fragments: how fast the air (thin up there) slows them and how fast they fall. */
    public static final double SHARD_DRAG = 0.02, SHARD_GRAVITY = 0.004;

    /** A re-entering satellite: ticks it burns before breaking up, and ticks it is followed at most. */
    public static final int REENTRY_BREAKUP = 70, REENTRY_LIFE = 170;
    /** Fastest it crosses the sky, blocks a tick, and how fast it starts down. */
    public static final double REENTRY_SPEED = 3, REENTRY_DIVE = 0.9;
    /** Its fall: gravity and the air's drag a tick. */
    public static final double REENTRY_GRAVITY = 0.035, REENTRY_DRAG = 0.995;
    /** It has burnt out this many blocks over the ground at the latest. */
    public static final double REENTRY_FLOOR = 60;

    private SatelliteFxCurves() {
    }

    // ------------------------------------------------------------------ orbit insertion

    /**
     * Degrees a fairing half has turned {@code ticks} after insertion: still shut, then swinging open on its hinge faster and
     * faster, then tumbling free at a steady {@link #TUMBLE_RATE}, with no jump in between.
     */
    public static double peelDegrees(double ticks) {
        double t = ticks - FAIRING_DELAY;
        if (t <= 0) return 0;
        // Constant angular acceleration up to the tumble rate, then the tumble rate.
        if (t < PEEL_TICKS) return TUMBLE_RATE * t * t / (2.0 * PEEL_TICKS);
        return TUMBLE_RATE * (t - PEEL_TICKS / 2.0);
    }

    /** Blocks a fairing half has moved out sideways {@code ticks} after insertion: easing out as it peels, then parting steadily. */
    public static double fairingDrift(double ticks) {
        double t = ticks - FAIRING_DELAY;
        if (t <= 0) return 0;
        if (t < PEEL_TICKS) return FAIRING_SPEED * t * t / (2.0 * PEEL_TICKS);
        return FAIRING_SPEED * (t - PEEL_TICKS / 2.0);
    }

    /** Blocks a fairing half has fallen {@code ticks} after insertion, once off its hinge. */
    public static double fairingFall(double ticks) {
        double t = ticks - FAIRING_DELAY - PEEL_TICKS;
        return t <= 0 ? 0 : 0.5 * FAIRING_GRAVITY * t * t;
    }

    /** Blocks the satellite has been pushed out along the rocket's axis {@code ticks} after insertion. */
    public static double releaseDrift(double ticks) {
        return Math.max(0, ticks - RELEASE_DELAY) * RELEASE_SPEED;
    }

    /** 1 while something lasting {@code life} ticks is in full view, falling to 0 over its last {@code fade} ticks. */
    public static double fade(double ticks, double life, double fade) {
        if (ticks < 0 || ticks >= life) return 0;
        if (fade <= 0) return 1;
        return Math.min(1, (life - ticks) / fade);
    }

    // ------------------------------------------------------------------ a kill

    /**
     * Where a fragment thrown from {@code from} at {@code velocity} (blocks a tick) is {@code ticks} later: slowed by the thin air,
     * falling slowly.
     */
    public static V3 shard(V3 from, V3 velocity, double ticks) {
        double t = Math.max(0, ticks);
        double carried = (1 - Math.exp(-SHARD_DRAG * t)) / SHARD_DRAG;
        return new V3(from.x() + velocity.x() * carried, from.y() + velocity.y() * carried - 0.5 * SHARD_GRAVITY * t * t,
                from.z() + velocity.z() * carried);
    }

    /**
     * How brightly a tumbling fragment catches the sun at {@code time} (ticks), 0.25 to 1: mostly dull, with short bright glints
     * that come at its own pace ({@code seed}).
     */
    public static double glitter(double time, int seed) {
        double phase = (seed & 0xFFFF) * 0.0137;
        double rate = 0.21 + (seed >>> 16 & 0xFF) / 255.0 * 0.25;
        double s = Math.max(0, Math.sin(time * rate + phase * 7));
        return 0.25 + 0.75 * Math.pow(s, 10);
    }

    // ------------------------------------------------------------------ re-entry

    /** The velocity a decaying satellite moving at {@code orbital} (blocks a tick) starts its last plunge with: steeply down. */
    public static V3 reentryStart(V3 orbital) {
        double h = Math.sqrt(orbital.x() * orbital.x() + orbital.z() * orbital.z());
        double speed = Math.min(h, REENTRY_SPEED);
        double x = h > 1.0e-6 ? orbital.x() / h * speed : 0, z = h > 1.0e-6 ? orbital.z() / h * speed : 0;
        return new V3(x, -REENTRY_DIVE, z);
    }

    /** One tick of the plunge: the new velocity after drag and gravity. */
    public static V3 reentryStep(V3 velocity) {
        return new V3(velocity.x() * REENTRY_DRAG, velocity.y() * REENTRY_DRAG - REENTRY_GRAVITY, velocity.z() * REENTRY_DRAG);
    }

    /**
     * How brightly it burns {@code ticks} into its plunge, 0 to 1.6, at height {@code y} over ground at {@code groundY}: lighting up
     * fast, flaring as it breaks up, then dying away, and out before it gets near the ground.
     */
    public static double reentryGlow(double ticks, double y, double groundY) {
        if (ticks < 0 || ticks >= REENTRY_LIFE) return 0;
        double rise = Math.min(1, ticks / 12.0);
        double flare = 1 + 0.6 * Math.exp(-Math.abs(ticks - REENTRY_BREAKUP) / 8.0);
        double out = Math.min(1, (REENTRY_LIFE - ticks) / 50.0);
        double high = Math.max(0, Math.min(1, (y - groundY - REENTRY_FLOOR) / 120.0));
        return rise * flare * out * high;
    }
}
