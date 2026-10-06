package org.papiricoh.create_nuclearindustry.data.recipe;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.api.data.recipe.StandardProcessingRecipeGen;
import com.simibubi.create.content.kinetics.press.PressingRecipe;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.enrichment.EnrichmentTier;
import org.papiricoh.create_nuclearindustry.enrichment.recipe.MinEnrichmentIngredient;

import java.util.concurrent.CompletableFuture;

import static org.papiricoh.create_nuclearindustry.data.recipe.RecipeHelpers.enriched;

@SuppressWarnings("unused")
public class NuclearPressingRecipes extends StandardProcessingRecipeGen<PressingRecipe> {

    {
        // Only fuel-grade oxide is sintered into pellets.
        for (EnrichmentTier tier : new EnrichmentTier[]{EnrichmentTier.LOW, EnrichmentTier.HIGH}) {
            create(tier.id() + "_uranium_fuel_pellet", b -> b
                    .require(MinEnrichmentIngredient.tier(AllNuclearItems.URANIUM_DIOXIDE.get(), tier))
                    .output(enriched(AllNuclearItems.URANIUM_FUEL_PELLET.get(), tier)));
        }
    }

    public NuclearPressingRecipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, Create_NuclearIndustry.MODID);
    }

    @Override
    protected IRecipeTypeInfo getRecipeType() {
        return AllRecipeTypes.PRESSING;
    }
}
