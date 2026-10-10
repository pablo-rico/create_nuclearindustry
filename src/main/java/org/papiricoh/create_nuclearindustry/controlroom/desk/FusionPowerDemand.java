package org.papiricoh.create_nuclearindustry.controlroom.desk;

/**
 * What an automatic fusion power controller's single output asks of a tokamak. Fuelling is the
 * cheap knob, so it acts first: the lower half of the output runs the fuelling from nothing to the
 * most the tokamak takes. Only once the fuelling is saturated and the power is still short does the
 * upper half raise the auxiliary heating, which costs a lot of shaft power, from the level the
 * operator left (the floor, never undercut, since a plasma that is not ignited needs it) up to full.
 * <p>
 * Fusion power rises with fuelling, while the {@link PidController} raises its output when the
 * measurement runs <em>above</em> the setpoint; {@link #step} hands it negated values so it acts
 * the right way round. No game classes, so it can be tested on its own.
 *
 * @param fuel    fuelling as a share of the most the tokamak takes, 0-1
 * @param heating auxiliary heating level, 0-1
 */
public record FusionPowerDemand(double fuel, double heating) {
    /** The share of the output that runs the fuelling; above it the heating rises. */
    public static final double FUEL_SHARE = 0.5;

    public static FusionPowerDemand of(double output, double heatingFloor) {
        double u = clamp01(output);
        double floor = clamp01(heatingFloor);
        double fuel = Math.min(1.0, u / FUEL_SHARE);
        double extra = Math.max(0.0, (u - FUEL_SHARE) / (1.0 - FUEL_SHARE));
        return new FusionPowerDemand(fuel, floor + extra * (1.0 - floor));
    }

    /** The output that gives this fuelling and heating: where a controller takes over bumplessly. */
    public static double outputFor(double fuel, double heating, double heatingFloor) {
        double f = clamp01(fuel);
        double floor = clamp01(heatingFloor);
        if (f < 1.0 || heating <= floor || floor >= 1.0) {
            return f * FUEL_SHARE;
        }
        return FUEL_SHARE + clamp01((heating - floor) / (1.0 - floor)) * (1.0 - FUEL_SHARE);
    }

    /**
     * One step of the loop.
     *
     * @param setpoint      wanted fusion power, as a fraction of full scale
     * @param measurement   fusion power now, as a fraction of full scale
     * @param dt            seconds since the last step
     * @param currentOutput the output that matches the tokamak now (see {@link #outputFor}); used on the first step only
     * @param heatingFloor  the heating level the operator left
     */
    public static FusionPowerDemand step(PidController pid, double setpoint, double measurement, double dt,
                                         double currentOutput, double heatingFloor, PidController.Tuning tuning) {
        double output = pid.step(-setpoint, -measurement, dt, currentOutput, 0, tuning);
        return of(output, heatingFloor);
    }

    /** Whether the heating is pushed above the operator's level. */
    public boolean boosting(double heatingFloor) {
        return heating > clamp01(heatingFloor) + 1.0e-6;
    }

    private static double clamp01(double value) {
        return Double.isNaN(value) ? 0.0 : Math.max(0.0, Math.min(1.0, value));
    }
}
