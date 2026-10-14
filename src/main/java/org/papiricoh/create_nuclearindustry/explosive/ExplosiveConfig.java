package org.papiricoh.create_nuclearindustry.explosive;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.papiricoh.create_nuclearindustry.Config;
import org.papiricoh.create_nuclearindustry.civildefense.CivilDefenseConfig;
import org.papiricoh.create_nuclearindustry.explosive.blast.DetonationProfile;

/**
 * Common config for what a nuclear detonation does to the world, under {@code explosive.*}: the crater,
 * the blast zones, the thermal flash, prompt radiation, the fallout's drift, falling debris and who sees
 * it. The per-tick budgets stay in {@link org.papiricoh.create_nuclearindustry.PerformanceConfig}. Values
 * are defined on the shared builder by {@link Config} and copied into public statics on load.
 */
public final class ExplosiveConfig {
    public static boolean crater = true;
    public static double severeOverpressure = 35.0;
    public static double moderateOverpressure = 12.0;
    public static double blastProofResistance = 1200.0;
    public static double thermalReach = 1.6;
    public static boolean thermalFires = true;
    public static int thermalFiresPerTick = 24;
    public static double promptEdgeDose = 3000.0;
    public static double windStretch = 1.0;
    public static int fallingDebrisPerTick = 40;
    public static int maxFallingDebris = 1500;
    public static double airburstHeight = 2.0;
    public static int visualRange = 1536;

    private static ModConfigSpec.BooleanValue craterValue;
    private static ModConfigSpec.DoubleValue severeOverpressureValue;
    private static ModConfigSpec.DoubleValue moderateOverpressureValue;
    private static ModConfigSpec.DoubleValue blastProofResistanceValue;
    private static ModConfigSpec.DoubleValue thermalReachValue;
    private static ModConfigSpec.BooleanValue thermalFiresValue;
    private static ModConfigSpec.IntValue thermalFiresPerTickValue;
    private static ModConfigSpec.DoubleValue promptEdgeDoseValue;
    private static ModConfigSpec.DoubleValue windStretchValue;
    private static ModConfigSpec.IntValue fallingDebrisPerTickValue;
    private static ModConfigSpec.IntValue maxFallingDebrisValue;
    private static ModConfigSpec.DoubleValue airburstHeightValue;
    private static ModConfigSpec.IntValue visualRangeValue;

    private ExplosiveConfig() {}

    public static void define(ModConfigSpec.Builder builder) {
        craterValue = builder
                .comment("Whether nuclear detonations change the ground: the crater, the blast zones, scorching and fires.",
                        "Off, they still hurt, irradiate and contaminate.")
                .define("explosive.crater.enabled", true);
        blastProofResistanceValue = builder
                .comment("Blocks with an explosion resistance above this survive a nuclear detonation (bedrock, blast doors).")
                .defineInRange("explosive.crater.blastProofResistance", 1200.0, 0.0, 1.0e7);
        severeOverpressureValue = builder
                .comment("Blast overpressure, kPa, inside which everything standing above the ground is blown away.")
                .defineInRange("explosive.zones.severeOverpressure", 35.0, 1.0, 10000.0);
        moderateOverpressureValue = builder
                .comment("Blast overpressure, kPa, inside which part of what stands above the ground is wrecked and roofs cave in.")
                .defineInRange("explosive.zones.moderateOverpressure", 12.0, 1.0, 10000.0);
        thermalReachValue = builder
                .comment("How far the thermal flash reaches, as a multiple of the warhead's destruction radius: whatever sees",
                        "the fireball inside it burns, and the ground scorches, trees char and fires start closer in.")
                .defineInRange("explosive.thermal.reach", 1.6, 0.0, 10.0);
        thermalFiresValue = builder
                .comment("Whether the thermal flash sets fires on flammable blocks in the open.")
                .define("explosive.thermal.fires", true);
        thermalFiresPerTickValue = builder
                .comment("Most fires the flash starts per tick while a detonation is worked through.")
                .defineInRange("explosive.thermal.firesPerTick", 24, 0, 10000);
        promptEdgeDoseValue = builder
                .comment("Prompt radiation dose at the edge of the fireball, in the dose players carry (sickness from",
                        "radiation.sicknessThreshold). It falls with the square of the distance and with the air in between,",
                        "and walls cut it like other radiation. 0 turns prompt radiation off.")
                .defineInRange("explosive.prompt.edgeDose", 3000.0, 0.0, 1.0e7);
        windStretchValue = builder
                .comment("How much the wind stretches a detonation's fallout into a plume: 0 keeps a circle, 1 reaches twice as",
                        "far downwind and half as far upwind.")
                .defineInRange("explosive.fallout.windStretch", 1.0, 0.0, 1.0);
        fallingDebrisPerTickValue = builder
                .comment("Unsupported blocks at the crater's rim and in the wrecked zone that fall as falling blocks, per tick.")
                .defineInRange("explosive.debris.fallingPerTick", 40, 0, 10000);
        maxFallingDebrisValue = builder
                .comment("Most falling blocks one detonation makes; past it, unsupported blocks just stay.")
                .defineInRange("explosive.debris.maxFalling", 1500, 0, 1000000);
        airburstHeightValue = builder
                .comment("How high a missile set to airburst goes off above its target, as a multiple of its fireball's radius.")
                .defineInRange("explosive.airburstHeight", 2.0, 1.1, 20.0);
        visualRangeValue = builder
                .comment("Players within this many blocks of a detonation see its flash, fireball, cloud and shock wave.")
                .defineInRange("explosive.visualRange", 1536, 0, 100000);
    }

    public static void bake() {
        crater = craterValue.get();
        blastProofResistance = blastProofResistanceValue.get();
        severeOverpressure = severeOverpressureValue.get();
        moderateOverpressure = Math.min(moderateOverpressureValue.get(), severeOverpressure);
        thermalReach = thermalReachValue.get();
        thermalFires = thermalFiresValue.get();
        thermalFiresPerTick = thermalFiresPerTickValue.get();
        promptEdgeDose = promptEdgeDoseValue.get();
        windStretch = windStretchValue.get();
        fallingDebrisPerTick = fallingDebrisPerTickValue.get();
        maxFallingDebris = maxFallingDebrisValue.get();
        airburstHeight = airburstHeightValue.get();
        visualRange = visualRangeValue.get();
    }

    /** The zones' measures, from this config and the blast wave's and contamination's. */
    public static DetonationProfile.Settings settings() {
        return new DetonationProfile.Settings(CivilDefenseConfig.blastWaveYieldPerPower, severeOverpressure, moderateOverpressure,
                thermalReach, NuclearDetonation.FALLOUT_PER_POWER, Config.contaminationMax);
    }
}
