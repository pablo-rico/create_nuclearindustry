package org.papiricoh.create_nuclearindustry.fusion.heating;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Common config for the tokamak's neutral beam injectors, radio-frequency antennas and viewports, under {@code plasmaHeating.*}. Values are defined on the shared builder
 * by {@link org.papiricoh.create_nuclearindustry.Config} and copied into public statics on load.
 */
public final class PlasmaHeatingConfig {
    private static ModConfigSpec.DoubleValue NBI_POWER;
    private static ModConfigSpec.DoubleValue NBI_RATED_SPEED;
    private static ModConfigSpec.DoubleValue NBI_MIN_SPEED;
    private static ModConfigSpec.DoubleValue NBI_STRESS;
    private static ModConfigSpec.DoubleValue NBI_DEUTERIUM;
    private static ModConfigSpec.DoubleValue RF_POWER;
    private static ModConfigSpec.DoubleValue RF_RATED_SPEED;
    private static ModConfigSpec.DoubleValue RF_MIN_SPEED;
    private static ModConfigSpec.DoubleValue RF_STRESS;
    private static ModConfigSpec.BooleanValue VIEWPORT_LIGHT;

    public static double neutralBeamPower = 20.0;
    public static double neutralBeamRatedSpeed = 128.0;
    public static double neutralBeamMinSpeed = 32.0;
    public static double neutralBeamStress = 32768.0;
    public static double neutralBeamDeuteriumPerMw = 0.02;
    public static double radioFrequencyPower = 12.0;
    public static double radioFrequencyRatedSpeed = 64.0;
    public static double radioFrequencyMinSpeed = 16.0;
    public static double radioFrequencyStress = 13312.0;
    public static boolean viewportLight = true;

    private PlasmaHeatingConfig() {}

    public static void define(ModConfigSpec.Builder builder) {
        NBI_POWER = builder
                .comment("Heating power one Neutral Beam Injector gives the plasma at its rated speed, in MW.")
                .defineInRange("plasmaHeating.neutralBeam.ratedPower", 20.0, 0.0, 1000.0);
        NBI_RATED_SPEED = builder
                .comment("Shaft speed at which a Neutral Beam Injector reaches its rated power, in RPM.")
                .defineInRange("plasmaHeating.neutralBeam.ratedSpeed", 128.0, 1.0, 256.0);
        NBI_MIN_SPEED = builder
                .comment("Shaft speed below which a Neutral Beam Injector gives nothing, in RPM. From here its power rises",
                        "linearly to the rated power. Must be below the rated speed.")
                .defineInRange("plasmaHeating.neutralBeam.minSpeed", 32.0, 0.0, 255.0);
        NBI_STRESS = builder
                .comment("Stress impact of a Neutral Beam Injector, in SU per RPM. The default makes a megawatt of beam cost",
                        "about 1600 SU/RPM, roughly two and a half times what the plasma turbine gets back from a megawatt",
                        "of fusion power: a tokamak needs a gain above about 3 to make net energy.")
                .defineInRange("plasmaHeating.neutralBeam.stress", 32768.0, 0.0, 1.0e6);
        NBI_DEUTERIUM = builder
                .comment("Deuterium the beams take from the tokamak's tank, in mB per tick per MW of beam power. Without it",
                        "the beams lose power in proportion.")
                .defineInRange("plasmaHeating.neutralBeam.deuteriumPerMw", 0.02, 0.0, 10.0);
        RF_POWER = builder
                .comment("Heating power one Radio-Frequency Antenna sends into the plasma at its rated speed, in MW. A plasma",
                        "below about 15% of its density limit absorbs none of it, and all of it above about 35%.")
                .defineInRange("plasmaHeating.radioFrequency.ratedPower", 12.0, 0.0, 1000.0);
        RF_RATED_SPEED = builder
                .comment("Shaft speed at which a Radio-Frequency Antenna reaches its rated power, in RPM.")
                .defineInRange("plasmaHeating.radioFrequency.ratedSpeed", 64.0, 1.0, 256.0);
        RF_MIN_SPEED = builder
                .comment("Shaft speed below which a Radio-Frequency Antenna gives nothing, in RPM. Must be below the rated speed.")
                .defineInRange("plasmaHeating.radioFrequency.minSpeed", 16.0, 0.0, 255.0);
        RF_STRESS = builder
                .comment("Stress impact of a Radio-Frequency Antenna, in SU per RPM.")
                .defineInRange("plasmaHeating.radioFrequency.stress", 13312.0, 0.0, 1.0e6);
        VIEWPORT_LIGHT = builder
                .comment("Whether Tokamak Viewports give off light while the plasma is lit. Off, they stay dark windows.")
                .define("plasmaHeating.viewportLight", true);
    }

    public static void bake() {
        neutralBeamPower = NBI_POWER.get();
        neutralBeamRatedSpeed = NBI_RATED_SPEED.get();
        neutralBeamMinSpeed = Math.min(NBI_MIN_SPEED.get(), neutralBeamRatedSpeed - 1.0);
        neutralBeamStress = NBI_STRESS.get();
        neutralBeamDeuteriumPerMw = NBI_DEUTERIUM.get();
        radioFrequencyPower = RF_POWER.get();
        radioFrequencyRatedSpeed = RF_RATED_SPEED.get();
        radioFrequencyMinSpeed = Math.min(RF_MIN_SPEED.get(), radioFrequencyRatedSpeed - 1.0);
        radioFrequencyStress = RF_STRESS.get();
        viewportLight = VIEWPORT_LIGHT.get();
    }

    /** The beam injector's drive curve, from the current config. */
    public static HeatingDrive.Params neutralBeam() {
        return new HeatingDrive.Params(neutralBeamPower, neutralBeamRatedSpeed, Math.max(0, neutralBeamMinSpeed));
    }

    /** The antenna's drive curve, from the current config. */
    public static HeatingDrive.Params radioFrequency() {
        return new HeatingDrive.Params(radioFrequencyPower, radioFrequencyRatedSpeed, Math.max(0, radioFrequencyMinSpeed));
    }
}
