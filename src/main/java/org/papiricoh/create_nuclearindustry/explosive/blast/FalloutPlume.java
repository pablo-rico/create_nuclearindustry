package org.papiricoh.create_nuclearindustry.explosive.blast;

/**
 * Where a detonation's fallout comes down: carried by the wind, it stretches into a plume reaching far
 * downwind, narrow across it and short upwind, instead of a circle. The share of ground zero's
 * contamination a spot gets falls linearly to nothing at the plume's edge. No game classes.
 */
public final class FalloutPlume {
    /** How far the plume reaches downwind, upwind and to the sides, as multiples of the fallout's radius. */
    public static final double DOWNWIND = 2.0, UPWIND = 0.5, ACROSS = 0.6;

    private FalloutPlume() {}

    /**
     * The share, 0-1, of ground zero's contamination at ({@code dx}, {@code dz}) from it, for a wind
     * blowing towards {@code windAngle} radians (0 along +x, π/2 along +z). {@code stretch} 0 gives a
     * circle of {@code radius}, 1 the full plume.
     */
    public static double share(double dx, double dz, double radius, double windAngle, double stretch) {
        if (radius <= 0) {
            return 0;
        }
        double cos = Math.cos(windAngle);
        double sin = Math.sin(windAngle);
        double along = dx * cos + dz * sin;
        double across = -dx * sin + dz * cos;
        double ahead = radius * (1 + (DOWNWIND - 1) * stretch);
        double behind = radius * (1 + (UPWIND - 1) * stretch);
        double side = radius * (1 + (ACROSS - 1) * stretch);
        double a = along / (along >= 0 ? ahead : behind);
        double c = across / side;
        return Math.max(0, 1 - Math.sqrt(a * a + c * c));
    }

    /** The furthest the plume reaches from ground zero in any direction. */
    public static double reach(double radius, double stretch) {
        return radius * Math.max(1, 1 + (DOWNWIND - 1) * stretch);
    }
}
