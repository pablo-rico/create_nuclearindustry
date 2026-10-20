package org.papiricoh.create_nuclearindustry.data.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.recipes.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import com.simibubi.create.api.data.recipe.SequencedAssemblyRecipeGen;
import com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe;
import com.simibubi.create.content.kinetics.press.PressingRecipe;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.missile.silo.SiloRegistry;
import org.papiricoh.create_nuclearindustry.missile.orbit.OrbitRegistry;
import java.util.concurrent.CompletableFuture;

/** The orbital payloads and the hardware that launches and controls them. */
public class OrbitRecipes extends RecipeProvider {
    public OrbitRecipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) { super(output, registries); }
    @Override protected void buildRecipes(RecipeOutput output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.TRANSPORTATION, SiloRegistry.SATELLITE_FAIRING.get())
                .pattern(" S ").pattern("S S").pattern(" B ")
                .define('S', CreateItems.get("sturdy_sheet")).define('B', CreateItems.get("brass_sheet"))
                .unlockedBy("has_sturdy_sheet", has(CreateItems.get("sturdy_sheet")))
                .save(output, id("crafting/rocket_satellite_fairing"));
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, OrbitRegistry.GROUND_STATION_ITEM.get())
                .pattern(" B ").pattern("BEB").pattern("PSP")
                .define('B', CreateItems.get("brass_sheet"))
                .define('E', org.papiricoh.create_nuclearindustry.AllNuclearItems.CONTROL_MODULES.get(
                        org.papiricoh.create_nuclearindustry.controlroom.desk.ModuleType.CONTROLLER).get())
                .define('P', CreateItems.get("precision_mechanism")).define('S', CreateItems.get("shaft"))
                .unlockedBy("has_precision_mechanism", has(CreateItems.get("precision_mechanism")))
                .save(output, id("crafting/ground_station"));
    }
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, path); }
    public static DataProvider assembly(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        return new SatelliteSequencedRecipes(output, registries);
    }
}

class SatelliteSequencedRecipes extends SequencedAssemblyRecipeGen {
    {
        create("early_warning_satellite", b -> b.require(CreateItems.get("precision_mechanism"))
                .transitionTo(OrbitRegistry.INCOMPLETE_SATELLITE.get()).addOutput(OrbitRegistry.EARLY_WARNING_SATELLITE.get(), 1).loops(1)
                .addStep(DeployerApplicationRecipe::new, step -> step.require(CreateItems.get("golden_sheet")))
                .addStep(DeployerApplicationRecipe::new, step -> step.require(CreateItems.get("electron_tube")))
                .addStep(DeployerApplicationRecipe::new, step -> step.require(SiloRegistry.STAR_TRACKER.get()))
                .addStep(PressingRecipe::new, step -> step));
        create("comms_satellite", b -> b.require(CreateItems.get("precision_mechanism"))
                .transitionTo(OrbitRegistry.INCOMPLETE_SATELLITE.get()).addOutput(OrbitRegistry.COMMS_SATELLITE.get(), 1).loops(1)
                .addStep(DeployerApplicationRecipe::new, step -> step.require(CreateItems.get("brass_sheet")))
                .addStep(DeployerApplicationRecipe::new, step -> step.require(CreateItems.get("electron_tube")))
                .addStep(DeployerApplicationRecipe::new, step -> step.require(Items.LIGHTNING_ROD))
                .addStep(PressingRecipe::new, step -> step));
    }
    SatelliteSequencedRecipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) { super(output, registries, Create_NuclearIndustry.MODID); }
}
