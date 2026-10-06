package org.papiricoh.create_nuclearindustry.data.recipe;

import com.simibubi.create.api.data.recipe.MixingRecipeGen;
import com.simibubi.create.content.processing.recipe.HeatCondition;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluids;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.AllNuclearTags;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.enrichment.EnrichmentTier;
import org.papiricoh.create_nuclearindustry.enrichment.recipe.MinEnrichmentIngredient;

import java.util.concurrent.CompletableFuture;

import static org.papiricoh.create_nuclearindustry.data.recipe.RecipeHelpers.enriched;

@SuppressWarnings("unused")
public class NuclearMixingRecipes extends MixingRecipeGen {

    // ------------------------------------------------------------------ reagents

    // Gunpowder is the only sulfur source in vanilla.
    GeneratedRecipe SULFURIC_ACID = create("sulfuric_acid", b -> b
            .require(Fluids.WATER, 500)
            .require(Items.GUNPOWDER)
            .output(AllNuclearFluids.SULFURIC_ACID.get(), 250)
            .requiresHeat(HeatCondition.HEATED));

    // CaF2 + H2SO4 -> 2 HF + CaSO4
    GeneratedRecipe HYDROFLUORIC_ACID = create("hydrofluoric_acid", b -> b
            .require(AllNuclearItems.FLUORITE.get())
            .require(AllNuclearFluids.SULFURIC_ACID.get(), 250)
            .output(AllNuclearFluids.HYDROFLUORIC_ACID.get(), 500)
            .requiresHeat(HeatCondition.HEATED));

    // ------------------------------------------------------------------ uranium front end

    // Acid leaching of the washed concentrate precipitates U3O8.
    GeneratedRecipe YELLOWCAKE = create("yellowcake", b -> b
            .require(AllNuclearItems.URANIUM_CONCENTRATE.get())
            .require(AllNuclearFluids.SULFURIC_ACID.get(), 100)
            .output(AllNuclearItems.YELLOWCAKE.get())
            .requiresHeat(HeatCondition.HEATED));

    // Hydrofluorination: U3O8 + HF (+ F2) -> UF6
    GeneratedRecipe URANIUM_HEXAFLUORIDE = create("uranium_hexafluoride", b -> b
            .require(AllNuclearItems.YELLOWCAKE.get())
            .require(AllNuclearFluids.HYDROFLUORIC_ACID.get(), 250)
            .output(AllNuclearFluids.URANIUM_HEXAFLUORIDE.get(), 500)
            .requiresHeat(HeatCondition.SUPERHEATED));

    // ------------------------------------------------------------------ lithium

    // Acid roast of the concentrate, then carbonate precipitation.
    GeneratedRecipe LITHIUM_CARBONATE = create("lithium_carbonate", b -> b
            .require(AllNuclearItems.SPODUMENE_CONCENTRATE.get())
            .require(AllNuclearFluids.SULFURIC_ACID.get(), 100)
            .output(AllNuclearItems.LITHIUM_CARBONATE.get())
            .requiresHeat(HeatCondition.HEATED));

    // ------------------------------------------------------------------ spent fuel reprocessing (PUREX)

    // Saltpetre (from gunpowder) + H2SO4 -> HNO3
    GeneratedRecipe NITRIC_ACID = create("nitric_acid", b -> b
            .require(Items.GUNPOWDER)
            .require(AllNuclearFluids.SULFURIC_ACID.get(), 250)
            .output(AllNuclearFluids.NITRIC_ACID.get(), 250)
            .requiresHeat(HeatCondition.HEATED));

    GeneratedRecipe NITRIC_ACID_FROM_SALTPETER = create("nitric_acid_from_saltpeter", b -> b
            .require(AllNuclearTags.Items.SALTPETER_DUSTS)
            .require(AllNuclearFluids.SULFURIC_ACID.get(), 250)
            .output(AllNuclearFluids.NITRIC_ACID.get(), 500)
            .requiresHeat(HeatCondition.HEATED));

    // Dissolving the sheared fuel in nitric acid: the uranium comes back one tier below what went in,
    // the plutonium and fission products go into solution. Leaner fuel bred more plutonium.
    GeneratedRecipe DISSOLVE_LOW = dissolve(EnrichmentTier.LOW, 500);
    GeneratedRecipe DISSOLVE_HIGH = dissolve(EnrichmentTier.HIGH, 250);
    // Spent MOX keeps less plutonium than went in: recycling it always loses some.
    GeneratedRecipe DISSOLVE_MOX = dissolve(EnrichmentTier.DEPLETED, 250);

