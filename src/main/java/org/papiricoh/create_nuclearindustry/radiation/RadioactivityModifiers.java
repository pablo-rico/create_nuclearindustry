package org.papiricoh.create_nuclearindustry.radiation;

import net.minecraft.world.item.ItemStack;
import org.papiricoh.create_nuclearindustry.isotope.Isotopes;
import org.papiricoh.create_nuclearindustry.spentfuel.SpentFuel;

/**
 * Per-stack adjustments on top of the {@link Radioactivity} data map, for items whose activity
 * depends on their history (fresh spent fuel is hotter than old, an isotope weakens as it decays).
 * Applied by {@link RadiationHandler}.
 */
public final class RadioactivityModifiers {

    private RadioactivityModifiers() {}

    /**
     * Multiplier on the dose rate of one item of {@code stack}. Spent fuel follows its cooling grade;
     * the data map gives Warm fuel's rate, so fuel from before cooling existed is unchanged. An
     * isotope source follows its activity left: the data map gives a fresh source's rate.
     */
    public static double doseScale(ItemStack stack) {
        if (Isotopes.isotopeOf(stack) != null) {
            return Isotopes.doseScale(stack);
        }
        return SpentFuel.isSpentFuel(stack) ? SpentFuel.doseScale(SpentFuel.gradeOf(stack)) : 1.0;
    }

    /** Decay heat per item added to the data map's, in the units of {@link Radioactivity#heatPerSecond()}: Hot spent fuel burns. */
    public static double extraHeat(ItemStack stack) {
        return SpentFuel.isSpentFuel(stack) ? SpentFuel.burnHeat(SpentFuel.gradeOf(stack)) : 0.0;
    }
}
