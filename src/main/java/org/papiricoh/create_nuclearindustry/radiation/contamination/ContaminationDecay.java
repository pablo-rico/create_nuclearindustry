package org.papiricoh.create_nuclearindustry.radiation.contamination;

/**
 * Turns the configured "days for ground contamination to fade" into the half-life chunks decay
 * with: the maximum a chunk can hold halves at a steady pace and falls below the clean level
 * exactly after those days, so anything weaker is gone sooner. No game classes, for JUnit.
 */
public final class ContaminationDecay {
    public static final int TICKS_PER_DAY = 24000;

    private ContaminationDecay() {}

    /** Half-life in ticks for {@code max} to fall to {@code clean} in {@code days}; 0 (never decays) when days is 0. */
    public static long halfLifeTicks(double days, double max, double clean) {
        if (days <= 0) {
            return 0;
        }
        double halvings = Math.log(Math.max(max, clean * 2) / clean) / Math.log(2);
        return Math.max(1, Math.round(days * TICKS_PER_DAY / halvings));
    }

    /** Ticks for {@code level} to fall below {@code clean} with the given half-life; 0 if it already is, -1 if never. */
    public static long ticksToClear(double level, double clean, long halfLifeTicks) {
        if (level < clean) {
            return 0;
        }
        if (halfLifeTicks <= 0) {
            return -1;
        }
        return (long) Math.ceil(halfLifeTicks * Math.log(level / clean) / Math.log(2));
    }
}
