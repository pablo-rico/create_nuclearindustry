package org.papiricoh.create_nuclearindustry.missile;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.papiricoh.create_nuclearindustry.Config;

/**
 * Common config for missiles, under {@code missile.*}. Values are defined on the shared builder by
 * {@link Config} and copied into public statics on load.
 */
public final class MissileConfig {
    public static int launchCountdownSeconds = 10;

    private static ModConfigSpec.IntValue launchCountdownSecondsValue;

    private MissileConfig() {}

    public static void define(ModConfigSpec.Builder builder) {
        launchCountdownSecondsValue = builder
                .comment("Seconds a launch pad counts down once it is powered and loaded before the missile lifts off.",
                        "The sirens around the target are warned for the whole countdown.")
                .defineInRange("missile.launch.countdownSeconds", 10, 1, 600);
    }

    public static void bake() {
        launchCountdownSeconds = launchCountdownSecondsValue.get();
    }

    /** The launch countdown, in ticks. */
    public static int launchCountdownTicks() {
        return launchCountdownSeconds * 20;
    }
}
