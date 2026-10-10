package org.papiricoh.create_nuclearindustry.spentfuel;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Common config for spent fuel cooling: pool racks and dry casks, under {@code spentFuel.*}. Values are defined on the shared builder
 * by {@link org.papiricoh.create_nuclearindustry.Config} and copied into public statics on load.
 */
public final class SpentFuelConfig {

    private SpentFuelConfig() {}

    public static void define(ModConfigSpec.Builder builder) {}

    public static void bake() {}
}
