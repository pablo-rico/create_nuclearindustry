package org.papiricoh.create_nuclearindustry.civildefense.seismic;

/**
 * How strongly a seismic detector feels a detonation, and where it came from. Pure, so the scale can
 * be unit tested and tuned from the config.
 */
public final class SeismicModel {
    /** Compass points, clockwise from north; indices of {@link #compassPoint}. */
    public static final String[] COMPASS = {"n", "ne", "e", "se", "s", "sw", "w", "nw"};

    private SeismicModel() {}

    /**
     * Ground motion at {@code distance} blocks from a blast of {@code power}: the full power nearby,
     * then falling with the square of the distance past {@code falloff} blocks, like a wave spreading
     * over the ground.
     */
    public static double groundMotion(double power, double distance, double falloff) {
        if (power <= 0) {
            return 0;
        }
        double scaled = Math.max(0, distance) / Math.max(1.0e-6, falloff);
        return power / (1.0 + scaled * scaled);
    }

    /** Comparator signal for a ground motion: 1 for anything felt, 15 at {@code fullScale} or more. */
    public static int comparatorSignal(double motion, double fullScale) {
        if (motion <= 0) {
            return 0;
        }
        int signal = (int) Math.ceil(15.0 * motion / Math.max(1.0e-6, fullScale));
        return Math.max(1, Math.min(15, signal));
    }

    /** Bearing of a point {@code (dx, dz)} away, in degrees clockwise from north (north is -Z). */
    public static double bearing(double dx, double dz) {
        double degrees = Math.toDegrees(Math.atan2(dx, -dz));
        return degrees < 0 ? degrees + 360.0 : degrees;
    }

    /** Index in {@link #COMPASS} of the nearest of the eight compass points. */
    public static int compassPoint(double bearing) {
        double normalized = ((bearing % 360.0) + 360.0) % 360.0;
        return (int) Math.round(normalized / 45.0) % 8;
    }
}
