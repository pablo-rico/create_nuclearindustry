package org.papiricoh.create_nuclearindustry.reactor.pwr;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Common config for the pressurized water loop, steam generator, containment and the accident chain, under {@code pwr.*}. Values are defined on the shared builder
 * by {@link org.papiricoh.create_nuclearindustry.Config} and copied into public statics on load.
 */
public final class PwrConfig {

    private PwrConfig() {}

    public static void define(ModConfigSpec.Builder builder) {}

    public static void bake() {}
}
