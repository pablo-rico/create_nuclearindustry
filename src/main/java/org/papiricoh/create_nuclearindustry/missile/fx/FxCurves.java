package org.papiricoh.create_nuclearindustry.missile.fx;

/**
 * The timings of the rocket effects' one-off moments: the dust ring racing out from a launch, the flashes, the plasma round a
 * warhead coming back in and the shaking near lift-off. Free of game classes, for JUnit.
 */
public final class FxCurves {
    /** The dust ring starts this far from the silo... */
    public static final double DUST_START = 6;
    /** ...and stops here, blocks. */
    public static final double DUST_END = 24;
    /** Seconds the ring takes to get there. */
    public static final double DUST_SECONDS = 3.0;
    /** Blocks from the launch within which the view shakes. */
    public static final double SHAKE_REACH = 96;
    /** Blocks above its launch site after which a rocket no longer shakes the ground. */
    public static final double SHAKE_HEIGHT = 160;

    private FxCurves() {
    }

    public static double clamp01(double v) {
        return Math.max(0, Math.min(1, v));
    }

    public static double smoothstep(double edge0, double edge1, double v) {
        double t = clamp01((v - edge0) / (edge1 - edge0));
        return t * t * (3 - 2 * t);
    }

    /** The dust ring's radius {@code seconds} after lift-off: fast at first, easing out at {@link #DUST_END}. */
    public static double dustRadius(double seconds) {
        double t = clamp01(seconds / DUST_SECONDS);
        double eased = 1 - Math.pow(1 - t, 3);
        return DUST_START + (DUST_END - DUST_START) * eased;
    }

    /** How thick the dust ring is, 0 to 1: it rises in a moment and settles as it spreads. */
    public static double dustAlpha(double seconds) {
        if (seconds < 0 || seconds > DUST_SECONDS * 1.6) return 0;
        double rise = clamp01(seconds / 0.25);
        double fall = 1 - clamp01((seconds - 0.4) / (DUST_SECONDS * 1.6 - 0.4));
        return rise * fall * fall;
    }

    /** A flash {@code seconds} after it went off, lasting about {@code length}: at once at full, then dying away. */
    public static double flash(double seconds, double length) {
        if (seconds < 0 || seconds > length) return 0;
        double t = seconds / length;
        return (1 - t) * (1 - t) * Math.exp(-3 * t);
    }

    /**
     * How deep into the air something at height {@code y} is, 0 at {@code atmosphereTop} (and above) to 1 at sea level. The
     * game's air begins at its top: that is where a falling warhead's re-entry starts.
     */
    public static double airDepth(double y, double atmosphereTop) {
        double sea = 63;
        return clamp01((atmosphereTop - y) / Math.max(1, atmosphereTop - sea));
    }

    /**
     * How hot the air round a warhead glows, 0 to 1, moving at {@code speed} blocks a tick {@code depth} into the air
     * ({@link #airDepth}): nothing above the air or below a couple of blocks a tick, full a quarter of the way down.
     */
    public static double reentryHeat(double speed, double depth) {
        double fast = smoothstep(1.5, 7.0, speed);
        double air = smoothstep(0, 0.25, depth);
        return fast * air;
    }

    /**
     * How hard the ground shakes, 0 to 1, {@code distance} blocks from a rocket burning {@code above} blocks over its
     * launch site.
     */
    public static double shake(double distance, double above) {
        double near = 1 - clamp01(distance / SHAKE_REACH);
        double low = 1 - clamp01(above / SHAKE_HEIGHT);
        return near * near * low;
    }
}
