package org.papiricoh.create_nuclearindustry.data.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.conditions.ModLoadedCondition;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.antimatter.AntimatterRegistry;
import org.papiricoh.create_nuclearindustry.propulsion.PropulsionRegistry;

import java.util.concurrent.CompletableFuture;

/**
 * Crafting recipes of the nuclear engines, loaded only with Sable installed: without its ships there
 * is nothing for them to push. The thermal engine is built round a reactor heat exchanger, the
 * antimatter engine round a Penning trap and superconducting magnets.
 */
public class PropulsionRecipes extends RecipeProvider {

    public PropulsionRecipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput unconditioned) {
        RecipeOutput output = unconditioned.withConditions(new ModLoadedCondition("sable"));
        ShapedRecipeBuilder.shaped(RecipeCategory.TRANSPORTATION, PropulsionRegistry.THERMAL_ENGINE_ITEM.get())
                .pattern("SPS")
                .pattern("TET")
                .pattern("SCS")
                .define('S', CreateItems.get("sturdy_sheet"))
                .define('P', CreateItems.get("precision_mechanism"))
                .define('T', CreateItems.get("fluid_tank"))
                .define('E', CreateItems.get("electron_tube"))
                .define('C', AllNuclearItems.REACTOR_CASING.get())
                .unlockedBy("has_reactor_casing", has(AllNuclearItems.REACTOR_CASING.get()))
                .save(output, id("crafting/nuclear_thermal_engine"));
        ShapedRecipeBuilder.shaped(RecipeCategory.TRANSPORTATION, PropulsionRegistry.THERMAL_EXCHANGER_ITEM.get(), 2)
                .pattern("CSC")
                .pattern("SHS")
                .pattern("CSC")
                .define('C', CreateItems.get("copper_sheet"))
                .define('S', CreateItems.get("iron_sheet"))
                .define('H', AllNuclearItems.HEAT_EXCHANGER.get())
                .unlockedBy("has_heat_exchanger", has(AllNuclearItems.HEAT_EXCHANGER.get()))
                .save(output, id("crafting/thermal_engine_exchanger"));
        ShapedRecipeBuilder.shaped(RecipeCategory.TRANSPORTATION, PropulsionRegistry.THERMAL_NOZZLE_ITEM.get())
                .pattern("SOS")
                .pattern("S S")
                .pattern("S S")
                .define('S', CreateItems.get("sturdy_sheet"))
                .define('O', Items.OBSIDIAN)
                .unlockedBy("has_nuclear_thermal_engine", has(PropulsionRegistry.THERMAL_ENGINE_ITEM.get()))
                .save(output, id("crafting/thermal_rocket_nozzle"));
        ShapedRecipeBuilder.shaped(RecipeCategory.TRANSPORTATION, PropulsionRegistry.ANTIMATTER_ENGINE_ITEM.get())
                .pattern("UTU")
                .pattern("PAP")
                .pattern("UCU")
                .define('U', AllNuclearItems.SUPERCONDUCTOR_INGOT.get())
                .define('T', AntimatterRegistry.SMALL_PENNING_TRAP_ITEM.get())
                .define('P', CreateItems.get("precision_mechanism"))
                .define('A', AntimatterRegistry.ANTIMATTER_CONDUIT_ITEM.get())
                .define('C', CreateItems.get("brass_casing"))
                .unlockedBy("has_small_penning_trap", has(AntimatterRegistry.SMALL_PENNING_TRAP_ITEM.get()))
                .save(output, id("crafting/antimatter_engine"));
        ShapedRecipeBuilder.shaped(RecipeCategory.TRANSPORTATION, PropulsionRegistry.MAGNETIC_COIL_ITEM.get(), 2)
                .pattern(" U ")
                .pattern("CEC")
                .pattern(" U ")
                .define('U', AllNuclearItems.SUPERCONDUCTOR_INGOT.get())
                .define('C', CreateItems.get("copper_sheet"))
                .define('E', AllNuclearItems.FUSION_ELECTROMAGNET.get())
                .unlockedBy("has_superconductor", has(AllNuclearItems.SUPERCONDUCTOR_INGOT.get()))
                .save(output, id("crafting/magnetic_coil"));
        ShapedRecipeBuilder.shaped(RecipeCategory.TRANSPORTATION, PropulsionRegistry.MAGNETIC_NOZZLE_ITEM.get())
                .pattern("USU")
                .pattern("S S")
                .pattern("S S")
                .define('U', AllNuclearItems.SUPERCONDUCTOR_INGOT.get())
                .define('S', CreateItems.get("sturdy_sheet"))
                .unlockedBy("has_antimatter_engine", has(PropulsionRegistry.ANTIMATTER_ENGINE_ITEM.get()))
                .save(output, id("crafting/magnetic_nozzle"));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, path);
    }
}
