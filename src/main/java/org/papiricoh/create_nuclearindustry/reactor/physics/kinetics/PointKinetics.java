package org.papiricoh.create_nuclearindustry.reactor.physics.kinetics;

/**
 * Point reactor kinetics: the core's power {@code n}, relative to its rated power, and the
 * precursors of its delayed neutrons, normalised so each group equals {@code n} at equilibrium.
 *
 * <pre>
 *   n'  = ((rho - beta) n + sum beta_i c_i) / Lambda + S
 *   c_i' = lambda_i (n - c_i)
 * </pre>
 *
 * Integrated with implicit Euler in closed form: the precursor update is solved into the power
 * equation, so a step costs one division per group and stays stable for any reactivity below prompt
 * criticality whatever the step. Above it the step is split until each piece is small against the
 * prompt period. Pure arithmetic, no game classes.
 */
public final class PointKinetics {
    /** Each piece of a step may grow the prompt term by at most this much. */
    private static final double MAX_PROMPT_STEP = 0.3;
    private static final int MAX_PIECES = 32;

    /**
     * @param generationTime  prompt neutron generation time Lambda, seconds
     * @param source          neutron source keeping a shut-down core faintly alive, relative power per second
     * @param maxPower        hard cap on the relative power
     * @param substeps        pieces each step is split into at least
     */
    public record Params(double generationTime, double source, double maxPower, int substeps) {
        public static final Params DEFAULT = new Params(1.0e-3, 1.0e-3, 10.0, 4);
    }

    private double power;
    private double[] precursors = new double[0];
    private double lastPeriod = Double.POSITIVE_INFINITY;

    /** Relative power. */
    public double power() {
        return power;
    }

    public double[] precursors() {
        return precursors.clone();
    }

    /** Power and precursors in equilibrium at {@code n} (a core held there for a long time). */
    public void atEquilibrium(DelayedNeutrons groups, double n) {
        power = Math.max(0, n);
        precursors = new double[groups.groups()];
        java.util.Arrays.fill(precursors, power);
    }

    /** Restores a saved state; missing or wrong-sized precursors start in equilibrium with the power. */
    public void restore(DelayedNeutrons groups, double n, double[] saved) {
        power = Math.max(0, Double.isFinite(n) ? n : 0);
        if (saved == null || saved.length != groups.groups()) {
            atEquilibrium(groups, power);
            return;
        }
        precursors = new double[saved.length];
        for (int i = 0; i < saved.length; i++) {
            precursors[i] = Double.isFinite(saved[i]) ? Math.max(0, saved[i]) : power;
        }
    }

    /**
     * Advances by {@code seconds} at reactivity {@code rho} (a fraction: 0.001 = 100 pcm).
     *
     * @return the relative power at the end
     */
    public double step(Params p, DelayedNeutrons groups, double rho, double seconds) {
        if (precursors.length != groups.groups()) {
            atEquilibrium(groups, power);
        }
        double beta = groups.total();
        double lambdaGen = Math.max(1.0e-6, p.generationTime());
        int pieces = Math.max(1, p.substeps());
        double prompt = (rho - beta) / lambdaGen;
        while (prompt * seconds / pieces > MAX_PROMPT_STEP && pieces < MAX_PIECES) {
            pieces *= 2;
        }
        pieces = Math.min(pieces, MAX_PIECES);
        double dt = seconds / pieces;
        double start = power;
        for (int k = 0; k < pieces; k++) {
            piece(p, groups, rho, dt, lambdaGen);
        }
        if (start > 1.0e-9 && power > 1.0e-9 && power != start) {
            double ratio = Math.log(power / start);
            lastPeriod = Math.abs(ratio) < 1.0e-12 ? Double.POSITIVE_INFINITY : seconds / ratio;
        } else {
            lastPeriod = Double.POSITIVE_INFINITY;
        }
        return power;
    }

    private void piece(Params p, DelayedNeutrons groups, double rho, double dt, double lambdaGen) {
        double[] beta = groups.beta();
        double[] lambda = groups.lambda();
        double numerator = power + dt * Math.max(0, p.source());
        double denominator = 1.0 - dt * (rho - groups.total()) / lambdaGen;
        for (int i = 0; i < beta.length; i++) {
            double damp = 1.0 + lambda[i] * dt;
            numerator += dt * beta[i] * precursors[i] / (lambdaGen * damp);
            denominator -= dt * beta[i] * lambda[i] * dt / (lambdaGen * damp);
        }
        double next;
        if (denominator > 1.0e-6) {
            next = numerator / denominator;
        } else {
            // Far above prompt criticality: the cap is reached within the step anyway.
            next = p.maxPower();
        }
        power = Math.max(0, Math.min(p.maxPower(), next));
        for (int i = 0; i < beta.length; i++) {
            precursors[i] = (precursors[i] + lambda[i] * dt * power) / (1.0 + lambda[i] * dt);
        }
    }

    /**
     * The reactor period of the last step, seconds: how long the power takes to grow by e (negative
     * while it falls, infinite when steady).
     */
    public double period() {
        return lastPeriod;
    }

    /** Drops everything to zero (structure gone, no fuel). */
    public void clear() {
        power = 0;
        java.util.Arrays.fill(precursors, 0);
        lastPeriod = Double.POSITIVE_INFINITY;
    }
}
