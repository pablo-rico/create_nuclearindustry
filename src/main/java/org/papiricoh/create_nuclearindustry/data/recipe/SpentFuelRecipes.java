package org.papiricoh.create_nuclearindustry.data.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.spentfuel.SpentFuelRegistry;

import java.util.concurrent.CompletableFuture;

/** Crafting recipes for the spent fuel pool and dry storage. Graded reprocessing lives with the crushing recipes. */
public class SpentFuelRecipes extends RecipeProvider {

    public SpentFuelRecipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        // Borated steel cells, as in real pools: the boron keeps packed assemblies far from critical.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, SpentFuelRegistry.SPENT_FUEL_RACK_ITEM.get(), 2)
                .pattern("PBP")
                .pattern("BIB")
                .pattern("PBP")
                .define('P', CreateItems.get("iron_sheet"))
                .define('B', AllNuclearItems.BORON.get())
                .define('I', Items.IRON_BARS)
                .unlockedBy("has_spent_fuel", has(AllNuclearItems.DEPLETED_URANIUM_REACTOR_FUEL.get()))
                .save(output, id("crafting/spent_fuel_rack"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, SpentFuelRegistry.POOL_COOLER_ITEM.get())
                .pattern(" S ")
                .pattern("CMC")
                .pattern("CCC")
                .define('S', CreateItems.get("shaft"))
                .define('M', CreateItems.get("mechanical_pump"))
                .define('C', CreateItems.get("copper_sheet"))
                .unlockedBy("has_spent_fuel", has(AllNuclearItems.DEPLETED_URANIUM_REACTOR_FUEL.get()))
                .save(output, id("crafting/pool_cooler"));
        // A steel canister inside a concrete overpack lined with depleted uranium.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, SpentFuelRegistry.DRY_CASK_ITEM.get())
                .pattern("CPC")
                .pattern("PDP")
                .pattern("CPC")
                .define('C', Items.GRAY_CONCRETE)
                .define('P', CreateItems.get("iron_sheet"))
                .define('D', AllNuclearItems.DEPLETED_URANIUM_BLOCK.get())
                .unlockedBy("has_spent_fuel", has(AllNuclearItems.DEPLETED_URANIUM_REACTOR_FUEL.get()))
                .save(output, id("crafting/dry_cask"));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, path);
    }
}
