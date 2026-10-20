package org.papiricoh.create_nuclearindustry.data.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.missile.silo.SiloRegistry;

import java.util.concurrent.CompletableFuture;

/** Crafting recipe for the silo's inert kill vehicle payload. */
public final class KillVehicleRecipes extends RecipeProvider {
    public KillVehicleRecipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) { super(output, registries); }

    @Override protected void buildRecipes(RecipeOutput output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.TRANSPORTATION, SiloRegistry.KILL_VEHICLE.get())
                .pattern("SES").pattern("PTP").pattern("SES")
                .define('S', CreateItems.get("sturdy_sheet"))
                .define('E', CreateItems.get("electron_tube"))
                .define('P', CreateItems.get("precision_mechanism"))
                .define('T', SiloRegistry.STAR_TRACKER.get())
                .unlockedBy("has_star_tracker", has(SiloRegistry.STAR_TRACKER.get()))
                .save(output, ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "crafting/rocket_kill_vehicle"));
    }
}
