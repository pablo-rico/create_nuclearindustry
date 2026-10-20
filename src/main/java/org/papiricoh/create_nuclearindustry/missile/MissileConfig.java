package org.papiricoh.create_nuclearindustry.missile;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.papiricoh.create_nuclearindustry.Config;
import org.papiricoh.create_nuclearindustry.missile.physics.BallisticModel;

import java.util.List;

/**
 * Common config for missiles, under {@code missile.*}. Values are defined on the shared builder by
 * {@link Config} and copied into public statics on load.
 */
public final class MissileConfig {
    public static int launchCountdownSeconds = 10;
    public static double minimumRpm = 32;
    public static double stressImpact = 8;
    private static ModConfigSpec.DoubleValue minimumRpmValue, stressImpactValue;
    public static double gravity = BallisticModel.Params.DEFAULT.gravity();
    public static double riseHeight = BallisticModel.Params.DEFAULT.riseHeight();
    public static double minRange = BallisticModel.Params.DEFAULT.minRange();
    public static double atmosphereTop = BallisticModel.Params.DEFAULT.atmosphereTop();
    public static int terminalTicks = BallisticModel.Params.DEFAULT.terminalTicks();
    public static int showDistance = 256;
    public static int fxRange = 2048;
    public static int fxFarUpdateTicks = 4;
    public static double cepScale = 1.0;
    public static double extraBlockMass = 0.05;
    public static double radarBaseRange = 3000;
    public static double radarEarthRadius = 16000;
    public static double radarNetworkRange = 4096;
    public static double maxRange = 0.0;
    public static int protectedSpawnRadius = 0;
    public static List<String> allowedDimensions = List.of();
    public static int launchPermissionLevel = 0;
    public static int launchCooldownSeconds = 0;
    public static LaunchBroadcast launchBroadcast = LaunchBroadcast.IMPACT_AREA;

    private static ModConfigSpec.IntValue launchCountdownSecondsValue;
    private static ModConfigSpec.DoubleValue gravityValue;
    private static ModConfigSpec.DoubleValue riseHeightValue;
    private static ModConfigSpec.DoubleValue minRangeValue;
    private static ModConfigSpec.DoubleValue atmosphereTopValue;
    private static ModConfigSpec.IntValue terminalTicksValue;
    private static ModConfigSpec.IntValue showDistanceValue;
    private static ModConfigSpec.IntValue fxRangeValue;
    private static ModConfigSpec.IntValue fxFarUpdateTicksValue;
    private static ModConfigSpec.DoubleValue cepScaleValue;
    private static ModConfigSpec.DoubleValue extraBlockMassValue;
    private static ModConfigSpec.DoubleValue radarBaseRangeValue, radarEarthRadiusValue, radarNetworkRangeValue;
    private static ModConfigSpec.DoubleValue maxRangeValue;
    private static ModConfigSpec.IntValue protectedSpawnRadiusValue;
    private static ModConfigSpec.ConfigValue<List<? extends String>> allowedDimensionsValue;
    private static ModConfigSpec.IntValue launchPermissionLevelValue;
    private static ModConfigSpec.IntValue launchCooldownSecondsValue;
    private static ModConfigSpec.EnumValue<LaunchBroadcast> launchBroadcastValue;

    private MissileConfig() {}

