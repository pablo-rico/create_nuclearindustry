package org.papiricoh.create_nuclearindustry.isotope.physics;

/**
 * The gamma irradiator: cobalt-60 sources over a depot or belt sterilise food with their gamma rays.
 * Food is sterile once it has absorbed {@code sterilisingKGy} (10 kGy is the classic upper limit for
 * food; it kills the bacteria and parasites of rotten flesh and raw chicken). The dose rate is in
 * proportion to the sources' total activity, so a weakening cobalt source takes longer per item and,
 * past {@code maxTicks}, no longer sterilises at all: it must be replaced. The sources also irradiate
 * whoever stands next to the machine. Pure arithmetic.
 */
public final class GammaIrradiation {
    /** Processing ticks returned when the sources are too weak to sterilise anything. */
    public static final int TOO_WEAK = -1;

    /**
     * @param sterilisingKGy   dose that sterilises one item, kGy
     * @param kGyPerTickAtFull dose rate of one fresh source (activity 1), kGy per tick
     * @param maxTicks         longest an item may take; weaker sources do not process
     * @param emissionAtFull   dose per second next to the machine with one fresh source, unshielded
     */
    public record Params(double sterilisingKGy, double kGyPerTickAtFull, int maxTicks, double emissionAtFull) {
        /** 10 kGy in 100 ticks (five seconds) from one fresh source; a source sterilises until it falls under 1/12. */
        public static final Params DEFAULT = new Params(10.0, 0.1, 1200, 4.0);
    }

    private GammaIrradiation() {}

    /** Total activity of the sources loaded, relative; negatives count as nothing. */
    public static double sourceActivity(double... activities) {
        double total = 0;
        for (double activity : activities) {
            total += Math.max(0.0, activity);
        }
        return total;
    }

    /** Dose rate on the depot, kGy per tick. */
    public static double doseRate(Params p, double sourceActivity) {
        return Math.max(0.0, p.kGyPerTickAtFull()) * Math.max(0.0, sourceActivity);
    }

    /** Ticks one item takes to sterilise, rounded up; {@link #TOO_WEAK} if over {@code maxTicks} or no source. */
    public static int processingTicks(Params p, double sourceActivity) {
        double rate = doseRate(p, sourceActivity);
        if (!(rate > 0)) {
            return TOO_WEAK;
        }
        double ticks = Math.ceil(Math.max(0.0, p.sterilisingKGy()) / rate - 1.0e-9);
        if (ticks > p.maxTicks()) {
            return TOO_WEAK;
        }
        return (int) Math.max(1, ticks);
    }

    /** Whether the sources can sterilise at all. */
    public static boolean canSterilise(Params p, double sourceActivity) {
        return processingTicks(p, sourceActivity) != TOO_WEAK;
    }

    /** The weakest total activity that still sterilises within {@code maxTicks}. */
    public static double minActivity(Params p) {
        double perTickAtFull = Math.max(1.0e-12, p.kGyPerTickAtFull()) * Math.max(1, p.maxTicks());
        return Math.max(0.0, p.sterilisingKGy()) / perTickAtFull;
    }

    /** Dose absorbed in {@code ticks} under the sources, kGy. */
    public static double absorbed(Params p, double sourceActivity, int ticks) {
        return doseRate(p, sourceActivity) * Math.max(0, ticks);
    }

    /** Whether food that absorbed this much is sterile. */
    public static boolean sterile(Params p, double absorbedKGy) {
        return absorbedKGy >= p.sterilisingKGy() * (1 - 1.0e-9);
    }

    /** Scaled days a source of {@code activity} keeps sterilising on its own, for the goggles. */
    public static double daysOfService(Params p, double activity, double halfLifeDays) {
        return IsotopeDecay.daysUntil(activity, halfLifeDays, minActivity(p));
    }

    /** Dose per second next to the irradiator, unshielded: the gamma source strength. */
    public static double emission(Params p, double sourceActivity) {
        return Math.max(0.0, p.emissionAtFull()) * Math.max(0.0, sourceActivity);
    }

    /** Comparator signal: 0 too weak, 1..15 with the source strength (15 at one fresh source or more). */
    public static int comparator(Params p, double sourceActivity) {
        if (!canSterilise(p, sourceActivity)) {
            return 0;
        }
        return 1 + (int) Math.floor(Math.min(1.0, sourceActivity) * 14 + 1.0e-9);
    }
}
