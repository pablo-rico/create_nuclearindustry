package org.papiricoh.create_nuclearindustry.data.recipe;

import com.simibubi.create.api.data.recipe.StandardProcessingRecipeGen;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.enrichment.EnrichmentTier;
import org.papiricoh.create_nuclearindustry.processing.AllNuclearRecipeTypes;
import org.papiricoh.create_nuclearindustry.processing.recipe.CentrifugingRecipe;
import org.papiricoh.create_nuclearindustry.waste.WasteRegistry;

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

    // PUREX extraction: plutonium leaves in the product, the acid is recovered as tails,
    // fission products settle out as high-level waste.
    GeneratedRecipe PLUTONIUM_NITRATE = create("plutonium_nitrate", b -> b
            .require(AllNuclearFluids.SPENT_FUEL_SOLUTION.get(), 1000)
            .output(AllNuclearFluids.PLUTONIUM_NITRATE.get(), 250)
            .output(AllNuclearFluids.NITRIC_ACID.get(), 250)
            .output(WasteRegistry.HIGH_LEVEL_WASTE.get(), 2)
            .output(0.5f, WasteRegistry.HIGH_LEVEL_WASTE.get())
            .duration(300));

    // Spinning contaminated water throws out the fallout as low-level waste and leaves clean water.
    GeneratedRecipe CONTAMINATED_WATER = create("contaminated_water", b -> b
            .require(AllNuclearFluids.CONTAMINATED_WATER.get(), 1000)
            .output(net.minecraft.world.level.material.Fluids.WATER, 1000)
            .output(0.1f, WasteRegistry.LOW_LEVEL_WASTE.get())
            .duration(100));

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
