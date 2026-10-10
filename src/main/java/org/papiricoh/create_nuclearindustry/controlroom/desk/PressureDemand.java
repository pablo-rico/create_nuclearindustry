package org.papiricoh.create_nuclearindustry.controlroom.desk;

/**
 * What a pressure controller's single output asks of the pressurizer. The {@link PidController}
 * raises its output when the pressure runs above the setpoint, so the output is split at the middle:
 * below {@link #NEUTRAL} the heaters run, more the lower it goes; above it the spray opens, more the
 * higher it goes. A plant in balance still loses heat from the pressurizer, so it settles a little
 * below the middle, with the heaters making up the loss as real proportional heaters do.
 * No game classes, so it can be tested on its own.
 *
 * @param heaters share of the heaters' power, 0-1
 * @param spray   spray valve opening, 0-1
 */
public record PressureDemand(double heaters, double spray) {
    /** The output where the controller neither heats nor sprays. */
    public static final double NEUTRAL = 0.5;

    public static PressureDemand of(double output) {
        double clamped = Math.max(0.0, Math.min(1.0, output));
        return new PressureDemand(Math.max(0.0, (NEUTRAL - clamped) / NEUTRAL),
                Math.max(0.0, (clamped - NEUTRAL) / (1.0 - NEUTRAL)));
    }

    /** Positive while heating, negative while spraying, -1 to 1: what the desk shows. */
    public double signed() {
        return heaters > 0 ? heaters : -spray;
    }
}
