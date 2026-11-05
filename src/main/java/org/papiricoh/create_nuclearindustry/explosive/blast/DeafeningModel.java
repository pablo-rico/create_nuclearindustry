package org.papiricoh.create_nuclearindustry.explosive.blast;

/**
 * How badly a shock wave deafens a listener: nothing below a threshold overpressure, then worse and for longer the harder it
 * hits, as ruptured eardrums and temporary threshold shifts do. No game classes, so it is tested on its own.
 */
public final class DeafeningModel {
    /** The worst grade: almost nothing heard but the ringing. */
    public static final int MAX_GRADE = 3;
    /** Longest it lasts, seconds. */
    public static final double MAX_SECONDS = 45;

    private DeafeningModel() {}

    /** How deaf {@code kPa} makes a listener: -1 not at all, 0 mild to {@link #MAX_GRADE} severe; each grade is twice the pressure. */
    public static int grade(double kPa, double thresholdKPa) {
        if (thresholdKPa <= 0 || kPa < thresholdKPa) {
            return -1;
        }
        return (int) Math.min(MAX_GRADE, Math.floor(Math.log(kPa / thresholdKPa) / Math.log(2)));
    }

    /** Seconds it lasts: five at the threshold, growing with the square root of the pressure. */
    public static double seconds(double kPa, double thresholdKPa) {
        if (grade(kPa, thresholdKPa) < 0) {
            return 0;
        }
        return Math.min(MAX_SECONDS, 5 * Math.sqrt(kPa / thresholdKPa));
    }
}
