package org.papiricoh.create_nuclearindustry.fuelcycle.jei;

import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;

/**
 * JEI for the renewable fuel cycle: the acid leaching and irradiation fan treatments' categories,
 * recipes and catalysts. Called by {@link org.papiricoh.create_nuclearindustry.integration.jei.NuclearJeiPlugin};
 * only loaded when JEI is.
 */
public final class FuelCycleJei {

    private FuelCycleJei() {}

    public static void registerCategories(IRecipeCategoryRegistration registration) {}

    public static void registerRecipes(IRecipeRegistration registration) {}

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {}
}
