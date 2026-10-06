package org.papiricoh.create_nuclearindustry.data.recipe;

import com.simibubi.create.api.data.recipe.StandardProcessingRecipeGen;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.enrichment.EnrichmentTier;
import org.papiricoh.create_nuclearindustry.processing.AllNuclearRecipeTypes;
import org.papiricoh.create_nuclearindustry.processing.recipe.CentrifugingRecipe;

import java.util.concurrent.CompletableFuture;

/**
 * The enrichment cascade. Each stage takes 1000 mB of feed and splits it into an enriched
 * product and tails one tier lower; piping the tails back into the previous stage
 * (reflux) recovers most of the feed.
 */
@SuppressWarnings("unused")
public class NuclearCentrifugingRecipes extends StandardProcessingRecipeGen<CentrifugingRecipe> {

    GeneratedRecipe NATURAL_TO_LOW = stage(EnrichmentTier.NATURAL, 250, 160);
    GeneratedRecipe LOW_TO_HIGH = stage(EnrichmentTier.LOW, 400, 240);
    GeneratedRecipe HIGH_TO_WEAPONS = stage(EnrichmentTier.HIGH, 400, 320);

    // Lithium-6 is 7.5% of natural lithium; it is separated the same way in the gas phase.
    GeneratedRecipe LITHIUM_6 = create("lithium_6", b -> b
            .require(AllNuclearItems.LITHIUM.get()).require(AllNuclearItems.LITHIUM.get())
            .require(AllNuclearItems.LITHIUM.get()).require(AllNuclearItems.LITHIUM.get())
            .require(AllNuclearItems.LITHIUM.get()).require(AllNuclearItems.LITHIUM.get())
            .require(AllNuclearItems.LITHIUM.get()).require(AllNuclearItems.LITHIUM.get())
            .output(AllNuclearItems.LITHIUM_6.get())
            .output(AllNuclearItems.LITHIUM.get(), 6)
            .duration(200));

    public NuclearCentrifugingRecipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, Create_NuclearIndustry.MODID);
    }

    private GeneratedRecipe stage(EnrichmentTier feed, int product, int duration) {
        EnrichmentTier enriched = feed.next();
        EnrichmentTier tails = feed.previous();
        return create(enriched.id() + "_uranium_hexafluoride", b -> b
                .require(feed.hexafluoride(), 1000)
                .output(enriched.hexafluoride(), product)
                .output(tails.hexafluoride(), 1000 - product)
                .duration(duration));
    }

    @Override
    protected IRecipeTypeInfo getRecipeType() {
        return AllNuclearRecipeTypes.CENTRIFUGING;
    }
}
