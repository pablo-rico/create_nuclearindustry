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

    /**
     * Share of the core's reactivity a full set of rods can hold back, as a multiple of the rod bank
     * worth: rod blocks over fuel blocks times the boron's effectiveness and the fuel's control worth,
     * up to {@code cap}. The cap is only a guard: with {@code boronAbsorption} 1.5 a core with as many
     * rod blocks as fuel blocks holds 1.5 banks, which is what makes "rods in at least equal to the
     * fuel" a subcritical core whatever the fuel (see the shutdown margin tests).
     */
    public static double absorption(int rodBlocks, int fuelRods, double boronAbsorption, double controlWorth, double cap) {
        if (rodBlocks <= 0 || fuelRods <= 0) {
            return 0;
        }
        double ratio = (double) rodBlocks / fuelRods;
        return Math.max(0, Math.min(Math.max(0, cap), ratio * Math.max(0, boronAbsorption) * Math.max(0, controlWorth)));
    }

    /**
     * Rod blocks the inserted rods are worth: the channels' capacity in blocks shared evenly between
     * them, each counted along the S curve. A channel half in counts for half its blocks with a
     * straight curve, a little less with the S (its worth is in the middle of the core).
     */
    public static double effectiveRods(double[] insertions, int capacity, double shape) {
        if (insertions == null || insertions.length == 0 || capacity <= 0) {
            return 0;
        }
        double sum = 0;
        for (double x : insertions) {
            sum += curve(x, shape);
        }
        return capacity * sum / insertions.length;
    }
}
