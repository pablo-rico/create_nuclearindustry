package org.papiricoh.create_nuclearindustry.reactor.physics.kinetics;

/**
 * The delayed neutron groups of a fuel: for each group its share of the fission neutrons (beta, as
 * a fraction, not pcm) and the decay constant of its precursors (per second). The delayed neutrons
 * are what makes a reactor controllable: below prompt criticality (reactivity under the total beta)
 * the power can only rise as fast as the precursors decay.
 */
public record DelayedNeutrons(double[] beta, double[] lambda) {
    /** U-235 thermal fission: group shares in pcm, 650 in all. */
    private static final double[] U235_BETA_PCM = {21.5, 142.4, 127.4, 256.8, 74.8, 27.3};
    private static final double[] U235_LAMBDA = {0.0124, 0.0305, 0.111, 0.301, 1.14, 3.01};

    public DelayedNeutrons {
        if (beta.length != lambda.length || beta.length == 0) {
            throw new IllegalArgumentException("one decay constant per delayed group");
        }
    }

    /**
     * Six groups with U-235's shape, scaled to {@code totalPcm} in all (MOX and thorium fuels have fewer
     * delayed neutrons) and with their decay sped up by {@code timeScale}.
     */
    public static DelayedNeutrons sixGroups(double totalPcm, double timeScale) {
        double[] beta = new double[6];
        double[] lambda = new double[6];
        double sum = 0;
        for (double b : U235_BETA_PCM) {
            sum += b;
        }
        for (int i = 0; i < 6; i++) {
            beta[i] = U235_BETA_PCM[i] / sum * Math.max(1, totalPcm) * 1.0e-5;
            lambda[i] = U235_LAMBDA[i] * Math.max(1.0e-3, timeScale);
        }
        return new DelayedNeutrons(beta, lambda);
    }

    /** One group holding all the delayed neutrons, with an effective decay constant. */
    public static DelayedNeutrons oneGroup(double totalPcm, double lambda) {
        return new DelayedNeutrons(new double[]{Math.max(1, totalPcm) * 1.0e-5}, new double[]{Math.max(1.0e-4, lambda)});
    }

    /** Total delayed fraction, as a fraction. */
    public double total() {
        double sum = 0;
        for (double b : beta) {
            sum += b;
        }
        return sum;
    }

    public int groups() {
        return beta.length;
    }
}
