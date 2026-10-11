package org.papiricoh.create_nuclearindustry.data.recipe;

import com.simibubi.create.api.data.recipe.StandardProcessingRecipeGen;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.accelerator.AcceleratorRegistry;
import org.papiricoh.create_nuclearindustry.processing.AllNuclearRecipeTypes;
import org.papiricoh.create_nuclearindustry.processing.recipe.ElectrolysisRecipe;

import java.util.concurrent.CompletableFuture;

/**
 * Crafting recipes for the particle accelerator: beam pipes of iron and glass, copper cavities, iron
 * and copper magnets, and machines built round Create's electron tubes and precision mechanisms.
 */
public class AcceleratorRecipes extends RecipeProvider {

    public AcceleratorRecipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        // The controller: a brass-cased console of valves and a precision timing mechanism.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, AcceleratorRegistry.CONTROLLER_ITEM.get())
                .pattern("EPE")
                .pattern("BRB")
                .pattern("SCS")
                .define('E', CreateItems.get("electron_tube"))
                .define('P', CreateItems.get("precision_mechanism"))
                .define('B', CreateItems.get("brass_sheet"))
                .define('R', Items.COMPARATOR)
                .define('S', CreateItems.get("iron_sheet"))
                .define('C', CreateItems.get("brass_casing"))
                .unlockedBy("has_precision_mechanism", has(CreateItems.get("precision_mechanism")))
                .save(output, id("crafting/particle_accelerator_controller"));
        // The ion source: a hydrogen discharge in a copper chamber, fed by a pipe and driven by a shaft.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, AcceleratorRegistry.ION_SOURCE_ITEM.get())
                .pattern("TET")
                .pattern("FCF")
                .pattern("TST")
                .define('T', CreateItems.get("copper_sheet"))
                .define('E', CreateItems.get("electron_tube"))
                .define('F', CreateItems.get("fluid_pipe"))
                .define('C', CreateItems.get("copper_casing"))
                .define('S', CreateItems.get("shaft"))
                .unlockedBy("has_copper_casing", has(CreateItems.get("copper_casing")))
                .save(output, id("crafting/particle_accelerator_ion_source"));
        // Beam pipe: iron sheet with a glass window to watch the beam.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, AcceleratorRegistry.BEAM_PIPE_ITEM.get(), 3)
                .pattern("SGS")
                .define('S', CreateItems.get("iron_sheet"))
                .define('G', Items.GLASS)
                .unlockedBy("has_iron_sheet", has(CreateItems.get("iron_sheet")))
                .save(output, id("crafting/beam_pipe"));
        // RF cavity: a copper resonator round the pipe, coupled through electron tubes.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, AcceleratorRegistry.RF_CAVITY_ITEM.get())
                .pattern("CEC")
                .pattern("CPC")
                .pattern("CEC")
                .define('C', CreateItems.get("copper_sheet"))
                .define('E', CreateItems.get("electron_tube"))
                .define('P', AcceleratorRegistry.BEAM_PIPE_ITEM.get())
                .unlockedBy("has_beam_pipe", has(AcceleratorRegistry.BEAM_PIPE_ITEM.get()))
                .save(output, id("crafting/beam_rf_cavity"));
        // Quadrupole: four copper coils on an iron yoke round the pipe.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, AcceleratorRegistry.QUADRUPOLE_ITEM.get())
                .pattern("ICI")
                .pattern("CPC")
                .pattern("ICI")
                .define('I', Items.IRON_INGOT)
                .define('C', Items.COPPER_INGOT)
                .define('P', AcceleratorRegistry.BEAM_PIPE_ITEM.get())
                .unlockedBy("has_beam_pipe", has(AcceleratorRegistry.BEAM_PIPE_ITEM.get()))
                .save(output, id("crafting/beam_quadrupole"));
        // Bending magnet: a heavier yoke, two coils.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, AcceleratorRegistry.BENDING_MAGNET_ITEM.get())
                .pattern("III")
                .pattern("CPC")
                .pattern("III")
                .define('I', Items.IRON_INGOT)
                .define('C', Items.COPPER_INGOT)
                .define('P', AcceleratorRegistry.BEAM_PIPE_ITEM.get())
                .unlockedBy("has_beam_pipe", has(AcceleratorRegistry.BEAM_PIPE_ITEM.get()))
                .save(output, id("crafting/beam_bending_magnet"));
        // Beam position monitor: pickup electrodes in the pipe read out like a comparator.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, AcceleratorRegistry.POSITION_MONITOR_ITEM.get())
                .pattern("E")
                .pattern("P")
                .pattern("R")
                .define('E', CreateItems.get("electron_tube"))
                .define('P', AcceleratorRegistry.BEAM_PIPE_ITEM.get())
                .define('R', Items.COMPARATOR)
                .unlockedBy("has_beam_pipe", has(AcceleratorRegistry.BEAM_PIPE_ITEM.get()))
                .save(output, id("crafting/beam_position_monitor"));
        // Klystron: a shaft-driven generator feeding a copper tube amplifier.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, AcceleratorRegistry.KLYSTRON_ITEM.get())
                .pattern("TET")
                .pattern("CMC")
                .pattern(" S ")
                .define('T', CreateItems.get("copper_sheet"))
                .define('E', CreateItems.get("electron_tube"))
                .define('C', CreateItems.get("copper_casing"))
                .define('M', CreateItems.get("precision_mechanism"))
                .define('S', CreateItems.get("shaft"))
                .unlockedBy("has_copper_casing", has(CreateItems.get("copper_casing")))
                .save(output, id("crafting/particle_accelerator_klystron"));
        // Magnet power supply: a shaft-driven dynamo and its regulator.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, AcceleratorRegistry.MAGNET_POWER_SUPPLY_ITEM.get())
                .pattern("BRB")
                .pattern("CMC")
                .pattern(" S ")
                .define('B', CreateItems.get("brass_sheet"))
                .define('R', Items.REDSTONE)
                .define('C', Items.COPPER_INGOT)
                .define('M', CreateItems.get("brass_casing"))
                .define('S', CreateItems.get("shaft"))
                .unlockedBy("has_brass_casing", has(CreateItems.get("brass_casing")))
                .save(output, id("crafting/particle_accelerator_magnet_power_supply"));
        // Beamline vacuum pump: Create's pump behind a copper casing.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, AcceleratorRegistry.VACUUM_PUMP_ITEM.get())
                .pattern("IFI")
                .pattern("CPC")
                .pattern(" S ")
                .define('I', CreateItems.get("iron_sheet"))
                .define('F', CreateItems.get("fluid_pipe"))
                .define('C', CreateItems.get("copper_casing"))
                .define('P', CreateItems.get("mechanical_pump"))
                .define('S', CreateItems.get("shaft"))
                .unlockedBy("has_mechanical_pump", has(CreateItems.get("mechanical_pump")))
                .save(output, id("crafting/particle_accelerator_vacuum_pump"));
        // Beam dump: graphite to stop the beam, iron round it.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, AcceleratorRegistry.BEAM_DUMP_ITEM.get())
                .pattern("IGI")
                .pattern("GBG")
                .pattern("IGI")
                .define('I', Items.IRON_INGOT)
                .define('G', AllNuclearItems.GRAPHITE.get())
                .define('B', Items.COPPER_BLOCK)
                .unlockedBy("has_graphite", has(AllNuclearItems.GRAPHITE.get()))
                .save(output, id("crafting/beam_dump"));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, path);
    }

    /**
     * Hydrogen for the ion source: electrolysing brine (the chlor-alkali process) gives hydrogen at
     * the cathode. Plain water already goes to heavy water, so brine keeps the two apart.
     */
    @SuppressWarnings("unused")
    public static class Electrolysis extends StandardProcessingRecipeGen<ElectrolysisRecipe> {
        GeneratedRecipe HYDROGEN = create("hydrogen", b -> b
                .require(AllNuclearFluids.BRINE.get(), 250)
                .output(AllNuclearFluids.HYDROGEN.get(), 250)
                .duration(60));

        public Electrolysis(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries, Create_NuclearIndustry.MODID);
        }

        @Override
        protected IRecipeTypeInfo getRecipeType() {
            return AllNuclearRecipeTypes.ELECTROLYSIS;
        }
    }
}
