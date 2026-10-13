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
import org.papiricoh.create_nuclearindustry.fusion.cryo.CryoRegistry;

import java.util.concurrent.CompletableFuture;

/**
 * Crafting recipes for the helium well, the cryogenic plant and the tokamak's cryogenic ports and vacuum pumps:
 * cryostat casing for the cold parts, Create's pumps, shafts and precision mechanisms for the
 * moving ones.
 */
public class CryoCraftingRecipes extends RecipeProvider {

    public CryoCraftingRecipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        well(output);
        plant(output);
        tokamakParts(output);
    }

    /** The helium well comes before any cryostat casing: Create andesite parts, a shaft and copper tubing. */
    private void well(RecipeOutput output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CryoRegistry.HELIUM_WELL_ITEM.get())
                .pattern(" S ")
                .pattern("CAC")
                .pattern(" P ")
                .define('S', CreateItems.get("shaft"))
                .define('C', CreateItems.get("copper_sheet"))
                .define('A', CreateItems.get("andesite_casing"))
                .define('P', CreateItems.get("fluid_pipe"))
                .unlockedBy("has_andesite_casing", has(CreateItems.get("andesite_casing")))
                .save(output, id("crafting/helium_well"));
    }

    /** Insulated casing around copper heat exchangers; the compressor is a Create pump on a shaft. */
    private void plant(RecipeOutput output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, CryoRegistry.CRYOPLANT_CASING_ITEM.get(), 4)
                .pattern("ICI")
                .pattern("PXP")
                .pattern("ICI")
                .define('I', CreateItems.get("iron_sheet"))
                .define('C', CreateItems.get("copper_sheet"))
                .define('P', CreateItems.get("fluid_pipe"))
                .define('X', AllNuclearItems.FUSION_CRYOSTAT_CASING.get())
                .unlockedBy("has_cryostat_casing", has(AllNuclearItems.FUSION_CRYOSTAT_CASING.get()))
                .save(output, id("crafting/cryoplant_casing"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CryoRegistry.CRYOPLANT_CONTROLLER_ITEM.get())
                .pattern(" E ")
                .pattern("MCM")
                .pattern(" R ")
                .define('E', CreateItems.get("electron_tube"))
                .define('M', CreateItems.get("precision_mechanism"))
                .define('C', CryoRegistry.CRYOPLANT_CASING_ITEM.get())
                .define('R', Items.REDSTONE)
                .unlockedBy("has_cryoplant_casing", has(CryoRegistry.CRYOPLANT_CASING_ITEM.get()))
                .save(output, id("crafting/cryoplant_controller"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CryoRegistry.CRYOPLANT_PORT_ITEM.get(), 2)
                .pattern(" P ")
                .pattern("PCP")
                .pattern(" P ")
                .define('P', CreateItems.get("fluid_pipe"))
                .define('C', CryoRegistry.CRYOPLANT_CASING_ITEM.get())
                .unlockedBy("has_cryoplant_casing", has(CryoRegistry.CRYOPLANT_CASING_ITEM.get()))
                .save(output, id("crafting/cryoplant_port"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CryoRegistry.CRYOPLANT_COMPRESSOR_ITEM.get())
                .pattern(" S ")
                .pattern("PCP")
                .pattern(" M ")
                .define('S', CreateItems.get("shaft"))
                .define('P', CreateItems.get("mechanical_pump"))
                .define('C', CryoRegistry.CRYOPLANT_CASING_ITEM.get())
                .define('M', CreateItems.get("precision_mechanism"))
                .unlockedBy("has_cryoplant_casing", has(CryoRegistry.CRYOPLANT_CASING_ITEM.get()))
                .save(output, id("crafting/cryoplant_compressor"));
    }

    /** Shell parts start from cryostat casing, like the other tokamak fittings. */
    private void tokamakParts(RecipeOutput output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CryoRegistry.FUSION_CRYO_PORT_ITEM.get(), 2)
                .pattern(" P ")
                .pattern("SCS")
                .pattern(" T ")
                .define('P', CreateItems.get("fluid_pipe"))
                .define('S', AllNuclearItems.SUPERCONDUCTOR_INGOT.get())
                .define('C', AllNuclearItems.FUSION_CRYOSTAT_CASING.get())
                .define('T', CreateItems.get("fluid_tank"))
                .unlockedBy("has_cryostat_casing", has(AllNuclearItems.FUSION_CRYOSTAT_CASING.get()))
                .save(output, id("crafting/fusion_cryo_port"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CryoRegistry.FUSION_VACUUM_PUMP_ITEM.get())
                .pattern(" S ")
                .pattern("PCP")
                .pattern(" M ")
                .define('S', CreateItems.get("shaft"))
                .define('P', CreateItems.get("propeller"))
                .define('C', AllNuclearItems.FUSION_CRYOSTAT_CASING.get())
                .define('M', CreateItems.get("precision_mechanism"))
                .unlockedBy("has_cryostat_casing", has(AllNuclearItems.FUSION_CRYOSTAT_CASING.get()))
                .save(output, id("crafting/fusion_vacuum_pump"));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, path);
    }
}
