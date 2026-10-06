package org.papiricoh.create_nuclearindustry.data.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.enrichment.EnrichmentTier;

import java.util.concurrent.CompletableFuture;

import static org.papiricoh.create_nuclearindustry.data.recipe.RecipeHelpers.enriched;

/** Crafting-table recipes for blocks and items added or changed by the rewrite. */
public class NuclearCraftingRecipes extends RecipeProvider {

    public NuclearCraftingRecipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        // The fuel channel no longer embeds fuel: assemblies are loaded through the fuel port.
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, AllNuclearItems.URANIUM_ROD.get(), 2)
                .pattern(" B ")
                .pattern("BCB")
                .pattern(" B ")
                .define('B', Items.IRON_BARS)
                .define('C', AllNuclearItems.REACTOR_CASING.get())
                .unlockedBy("has_reactor_casing", has(AllNuclearItems.REACTOR_CASING.get()))
                .save(output, id("crafting/uranium_rod"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, AllNuclearItems.ELECTROLYZER.get())
                .pattern(" P ")
                .pattern("ASA")
                .pattern(" C ")
                .define('P', CreateItems.get("fluid_pipe"))
                .define('A', CreateItems.get("copper_sheet"))
                .define('S', CreateItems.get("shaft"))
                .define('C', AllNuclearItems.REACTOR_CASING.get())
                .unlockedBy("has_reactor_casing", has(AllNuclearItems.REACTOR_CASING.get()))
                .save(output, id("crafting/electrolyzer"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, AllNuclearItems.EMPTY_GAS_CELL.get(), 2)
                .pattern(" C ")
                .pattern("CGC")
                .pattern(" C ")
                .define('C', CreateItems.get("copper_sheet"))
                .define('G', Items.GLASS)
                .unlockedBy("has_copper_sheet", has(CreateItems.get("copper_sheet")))
                .save(output, id("crafting/empty_gas_cell"));

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, AllNuclearItems.FUSION_CENTRAL_SOLENOID.get())
                .pattern(" S ")
                .pattern("ECE")
                .pattern(" S ")
                .define('S', AllNuclearItems.SUPERCONDUCTOR_INGOT.get())
                .define('E', AllNuclearItems.FUSION_ELECTROMAGNET.get())
                .define('C', AllNuclearItems.FUSION_CRYOSTAT_CASING.get())
                .unlockedBy("has_superconductor", has(AllNuclearItems.SUPERCONDUCTOR_INGOT.get()))
                .save(output, id("crafting/fusion_central_solenoid"));

        ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, AllNuclearItems.FUSION_BREEDING_BLANKET.get())
                .requires(AllNuclearItems.LITHIUM_6_BREEDER_ASSEMBLY.get())
                .requires(AllNuclearItems.FUSION_CRYOSTAT_CASING.get())
                .requires(AllNuclearItems.BERYLLIUM.get())
                .unlockedBy("has_breeder_assembly", has(AllNuclearItems.LITHIUM_6_BREEDER_ASSEMBLY.get()))
                .save(output, id("crafting/fusion_breeding_blanket"));

        // Old flat-ring corners become plain vessel segments.
        ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, AllNuclearItems.FUSION_ACCELERATOR_SEGMENT.get())
                .requires(AllNuclearItems.FUSION_ACCELERATOR_CORNER.get())
                .unlockedBy("has_corner", has(AllNuclearItems.FUSION_ACCELERATOR_CORNER.get()))
                .save(output, id("crafting/fusion_accelerator_segment_from_corner"));

        // Legacy item from an unfinished recipe chain: turn any leftovers into usable oxide.
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, enriched(AllNuclearItems.URANIUM_DIOXIDE.get(), EnrichmentTier.LOW))
                .requires(AllNuclearItems.ENRICHED_URANIUM_BLEND.get())
                .unlockedBy("has_blend", has(AllNuclearItems.ENRICHED_URANIUM_BLEND.get()))
                .save(output, id("crafting/uranium_dioxide_from_blend"));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, path);
    }
}
