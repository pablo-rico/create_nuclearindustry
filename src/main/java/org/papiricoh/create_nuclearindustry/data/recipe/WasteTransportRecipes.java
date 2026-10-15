package org.papiricoh.create_nuclearindustry.data.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.reactor.pwr.PwrRegistry;
import org.papiricoh.create_nuclearindustry.wastetransport.WasteTransportRegistry;

import java.util.concurrent.CompletableFuture;

/**
 * The waste containers: Create's fluid tank and item vault jacketed in containment concrete and
 * iron sheet. Runs under a name of its own, as every recipe provider shares the vanilla name.
 */
public final class WasteTransportRecipes implements DataProvider {
    private final Crafting crafting;

    public WasteTransportRecipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        crafting = new Crafting(output, registries);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return crafting.run(cache);
    }

    @Override
    public String getName() {
        return "Waste transport recipes";
    }

    static class Crafting extends RecipeProvider {
        Crafting(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries);
        }

        @Override
        protected void buildRecipes(RecipeOutput output) {
            jacketed(output, WasteTransportRegistry.RADIOACTIVE_WASTE_CISTERN_ITEM.get(), CreateItems.get("fluid_tank"), "radioactive_waste_cistern");
            jacketed(output, WasteTransportRegistry.SHIELDED_WASTE_CASK_ITEM.get(), CreateItems.get("item_vault"), "shielded_waste_cask");
        }

        /** Concrete on the corners, iron sheet on the sides, the Create container in the middle. */
        private static void jacketed(RecipeOutput output, Item result, Item core, String name) {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, result)
                    .pattern("CPC")
                    .pattern("PTP")
                    .pattern("CPC")
                    .define('C', PwrRegistry.CONTAINMENT_CONCRETE_ITEM.get())
                    .define('P', CreateItems.get("iron_sheet"))
                    .define('T', core)
                    .unlockedBy("has_containment_concrete", has(PwrRegistry.CONTAINMENT_CONCRETE_ITEM.get()))
                    .save(output, ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "crafting/" + name));
        }
    }
}
