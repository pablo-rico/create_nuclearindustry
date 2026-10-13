package org.papiricoh.create_nuclearindustry.reactor.physics.kinetics;

/**
 * How much reactivity the control rods hold back. A rod is worth little at the edges of the core,
 * where the flux is low, and most in the middle: its integral worth follows an S curve in how far
 * it is in. {@code shape} blends a straight line (0) with the full S (1). Pure arithmetic.
 */
public final class RodWorth {

    private RodWorth() {}

    /** Share of a channel's worth a rod inserted {@code x} (0..1) holds back. */
    public static double curve(double x, double shape) {
        double in = Math.max(0, Math.min(1, x));
        double s = Math.max(0, Math.min(1, shape));
        return (1 - s) * in + s * (in - Math.sin(2 * Math.PI * in) / (2 * Math.PI));
    }

    /**
     * Reactivity the rods hold back, pcm: {@code totalWorth} shared evenly between the channels,
     * each along the S curve.
     */
    public static double bank(double[] insertions, double totalWorth, double shape) {
        if (insertions == null || insertions.length == 0 || totalWorth <= 0) {
            return 0;
        }
        double sum = 0;
        for (double x : insertions) {
            sum += curve(x, shape);
        }
        return totalWorth * sum / insertions.length;
    }
}
