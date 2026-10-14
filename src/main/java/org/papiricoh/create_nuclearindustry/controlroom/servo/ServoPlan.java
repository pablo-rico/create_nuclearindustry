package org.papiricoh.create_nuclearindustry.controlroom.servo;

/**
 * The arithmetic of a {@link ServoGearboxBlockEntity}: where a redstone signal puts the actuator it
 * drives, what a comparator reads back, and how far a move goes. Positions are the actuator's own
 * offset, in blocks out from fully retracted (a piston's extension, a pulley's rope). No game
 * classes, so it is tested on its own.
 */
public final class ServoPlan {
    /** Redstone's strongest signal: it puts the actuator at the end of the stroke. */
    public static final int FULL_SIGNAL = 15;
    /** An actuator closer than this to a block position stands at it. */
    public static final float AT_POSITION = 0.05f;

    private ServoPlan() {}

    /**
     * The blocks the servo can use: the stroke it is set to, cut to how far the actuator reaches
     * (the poles of a piston, the rope of a pulley).
     */
    public static int usableStroke(int stroke, int range) {
        return Math.max(0, Math.min(stroke, range));
    }

    /**
     * The whole-block position a signal asks for: {@code signal} fifteenths of the usable stroke,
     * rounded, counted from retracted when the signal extends and from the end of the stroke when it
     * retracts.
     */
    public static int target(int signal, int stroke, int range, boolean extendWithSignal) {
        int usable = usableStroke(stroke, range);
        int level = clampSignal(extendWithSignal ? signal : FULL_SIGNAL - signal);
        return Math.round(level * usable / (float) FULL_SIGNAL);
    }

    /**
     * What a comparator reads from the servo: where the actuator stands along the usable stroke, on
     * the same scale as the signal that would put it there, so a comparator fed back reads the
     * signal the servo answers.
     */
    public static int comparator(float offset, int stroke, int range, boolean extendWithSignal) {
        int usable = usableStroke(stroke, range);
        if (usable <= 0) {
            return extendWithSignal ? 0 : FULL_SIGNAL;
        }
        float fraction = Math.max(0, Math.min(1, offset / usable));
        int level = Math.round(fraction * FULL_SIGNAL);
        return extendWithSignal ? level : FULL_SIGNAL - level;
    }

    /** Whole blocks from the actuator's position to {@code target}: positive outwards, negative back in. */
    public static int stepsTo(float offset, int target) {
        return target - Math.round(offset);
    }

    /** Whether the actuator stands at {@code target}. */
    public static boolean arrived(float offset, int target) {
        return Math.abs(offset - target) < AT_POSITION;
    }

    /**
     * Whether a move that set off from {@code start} towards {@code target} is now further away than
     * it began, by more than {@code slack} blocks: the actuator turns the other way than the servo
     * reckoned.
     */
    public static boolean movingAway(float start, float now, int target, float slack) {
        return Math.abs(now - target) > Math.abs(start - target) + slack;
    }

    private static int clampSignal(int signal) {
        return Math.max(0, Math.min(FULL_SIGNAL, signal));
    }
}
