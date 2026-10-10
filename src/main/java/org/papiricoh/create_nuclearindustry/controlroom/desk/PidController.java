package org.papiricoh.create_nuclearindustry.controlroom.desk;

/**
 * A reverse-acting PID controller for automatic rod control: when the measurement runs above the
 * setpoint the output (rod insertion, 0-1) goes up. Errors are fractions of full scale, so the same
 * gains suit power and temperature. On top of the textbook terms it has:
 * <ul>
 *     <li>bumpless start: the first step returns the output it was handed, and the integral takes over from there;</li>
 *     <li>a deadband: inside it the output holds and nothing integrates, so the rods do not hunt;</li>
 *     <li>a rate limit on the output, because rods driven by pistons cannot follow jumps anyway;</li>
 *     <li>anti-windup by tracking: whenever the output is clamped or rate-limited the integral is set
 *     back to what the output really is, so it never builds up behind a saturated actuator;</li>
 *     <li>derivative on the measurement, so moving the setpoint gives no kick;</li>
 *     <li>output quantized to whole rod segments, the only positions pistons can hold.</li>
 * </ul>
 * No game classes, so it can be tested on its own; the state is saved by the module that owns it.
 */
public final class PidController {

    /**
     * @param kp        output per unit of error
     * @param ki        output per unit of error per second
     * @param kd        output per unit of measurement change per second
     * @param deadband  error, in fractions of full scale, inside which the output holds
     * @param rateLimit largest output change per second, or 0 for no limit
     */
    public record Tuning(double kp, double ki, double kd, double deadband, double rateLimit) {}

    private boolean initialized;
    private double integral;
    private double output;
    private double previousMeasurement;

    public boolean initialized() {
        return initialized;
    }

    public double integral() {
        return integral;
    }

    /** The last output before quantization. */
    public double output() {
        return output;
    }

    public double previousMeasurement() {
        return previousMeasurement;
    }

    /** Forgets everything; the next step starts bumpless again. */
    public void reset() {
        initialized = false;
        integral = 0;
        output = 0;
        previousMeasurement = 0;
    }

    /**
     * One control step.
     *
     * @param setpoint    wanted value, as a fraction of full scale
     * @param measurement measured value, as a fraction of full scale
     * @param dt          seconds since the last step
     * @param current     the actuator's position now, 0-1; used on the first step only
     * @param steps       positions the actuator can hold between 0 and 1 (rod segments), or 0 for any
     * @return the new output, 0-1, quantized to {@code steps}
     */
    public double step(double setpoint, double measurement, double dt, double current, int steps, Tuning tuning) {
        double error = measurement - setpoint;
        if (!initialized) {
            initialized = true;
            output = clamp01(current);
            integral = output - tuning.kp() * error;
            previousMeasurement = measurement;
            return quantize(output, steps);
        }
        double derivative = dt > 0 ? tuning.kd() * (measurement - previousMeasurement) / dt : 0;
        previousMeasurement = measurement;
        if (Math.abs(error) <= tuning.deadband()) {
            return quantize(output, steps);
        }
        double proportional = tuning.kp() * error;
        double nextIntegral = integral + tuning.ki() * error * dt;
        double wanted = proportional + nextIntegral + derivative;
        double limited = clamp01(wanted);
        if (tuning.rateLimit() > 0) {
            double maxStep = tuning.rateLimit() * dt;
            limited = Math.max(output - maxStep, Math.min(output + maxStep, limited));
        }
        // Tracking anti-windup: the integral follows the output the actuator really gets.
        integral = limited != wanted ? limited - proportional - derivative : nextIntegral;
        output = limited;
        return quantize(output, steps);
    }

    /** Restores a saved state. */
    public void load(boolean initialized, double integral, double output, double previousMeasurement) {
        this.initialized = initialized;
        this.integral = finite(integral);
        this.output = clamp01(finite(output));
        this.previousMeasurement = finite(previousMeasurement);
    }

    public static double quantize(double value, int steps) {
        double clamped = clamp01(value);
        return steps > 0 ? Math.round(clamped * steps) / (double) steps : clamped;
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static double finite(double value) {
        return Double.isFinite(value) ? value : 0.0;
    }
}