    public static void define(ModConfigSpec.Builder builder) {
        minimumRpmValue = builder.defineInRange("missile.machinery.minimumRpm", 32.0, 1.0, 256.0);
        stressImpactValue = builder.defineInRange("missile.machinery.stressImpact", 8.0, 0.0, 1024.0);
        launchCountdownSecondsValue = builder
                .comment("Seconds a launch pad counts down once it is powered and loaded before the missile lifts off.",
                        "Missile alerts require an operating radar; countdowns are private.")
                .defineInRange("missile.launch.countdownSeconds", 10, 1, 600);
        gravityValue = builder
                .comment("Gravity on a missile in flight, blocks per tick squared. It sets how long flights take and how much",
                        "speed a missile needs to reach a target: lower gives longer, higher flights and longer reach.")
                .defineInRange("missile.flight.gravity", BallisticModel.Params.DEFAULT.gravity(), 0.0005, 0.08);
        riseHeightValue = builder
                .comment("Blocks a missile climbs straight up before it turns towards its target, clear of its silo.")
                .defineInRange("missile.flight.riseHeight", BallisticModel.Params.DEFAULT.riseHeight(), 2.0, 256.0);
        minRangeValue = builder
                .comment("Shortest distance, blocks, a shot is flown as: a nearer target is lobbed high instead of skimmed.")
                .defineInRange("missile.flight.minRange", BallisticModel.Params.DEFAULT.minRange(), 16.0, 4096.0);
        atmosphereTopValue = builder
                .comment("Height where the air ends: above it a missile coasts in midcourse, below it it re-enters.")
                .defineInRange("missile.flight.atmosphereTop", BallisticModel.Params.DEFAULT.atmosphereTop(), 64.0, 100000.0);
        terminalTicksValue = builder
                .comment("Ticks before impact counted as a flight's terminal phase.")
                .defineInRange("missile.flight.terminalTicks", BallisticModel.Params.DEFAULT.terminalTicks(), 1, 2400);
        showDistanceValue = builder
                .comment("Players within this many blocks (horizontally) of a missile see it fly. Further away it flies on",
                        "without an entity or loaded chunks.")
                .defineInRange("missile.flight.showDistance", 256, 16, 4096);
        fxRangeValue = builder
                .comment("Players within this many blocks (horizontally) of a missile are sent where it is, to draw its plume, trail",
                        "and sounds from afar. Positions only, never its target.")
                .defineInRange("missile.fx.range", 2048, 256, 8192);
        fxFarUpdateTicksValue = builder
                .comment("Ticks between updates for missiles further than 256 blocks from a player; nearer ones update every tick.")
                .defineInRange("missile.fx.farUpdateTicks", 4, 1, 20);
        cepScaleValue = builder
                .comment("Multiplier on every rocket's circular error probable (the radius half its shots land within), which depends on its",
                        "guidance module: empty, a precision mechanism or a star tracker. 0 makes every rocket land exactly on its target.")
                .defineInRange("missile.guidance.cepScale", 1.0, 0.0, 10.0);
        extraBlockMassValue = builder
                .comment("Dry mass (rocket units; a tank weighs 0.1, a full one 2.1) each block glued or attached to a silo rocket beyond its",
                        "column adds to the stage it falls with, or to the payload.")
                .defineInRange("missile.rocket.extraBlockMass", 0.05, 0.0, 10.0);
        radarBaseRangeValue = builder
                .comment("Blocks a missile radar turning at the minimum speed sees a bare rocket body at. It grows with the fourth root of the",
                        "speed and of the target's signature: a rocket burning shows much further, a lone reentry vehicle less far.",
                        "0 sees every flight in the dimension.")
                .defineInRange("missile.radar.baseRange", 3000.0, 0.0, 1.0e6);
        radarEarthRadiusValue = builder
                .comment("Radius, blocks, of the round world radars see over: nothing below their horizon shows, so a launch far off appears",
                        "only once it climbs. 0 for no horizon.")
                .defineInRange("missile.radar.earthRadius", 16000.0, 0.0, 1.0e8);
        radarNetworkRangeValue = builder
                .comment("Radars with the same link code share what they see if they stand within this many blocks of each other. 0 for any",
                        "distance.")
                .defineInRange("missile.radar.networkRange", 4096.0, 0.0, 1.0e6);
        maxRangeValue = builder
                .comment("Furthest target, blocks (horizontally) from the launch pad, a missile may be launched at; 0 leaves only",
                        "the missile's own reach.")
                .defineInRange("missile.maxRange", 0.0, 0.0, 1.0e6);
        protectedSpawnRadiusValue = builder
                .comment("No missile may be aimed within this many blocks (horizontally) of the world spawn, in the overworld.",
                        "0 protects nothing.")
                .defineInRange("missile.protectedSpawnRadius", 0, 0, 100000);
        allowedDimensionsValue = builder
                .comment("Dimensions launch pads work in, such as \"minecraft:overworld\". Empty allows every dimension.")
                .defineListAllowEmpty("missile.allowedDimensions", List.of(), () -> "minecraft:overworld",
                        o -> o instanceof String name && net.minecraft.resources.ResourceLocation.tryParse(name) != null);
        launchPermissionLevelValue = builder
                .comment("Permission level (0 to 4, as for commands; 2 is an operator) the player answerable for a launch pad must",
                        "have for it to launch: the last one to load it, or whoever placed it.")
                .defineInRange("missile.launchPermissionLevel", 0, 0, 4);
        launchCooldownSecondsValue = builder
                .comment("Seconds a player must wait after one of their launch pads fires before another of theirs will. 0 for none.")
                .defineInRange("missile.launchCooldownSeconds", 0, 0, 604800);
        launchBroadcastValue = builder
                .comment("Deprecated and ignored. Missile alerts now require an operational radar.")
                .defineEnum("missile.launchBroadcast", LaunchBroadcast.IMPACT_AREA);
    }

    public static void bake() {
        minimumRpm = minimumRpmValue.get();
        stressImpact = stressImpactValue.get();
        launchCountdownSeconds = launchCountdownSecondsValue.get();
        gravity = gravityValue.get();
        riseHeight = riseHeightValue.get();
        minRange = minRangeValue.get();
        atmosphereTop = atmosphereTopValue.get();
        terminalTicks = terminalTicksValue.get();
        showDistance = showDistanceValue.get();
        fxRange = fxRangeValue.get();
        fxFarUpdateTicks = fxFarUpdateTicksValue.get();
        cepScale = cepScaleValue.get();
        extraBlockMass = extraBlockMassValue.get();
        radarBaseRange = radarBaseRangeValue.get();
        radarEarthRadius = radarEarthRadiusValue.get();
        radarNetworkRange = radarNetworkRangeValue.get();
        maxRange = maxRangeValue.get();
        protectedSpawnRadius = protectedSpawnRadiusValue.get();
        allowedDimensions = List.copyOf(allowedDimensionsValue.get());
        launchPermissionLevel = launchPermissionLevelValue.get();
        launchCooldownSeconds = launchCooldownSecondsValue.get();
        launchBroadcast = LaunchBroadcast.NONE; // Legacy option is ignored; radar supplies missile alerts.
    }

    /** The launch countdown, in ticks. */
    public static int launchCountdownTicks() {
        return launchCountdownSeconds * 20;
    }

    public static BallisticModel.Params flightParams() {
        return new BallisticModel.Params(gravity, riseHeight, minRange, atmosphereTop, terminalTicks);
    }
}
