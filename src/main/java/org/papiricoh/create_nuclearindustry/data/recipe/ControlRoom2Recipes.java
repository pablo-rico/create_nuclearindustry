package org.papiricoh.create_nuclearindustry.data.recipe;

import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
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
import org.papiricoh.create_nuclearindustry.controlroom.ControlRoom2Registry;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ModuleType;

import java.util.concurrent.CompletableFuture;

/**
 * Crafting recipes for the chart recorder, the annunciator panel and the rod controller module.
 * Datagen allows one provider per name and every {@link RecipeProvider} is called "Recipes", so the
 * recipes are built by a private one wrapped under a name of its own.
 */
public class ControlRoom2Recipes implements DataProvider {
    private final RecipeProvider recipes;

    public ControlRoom2Recipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        this.recipes = new RecipeProvider(output, registries) {
            @Override
            protected void buildRecipes(RecipeOutput recipeOutput) {
                ControlRoom2Recipes.buildRecipes(recipeOutput);
            }
        };
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        return recipes.run(output);
    }

    @Override
    public String getName() {
        return "Control room instrument recipes";
    }

    private static void buildRecipes(RecipeOutput output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ControlRoom2Registry.CHART_RECORDER_ITEM.get())
                .pattern("IGI")
                .pattern("PEP")
                .pattern("IRI")
                .define('I', CreateItems.get("iron_sheet"))
                .define('G', Items.GLASS_PANE)
                .define('P', Items.PAPER)
                .define('E', CreateItems.get("electron_tube"))
                .define('R', Items.REDSTONE)
                .unlockedBy("has_electron_tube", InventoryChangeTrigger.TriggerInstance.hasItems(CreateItems.get("electron_tube")))
                .save(output, id("crafting/chart_recorder"));
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ControlRoom2Registry.ANNUNCIATOR_PANEL_ITEM.get())
                .pattern("ILI")
                .pattern("LEL")
                .pattern("INI")
                .define('I', CreateItems.get("iron_sheet"))
                .define('L', Items.REDSTONE_LAMP)
                .define('E', CreateItems.get("electron_tube"))
                .define('N', Items.NOTE_BLOCK)
                .unlockedBy("has_electron_tube", InventoryChangeTrigger.TriggerInstance.hasItems(CreateItems.get("electron_tube")))
                .save(output, id("crafting/annunciator_panel"));
        // Like every module: a plate with the control on it, two at a time.
        ShapelessRecipeBuilder.shapeless(RecipeCategory.REDSTONE, AllNuclearItems.CONTROL_MODULES.get(ModuleType.CONTROLLER).get(), 2)
                .requires(CreateItems.get("iron_sheet"))
                .requires(Items.COMPARATOR)
                .requires(CreateItems.get("electron_tube"))
                .unlockedBy("has_control_desk", InventoryChangeTrigger.TriggerInstance.hasItems(AllNuclearItems.REACTOR_CONTROL_PANEL.get()))
                .save(output, id("crafting/" + ModuleType.CONTROLLER.itemId()));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, path);
    }
}
