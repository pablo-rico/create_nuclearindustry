package org.papiricoh.create_nuclearindustry.explosive.blast;

/**
 * How long a fireball's flash dazzles someone who sees it: longer the closer and the bigger the fireball, much longer looking
 * straight at it than seeing it from the corner of the eye, longer at night with the eyes dark-adapted, and not at all through
 * protective goggles or with the back turned. Flash blindness is real and temporary: the retina is bleached and an afterimage
 * lingers. No game classes, so it is tested on its own.
 */
public final class FlashBlindnessModel {
    /** The worst grade: the screen almost white. */
    public static final int MAX_GRADE = 3;
    /** Facing further away than this (cosine of the angle off the line of sight) sees too little to be dazzled. */
    public static final double MIN_FACING = -0.2;

    private FlashBlindnessModel() {}

    /**
     * Seconds of flash blindness for a witness {@code distance} blocks from a fireball {@code fireballRadius} wide who looks
     * {@code facing} (the cosine of the angle between where they look and the fireball, 1 straight at it) at {@code night} or
     * by day, {@code protectedEyes} behind goggles; at most {@code maxSeconds}.
     */
    public static double seconds(double distance, double fireballRadius, double facing, boolean night, boolean protectedEyes,
                                 double maxSeconds) {
        if (protectedEyes || fireballRadius <= 0 || facing < MIN_FACING) {
            return 0;
        }
        // The fireball's apparent size against the sky: what bleaches the retina.
        double apparent = fireballRadius / Math.max(1, distance);
        double look = facing >= 0.9 ? 1.0 : 0.15 + 0.85 * Math.max(0, (facing - MIN_FACING) / (0.9 - MIN_FACING));
        double base = 120 * Math.sqrt(apparent) * look * (night ? 2.0 : 1.0);
        return base < 1 ? 0 : Math.min(maxSeconds, base);
    }

    /** How strongly the screen washes out for {@code seconds} of blindness: 0 mild to {@link #MAX_GRADE}. */
    public static int grade(double seconds, double maxSeconds) {
        if (seconds <= 0 || maxSeconds <= 0) {
            return -1;
        }
        return (int) Math.min(MAX_GRADE, Math.floor(4 * seconds / maxSeconds));
    }
}
