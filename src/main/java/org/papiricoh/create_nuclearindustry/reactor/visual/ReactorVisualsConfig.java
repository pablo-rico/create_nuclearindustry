package org.papiricoh.create_nuclearindustry.reactor.visual;

import net.minecraft.util.Mth;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Common config for the visible and audible fission reactor, under {@code reactorVisuals.*}. Values
 * are defined on the shared builder by {@link org.papiricoh.create_nuclearindustry.Config} and
 * copied into public statics on load.
 */
public final class ReactorVisualsConfig {
    private static ModConfigSpec.IntValue ROD_UPDATE_INTERVAL;

    public static int rodUpdateInterval = 20;

    private ReactorVisualsConfig() {}

    public static void define(ModConfigSpec.Builder builder) {
        ROD_UPDATE_INTERVAL = builder
                .comment("Ticks between updates of what the reactor's fuel channels show (fuel, glow, coolant).")
                .defineInRange("reactorVisuals.rodUpdateInterval", 20, 1, 1200);
    }

    public static void bake() {
        rodUpdateInterval = Mth.clamp(ROD_UPDATE_INTERVAL.get(), 1, 1200);
    }
}
