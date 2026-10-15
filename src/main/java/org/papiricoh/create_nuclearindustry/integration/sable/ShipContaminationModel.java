package org.papiricoh.create_nuclearindustry.integration.sable;

/**
 * How a ship takes up contamination from what it flies through: a share of the gap to the
 * contamination around it each check, never more than that. Dust settles near the ground, so the
 * higher a ship flies over it the less reaches it. No game classes.
 */
public final class ShipContaminationModel {
    /** Height over the ground, blocks, at which a ship takes up half of what it would on it. */
    public static final double HALF_HEIGHT = 32.0;

    private ShipContaminationModel() {}

    /** What a ship holding {@code current} holds after one check among {@code ambient}, taking up {@code uptake} of the gap. */
    public static float uptake(float current, double ambient, double uptake) {
        if (ambient <= current || uptake <= 0) {
            return current;
        }
        return (float) (current + (ambient - current) * Math.min(1.0, uptake));
    }

    /** The share of the ground's contamination a ship {@code height} blocks above it is among. */
    public static double heightFactor(double height) {
        double above = Math.max(0, height) / HALF_HEIGHT;
        return 1.0 / (1.0 + above * above);
    }
}
