package org.papiricoh.create_nuclearindustry.waste;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Common config for waste classes, treatments and the geological repository, under {@code waste.*}. Values are defined on the shared builder
 * by {@link org.papiricoh.create_nuclearindustry.Config} and copied into public statics on load.
 */
public final class WasteConfig {

    private WasteConfig() {}

    public static void define(ModConfigSpec.Builder builder) {}

    public static void bake() {}
}
