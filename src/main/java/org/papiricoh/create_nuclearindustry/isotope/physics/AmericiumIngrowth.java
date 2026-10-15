package org.papiricoh.create_nuclearindustry.isotope.physics;

/**
 * Americium in aged plutonium. Reactor plutonium carries some plutonium-241, which beta-decays to
 * americium-241, so the older the plutonium the more americium a separation gets out of it:
 * <pre>
 *   ingrown(t) = 1 - 2^(-t / T½(Pu-241))
 * </pre>
 * The separation's americium chance per plutonium item is {@code maxYield * ingrown}. Pu-241 lives
 * 14.3 years; at one game day per real year that is the default 14.3 days, so a plutonium stockpile
 * a couple of weeks old gives about half the americium it ever will. Pure arithmetic.
 */
public final class AmericiumIngrowth {

    /**
     * @param ingrowthHalfLifeDays Pu-241 half-life, game days (scaled)
     * @param maxYield             americium chance per plutonium item once fully grown in, 0..1
     */
    public record Params(double ingrowthHalfLifeDays, double maxYield) {
        public static final Params DEFAULT = new Params(14.3, 0.5);
    }

    private AmericiumIngrowth() {}

    /** Share of the eventual americium grown in after {@code days}, 0..1. */
    public static double ingrown(Params p, double days) {
        return 1.0 - IsotopeDecay.remaining(p.ingrowthHalfLifeDays(), days);
    }

    /** Chance per plutonium item that a separation gives one americium, 0..maxYield. */
    public static double chance(Params p, double days) {
        if (!(p.ingrowthHalfLifeDays() > 0)) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, p.maxYield())) * ingrown(p, days);
    }

    /**
     * Americium items from separating {@code count} plutonium items of this age, with a uniform
     * {@code roll} in [0, 1): {@code floor(count * chance + roll)}, so on average exactly
     * {@code count * chance} and never off by more than one.
     */
    public static int yield(Params p, double days, int count, double roll) {
        if (count <= 0) {
            return 0;
        }
        double expected = count * chance(p, days);
        double r = Math.max(0.0, Math.min(1.0 - 1.0e-12, roll));
        return (int) Math.floor(expected + r);
    }

    /** Scaled days for the chance to reach {@code chance}; infinite if it never does. */
    public static double daysFor(Params p, double chance) {
        double max = Math.max(0.0, Math.min(1.0, p.maxYield()));
        if (chance <= 0) {
            return 0.0;
        }
        if (chance >= max || !(p.ingrowthHalfLifeDays() > 0)) {
            return Double.POSITIVE_INFINITY;
        }
        return IsotopeDecay.daysUntil(1.0, p.ingrowthHalfLifeDays(), 1.0 - chance / max);
    }
}
