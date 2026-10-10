package org.papiricoh.create_nuclearindustry.fusion.physics;

/**
 * Pressure in the tokamak's vacuum chamber, as log10 of pascals: 5 at atmosphere, about -4 at a good
 * plasma vacuum.
 *
 * <p>Pumps bring the pressure down at a steady number of decades per tick towards a base pressure
 * set by how much pumping the chamber has (relative speed 1 = the pumps a tokamak of its size needs,
 * at rated speed). With less pumping the base pressure rises fast, so a tokamak short of pumps never
 * reaches plasma vacuum. Without pumps the walls outgas and the pressure creeps back up, faster
 * while a plasma heats them. An open (unformed) chamber is at atmosphere. Pure arithmetic.
 */
public final class VacuumModel {
    public static final double ATMOSPHERE = 5.0;

    /**
     * @param basePressure    best pressure with full pumping, log10 Pa
     * @param plasmaLimit     highest pressure a plasma can be lit or kept at, log10 Pa
     * @param pumpDownPerTick decades per tick the pumps take off at full pumping
     * @param outgasPerTick   decades per tick the pressure rises when it is above what the pumps hold
     * @param pumpingSlope    decades the base pressure rises per decade of missing pumping speed
     * @param plasmaLoad      decades a lit plasma adds to the base pressure
     */
    public record Params(double basePressure, double plasmaLimit, double pumpDownPerTick, double outgasPerTick,
                         double pumpingSlope, double plasmaLoad) {
        public static final Params DEFAULT = new Params(-4.0, -3.0, 8.0 / 300.0, 1.0 / 60.0, 10.0, 0.3);
    }

    private double pressure = ATMOSPHERE;

    /**
     * @param pumping relative pumping speed (1 = enough pumps at rated speed)
     * @param plasma  a plasma is lit
     */
    public void tick(Params p, double pumping, boolean plasma) {
        double base = basePressure(p, pumping) + (plasma ? p.plasmaLoad() : 0);
        base = Math.min(ATMOSPHERE, base);
        if (pressure > base) {
            pressure = Math.max(base, pressure - p.pumpDownPerTick() * Math.min(2.0, Math.max(0, pumping)));
        } else if (pressure < base) {
            pressure = Math.min(base, pressure + p.outgasPerTick());
        }
    }

    /** The chamber is open: back to atmosphere at once. */
    public void vent() {
        pressure = ATMOSPHERE;
    }

    /** Lowest pressure this much pumping can hold, log10 Pa. */
    public static double basePressure(Params p, double pumping) {
        if (pumping <= 0) {
            return ATMOSPHERE;
        }
        return Math.min(ATMOSPHERE, p.basePressure() + p.pumpingSlope() * Math.max(0, -Math.log10(pumping)));
    }

    /** Whether this much pumping can bring the chamber to plasma vacuum. */
    public static boolean canReachPlasmaVacuum(Params p, double pumping) {
        return basePressure(p, pumping) <= p.plasmaLimit();
    }

    /** Pumps at rated speed a tokamak of this major radius needs. */
    public static int pumpsNeeded(int majorRadius) {
        return Math.max(1, (int) Math.ceil(majorRadius / 4.0));
    }

    public boolean atPlasmaVacuum(Params p) {
        return pressure <= p.plasmaLimit();
    }

    public double getPressure() {
        return pressure;
    }

    public void restore(double pressure) {
        this.pressure = Math.max(-10, Math.min(ATMOSPHERE, pressure));
    }
}
