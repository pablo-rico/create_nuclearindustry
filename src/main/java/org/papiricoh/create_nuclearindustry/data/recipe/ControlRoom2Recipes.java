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
import net.minecraft.world.level.ItemLike;
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
        ShapelessRecipeBuilder.shapeless(RecipeCategory.REDSTONE, AllNuclearItems.CONTROL_MODULES.get(ModuleType.LED_DISPLAY).get(), 2)
                .requires(CreateItems.get("iron_sheet"))
                .requires(Items.REDSTONE)
                .requires(Items.GLOWSTONE_DUST)
                .unlockedBy("has_control_desk", InventoryChangeTrigger.TriggerInstance.hasItems(AllNuclearItems.REACTOR_CONTROL_PANEL.get()))
                .save(output, id("crafting/" + ModuleType.LED_DISPLAY.itemId()));
        module(output, ModuleType.GUARDED_BUTTON, Items.STONE_BUTTON, Items.IRON_TRAPDOOR, Items.YELLOW_DYE);
        module(output, ModuleType.BAR_GRAPH, Items.REDSTONE, Items.GLOWSTONE_DUST, Items.LIME_DYE);
        module(output, ModuleType.TREND_RECORDER, Items.GLASS_PANE, CreateItems.get("electron_tube"), Items.PAPER);
        module(output, ModuleType.CORE_MAP, Items.GLASS_PANE, CreateItems.get("electron_tube"), Items.MAP);
        module(output, ModuleType.PLASMA_SECTION, Items.GLASS_PANE, CreateItems.get("electron_tube"), Items.AMETHYST_SHARD);
        // Create's link with an electron tube to decode the code.
        ShapelessRecipeBuilder.shapeless(RecipeCategory.REDSTONE, ControlRoom2Registry.ADVANCED_REDSTONE_LINK_ITEM.get())
                .requires(CreateItems.get("redstone_link"))
                .requires(CreateItems.get("electron_tube"))
                .unlockedBy("has_redstone_link", InventoryChangeTrigger.TriggerInstance.hasItems(CreateItems.get("redstone_link")))
                .save(output, id("crafting/advanced_redstone_link"));
        // Create's gearshift in a brass casing, with an electron tube and a comparator to measure the stroke.
        ShapelessRecipeBuilder.shapeless(RecipeCategory.REDSTONE, ControlRoom2Registry.SERVO_GEARBOX_ITEM.get())
                .requires(CreateItems.get("gearshift"))
                .requires(CreateItems.get("brass_casing"))
                .requires(CreateItems.get("electron_tube"))
                .requires(Items.COMPARATOR)
                .unlockedBy("has_gearshift", InventoryChangeTrigger.TriggerInstance.hasItems(CreateItems.get("gearshift")))
                .save(output, id("crafting/servo_gearbox"));
        // A desk's top on a full cabinet, and a desk's top on its own for the wall.
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ControlRoom2Registry.CONTROL_TABLE_ITEM.get(), 2)
                .pattern("SSS")
                .pattern("PEP")
                .pattern("PPP")
                .define('S', Items.SMOOTH_STONE_SLAB)
                .define('P', CreateItems.get("iron_sheet"))
                .define('E', CreateItems.get("electron_tube"))
                .unlockedBy("has_control_desk", InventoryChangeTrigger.TriggerInstance.hasItems(AllNuclearItems.REACTOR_CONTROL_PANEL.get()))
                .save(output, id("crafting/control_table"));
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ControlRoom2Registry.WALL_CONTROL_PANEL_ITEM.get(), 2)
                .pattern("PEP")
                .pattern("SSS")
                .define('S', Items.SMOOTH_STONE_SLAB)
                .define('P', CreateItems.get("iron_sheet"))
                .define('E', CreateItems.get("electron_tube"))
                .unlockedBy("has_control_desk", InventoryChangeTrigger.TriggerInstance.hasItems(AllNuclearItems.REACTOR_CONTROL_PANEL.get()))
                .save(output, id("crafting/wall_control_panel"));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, path);
    }

    /** Like every module: a plate with the control on it, two at a time. */
    private static void module(RecipeOutput output, ModuleType type, ItemLike... parts) {
        ShapelessRecipeBuilder builder = ShapelessRecipeBuilder.shapeless(RecipeCategory.REDSTONE, AllNuclearItems.CONTROL_MODULES.get(type).get(), 2)
                .requires(CreateItems.get("iron_sheet"));
        for (ItemLike part : parts) {
            builder.requires(part);
        }
        builder.unlockedBy("has_control_desk", InventoryChangeTrigger.TriggerInstance.hasItems(AllNuclearItems.REACTOR_CONTROL_PANEL.get()))
                .save(output, id("crafting/" + type.itemId()));
    }
}
