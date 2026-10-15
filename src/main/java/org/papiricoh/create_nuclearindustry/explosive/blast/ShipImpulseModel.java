package org.papiricoh.create_nuclearindustry.explosive.blast;

/**
 * How hard a blast wave throws a ship: its overpressure on the face the ship turns to the blast,
 * capped so no ship leaves faster than {@link #MAX_SPEED}. Applied where the wave first meets the
 * ship, so one struck off its centre also spins. No game classes.
 */
public final class ShipImpulseModel {
    /** Impulse, mass × blocks/s, per kPa on each square block of exposed face. */
    public static final double PER_KPA_AREA = 0.5;
    /** Fastest a blast throws a ship, blocks per second. */
    public static final double MAX_SPEED = 40.0;

    private ShipImpulseModel() {}

    /**
     * Impulse on a ship of {@code mass} showing {@code exposedArea} square blocks to an overpressure
     * of {@code kPa}, times {@code scale}.
     */
    public static double impulse(double kPa, double exposedArea, double mass, double scale) {
        if (kPa <= 0 || exposedArea <= 0 || mass <= 0 || scale <= 0) {
            return 0;
        }
        return Math.min(kPa * exposedArea * PER_KPA_AREA * scale, MAX_SPEED * mass);
    }

    /**
     * The area a box of {@code sx × sy × sz} blocks shows to a wave coming along
     * ({@code dx, dy, dz}): each face pair weighted by how squarely the wave meets it.
     */
    public static double exposedArea(double sx, double sy, double sz, double dx, double dy, double dz) {
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 1.0e-9) {
            return Math.max(sx * sy, Math.max(sy * sz, sx * sz));
        }
        return (Math.abs(dx) * sy * sz + Math.abs(dy) * sx * sz + Math.abs(dz) * sx * sy) / length;
    }
}
