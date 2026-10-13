package org.papiricoh.create_nuclearindustry.data.recipe;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.api.data.recipe.MixingRecipeGen;
import com.simibubi.create.api.data.recipe.StandardProcessingRecipeGen;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import com.simibubi.create.content.processing.recipe.HeatCondition;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.waste.WasteRegistry;
import org.papiricoh.create_nuclearindustry.waste.vitrification.VitrificationRecipe;
import org.papiricoh.create_nuclearindustry.waste.vitrification.WasteRecipeTypes;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Waste treatment: each class of waste has its own route into a package that a repository can
 * hold. Low-level waste is compacted, intermediate-level waste set in cement, high-level waste
 * melted into glass.
 *
 * <p>Create names a recipe generator after its mod and recipe type, so a second compacting or
 * mixing generator would clash with the shared ones; this provider runs the waste generators
 * under a name of its own.
 */
public final class WasteTreatmentRecipes implements DataProvider {
    private final List<DataProvider> generators;

    public WasteTreatmentRecipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        generators = List.of(new Compacting(output, registries), new Mixing(output, registries), new Vitrification(output, registries));
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return CompletableFuture.allOf(generators.stream().map(generator -> generator.run(cache)).toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "Waste treatment recipes";
    }

    /** Low-level waste (rags, filters, scrubber residue) crushed into a drum. */
    @SuppressWarnings("unused")
    static class Compacting extends StandardProcessingRecipeGen<BasinRecipe> {
        GeneratedRecipe COMPACTED_WASTE_DRUM = create("compacted_waste_drum", b -> b
                .require(WasteRegistry.LOW_LEVEL_WASTE.get()).require(WasteRegistry.LOW_LEVEL_WASTE.get())
                .require(WasteRegistry.LOW_LEVEL_WASTE.get()).require(WasteRegistry.LOW_LEVEL_WASTE.get())
                .require(WasteRegistry.LOW_LEVEL_WASTE.get()).require(WasteRegistry.LOW_LEVEL_WASTE.get())
                .require(WasteRegistry.LOW_LEVEL_WASTE.get()).require(WasteRegistry.LOW_LEVEL_WASTE.get())
                .require(CreateItems.get("iron_sheet"))
                .output(WasteRegistry.COMPACTED_WASTE_DRUM_ITEM.get()));

        Compacting(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries, Create_NuclearIndustry.MODID);
        }

        @Override
        protected IRecipeTypeInfo getRecipeType() {
            return AllRecipeTypes.COMPACTING;
        }
    }

    /** Intermediate-level waste grouted with sand, gravel and water in a steel drum; bentonite for backfill. */
    @SuppressWarnings("unused")
    static class Mixing extends MixingRecipeGen {
        GeneratedRecipe CEMENTED_WASTE_DRUM = create("cemented_waste_drum", b -> b
                .require(AllNuclearItems.RADIOACTIVE_WASTE.get()).require(AllNuclearItems.RADIOACTIVE_WASTE.get())
                .require(AllNuclearItems.RADIOACTIVE_WASTE.get()).require(AllNuclearItems.RADIOACTIVE_WASTE.get())
                .require(ItemTags.SAND)
                .require(Items.GRAVEL)
                .require(CreateItems.get("iron_sheet"))
                .require(Fluids.WATER, 500)
                .output(WasteRegistry.CEMENTED_WASTE_DRUM_ITEM.get()));

        // Clay altered with borax swells like sodium bentonite, the clay real repositories pack around canisters.
        GeneratedRecipe BENTONITE = create("bentonite", b -> b
                .require(Items.CLAY)
                .require(AllNuclearItems.BORAX_SALT.get())
                .output(WasteRegistry.BENTONITE_ITEM.get(), 2));

        Mixing(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries, Create_NuclearIndustry.MODID);
        }
    }

    /** High-level waste melted with sand and borax into borosilicate glass, poured into a canister. */
    @SuppressWarnings("unused")
    static class Vitrification extends StandardProcessingRecipeGen<VitrificationRecipe> {
        GeneratedRecipe VITRIFIED_WASTE_CANISTER = create("vitrified_waste_canister", b -> b
                .require(WasteRegistry.HIGH_LEVEL_WASTE.get()).require(WasteRegistry.HIGH_LEVEL_WASTE.get())
                .require(WasteRegistry.HIGH_LEVEL_WASTE.get()).require(WasteRegistry.HIGH_LEVEL_WASTE.get())
                .require(ItemTags.SAND).require(ItemTags.SAND)
                .require(AllNuclearItems.BORAX_SALT.get())
                .output(WasteRegistry.VITRIFIED_WASTE_CANISTER_ITEM.get())
                .requiresHeat(HeatCondition.HEATED)
                .duration(200));

        Vitrification(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries, Create_NuclearIndustry.MODID);
        }

        @Override
        protected IRecipeTypeInfo getRecipeType() {
            return WasteRecipeTypes.VITRIFICATION;
        }
    }
}
