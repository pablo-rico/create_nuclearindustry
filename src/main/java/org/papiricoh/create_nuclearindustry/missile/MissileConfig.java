package org.papiricoh.create_nuclearindustry.missile;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.papiricoh.create_nuclearindustry.Config;
import org.papiricoh.create_nuclearindustry.missile.physics.BallisticModel;

/**
 * Common config for missiles, under {@code missile.*}. Values are defined on the shared builder by
 * {@link Config} and copied into public statics on load.
 */
public final class MissileConfig {
    public static int launchCountdownSeconds = 10;
    public static double gravity = BallisticModel.Params.DEFAULT.gravity();
    public static double riseHeight = BallisticModel.Params.DEFAULT.riseHeight();
    public static double minRange = BallisticModel.Params.DEFAULT.minRange();
    public static double atmosphereTop = BallisticModel.Params.DEFAULT.atmosphereTop();
    public static int terminalTicks = BallisticModel.Params.DEFAULT.terminalTicks();
    public static int showDistance = 256;

    private static ModConfigSpec.IntValue launchCountdownSecondsValue;
    private static ModConfigSpec.DoubleValue gravityValue;
    private static ModConfigSpec.DoubleValue riseHeightValue;
    private static ModConfigSpec.DoubleValue minRangeValue;
    private static ModConfigSpec.DoubleValue atmosphereTopValue;
    private static ModConfigSpec.IntValue terminalTicksValue;
    private static ModConfigSpec.IntValue showDistanceValue;

    private MissileConfig() {}

    public static void define(ModConfigSpec.Builder builder) {
        launchCountdownSecondsValue = builder
                .comment("Seconds a launch pad counts down once it is powered and loaded before the missile lifts off.",
                        "The sirens around the target are warned for the whole countdown.")
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
    }

    public static void bake() {
        launchCountdownSeconds = launchCountdownSecondsValue.get();
        gravity = gravityValue.get();
        riseHeight = riseHeightValue.get();
        minRange = minRangeValue.get();
        atmosphereTop = atmosphereTopValue.get();
        terminalTicks = terminalTicksValue.get();
        showDistance = showDistanceValue.get();
    }

    /** The launch countdown, in ticks. */
    public static int launchCountdownTicks() {
        return launchCountdownSeconds * 20;
    }

    public static BallisticModel.Params flightParams() {
        return new BallisticModel.Params(gravity, riseHeight, minRange, atmosphereTop, terminalTicks);
    }
}
