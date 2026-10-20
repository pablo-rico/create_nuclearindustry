package org.papiricoh.create_nuclearindustry.missile.physics;

/**
 * How well a rocket's guidance knows where it is, set by what its guidance module holds. Errors grow with the distance flown:
 * a fixed part (the launch fix and the warhead's own fall) plus a share of the range (drift of the gyros, or of the stars'
 * fixes). The result is a circular error probable, the radius half the shots land within ({@link AccuracyModel}).
 */
public enum GuidanceTier {
    /** An empty module: dead reckoning on the launch heading. */
    BASIC(16.0, 0.03),
    /** A Create precision mechanism: gyros and accelerometers integrating the whole boost. */
    INERTIAL(4.0, 0.008),
    /** A star tracker: inertial guidance corrected by fixes on the stars above the atmosphere. */
    STELLAR(1.5, 0.0015);

    private static final GuidanceTier[] VALUES = values();

    /** Blocks of CEP whatever the range. */
    private final double fixed;
    /** Blocks of CEP per block of range. */
    private final double perBlock;

    GuidanceTier(double fixed, double perBlock) {
        this.fixed = fixed;
        this.perBlock = perBlock;
    }

    /** The circular error probable, blocks, of a shot {@code range} blocks long (horizontally). */
    public double cep(double range) {
        return fixed + perBlock * Math.max(0, range);
    }

    /** The tier saved or sent as {@code ordinal}, or {@link #BASIC} for anything unknown. */
    public static GuidanceTier byOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : BASIC;
    }
}
