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
import org.papiricoh.create_nuclearindustry.fusion.heating.PlasmaHeatingRegistry;

import java.util.concurrent.CompletableFuture;

/** Crafting recipes for the tokamak's beam injectors, antennas and viewports: cryostat casing with Create's precision parts. */
public class PlasmaHeatingRecipes extends RecipeProvider {

    public PlasmaHeatingRecipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        // Ion source and accelerator: the beam line comes out of an accelerator segment, focused by electromagnets.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, PlasmaHeatingRegistry.NEUTRAL_BEAM_INJECTOR_ITEM.get())
                .pattern("ETE")
                .pattern("MCM")
                .pattern("PSP")
                .define('E', CreateItems.get("electron_tube"))
                .define('T', AllNuclearItems.FUSION_ACCELERATOR_SEGMENT.get())
                .define('M', AllNuclearItems.FUSION_ELECTROMAGNET.get())
                .define('C', AllNuclearItems.FUSION_CRYOSTAT_CASING.get())
                .define('P', CreateItems.get("precision_mechanism"))
                .define('S', CreateItems.get("shaft"))
                .unlockedBy("has_cryostat_casing", has(AllNuclearItems.FUSION_CRYOSTAT_CASING.get()))
                .save(output, id("crafting/fusion_neutral_beam_injector"));
        // A shaft-driven generator feeding copper straps behind the casing.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, PlasmaHeatingRegistry.RF_ANTENNA_ITEM.get())
                .pattern("KEK")
                .pattern("KCK")
                .pattern("RSR")
                .define('K', CreateItems.get("copper_sheet"))
                .define('E', CreateItems.get("electron_tube"))
                .define('C', AllNuclearItems.FUSION_CRYOSTAT_CASING.get())
                .define('R', Items.REDSTONE)
                .define('S', CreateItems.get("shaft"))
                .unlockedBy("has_cryostat_casing", has(AllNuclearItems.FUSION_CRYOSTAT_CASING.get()))
                .save(output, id("crafting/fusion_rf_antenna"));
        // Fused quartz in a cryostat frame.
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, PlasmaHeatingRegistry.VIEWPORT_ITEM.get(), 4)
                .pattern("CGC")
                .pattern("GQG")
                .pattern("CGC")
                .define('C', AllNuclearItems.FUSION_CRYOSTAT_CASING.get())
                .define('G', AllNuclearItems.REINFORCED_TURBINE_GLASS.get())
                .define('Q', Items.QUARTZ)
                .unlockedBy("has_cryostat_casing", has(AllNuclearItems.FUSION_CRYOSTAT_CASING.get()))
                .save(output, id("crafting/fusion_viewport"));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, path);
    }
}
