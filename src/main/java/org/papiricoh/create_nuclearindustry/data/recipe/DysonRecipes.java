package org.papiricoh.create_nuclearindustry.data.recipe;

import com.simibubi.create.api.data.recipe.SequencedAssemblyRecipeGen;
import com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe;
import com.simibubi.create.content.kinetics.press.PressingRecipe;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.accelerator.AcceleratorRegistry;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ModuleType;
import org.papiricoh.create_nuclearindustry.dyson.DysonRegistry;

import java.util.concurrent.CompletableFuture;

/**
 * The Dyson swarm's hardware. The machines take superconductor and accelerator magnets, so they come after the antimatter; the
 * sails, spent by the thousand, are a cheap sequenced assembly of gold and rose quartz.
 */
public class DysonRecipes extends RecipeProvider {
    public DysonRecipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, DysonRegistry.EM_RAIL_EJECTOR_ITEM.get())
                .pattern("UBU").pattern("SPS").pattern("CAC")
                .define('U', AllNuclearItems.SUPERCONDUCTOR_INGOT.get())
                .define('B', AcceleratorRegistry.BENDING_MAGNET_ITEM.get())
                .define('S', CreateItems.get("sturdy_sheet"))
                .define('P', CreateItems.get("precision_mechanism"))
                .define('C', CreateItems.get("brass_casing"))
                .define('A', CreateItems.get("shaft"))
                .unlockedBy("has_superconductor", has(AllNuclearItems.SUPERCONDUCTOR_INGOT.get()))
                .save(output, id("crafting/em_rail_ejector"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, DysonRegistry.RAY_RECEIVER_ITEM.get())
                .pattern("GGG").pattern("GUG").pattern("CEC")
                .define('G', CreateItems.get("golden_sheet"))
                .define('U', AllNuclearItems.SUPERCONDUCTOR_INGOT.get())
                .define('E', CreateItems.get("electron_tube"))
                .define('C', CreateItems.get("brass_casing"))
                .unlockedBy("has_superconductor", has(AllNuclearItems.SUPERCONDUCTOR_INGOT.get()))
                .save(output, id("crafting/ray_receiver"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, DysonRegistry.SWARM_TERMINAL_ITEM.get())
                .pattern("BDB").pattern("BMB").pattern("BPB")
                .define('B', CreateItems.get("brass_sheet"))
                .define('D', CreateItems.get("display_board"))
                .define('M', AllNuclearItems.CONTROL_MODULES.get(ModuleType.CONTROLLER).get())
                .define('P', CreateItems.get("precision_mechanism"))
                .unlockedBy("has_precision_mechanism", has(CreateItems.get("precision_mechanism")))
                .save(output, id("crafting/swarm_terminal"));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, path);
    }

    public static DataProvider assembly(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        return new SolarSailSequencedRecipes(output, registries);
    }
}

/** Two sails from a golden sheet: a rose quartz film deployed on, wired with copper, pressed thin. */
class SolarSailSequencedRecipes extends SequencedAssemblyRecipeGen {
    {
        create("solar_sail", b -> b.require(CreateItems.get("golden_sheet"))
                .transitionTo(DysonRegistry.INCOMPLETE_SOLAR_SAIL.get()).addOutput(DysonRegistry.SOLAR_SAIL.get(), 2).loops(1)
                .addStep(DeployerApplicationRecipe::new, step -> step.require(CreateItems.get("polished_rose_quartz")))
                .addStep(DeployerApplicationRecipe::new, step -> step.require(CreateItems.get("copper_nugget")))
                .addStep(PressingRecipe::new, step -> step));
    }

    SolarSailSequencedRecipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, Create_NuclearIndustry.MODID);
    }
}
