package org.papiricoh.create_nuclearindustry.controlroom;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ModuleFunction;
import org.papiricoh.create_nuclearindustry.controlroom.desk.PidController;

/**
 * Common config for the chart recorder, the annunciator panel and automatic rod and pressure control, under {@code controlRoom.*}. Values are defined on the shared builder
 * by {@link org.papiricoh.create_nuclearindustry.Config} and copied into public statics on load.
 */
public final class ControlRoom2Config {
    private static ModConfigSpec.IntValue CHART_SAMPLES_PER_PAPER;
    private static ModConfigSpec.IntValue ANNUNCIATOR_INTERVAL;
    private static ModConfigSpec.IntValue PID_INTERVAL;
    private static ModConfigSpec.DoubleValue PID_GAIN;
    private static ModConfigSpec.DoubleValue PID_INTEGRAL;
    private static ModConfigSpec.DoubleValue PID_DERIVATIVE;
    private static ModConfigSpec.DoubleValue PID_DEADBAND;
    private static ModConfigSpec.DoubleValue PID_RATE_LIMIT;
    private static ModConfigSpec.DoubleValue PRESSURE_GAIN;
    private static ModConfigSpec.DoubleValue PRESSURE_INTEGRAL;
    private static ModConfigSpec.DoubleValue PRESSURE_DEADBAND;

    /** Samples a chart recorder draws on one sheet of paper. */
    public static int chartSamplesPerPaper = 240;
    /** Ticks between two checks of an annunciator panel's windows. */
    public static int annunciatorInterval = 10;
    /** Ticks between two steps of an automatic rod controller. */
    public static int pidInterval = 20;
    public static double pidGain = 1.5;
    public static double pidIntegral = 0.05;
    public static double pidDerivative = 0.0;
    public static double pidDeadband = 0.01;
    public static double pidRateLimit = 0.05;
    /** Pressure controller: the pressurizer answers in seconds, much faster than the rods, so it has its own tuning. */
    public static double pressureGain = 3.0;
    public static double pressureIntegral = 0.3;
    public static double pressureDeadband = 0.004;

    private ControlRoom2Config() {}

    public static void define(ModConfigSpec.Builder builder) {
        CHART_SAMPLES_PER_PAPER = builder
                .comment("Samples a Chart Recorder draws on one sheet of paper (240 fill the whole chart).")
                .defineInRange("controlRoom.chartSamplesPerPaper", 240, 1, 100000);
        ANNUNCIATOR_INTERVAL = builder
                .comment("Ticks between two checks of an Annunciator Panel's windows.")
                .defineInRange("controlRoom.annunciatorInterval", 10, 1, 200);
        PID_INTERVAL = builder
                .comment("Ticks between two steps of an automatic rod controller module.")
                .defineInRange("controlRoom.pid.interval", 20, 1, 1200);
        PID_GAIN = builder
                .comment("Proportional gain: rod insertion per unit of error, the error being a fraction of full scale",
                        "(1000 MW of power, 4000 °C of temperature).")
                .defineInRange("controlRoom.pid.gain", 1.5, 0.0, 100.0);
        PID_INTEGRAL = builder
                .comment("Integral gain: rod insertion per unit of error per second.")
                .defineInRange("controlRoom.pid.integral", 0.05, 0.0, 10.0);
        PID_DERIVATIVE = builder
                .comment("Derivative gain: rod insertion per unit of change of the measurement per second.")
                .defineInRange("controlRoom.pid.derivative", 0.0, 0.0, 100.0);
        PID_DEADBAND = builder
                .comment("Error, as a fraction of full scale, inside which the controller leaves the rods alone.")
                .defineInRange("controlRoom.pid.deadband", 0.01, 0.0, 0.5);
        PID_RATE_LIMIT = builder
                .comment("Largest change of the rod target per second, as a fraction of full insertion (0 for no limit).")
                .defineInRange("controlRoom.pid.rateLimit", 0.05, 0.0, 1.0);
        PRESSURE_GAIN = builder
                .comment("Automatic pressure controller, proportional gain: heater and spray demand per unit of error, the",
                        "error being a fraction of 250 bar. The controller's output runs from full heaters to full spray.")
                .defineInRange("controlRoom.pid.pressure.gain", 3.0, 0.0, 100.0);
        PRESSURE_INTEGRAL = builder
                .comment("Automatic pressure controller, integral gain: demand per unit of error per second.")
                .defineInRange("controlRoom.pid.pressure.integral", 0.3, 0.0, 10.0);
        PRESSURE_DEADBAND = builder
                .comment("Error, as a fraction of 250 bar, inside which the pressure controller holds its demand (0.004 is 1 bar).")
                .defineInRange("controlRoom.pid.pressure.deadband", 0.004, 0.0, 0.5);
    }

    public static void bake() {
        chartSamplesPerPaper = CHART_SAMPLES_PER_PAPER.get();
        annunciatorInterval = ANNUNCIATOR_INTERVAL.get();
        pidInterval = PID_INTERVAL.get();
        pidGain = PID_GAIN.get();
        pidIntegral = PID_INTEGRAL.get();
        pidDerivative = PID_DERIVATIVE.get();
        pidDeadband = PID_DEADBAND.get();
        pidRateLimit = PID_RATE_LIMIT.get();
        pressureGain = PRESSURE_GAIN.get();
        pressureIntegral = PRESSURE_INTEGRAL.get();
        pressureDeadband = PRESSURE_DEADBAND.get();
    }

    /** The rod controller tuning in force. */
    public static PidController.Tuning pidTuning() {
        return new PidController.Tuning(pidGain, pidIntegral, pidDerivative, pidDeadband, pidRateLimit);
    }

    /** The pressure controller tuning in force: no derivative and no rate limit, the heaters and spray follow at once. */
    public static PidController.Tuning pressureTuning() {
        return new PidController.Tuning(pressureGain, pressureIntegral, 0.0, pressureDeadband, 0.0);
    }

    /** The tuning a controller module set to {@code function} runs with. */
    public static PidController.Tuning tuningFor(ModuleFunction function) {
        return function == ModuleFunction.AUTO_PRESSURE ? pressureTuning() : pidTuning();
    }
}
