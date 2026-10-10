package org.papiricoh.create_nuclearindustry.data.recipe;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.api.data.recipe.StandardProcessingRecipeGen;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.waste.WasteTags;

import java.util.concurrent.CompletableFuture;

@SuppressWarnings("unused")
public class NuclearCompactingRecipes extends StandardProcessingRecipeGen<BasinRecipe> {

    // Untreated waste is sealed into a plain drum: intermediate-level, or high-level so that
    // automation built before PUREX gave high-level waste keeps working. Low-level waste has
    // its own, denser drum.
    GeneratedRecipe WASTE_BARREL = create("radioactive_waste_barrel", b -> b
            .require(WasteTags.BARREL_WASTE).require(WasteTags.BARREL_WASTE)
            .require(WasteTags.BARREL_WASTE).require(WasteTags.BARREL_WASTE)
            .require(CreateItems.get("iron_sheet")).require(CreateItems.get("iron_sheet"))
            .require(CreateItems.get("iron_sheet")).require(CreateItems.get("iron_sheet"))
            .output(AllNuclearItems.RADIOACTIVE_WASTE_BARREL.get()));

    // RTG heat sources sealed in an iron capsule.
    GeneratedRecipe PLUTONIUM_RTG_PELLET = create("plutonium_rtg_pellet", b -> b
            .require(AllNuclearItems.PLUTONIUM_DIOXIDE.get())
            .require(CreateItems.get("iron_sheet"))
            .output(AllNuclearItems.PLUTONIUM_RTG_PELLET.get()));

    GeneratedRecipe WASTE_RTG_PELLET = create("waste_rtg_pellet", b -> b
            .require(AllNuclearItems.RADIOACTIVE_WASTE.get()).require(AllNuclearItems.RADIOACTIVE_WASTE.get())
            .require(AllNuclearItems.RADIOACTIVE_WASTE.get()).require(AllNuclearItems.RADIOACTIVE_WASTE.get())
            .require(CreateItems.get("iron_sheet"))
            .output(AllNuclearItems.WASTE_RTG_PELLET.get()));

    // MOX: one part plutonium dioxide to three of depleted uranium dioxide.
    GeneratedRecipe MOX_FUEL_PELLET = create("mox_fuel_pellet", b -> b
            .require(AllNuclearItems.PLUTONIUM_DIOXIDE.get())
            .require(org.papiricoh.create_nuclearindustry.enrichment.recipe.MinEnrichmentIngredient.tier(
                    AllNuclearItems.URANIUM_DIOXIDE.get(), org.papiricoh.create_nuclearindustry.enrichment.EnrichmentTier.DEPLETED))
            .require(org.papiricoh.create_nuclearindustry.enrichment.recipe.MinEnrichmentIngredient.tier(
                    AllNuclearItems.URANIUM_DIOXIDE.get(), org.papiricoh.create_nuclearindustry.enrichment.EnrichmentTier.DEPLETED))
            .require(org.papiricoh.create_nuclearindustry.enrichment.recipe.MinEnrichmentIngredient.tier(
                    AllNuclearItems.URANIUM_DIOXIDE.get(), org.papiricoh.create_nuclearindustry.enrichment.EnrichmentTier.DEPLETED))
            .output(AllNuclearItems.MOX_FUEL_PELLET.get(), 4));

    public NuclearCompactingRecipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, Create_NuclearIndustry.MODID);
    }

    @Override
    protected IRecipeTypeInfo getRecipeType() {
        return AllRecipeTypes.COMPACTING;
    }
}
