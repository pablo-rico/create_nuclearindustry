package org.papiricoh.create_nuclearindustry.data.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.crafting.CompoundIngredient;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.enrichment.EnrichmentTier;
import org.papiricoh.create_nuclearindustry.enrichment.recipe.MinEnrichmentIngredient;
import org.papiricoh.create_nuclearindustry.reactor.visual.ReactorVisualsRegistry;

import java.util.concurrent.CompletableFuture;

/** Crafting recipe for the reactor viewport. */
public class ReactorVisualsRecipes extends RecipeProvider {
    private static final TagKey<Item> LEAD_INGOTS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "ingots/lead"));

    public ReactorVisualsRecipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        // Lead glass: clear glass leaded with lead or depleted uranium, set in reactor casing.
        Ingredient lead = CompoundIngredient.of(Ingredient.of(LEAD_INGOTS),
                MinEnrichmentIngredient.tier(AllNuclearItems.URANIUM.get(), EnrichmentTier.DEPLETED).toVanilla());
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ReactorVisualsRegistry.REACTOR_VIEWPORT_ITEM.get(), 4)
                .pattern("CLC")
                .pattern("LGL")
                .pattern("CLC")
                .define('C', AllNuclearItems.REACTOR_CASING.get())
                .define('L', lead)
                .define('G', Tags.Items.GLASS_BLOCKS_COLORLESS)
                .unlockedBy("has_reactor_casing", has(AllNuclearItems.REACTOR_CASING.get()))
                .save(output, ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "crafting/reactor_viewport"));
    }
}
