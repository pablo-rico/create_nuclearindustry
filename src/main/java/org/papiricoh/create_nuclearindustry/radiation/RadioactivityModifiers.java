package org.papiricoh.create_nuclearindustry.radiation;

import net.minecraft.world.item.ItemStack;

/**
 * Per-stack adjustments on top of the {@link Radioactivity} data map, for items whose activity
 * depends on their history (fresh spent fuel is hotter than old). Applied by {@link RadiationHandler}.
 */
public final class RadioactivityModifiers {

    private RadioactivityModifiers() {}

    /** Multiplier on the dose rate of one item of {@code stack}. */
    public static double doseScale(ItemStack stack) {
        return 1.0;
    }

    /** Decay heat per item added to the data map's, in the units of {@link Radioactivity#heatPerSecond()}. */
    public static double extraHeat(ItemStack stack) {
        return 0.0;
    }
}
