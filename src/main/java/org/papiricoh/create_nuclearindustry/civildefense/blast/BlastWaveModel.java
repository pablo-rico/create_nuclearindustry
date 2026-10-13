package org.papiricoh.create_nuclearindustry.civildefense.blast;

/**
 * Peak overpressure of a blast wave in free air, after Kinney and Graham's fit to the Brode / TNT
 * curves: it depends only on the scaled distance {@code Z = r / W^(1/3)} (metres, kilograms of TNT),
 * so a bigger device reaches the same pressure proportionally further. One block is one metre.
 * What the wave does to bodies and blocks follows from the pressure. No game classes, so it can be
 * unit tested.
 */
public final class BlastWaveModel {
    /** Sea-level air pressure, kPa: the fit gives overpressure as a multiple of it. */
    public static final double AMBIENT_KPA = 101.325;
    /** Fastest the wave throws anything, blocks per tick. */
    public static final double MAX_KNOCKBACK = 4.0;
    /** How far the wave reaches beyond the crater, as a multiple of the crater radius. */
    public static final double CRATER_REACH = 4.0;
    /** Scaled distances closer than this are inside the fireball; the fit is clamped there. */
    private static final double MIN_SCALED_DISTANCE = 0.05;

    private BlastWaveModel() {}

    /** TNT-equivalent yield, kg, of a detonation of {@code power} at {@code kgPerPower}. */
    public static double yield(double power, double kgPerPower) {
        return Math.max(0, power) * kgPerPower;
    }

    /** {@code Z = r / W^(1/3)}, m/kg^(1/3). */
    public static double scaledDistance(double distance, double yieldKg) {
        if (yieldKg <= 0) {
            return Double.POSITIVE_INFINITY;
        }
        return Math.max(0, distance) / Math.cbrt(yieldKg);
    }

    /** Kinney-Graham peak overpressure at scaled distance {@code z}, kPa. */
    public static double overpressureAtScaled(double z) {
        if (Double.isInfinite(z)) {
            return 0;
        }
        double s = Math.max(MIN_SCALED_DISTANCE, z);
        double ratio = 808.0 * (1 + sq(s / 4.5))
                / Math.sqrt((1 + sq(s / 0.048)) * (1 + sq(s / 0.32)) * (1 + sq(s / 1.35)));
        return ratio * AMBIENT_KPA;
    }

    /** Peak overpressure {@code distance} blocks from a {@code yieldKg} kg TNT-equivalent blast, kPa. */
    public static double overpressure(double distance, double yieldKg) {
        return overpressureAtScaled(scaledDistance(distance, yieldKg));
    }

    /** Fraction of the wave a block of {@code resistance} lets through: {@code exp(-resistance / scale)}. */
    public static double blockTransmission(double resistance, double scale) {
        if (resistance <= 0) {
            return 1.0;
        }
        return Math.exp(-resistance / Math.max(1.0e-6, scale));
    }

    /** Damage, half hearts, of {@code kPa}: linear above a threshold, nothing below it. */
    public static double damage(double kPa, double thresholdKPa, double perKPa) {
        return Math.max(0, kPa - thresholdKPa) * perKPa;
    }

    /** Outward speed the wave gives a body that does not resist it, blocks per tick. */
    public static double knockback(double kPa, double perKPa) {
        return Math.min(MAX_KNOCKBACK, Math.max(0, kPa) * perKPa);
    }

    /** How far the wave of a detonation with a {@code craterRadius} crater travels, blocks. */
    public static double maxRadius(int craterRadius, double cap) {
        return Math.max(0, Math.min(cap, CRATER_REACH * craterRadius));
    }

    private static double sq(double value) {
        return value * value;
    }
}
