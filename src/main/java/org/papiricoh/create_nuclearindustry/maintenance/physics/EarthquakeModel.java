package org.papiricoh.create_nuclearindustry.maintenance.physics;

/**
 * Natural earthquakes: how often, how strong (Gutenberg-Richter: each magnitude step ten times
 * rarer), how far they are felt, and the ground motion at a distance, on the scale reactors trip
 * on and seismic detectors read. No game classes.
 */
public final class EarthquakeModel {
    /** Blocks within which a magnitude 3 is felt; each magnitude doubles it. */
    public static final double FELT_AT_3 = 16.0;
    /** Ground motion a magnitude 3 gives at its epicentre. */
    public static final double MOTION_AT_3 = 1.0;

    private EarthquakeModel() {}

    /** Chance of an earthquake starting in one tick, for {@code perDay} a game day. */
    public static double chancePerTick(double perDay) {
        return Math.max(0, perDay) / WearPart.TICKS_PER_DAY;
    }

    /**
     * A magnitude between {@code min} and {@code max} from a uniform {@code roll}, Gutenberg-Richter
     * with slope {@code b} (1: a magnitude 5 is ten times rarer than a 4).
     */
    public static double magnitude(double roll, double min, double max, double b) {
        if (max <= min) {
            return min;
        }
        double r = Math.max(0, Math.min(1 - 1.0e-12, roll));
        double span = 1.0 - Math.pow(10, -b * (max - min));
        return min - Math.log10(1.0 - r * span) / b;
    }

    /** Blocks from the epicentre within which it is felt. */
    public static double feltRadius(double magnitude) {
        return FELT_AT_3 * Math.pow(2.0, magnitude - 3.0);
    }

    /** Ground motion {@code distance} blocks from the epicentre: ten times stronger each magnitude, fading with distance. */
    public static double groundMotion(double magnitude, double distance) {
        double scaled = Math.max(0, distance) / feltRadius(magnitude);
        return MOTION_AT_3 * Math.pow(10.0, magnitude - 3.0) / (1.0 + 4.0 * scaled * scaled);
    }

    /** Whether a reactor's seismic trip, set at {@code threshold}, scrams it under this motion. */
    public static boolean trips(double motion, double threshold) {
        return threshold > 0 && motion >= threshold;
    }

    /** Wear transient the shaking gives a plant's parts: 1 at the trip threshold, at most 3. */
    public static double severity(double motion, double threshold) {
        if (!(threshold > 0) || !(motion > 0)) {
            return 0.0;
        }
        return Math.min(3.0, motion / threshold);
    }

    /** The blast power a seismic detector reads it as, so its records compare with detonations. */
    public static float detectorPower(double magnitude) {
        return (float) (10.0 * Math.pow(4.0, magnitude - 3.0));
    }
}
