package org.papiricoh.create_nuclearindustry.reactor.visual;

/**
 * When the fission plant's sounds play and how loud and high they are, from the readings alone, so
 * the mapping can be tested without a client. The same thresholds mark the reactor's milestones:
 * full power, and the cold shutdown that ends a SCRAM.
 */
public final class ReactorSoundModel {
    /** Share of full power that counts as running flat out. */
    public static final double FULL_POWER_FRACTION = 0.95;
    /** Share of full power the reactor must fall below before reaching full power counts again. */
    public static final double FULL_POWER_RESET_FRACTION = 0.8;
    /** Core temperature, °C, below which a scrammed core is cold: the klaxon stops. */
    public static final double COLD_SHUTDOWN_TEMPERATURE = 100.0;
    /** Steam through one output port at which its hiss is loudest, mB/t: 320 MW of boiling at the default steam energy. */
    public static final double FULL_STEAM_FLOW = 80.0;
    /** Rotation at which a machine's sound is highest, RPM. */
    public static final double FULL_RPM = 256.0;

    private ReactorSoundModel() {}

    /** Power as a share of the core's rated power, 0..1; nothing without a rating. */
    public static double powerFraction(double powerMw, double ratedMw) {
        if (!(ratedMw > 0)) {
            return 0;
        }
        return clamp01(powerMw / ratedMw);
    }

    // ------------------------------------------------------------------ the core

    /** The core hums while the chain reaction runs. */
    public static boolean humWanted(boolean formed, double powerFraction) {
        return formed && powerFraction >= 0.001;
    }

    public static float humVolume(double powerFraction) {
        return (float) (0.3 + 0.7 * clamp01(powerFraction));
    }

    public static float humPitch(double powerFraction) {
        return (float) (0.7 + 0.5 * clamp01(powerFraction));
    }

    /** The klaxon sounds after a SCRAM until the core is cold. */
    public static boolean klaxonWanted(boolean scrammed, double temperature) {
        return scrammed && temperature >= COLD_SHUTDOWN_TEMPERATURE;
    }

    /**
     * The over-temperature alarm sounds in the warning zone and above.
     *
     * @param warning core temperature, °C, where the warning zone starts (the cladding failure temperature)
     */
    public static boolean alarmWanted(boolean formed, double temperature, double warning) {
        return formed && temperature > warning;
    }

    /** How far into the warning zone the core is, 0 at {@code warning}, 1 at {@code meltdown}. */
    public static double danger(double temperature, double warning, double meltdown) {
        return clamp01((temperature - warning) / Math.max(1, meltdown - warning));
    }

    public static float alarmVolume(double temperature, double warning, double meltdown) {
        return (float) (0.6 + 0.4 * danger(temperature, warning, meltdown));
    }

    /** The alarm rises a little in pitch as the core nears meltdown; only a little, so it never turns shrill. */
    public static float alarmPitch(double temperature, double warning, double meltdown) {
        return (float) (1.0 + 0.15 * danger(temperature, warning, meltdown));
    }

    // ------------------------------------------------------------------ steam and coolant

    /** Share of a port's full flow, 0..1. */
    public static double flowFraction(double flowPerPort) {
        return clamp01(flowPerPort / FULL_STEAM_FLOW);
    }

    public static boolean flowWanted(double flowPerPort) {
        return flowPerPort > 0.5;
    }

    public static float hissVolume(double flowPerPort) {
        return (float) (0.2 + 0.5 * flowFraction(flowPerPort));
    }

    public static float hissPitch(double flowPerPort) {
        return (float) (0.85 + 0.3 * flowFraction(flowPerPort));
    }

    public static float bubblingVolume(double flowPerPort) {
        return (float) (0.25 + 0.45 * flowFraction(flowPerPort));
    }

    public static float bubblingPitch(double flowPerPort) {
        return (float) (0.9 + 0.2 * flowFraction(flowPerPort));
    }

    /** A lifted relief valve blows hard; pitched up, the steam would hiss shrilly. */
    public static final float RELIEF_VOLUME = 1.0f;
    public static final float RELIEF_PITCH = 1.05f;

    // ------------------------------------------------------------------ machines

    /** Share of full speed, 0..1, either direction. */
    public static double rpmFraction(double rpm) {
        return clamp01(Math.abs(rpm) / FULL_RPM);
    }

    /** A gas centrifuge whines higher the faster it spins. */
    public static float centrifugePitch(double rpm) {
        return (float) (0.6 + 1.2 * rpmFraction(rpm));
    }

    public static float centrifugeVolume(double rpm) {
        return (float) (0.35 + 0.4 * rpmFraction(rpm));
    }

    /** An electrolyzer's cells buzz louder the more current the shaft drives. */
    public static float electrolyzerVolume(double rpm) {
        return (float) (0.3 + 0.5 * rpmFraction(rpm));
    }

    public static float electrolyzerPitch(double rpm) {
        return (float) (0.9 + 0.2 * rpmFraction(rpm));
    }

    /** The melter roars louder and higher over a superheated burner. */
    public static float melterVolume(boolean superheated) {
        return superheated ? 0.9f : 0.65f;
    }

    public static float melterPitch(boolean superheated) {
        return superheated ? 1.15f : 0.95f;
    }

    /** A radioisotope generator hums softly, fading with its pellet. */
    public static float rtgVolume(double charge) {
        return (float) (0.1 + 0.25 * clamp01(charge));
    }

    public static float rtgPitch(double charge) {
        return (float) (0.9 + 0.1 * clamp01(charge));
    }

    private static double clamp01(double value) {
        if (!Double.isFinite(value)) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, value));
    }
}
