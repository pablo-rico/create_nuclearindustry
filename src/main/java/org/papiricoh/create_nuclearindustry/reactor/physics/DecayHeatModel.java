package org.papiricoh.create_nuclearindustry.reactor.physics;

/**
 * Heat from the decay of fission products. It follows the fission heat as a sum of five groups of
 * products, from ones that decay in seconds to ones that last for days: the classic fit of the
 * decay heat curve (shares fitted to the t^-0.2 law). Right after a SCRAM a core that ran for a long
 * time still makes about the full {@code fraction} of its fission heat; that falls to under half in
 * a tenth of {@code lifetime}, under a third in one {@code lifetime} and a fifth in ten, so a core
 * that loses its water after a SCRAM still heats up for hours. Each group also takes its own time
 * to build up, so a core that ran only briefly has less of the long tail. Kept free of game classes
 * so it can be unit tested.
 */
public final class DecayHeatModel {
    /** Share of the decay heat of each group at equilibrium; they add up to one. */
    static final double[] SHARES = {0.45, 0.15, 0.17, 0.05, 0.18};
    /** Lifetime of each group, as a multiple of {@code lifetime}. */
    static final double[] TIMES = {0.01, 0.1, 1.0, 10.0, 100.0};

    private final double[] groups = new double[SHARES.length];

    /**
     * Advances one tick.
     *
     * @param fissionHeat heat the chain reaction makes this tick
     * @param fraction    share of the fission heat the decay heat settles at
     * @param lifetime    ticks for the middle group to fall to about a third once fission stops
     */
    public void tick(double fissionHeat, double fraction, double lifetime) {
        double target = Math.max(0.0, fissionHeat) * Math.max(0.0, fraction);
        double base = Math.max(1.0, lifetime);
        for (int i = 0; i < groups.length; i++) {
            double tau = Math.max(1.0, base * TIMES[i]);
            groups[i] = Math.max(0.0, groups[i] + (target * SHARES[i] - groups[i]) / tau);
        }
    }

    /** Decay heat, in the units of the fission heat passed to {@link #tick}. */
    public double heat() {
        double sum = 0;
        for (double group : groups) {
            sum += group;
        }
        return sum;
    }

    /** Sets the decay heat as if the core had run steadily (every group at its share). */
    public void set(double heat) {
        double total = Math.max(0.0, Double.isFinite(heat) ? heat : 0.0);
        for (int i = 0; i < groups.length; i++) {
            groups[i] = total * SHARES[i];
        }
    }

    /** Each group's heat, for saving. */
    public double[] groups() {
        return groups.clone();
    }

    /** Restores saved groups; anything else falls back to {@link #set} with their sum. */
    public void restore(double[] saved, double fallback) {
        if (saved == null || saved.length != groups.length) {
            set(fallback);
            return;
        }
        for (int i = 0; i < groups.length; i++) {
            groups[i] = Double.isFinite(saved[i]) ? Math.max(0.0, saved[i]) : 0.0;
        }
    }
}
