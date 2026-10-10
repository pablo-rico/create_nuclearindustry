package org.papiricoh.create_nuclearindustry.fusion.heating;

/**
 * Turns the shaft speed of a neutral beam injector or a radio-frequency antenna into the raw
 * heating power it offers the plasma. Pure arithmetic, so JUnit can check it; the block entities
 * copy {@link PlasmaHeatingConfig} into {@link Params} at the call site.
 *
 * <p>Nothing below {@code minSpeed}; from there the power rises linearly to {@code ratedPower} at
 * {@code ratedSpeed}, and stays there however much faster the shaft turns. The stress the block
 * puts on the network does not change with speed, so a heater running below its rated speed pays
 * more stress per megawatt.
 */
public final class HeatingDrive {

    /**
     * @param ratedPower MW at or above {@code ratedSpeed}
     * @param ratedSpeed RPM for full power
     * @param minSpeed   RPM below which the heater gives nothing; less than {@code ratedSpeed}
     */
    public record Params(double ratedPower, double ratedSpeed, double minSpeed) {
        public Params {
            if (!(ratedSpeed > minSpeed) || minSpeed < 0) {
                throw new IllegalArgumentException("Rated speed must be above the minimum speed: " + ratedSpeed + " <= " + minSpeed);
            }
        }
    }

    /**
     * Density, as a fraction of the Greenwald limit, under which the radio waves do not couple to the
     * plasma, and where they couple fully. The core physics applies the real coupling; these only let
     * the antenna's goggles warn about a plasma that is too thin.
     */
    public static final double RF_MIN_COUPLING_DENSITY = 0.15;
    public static final double RF_FULL_COUPLING_DENSITY = 0.35;

    private HeatingDrive() {}

    /** Raw heating power, MW, at this shaft speed (either direction). */
    public static double power(double speed, Params params) {
        double rpm = Math.abs(speed);
        if (params.ratedPower() <= 0 || rpm <= params.minSpeed()) {
            return 0;
        }
        if (rpm >= params.ratedSpeed()) {
            return params.ratedPower();
        }
        return params.ratedPower() * (rpm - params.minSpeed()) / (params.ratedSpeed() - params.minSpeed());
    }

    /** Share of the rated power, 0..1. */
    public static double load(double speed, Params params) {
        return params.ratedPower() <= 0 ? 0 : power(speed, params) / params.ratedPower();
    }

    /** Deuterium the beams need for this power, mB per tick. */
    public static double beamDeuterium(double powerMw, double deuteriumPerMw) {
        return Math.max(0, powerMw) * Math.max(0, deuteriumPerMw);
    }

    /** Rough share of the antenna's power a plasma of this density absorbs, 0..1 (smooth step). */
    public static double rfCoupling(double density) {
        double x = (density - RF_MIN_COUPLING_DENSITY) / (RF_FULL_COUPLING_DENSITY - RF_MIN_COUPLING_DENSITY);
        if (x <= 0) {
            return 0;
        }
        if (x >= 1) {
            return 1;
        }
        return x * x * (3 - 2 * x);
    }
}
