package org.papiricoh.create_nuclearindustry.spentfuel;

/**
 * Decay heat of a spent assembly after it leaves the core, as a fraction of its heat at discharge.
 * Fission products are lumped into three groups (short, medium and long-lived), each decaying
 * exponentially: the classic way to fit the decay heat curve. The time scale is the caller's, so a
 * server can make fuel cool faster or slower than one in-game day per day. No game classes.
 */
public final class SpentFuelDecay {
    /** Share of the heat at discharge each group carries; they add up to one. */
    static final double[] WEIGHTS = {0.50, 0.35, 0.15};
    /** Mean life of each group, in (scaled) in-game days. */
    static final double[] MEAN_LIFE_DAYS = {1.0, 4.0, 30.0};

    /** Above this fraction an assembly is {@link CoolingGrade#HOT} (about two days). */
    public static final double HOT_ABOVE = 0.40;
    /** Above this it is {@link CoolingGrade#WARM} (about a week). */
    public static final double WARM_ABOVE = 0.18;
    /** Above this it is {@link CoolingGrade#COOLED} (about three weeks); below, {@link CoolingGrade#AGED}. */
    public static final double COOLED_ABOVE = 0.08;

    private SpentFuelDecay() {}

    /** Fraction of the discharge heat left after {@code days}; one at or before discharge. */
    public static double heatFraction(double days) {
        double t = Math.max(0.0, days);
        double fraction = 0;
        for (int i = 0; i < WEIGHTS.length; i++) {
            fraction += WEIGHTS[i] * Math.exp(-t / MEAN_LIFE_DAYS[i]);
        }
        return fraction;
    }

    public static CoolingGrade gradeFor(double heatFraction) {
        if (heatFraction > HOT_ABOVE) {
            return CoolingGrade.HOT;
        }
        if (heatFraction > WARM_ABOVE) {
            return CoolingGrade.WARM;
        }
        return heatFraction > COOLED_ABOVE ? CoolingGrade.COOLED : CoolingGrade.AGED;
    }

    public static CoolingGrade gradeAfter(double days) {
        return gradeFor(heatFraction(days));
    }

    /** Days until an assembly reaches {@code grade}: zero for {@link CoolingGrade#HOT}. */
    public static double daysUntil(CoolingGrade grade) {
        return switch (grade) {
            case HOT -> 0.0;
            case WARM -> daysUntilFraction(HOT_ABOVE);
            case COOLED -> daysUntilFraction(WARM_ABOVE);
            case AGED -> daysUntilFraction(COOLED_ABOVE);
        };
    }

    /** Days until the heat falls to {@code fraction}; the curve only falls, so a bisection finds it. */
    public static double daysUntilFraction(double fraction) {
        if (fraction >= 1.0) {
            return 0.0;
        }
        double low = 0.0;
        double high = 1.0;
        while (heatFraction(high) > fraction) {
            high *= 2.0;
            if (high > 1.0e6) {
                return Double.POSITIVE_INFINITY;
            }
        }
        for (int i = 0; i < 60; i++) {
            double mid = 0.5 * (low + high);
            if (heatFraction(mid) > fraction) {
                low = mid;
            } else {
                high = mid;
            }
        }
        return high;
    }
}
