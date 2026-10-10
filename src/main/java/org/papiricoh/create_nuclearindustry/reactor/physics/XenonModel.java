package org.papiricoh.create_nuclearindustry.reactor.physics;

/**
 * Iodine-135 and xenon-135 in the core, both normalised to the neutron flux (0 = cold core,
 * 1 = flat-out). Fission makes iodine, iodine decays into xenon, and xenon both decays and is
 * burnt away by the flux. At steady power xenon settles low; after a shutdown the iodine already
 * made keeps feeding it while nothing burns it, so it peaks a few minutes later (the iodine pit)
 * and holds the reactor back until it decays. Kept free of game classes so it can be unit tested.
 */
public final class XenonModel {
    /** How strongly the flux burns xenon away; sets the steady level to {@code f / (1 + BURNOUT f)}. */
    public static final double BURNOUT = 3.0;
    /** Xenon at which the penalty reaches the full poison worth. */
    public static final double REFERENCE_XENON = 0.5;

    private double iodine;
    private double xenon;

    /**
     * Advances one tick.
     *
     * @param flux           neutron flux as a fraction of the maximum, 0-1
     * @param iodineHalfLife ticks for iodine to halve
     * @param xenonHalfLife  ticks for xenon to halve
     */
    public void tick(double flux, double iodineHalfLife, double xenonHalfLife) {
        double f = Math.max(0.0, Math.min(1.0, flux));
        double iodineLife = Math.max(1.0, iodineHalfLife) / Math.log(2.0);
        double xenonLife = Math.max(1.0, xenonHalfLife) / Math.log(2.0);
        iodine += (f - iodine) / iodineLife;
        xenon += (iodine - xenon * (1.0 + BURNOUT * f)) / xenonLife;
        iodine = Math.max(0.0, iodine);
        xenon = Math.max(0.0, xenon);
    }

    /** Fraction of neutron production the xenon swallows, up to {@code worth}. */
    public double penalty(double worth) {
        return Math.max(0.0, worth) * Math.min(1.0, xenon / REFERENCE_XENON);
    }

    public double iodine() {
        return iodine;
    }

    public double xenon() {
        return xenon;
    }

    public void set(double iodine, double xenon) {
        this.iodine = Math.max(0.0, iodine);
        this.xenon = Math.max(0.0, xenon);
    }

    public void clear() {
        set(0.0, 0.0);
    }
}
