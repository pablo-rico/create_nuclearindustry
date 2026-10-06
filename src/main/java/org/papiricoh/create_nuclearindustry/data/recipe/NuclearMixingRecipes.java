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
