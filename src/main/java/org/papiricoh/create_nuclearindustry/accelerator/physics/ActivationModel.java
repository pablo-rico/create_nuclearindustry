package org.papiricoh.create_nuclearindustry.accelerator.physics;

/**
 * Activation of the parts a beam is lost on (targets, dumps, badly focused cells).
 *
 * <p>Lost beam makes two kinds of radiation. The prompt dose exists only while the beam runs and is
 * proportional to the power lost. Activation builds up with the lost power towards a saturation
 * level ({@code saturationPerKw} per kW) with the half-life of the activated material, and keeps
 * radiating after the beam stops, decaying with the same half-life. Dismantling an activated part
 * leaves radioactive waste in proportion to its activity. Pure arithmetic.
 */
public final class ActivationModel {

    /**
     * @param saturationPerKw  activity a part reaches under one kW of loss, dose per second
     * @param halfLifeTicks    half-life of the activation
     * @param promptPerKw      prompt dose per second per kW lost, only while the beam runs
     * @param maxStrength      cap on the dose per second one hot spot gives off
     * @param wasteThreshold   activity from which dismantling leaves low-level waste
     * @param wastePerItem     activity per item of low-level waste
     * @param maxLowLevelWaste most low-level waste one part leaves
     * @param hotThreshold     activity from which dismantling also leaves intermediate-level waste
     */
    public record Params(double saturationPerKw, double halfLifeTicks, double promptPerKw, double maxStrength,
                         double wasteThreshold, double wastePerItem, int maxLowLevelWaste, double hotThreshold) {
        public static final Params DEFAULT = new Params(0.5, 12000, 2.0, 500, 1.0, 5.0, 4, 20.0);
    }

    /** What dismantling a part with this activity leaves behind. */
    public record Waste(int lowLevel, int intermediateLevel) {
        public static final Waste NONE = new Waste(0, 0);

        public boolean isEmpty() {
            return lowLevel <= 0 && intermediateLevel <= 0;
        }
    }

    private ActivationModel() {}

    /** Activity one tick later under {@code lossKw} of lost beam. */
    public static double tick(Params p, double activity, double lossKw) {
        double target = p.saturationPerKw() * Math.max(0, lossKw);
        double rate = 1 - Math.exp(-Math.log(2) / Math.max(1, p.halfLifeTicks()));
        return Math.max(0, activity + (target - activity) * rate);
    }

    /** Activity after {@code ticks} with no beam. */
    public static double decay(Params p, double activity, long ticks) {
        if (ticks <= 0) {
            return Math.max(0, activity);
        }
        return Math.max(0, activity) * Math.pow(0.5, ticks / Math.max(1, p.halfLifeTicks()));
    }

    /** Activity the part tends to under a steady loss. */
    public static double saturation(Params p, double lossKw) {
        return p.saturationPerKw() * Math.max(0, lossKw);
    }

    /** Prompt dose per second at a hot spot losing {@code lossKw} while the beam runs. */
    public static double prompt(Params p, double lossKw) {
        return p.promptPerKw() * Math.max(0, lossKw);
    }

    /** Dose per second a hot spot gives off: prompt plus activation, capped. */
    public static double strength(Params p, double activity, double lossKw) {
        return Math.min(p.maxStrength(), Math.max(0, activity) + prompt(p, lossKw));
    }

    public static Waste wasteFor(Params p, double activity) {
        if (activity < p.wasteThreshold()) {
            return Waste.NONE;
        }
        int low = (int) Math.min(p.maxLowLevelWaste(), Math.max(1, Math.ceil(activity / Math.max(1.0e-9, p.wastePerItem()))));
        int intermediate = activity >= p.hotThreshold() ? 1 : 0;
        return new Waste(low, intermediate);
    }
}
