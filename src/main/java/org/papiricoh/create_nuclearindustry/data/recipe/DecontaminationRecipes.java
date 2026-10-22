package org.papiricoh.create_nuclearindustry.data.recipe;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.api.data.recipe.StandardProcessingRecipeGen;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.radiation.decontamination.DecontaminationRegistry;
import org.papiricoh.create_nuclearindustry.waste.WasteRegistry;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * The Decontamination Sprinkler, and Irradiated Biomass pressed into low-level waste. Runs its
 * Create generator under a name of its own, as {@link WasteTreatmentRecipes} does.
 */
public final class DecontaminationRecipes implements DataProvider {
    private final List<DataProvider> generators;

    public DecontaminationRecipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        generators = List.of(new Compacting(output, registries), new Crafting(output, registries));
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return CompletableFuture.allOf(generators.stream().map(generator -> generator.run(cache)).toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "Decontamination recipes";
    }

    static class Crafting extends RecipeProvider {
        Crafting(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries);
        }

        @Override
        protected void buildRecipes(RecipeOutput output) {
            // A spray head on a pump housing: pipe in, propeller-driven head, casing and shaft below.
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, DecontaminationRegistry.SPRINKLER_ITEM.get())
                    .pattern(" H ")
                    .pattern("PCP")
                    .pattern("FSF")
                    .define('H', CreateItems.get("propeller"))
                    .define('P', CreateItems.get("iron_sheet"))
                    .define('C', AllNuclearItems.REACTOR_CASING.get())
                    .define('F', CreateItems.get("fluid_pipe"))
                    .define('S', CreateItems.get("shaft"))
                    .unlockedBy("has_reactor_casing", has(AllNuclearItems.REACTOR_CASING.get()))
                    .save(output, ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "crafting/decontamination_sprinkler"));
        }
    }

    /** Four handfuls of wilted sunflowers make one Low-Level Waste. */
    @SuppressWarnings("unused")
    static class Compacting extends StandardProcessingRecipeGen<BasinRecipe> {
        GeneratedRecipe IRRADIATED_BIOMASS = create("low_level_waste_from_irradiated_biomass", b -> b
                .require(DecontaminationRegistry.IRRADIATED_BIOMASS.get()).require(DecontaminationRegistry.IRRADIATED_BIOMASS.get())
                .require(DecontaminationRegistry.IRRADIATED_BIOMASS.get()).require(DecontaminationRegistry.IRRADIATED_BIOMASS.get())
                .output(WasteRegistry.LOW_LEVEL_WASTE.get()));

        Compacting(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries, Create_NuclearIndustry.MODID);
        }

        @Override
        protected IRecipeTypeInfo getRecipeType() {
            return AllRecipeTypes.COMPACTING;
        }
    }
}
