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
    public static boolean craterLoadsChunks = true;
    public static int chunksAtOnce = 8;
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
    public static int visualRange = 4096;
    public static int craterMaxReach = 768;
    public static boolean thermonuclearEnabled = true;
    public static int thermonuclearMaxRadius = 750;
    public static double thermonuclearMaxPower = 4320.0;
    public static double thermonuclearMaxCraterRadius = 110.0;
    public static int thermonuclearBlastWaveMaxRadius = 768;
    public static double thermonuclearThermalReach = 1.4;
    public static int thermonuclearCountdownSeconds = 15;
    public static double thermonuclearTamperFalloutReach = 0.5;
    public static double thermonuclearTamperHotSpot = 150.0;

    private static ModConfigSpec.BooleanValue craterValue;
    private static ModConfigSpec.BooleanValue craterLoadsChunksValue;
    private static ModConfigSpec.IntValue chunksAtOnceValue;
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
    private static ModConfigSpec.IntValue craterMaxReachValue;
    private static ModConfigSpec.BooleanValue thermonuclearEnabledValue;
    private static ModConfigSpec.IntValue thermonuclearMaxRadiusValue;
    private static ModConfigSpec.DoubleValue thermonuclearMaxPowerValue;
    private static ModConfigSpec.DoubleValue thermonuclearMaxCraterRadiusValue;
    private static ModConfigSpec.IntValue thermonuclearBlastWaveMaxRadiusValue;
    private static ModConfigSpec.DoubleValue thermonuclearThermalReachValue;
    private static ModConfigSpec.IntValue thermonuclearCountdownSecondsValue;
    private static ModConfigSpec.DoubleValue thermonuclearTamperFalloutReachValue;
    private static ModConfigSpec.DoubleValue thermonuclearTamperHotSpotValue;

    private ExplosiveConfig() {}

    public static void define(ModConfigSpec.Builder builder) {
        craterValue = builder
                .comment("Whether nuclear detonations change the ground: the crater, the blast zones, scorching and fires.",
                        "Off, they still hurt, irradiate and contaminate.")
                .define("explosive.crater.enabled", true);
        craterLoadsChunksValue = builder
                .comment("Whether the crater loads the chunks it reaches that are not loaded, generating them if they never were,",
                        "so a detonation far from every player still leaves its mark. Off, those chunks wait until something",
                        "else loads them.")
                .define("explosive.crater.loadChunks", true);
        chunksAtOnceValue = builder
                .comment("Most chunks one detonation waits on to load or generate at once; each is let go once the crater is",
                        "done with it. Lower spares the world generator, higher finishes far-reaching craters sooner.")
                .defineInRange("explosive.crater.chunksAtOnce", 8, 1, 256);
        craterMaxReachValue = builder
                .comment("Furthest, in blocks from ground zero, a detonation changes the ground: wrecking, scorching and fires",
                        "shrink to fit inside it. The main cost of a big detonation is the columns and chunks it works through,",
                        "which grow with the square of this. A full atomic bomb reaches about 400, so only thermonuclear",
                        "warheads are cut.")
                .defineInRange("explosive.crater.maxReach", 768, 64, 4096);
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
                .comment("Players within this many blocks of a detonation see its flash, fireball, cloud and shock wave. With",
                        "Distant Horizons the cloud shows over its distant terrain too, as far as this reaches.")
                .defineInRange("explosive.visualRange", 4096, 0, 100000);
        thermonuclearEnabledValue = builder
                .comment("Whether thermonuclear warheads can light their secondary. Off, every one fizzles as its fission primary.")
                .define("explosive.thermonuclear.enabled", true);
        thermonuclearMaxRadiusValue = builder
                .comment("Largest destruction radius of a thermonuclear warhead, blocks. A full one reaches 750, three times a",
                        "full atomic bomb.")
                .defineInRange("explosive.thermonuclear.maxRadius", 750, 250, 2000);
        thermonuclearMaxPowerValue = builder
                .comment("Largest blast power of a thermonuclear warhead, on the atomic bomb's scale (a full one is 160). The",
                        "blast wave's strength and the shaking follow it.")
                .defineInRange("explosive.thermonuclear.maxPower", 4320.0, 160.0, 1.0e6);
        thermonuclearMaxCraterRadiusValue = builder
                .comment("Widest crater bowl a thermonuclear warhead digs, blocks: the blast reaches far past it, but a bowl",
                        "growing with it would cost too much (a full atomic bomb digs 75).")
                .defineInRange("explosive.thermonuclear.maxCraterRadius", 110.0, 0.0, 400.0);
        thermonuclearBlastWaveMaxRadiusValue = builder
                .comment("Furthest a thermonuclear warhead's blast wave travels, blocks. Atomic warheads keep",
                        "civilDefense.blastWave.maxRadius.")
                .defineInRange("explosive.thermonuclear.blastWaveMaxRadius", 768, 0, 2048);
        thermonuclearThermalReachValue = builder
                .comment("How far a thermonuclear warhead's thermal flash reaches, a multiple of its destruction radius.")
                .defineInRange("explosive.thermonuclear.thermalReach", 1.4, 0.0, 10.0);
        thermonuclearCountdownSecondsValue = builder
                .comment("Seconds a thermonuclear bomb counts down once powered.")
                .defineInRange("explosive.thermonuclear.countdownSeconds", 15, 1, 600);
        thermonuclearTamperFalloutReachValue = builder
                .comment("How much wider a full depleted uranium tamper makes the fallout, a multiple of the destruction radius",
                        "on top of the usual 1.5.")
                .defineInRange("explosive.thermonuclear.tamperFalloutReach", 0.5, 0.0, 4.0);
        thermonuclearTamperHotSpotValue = builder
                .comment("Strength of the hot spot a full tamper leaves at ground zero, fading over days like a dirty bomb's.")
                .defineInRange("explosive.thermonuclear.tamperHotSpot", 150.0, 0.0, 1000.0);
    }

    public static void bake() {
        crater = craterValue.get();
        craterLoadsChunks = craterLoadsChunksValue.get();
        chunksAtOnce = chunksAtOnceValue.get();
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
        craterMaxReach = craterMaxReachValue.get();
        thermonuclearEnabled = thermonuclearEnabledValue.get();
        thermonuclearMaxRadius = thermonuclearMaxRadiusValue.get();
        thermonuclearMaxPower = thermonuclearMaxPowerValue.get();
        thermonuclearMaxCraterRadius = thermonuclearMaxCraterRadiusValue.get();
        thermonuclearBlastWaveMaxRadius = thermonuclearBlastWaveMaxRadiusValue.get();
        thermonuclearThermalReach = thermonuclearThermalReachValue.get();
        thermonuclearCountdownSeconds = thermonuclearCountdownSecondsValue.get();
        thermonuclearTamperFalloutReach = thermonuclearTamperFalloutReachValue.get();
        thermonuclearTamperHotSpot = thermonuclearTamperHotSpotValue.get();
    }

    /** The zones' measures, from this config and the blast wave's and contamination's. */
    public static DetonationProfile.Settings settings() {
        return settings(thermalReach);
    }

    /** As {@link #settings()}, with the thermal flash reaching {@code reach} destruction radii. */
    public static DetonationProfile.Settings settings(double reach) {
        return new DetonationProfile.Settings(CivilDefenseConfig.blastWaveYieldPerPower, severeOverpressure, moderateOverpressure,
                reach, NuclearDetonation.FALLOUT_PER_POWER, Config.contaminationMax);
    }

    /** What the server lets a thermonuclear warhead reach. */
    public static ThermonuclearStats.Caps thermonuclearCaps() {
        return new ThermonuclearStats.Caps(thermonuclearMaxRadius, thermonuclearMaxPower, thermonuclearMaxCraterRadius,
                thermonuclearTamperFalloutReach, thermonuclearTamperHotSpot);
    }
}
