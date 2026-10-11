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
import org.papiricoh.create_nuclearindustry.reactor.pwr.PwrRegistry;

import java.util.concurrent.CompletableFuture;

/** Crafting recipes for the containment building and the core catcher: reinforced concrete, steel plate and Create's fluid parts. */
public class ContainmentCraftingRecipes extends RecipeProvider {

    public ContainmentCraftingRecipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        // Smooth stone poured around iron bar rebar.
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, PwrRegistry.CONTAINMENT_CONCRETE_ITEM.get(), 8)
                .pattern("SRS")
                .pattern("RSR")
                .pattern("SRS")
                .define('S', Items.SMOOTH_STONE)
                .define('R', Items.IRON_BARS)
                .unlockedBy("has_reactor_casing", has(AllNuclearItems.REACTOR_CASING.get()))
                .save(output, id("crafting/containment_concrete"));
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, PwrRegistry.STEEL_LINER_ITEM.get(), 2)
                .pattern("PP")
                .pattern("PP")
                .define('P', CreateItems.get("iron_sheet"))
                .unlockedBy("has_containment_concrete", has(PwrRegistry.CONTAINMENT_CONCRETE_ITEM.get()))
                .save(output, id("crafting/steel_liner"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, PwrRegistry.CONTAINMENT_MONITOR_ITEM.get())
                .pattern(" E ")
                .pattern("PCP")
                .pattern(" R ")
                .define('E', CreateItems.get("electron_tube"))
                .define('P', CreateItems.get("iron_sheet"))
                .define('C', PwrRegistry.CONTAINMENT_CONCRETE_ITEM.get())
                .define('R', Items.COMPARATOR)
                .unlockedBy("has_containment_concrete", has(PwrRegistry.CONTAINMENT_CONCRETE_ITEM.get()))
                .save(output, id("crafting/containment_monitor"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, PwrRegistry.CONTAINMENT_PENETRATION_ITEM.get(), 2)
                .pattern(" C ")
                .pattern("FFF")
                .pattern(" C ")
                .define('C', PwrRegistry.CONTAINMENT_CONCRETE_ITEM.get())
                .define('F', CreateItems.get("fluid_pipe"))
                .unlockedBy("has_containment_concrete", has(PwrRegistry.CONTAINMENT_CONCRETE_ITEM.get()))
                .save(output, id("crafting/containment_penetration"));
        // Steel liner plate around an iron door, geared to a precision mechanism that drives it.
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, PwrRegistry.CONTAINMENT_DOOR_ITEM.get())
                .pattern("LL")
                .pattern("DM")
                .pattern("LL")
                .define('L', PwrRegistry.STEEL_LINER_ITEM.get())
                .define('D', Items.IRON_DOOR)
                .define('M', CreateItems.get("precision_mechanism"))
                .unlockedBy("has_steel_liner", has(PwrRegistry.STEEL_LINER_ITEM.get()))
                .save(output, id("crafting/containment_door"));
        // Copper catalyst plates in a steel box with a chimney.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, PwrRegistry.RECOMBINER_ITEM.get())
                .pattern(" F ")
                .pattern("CCC")
                .pattern("III")
                .define('F', CreateItems.get("fluid_pipe"))
                .define('C', CreateItems.get("copper_sheet"))
                .define('I', CreateItems.get("iron_sheet"))
                .unlockedBy("has_containment_concrete", has(PwrRegistry.CONTAINMENT_CONCRETE_ITEM.get()))
                .save(output, id("crafting/recombiner"));
        // A valve in front of a sand bed and graphite filters.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, PwrRegistry.FILTERED_VENT_ITEM.get())
                .pattern(" V ")
                .pattern("GCG")
                .pattern(" S ")
                .define('V', CreateItems.get("fluid_valve"))
                .define('G', AllNuclearItems.GRAPHITE.get())
                .define('C', PwrRegistry.CONTAINMENT_CONCRETE_ITEM.get())
                .define('S', Items.SAND)
                .unlockedBy("has_containment_concrete", has(PwrRegistry.CONTAINMENT_CONCRETE_ITEM.get()))
                .save(output, id("crafting/filtered_vent"));
        // Refractory obsidian laid in sacrificial concrete.
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, PwrRegistry.CORE_CATCHER_ITEM.get(), 4)
                .pattern("COC")
                .pattern("OCO")
                .pattern("COC")
                .define('C', PwrRegistry.CONTAINMENT_CONCRETE_ITEM.get())
                .define('O', Items.OBSIDIAN)
                .unlockedBy("has_containment_concrete", has(PwrRegistry.CONTAINMENT_CONCRETE_ITEM.get()))
                .save(output, id("crafting/core_catcher"));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, path);
    }
}
