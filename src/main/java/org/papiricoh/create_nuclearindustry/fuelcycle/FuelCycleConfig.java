package org.papiricoh.create_nuclearindustry.fuelcycle;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Common config for the renewable fuel cycle (acid leaching, irradiation, brine and thorium), under
 * {@code fuelcycle.*}. Values are defined on the shared builder by
 * {@link org.papiricoh.create_nuclearindustry.Config} and copied into public statics on load.
 */
public final class FuelCycleConfig {
    private static ModConfigSpec.DoubleValue IRRADIATION_MIN_ACTIVITY;

    public static double irradiationMinActivity = 2.0;

    private FuelCycleConfig() {}

    public static void define(ModConfigSpec.Builder builder) {
        IRRADIATION_MIN_ACTIVITY = builder
                .comment("How radioactive a block must be (its item's radioactivity, in rad/s) for a fan blowing past it to",
                        "irradiate the items in its air current. Blocks in the irradiation catalyst tag always do.")
                .defineInRange("fuelcycle.irradiationMinActivity", 2.0, 0.0, 1000.0);
    }

    public static void bake() {
        irradiationMinActivity = IRRADIATION_MIN_ACTIVITY.get();
    }
}
