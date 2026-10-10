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
import org.papiricoh.create_nuclearindustry.reactor.pwr.PwrRegistry;

import java.util.concurrent.CompletableFuture;

/** Crafting recipes for the pressurizer and the steam generator: Create's tanks, valves, pipes and sheets. */
public class PwrCraftingRecipes extends RecipeProvider {

    public PwrCraftingRecipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        pressurizer(output);
        steamGenerator(output);
    }

    /** A fluid tank wrapped in plate on reactor casing; heaters and the relief valve are upgrades of it. */
    private void pressurizer(RecipeOutput output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, PwrRegistry.PRESSURIZER_ITEM.get())
                .pattern("PPP")
                .pattern("PTP")
                .pattern(" C ")
                .define('P', CreateItems.get("iron_sheet"))
                .define('T', CreateItems.get("fluid_tank"))
                .define('C', AllNuclearItems.REACTOR_CASING.get())
                .unlockedBy("has_reactor_casing", has(AllNuclearItems.REACTOR_CASING.get()))
                .save(output, id("crafting/pressurizer"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, PwrRegistry.PRESSURIZER_HEATER_PORT_ITEM.get())
                .requires(PwrRegistry.PRESSURIZER_ITEM.get())
                .requires(CreateItems.get("copper_sheet"))
                .requires(Items.BLAZE_POWDER)
                .requires(Items.REDSTONE)
                .unlockedBy("has_pressurizer", has(PwrRegistry.PRESSURIZER_ITEM.get()))
                .save(output, id("crafting/pressurizer_heater_port"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, PwrRegistry.RELIEF_VALVE_ITEM.get())
                .requires(CreateItems.get("fluid_valve"))
                .requires(CreateItems.get("iron_sheet"))
                .requires(CreateItems.get("iron_sheet"))
                .requires(Items.REDSTONE)
                .unlockedBy("has_pressurizer", has(PwrRegistry.PRESSURIZER_ITEM.get()))
                .save(output, id("crafting/relief_valve"));
    }

    /** Copper tubes in an iron shell; the controller and ports follow the cooling tower's. */
    private void steamGenerator(RecipeOutput output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, PwrRegistry.STEAM_GENERATOR_CASING_ITEM.get(), 4)
                .pattern("IPI")
                .pattern("P P")
                .pattern("IPI")
                .define('I', CreateItems.get("iron_sheet"))
                .define('P', CreateItems.get("copper_sheet"))
                .unlockedBy("has_copper_sheet", has(CreateItems.get("copper_sheet")))
                .save(output, id("crafting/steam_generator_casing"));
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, PwrRegistry.STEAM_GENERATOR_TUBE_BUNDLE_ITEM.get(), 2)
                .pattern("PFP")
                .pattern("FPF")
                .pattern("PFP")
                .define('P', CreateItems.get("copper_sheet"))
                .define('F', CreateItems.get("fluid_pipe"))
                .unlockedBy("has_fluid_pipe", has(CreateItems.get("fluid_pipe")))
                .save(output, id("crafting/steam_generator_tube_bundle"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, PwrRegistry.STEAM_GENERATOR_CONTROLLER_ITEM.get())
                .pattern(" E ")
                .pattern("PCP")
                .pattern(" R ")
                .define('C', PwrRegistry.STEAM_GENERATOR_CASING_ITEM.get())
                .define('E', CreateItems.get("electron_tube"))
                .define('P', CreateItems.get("copper_sheet"))
                .define('R', Items.REDSTONE)
                .unlockedBy("has_steam_generator_casing", has(PwrRegistry.STEAM_GENERATOR_CASING_ITEM.get()))
                .save(output, id("crafting/steam_generator_controller"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, PwrRegistry.STEAM_GENERATOR_PORT_ITEM.get(), 2)
                .pattern(" P ")
                .pattern("PCP")
                .pattern(" P ")
                .define('C', PwrRegistry.STEAM_GENERATOR_CASING_ITEM.get())
                .define('P', CreateItems.get("fluid_pipe"))
                .unlockedBy("has_steam_generator_casing", has(PwrRegistry.STEAM_GENERATOR_CASING_ITEM.get()))
                .save(output, id("crafting/steam_generator_port"));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, path);
    }
}