    // Pu(NO3)4 precipitated as oxalate and calcined to PuO2; part of the acid is recovered.
    GeneratedRecipe PLUTONIUM_DIOXIDE = create("plutonium_dioxide", b -> b
            .require(AllNuclearFluids.PLUTONIUM_NITRATE.get(), 250)
            .output(AllNuclearItems.PLUTONIUM_DIOXIDE.get())
            .output(AllNuclearFluids.NITRIC_ACID.get(), 100)
            .requiresHeat(HeatCondition.HEATED));

    // Reduced to metal like uranium.
    GeneratedRecipe PLUTONIUM = create("plutonium", b -> b
            .require(AllNuclearItems.PLUTONIUM_DIOXIDE.get())
            .require(Items.BONE_MEAL)
            .output(AllNuclearItems.PLUTONIUM.get())
            .requiresHeat(HeatCondition.SUPERHEATED));

    private GeneratedRecipe dissolve(EnrichmentTier fuel, int solution) {
        return create("dissolve_" + fuel.id() + "_spent_fuel", b -> b
                .require(MinEnrichmentIngredient.tier(AllNuclearItems.SPENT_FUEL_FRAGMENTS.get(), fuel))
                .require(AllNuclearFluids.NITRIC_ACID.get(), 250)
                .output(enriched(AllNuclearItems.URANYL_NITRATE.get(), fuel.previous()))
                .output(AllNuclearFluids.SPENT_FUEL_SOLUTION.get(), solution)
                .requiresHeat(HeatCondition.HEATED));
    }

    // ------------------------------------------------------------------ per-tier conversions

    {
        for (EnrichmentTier tier : EnrichmentTier.values()) {
            // Deconversion: UF6 + 2 H2O -> UO2F2 + 4 HF, reduced to UO2. Part of the HF is recovered.
            create(tier.id() + "_uranium_dioxide", b -> b
                    .require(tier.hexafluoride(), 250)
                    .require(Fluids.WATER, 250)
                    .output(enriched(AllNuclearItems.URANIUM_DIOXIDE.get(), tier))
                    .output(AllNuclearFluids.HYDROFLUORIC_ACID.get(), 100)
                    .requiresHeat(HeatCondition.HEATED));

            // Calciothermic reduction to metal: what bombs, cannons and other mods' recipes use.
            create(tier.id() + "_uranium_metal", b -> b
                    .require(MinEnrichmentIngredient.tier(AllNuclearItems.URANIUM_DIOXIDE.get(), tier))
                    .require(Items.BONE_MEAL)
                    .output(enriched(AllNuclearItems.URANIUM.get(), tier))
                    .requiresHeat(HeatCondition.SUPERHEATED));

            // Recovered uranyl nitrate denitrates back to oxide of its tier.
            if (tier.ordinal() <= EnrichmentTier.LOW.ordinal()) {
                create(tier.id() + "_uranium_dioxide_from_uranyl_nitrate", b -> b
                        .require(MinEnrichmentIngredient.tier(AllNuclearItems.URANYL_NITRATE.get(), tier))
                        .output(enriched(AllNuclearItems.URANIUM_DIOXIDE.get(), tier))
                        .output(AllNuclearFluids.NITRIC_ACID.get(), 50)
                        .requiresHeat(HeatCondition.HEATED));
            }

            // Any uranium metal, including ingots from other mods, fluorinates back into the cascade.
            Ingredient metal = tier == EnrichmentTier.NATURAL
                    ? Ingredient.of(AllNuclearTags.Items.URANIUM_INGOTS)
                    : Ingredient.of(AllNuclearItems.URANIUM.get());
            create(tier.id() + "_uranium_hexafluoride_from_metal", b -> b
                    .require(MinEnrichmentIngredient.tier(metal, tier))
                    .require(AllNuclearFluids.HYDROFLUORIC_ACID.get(), 250)
                    .output(tier.hexafluoride(), 250)
                    .requiresHeat(HeatCondition.HEATED));
        }
    }

    public NuclearMixingRecipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, Create_NuclearIndustry.MODID);
    }
}
