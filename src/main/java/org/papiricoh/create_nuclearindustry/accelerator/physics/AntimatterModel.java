package org.papiricoh.create_nuclearindustry.accelerator.physics;

/**
 * Antiproton production at a target. Protons below the production threshold make none; above it
 * the yield rises with the energy, up to {@code maxYield} times the nominal output of a target
 * recipe. A magnetic horn and collector catch only part of what the target makes. Pure arithmetic.
 */
public final class AntimatterModel {

    /**
     * @param thresholdGev      beam energy below which no antiprotons are made, GeV
     * @param maxYield          most antiprotons per nominal unit, at very high energy
     * @param collectorEfficiency share of the antiprotons the collector catches, 0..1
     */
    public record Params(double thresholdGev, double maxYield, double collectorEfficiency) {
        public static final Params DEFAULT = new Params(25.0, 2.0, 0.8);
    }

    private AntimatterModel() {}

    /** Yield multiplier at this beam energy: zero below the threshold, rising one per threshold above it. */
    public static double yield(Params p, double energyGev) {
        if (energyGev <= p.thresholdGev() || p.thresholdGev() <= 0) {
            return 0;
        }
        return Math.min(p.maxYield(), (energyGev - p.thresholdGev()) / p.thresholdGev());
    }

    /** Antiprotons the collector keeps out of a target recipe of this nominal output, ng. */
    public static double collected(Params p, double nominal, double energyGev) {
        return Math.max(0, nominal) * AntimatterModel.yield(p, energyGev) * Math.max(0, Math.min(1, p.collectorEfficiency()));
    }
}
