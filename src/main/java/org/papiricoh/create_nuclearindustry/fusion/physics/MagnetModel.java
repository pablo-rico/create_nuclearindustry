package org.papiricoh.create_nuclearindustry.fusion.physics;

/**
 * The tokamak's superconducting toroidal field coils.
 *
 * <ul>
 *   <li>Coils start at room temperature. Liquid helium cools them at a steady rate (about 20 s from
 *       293 K to 4.5 K with enough helium), boiling off extra helium while they cool.</li>
 *   <li>Cold coils keep using helium: a standby share, plus a heat load that grows with the square of
 *       the field. Each sector of the torus counts. Helium shortage warms them.</li>
 *   <li>Above {@link Params#superconductingTemperature()} the coils are no longer superconducting: a
 *       coil carrying field there is quenching, and the controller trips the plasma.</li>
 *   <li>The field follows its target at a limited ramp rate while energised, and ramps to zero
 *       otherwise (faster when quenching, through the dump resistors).</li>
 * </ul>
 * Pure arithmetic; the controller copies the config into {@link Params}.
 */
public final class MagnetModel {
    public static final double AMBIENT_TEMPERATURE = 293.0;
    public static final double OPERATING_TEMPERATURE = 4.5;
    /** The startup sequence waits for the coils to be this cold before energising. */
    public static final double READY_TEMPERATURE = 6.0;
    /** Default temperature above which the coils stop superconducting (and the plasma trips). */
    public static final double SUPERCONDUCTING_TEMPERATURE = 20.0;

    /**
     * @param rampPerTick                field change per tick while energising or ramping down, fraction of rated
     * @param dumpPerTick                field change per tick while quenching
     * @param heliumPerSector            liquid helium per toroidal sector per tick at full field, mB
     * @param standbyShare               share of the full-field use a cold coil uses with no field
     * @param cooldownHeliumPerSector    liquid helium per sector per tick while cooling down, mB
     * @param cooldownTicks              ticks from ambient to operating temperature with enough helium
     * @param warmingPerTick             kelvin per tick a coil with no helium at all warms under full load
     * @param idleWarmingRate            share of its gap to ambient an uncooled coil closes each tick (the cryostat is well insulated)
     * @param superconductingTemperature coils above this are no longer superconducting, K
     */
    public record Params(double rampPerTick, double dumpPerTick, double heliumPerSector, double standbyShare,
                         double cooldownHeliumPerSector, double cooldownTicks, double warmingPerTick,
                         double idleWarmingRate, double superconductingTemperature) {
        public static final Params DEFAULT = new Params(0.005, 0.02, 0.25, 0.3, 0.5, 400, 0.5, 1.0 / 24000,
                SUPERCONDUCTING_TEMPERATURE);
    }

    /**
     * @param sectors      toroidal sectors of the torus
     * @param fieldTarget  field the magnet drives ask for, 0..1
     * @param cooling      liquid helium flows through the coils
     * @param energize     the coils should carry current
     * @param holdField    a plasma sits in the field: superconducting coils keep their current when the
     *                     drives ask for less, so only a trip (and the ramp-down after it) lowers the field
     * @param liquidHelium liquid helium available this tick, mB
     */
    public record Inputs(int sectors, double fieldTarget, boolean cooling, boolean energize, boolean holdField,
                         double liquidHelium) {}

    private double field;
    private double temperature = AMBIENT_TEMPERATURE;
    private double lastHeliumUse;
    private double lastHeliumDemand;

    /** Advances one tick and returns the liquid helium used, in mB (it all comes back as warm helium). */
    public double tick(Params p, Inputs in) {
        int sectors = Math.max(1, in.sectors());
        double demand = 0;
        if (in.cooling()) {
            boolean coolingDown = temperature > OPERATING_TEMPERATURE + 1.0e-6;
            double load = sectors * p.heliumPerSector() * (p.standbyShare() + (1 - p.standbyShare()) * field * field);
            demand = load + (coolingDown ? sectors * p.cooldownHeliumPerSector() : 0);
        }
        double used = Math.min(demand, Math.max(0, in.liquidHelium()));
        double supplied = demand <= 0 ? 0 : used / demand;
        lastHeliumUse = used;
        lastHeliumDemand = demand;

        if (in.cooling()) {
            boolean coolingDown = temperature > OPERATING_TEMPERATURE + 1.0e-6;
            double coolRate = (AMBIENT_TEMPERATURE - OPERATING_TEMPERATURE) / Math.max(1, p.cooldownTicks());
            // The shortfall heats the coils, worse with field in them.
            double heatLoad = 0.3 + 0.7 * field * field;
            temperature += (1 - supplied) * p.warmingPerTick() * heatLoad - (coolingDown ? supplied * coolRate : 0);
            temperature = Math.max(OPERATING_TEMPERATURE, temperature);
        } else {
            temperature += (AMBIENT_TEMPERATURE - temperature) * p.idleWarmingRate();
        }
        temperature = Math.min(AMBIENT_TEMPERATURE, temperature);

        double target = in.energize() && superconducting(p) ? clamp(in.fieldTarget(), 0, 1) : 0;
        if (in.holdField() && in.energize() && superconducting(p)) {
            target = Math.max(target, field);
        }
        double rate = superconducting(p) ? p.rampPerTick() : p.dumpPerTick();
        if (field < target) {
            field = Math.min(target, field + rate);
        } else if (field > target) {
            field = Math.max(target, field - rate);
        }
        return used;
    }

    public boolean superconducting(Params p) {
        return temperature < p.superconductingTemperature();
    }

    /** Coils carrying field above their superconducting temperature. */
    public boolean quenching(Params p) {
        return !superconducting(p) && field > 0;
    }

    public boolean ready() {
        return temperature <= READY_TEMPERATURE;
    }

    public double getField() {
        return field;
    }

    public double getTemperature() {
        return temperature;
    }

    public double getLastHeliumUse() {
        return lastHeliumUse;
    }

    /** Helium the coils wanted on the last tick, mB. */
    public double getLastHeliumDemand() {
        return lastHeliumDemand;
    }

    /** Liquid helium per tick that cold coils of this many sectors use at this field, mB. */
    public static double heliumUse(Params p, int sectors, double field) {
        return sectors * p.heliumPerSector() * (p.standbyShare() + (1 - p.standbyShare()) * field * field);
    }

    /** Deposits heat in the coils, such as a lost beam hitting them; may push them over into a quench. */
    public void heat(double kelvin) {
        temperature = clamp(temperature + Math.max(0, kelvin), OPERATING_TEMPERATURE, AMBIENT_TEMPERATURE);
    }

    /** Drops the field at once; the structure is gone. */
    public void dropField() {
        field = 0;
    }

    public void restore(double field, double temperature) {
        this.field = clamp(field, 0, 1);
        this.temperature = clamp(temperature, OPERATING_TEMPERATURE, AMBIENT_TEMPERATURE);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
