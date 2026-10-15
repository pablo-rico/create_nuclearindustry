package org.papiricoh.create_nuclearindustry.data.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.maintenance.MaintenanceRegistry;

import java.util.concurrent.CompletableFuture;

/** The Maintenance Kit: spare parts in a box, a wrench, a precision mechanism and iron. */
public class MaintenanceRecipes extends RecipeProvider {

    public MaintenanceRecipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, MaintenanceRegistry.MAINTENANCE_KIT.get())
                .pattern("IWI")
                .pattern("SPS")
                .define('I', Items.IRON_INGOT)
                .define('W', CreateItems.get("wrench"))
                .define('S', CreateItems.get("iron_sheet"))
                .define('P', CreateItems.get("precision_mechanism"))
                .unlockedBy("has_wrench", has(CreateItems.get("wrench")))
                .save(output, ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "crafting/maintenance_kit"));
    }
}
