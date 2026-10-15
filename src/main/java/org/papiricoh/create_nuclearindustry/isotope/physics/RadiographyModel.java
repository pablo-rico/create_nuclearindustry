package org.papiricoh.create_nuclearindustry.isotope.physics;

/**
 * Industrial gamma radiography with a cobalt-60 source: the camera shines the source through a
 * machine onto film and shows what is hidden inside (vessel integrity, cladding oxidation, component
 * wear). Each halving of the source's activity costs one half-value layer of penetration, so a weak
 * source sees less deep into a multiblock and needs a longer exposure. Every shot gives the operator a
 * dose in proportion to the source. Pure arithmetic.
 */
public final class RadiographyModel {

    /**
     * @param minActivity          weakest source that still exposes the film, relative
     * @param exposureTicksAtFull  exposure (use time) with a fresh source, ticks
     * @param maxExposureTicks     longest exposure, for the weakest usable source
     * @param fullPenetration      blocks a fresh source sees through
     * @param halfValueBlocks      penetration lost per halving of the activity, blocks
     * @param operatorDosePerShot  dose given to the operator per shot with a fresh source
     */
    public record Params(double minActivity, int exposureTicksAtFull, int maxExposureTicks, int fullPenetration,
                         double halfValueBlocks, double operatorDosePerShot) {
        /** One second with a fresh source, eight blocks deep, a block less per half-life; dead under 1/20. */
        public static final Params DEFAULT = new Params(0.05, 20, 200, 8, 1.0, 2.0);
    }

    private RadiographyModel() {}

    /** Whether the source can expose film at all. */
    public static boolean usable(Params p, double activity) {
        return activity >= p.minActivity() && activity > 0;
    }

    /** Exposure time, ticks: {@code exposureTicksAtFull / activity}, capped; 0 if unusable. */
    public static int exposureTicks(Params p, double activity) {
        if (!usable(p, activity)) {
            return 0;
        }
        double ticks = Math.ceil(Math.max(1, p.exposureTicksAtFull()) / Math.min(1.0, activity) - 1.0e-9);
        return (int) Math.max(1, Math.min(Math.max(1, p.maxExposureTicks()), ticks));
    }

    /**
     * How deep the camera sees into a machine, whole blocks: {@code fullPenetration + halfValueBlocks
     * log2(activity)}, at least 1 with a usable source, 0 without.
     */
    public static int penetration(Params p, double activity) {
        if (!usable(p, activity)) {
            return 0;
        }
        double depth = p.fullPenetration() + Math.max(0.0, p.halfValueBlocks()) * Math.log(Math.min(1.0, activity)) / Math.log(2.0);
        return (int) Math.max(1, Math.floor(depth + 1.0e-9));
    }

    /** Dose the operator takes per shot. */
    public static double operatorDose(Params p, double activity) {
        return usable(p, activity) ? Math.max(0.0, p.operatorDosePerShot()) * Math.min(1.0, activity) : 0.0;
    }
}
