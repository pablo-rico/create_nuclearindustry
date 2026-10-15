package org.papiricoh.create_nuclearindustry.isotope.physics;

/**
 * Radioactive decay of an isotope item: {@code A(t) = A0 * 2^(-t / T½)}, the same law as
 * {@code RtgFuel.decay}. Time is in game days (24000 ticks) times the server's time scale, so a
 * stack that sits in a chest is not ticked: its activity is worked out from the tick it was made
 * whenever someone looks at it (the {@code SpentFuel.refresh} pattern). A half-life of zero or less
 * means stable. Activities are relative (1 = a fresh item of nominal strength). No game classes.
 */
public final class IsotopeDecay {
    public static final long TICKS_PER_DAY = 24000L;
    /** Shortest stamp quantum: items made within a second of each other always stack. */
    public static final long MIN_QUANTUM_TICKS = 20L;
    /** Longest stamp quantum: one game day, as spent fuel's discharge day. */
    public static final long MAX_QUANTUM_TICKS = TICKS_PER_DAY;
    /** Stamp quantum as a share of the half-life: stacks merge only when their activities differ by under ~1.4 %. */
    public static final double QUANTUM_SHARE = 0.02;

    private IsotopeDecay() {}

    /** Share of the activity left after {@code days}: 1 at or before the start, and always for a stable isotope. */
    public static double remaining(double halfLifeDays, double days) {
        if (!(halfLifeDays > 0) || !(days > 0)) {
            return 1.0;
        }
        return Math.pow(2.0, -days / halfLifeDays);
    }

    /** Activity after {@code days}, from {@code initial}. */
    public static double activity(double initial, double halfLifeDays, double days) {
        return Math.max(0.0, initial) * remaining(halfLifeDays, days);
    }

    /** Decay constant λ = ln 2 / T½, per day; 0 for a stable isotope. */
    public static double decayConstant(double halfLifeDays) {
        return halfLifeDays > 0 ? Math.log(2.0) / halfLifeDays : 0.0;
    }

    /** Days on the decay curve between two game times, after the time scale; never negative. */
    public static double scaledDays(long fromTick, long nowTick, double timeScale) {
        if (nowTick <= fromTick) {
            return 0.0;
        }
        return (nowTick - fromTick) / (double) TICKS_PER_DAY * Math.max(0.0, timeScale);
    }

    /** Activity at {@code nowTick} of an item made at {@code fromTick} with {@code initial} activity. */
    public static double activityAt(double initial, double halfLifeDays, long fromTick, long nowTick, double timeScale) {
        return activity(initial, halfLifeDays, scaledDays(fromTick, nowTick, timeScale));
    }

    /**
     * Scaled days until the activity falls from {@code initial} to {@code threshold}: 0 if it is already
     * there, infinite for a stable isotope or a threshold of 0 or less.
     */
    public static double daysUntil(double initial, double halfLifeDays, double threshold) {
        if (initial <= threshold) {
            return 0.0;
        }
        if (!(halfLifeDays > 0) || !(threshold > 0)) {
            return Double.POSITIVE_INFINITY;
        }
        return halfLifeDays * Math.log(initial / threshold) / Math.log(2.0);
    }

    /** Game ticks until the activity reaches {@code threshold}, rounded up; {@link Long#MAX_VALUE} if never. */
    public static long ticksUntil(double initial, double halfLifeDays, double threshold, double timeScale) {
        double days = daysUntil(initial, halfLifeDays, threshold);
        if (days == 0.0) {
            return 0L;
        }
        if (Double.isInfinite(days) || !(timeScale > 0)) {
            return Long.MAX_VALUE;
        }
        double ticks = Math.ceil(days * TICKS_PER_DAY / timeScale - 1.0e-9);
        return ticks >= Long.MAX_VALUE ? Long.MAX_VALUE : (long) ticks;
    }

    /** Ticks of game time for one half-life, after the time scale; {@link Long#MAX_VALUE} if stable. */
    public static long halfLifeTicks(double halfLifeDays, double timeScale) {
        if (!(halfLifeDays > 0) || !(timeScale > 0)) {
            return Long.MAX_VALUE;
        }
        return Math.max(1L, Math.round(halfLifeDays * TICKS_PER_DAY / timeScale));
    }

    /**
     * How finely creation ticks are rounded so that items made close together stack: 2 % of the
     * half-life in ticks, between a second and a day.
     */
    public static long quantumTicks(double halfLifeDays, double timeScale) {
        long halfLife = halfLifeTicks(halfLifeDays, timeScale);
        if (halfLife == Long.MAX_VALUE) {
            return MAX_QUANTUM_TICKS;
        }
        long quantum = Math.round(halfLife * QUANTUM_SHARE);
        return Math.max(MIN_QUANTUM_TICKS, Math.min(MAX_QUANTUM_TICKS, quantum));
    }

    /** The creation tick to store: {@code gameTime} rounded down to the quantum. */
    public static long stampTick(long gameTime, long quantumTicks) {
        long quantum = Math.max(1L, quantumTicks);
        return Math.floorDiv(gameTime, quantum) * quantum;
    }

    /** Creation tick that gives {@code activity} now for an item stamped fresh at {@code initial}: for back-dating. */
    public static long tickForActivity(double initial, double activity, double halfLifeDays, long nowTick, double timeScale) {
        long ticks = ticksUntil(initial, halfLifeDays, activity, timeScale);
        if (ticks == Long.MAX_VALUE) {
            return nowTick;
        }
        return nowTick - ticks;
    }

    /** Whole percent of nominal activity for tooltips and item model predicates, 0..999. */
    public static int percent(double activity) {
        if (!(activity > 0)) {
            return 0;
        }
        return (int) Math.min(999, Math.round(activity * 100.0));
    }

    /** Whether an item of this activity is still any use (above its isotope's threshold). */
    public static boolean usable(double activity, double usableAbove) {
        return activity > usableAbove;
    }
}
