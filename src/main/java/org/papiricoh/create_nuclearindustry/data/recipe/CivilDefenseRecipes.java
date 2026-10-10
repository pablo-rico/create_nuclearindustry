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
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.CompoundIngredient;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.civildefense.CivilDefenseRegistry;
import org.papiricoh.create_nuclearindustry.enrichment.EnrichmentTier;
import org.papiricoh.create_nuclearindustry.enrichment.recipe.MinEnrichmentIngredient;

import java.util.concurrent.CompletableFuture;

/** Crafting recipes for sirens, the seismic detector and blast doors. */
public class CivilDefenseRecipes extends RecipeProvider {
    private static final TagKey<Item> LEAD_INGOTS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "ingots/lead"));

    public CivilDefenseRecipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, CivilDefenseRegistry.AIR_RAID_SIREN_ITEM.get())
                .pattern(" P ")
                .pattern("PNP")
                .pattern("ARA")
                .define('P', CreateItems.get("iron_sheet"))
                .define('N', Items.NOTE_BLOCK)
                .define('A', CreateItems.get("andesite_alloy"))
                .define('R', Items.REDSTONE)
                .unlockedBy("has_iron_sheet", has(CreateItems.get("iron_sheet")))
                .save(output, id("crafting/air_raid_siren"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, CivilDefenseRegistry.HAND_CRANK_SIREN_ITEM.get())
                .pattern(" P ")
                .pattern("PNP")
                .pattern("ACA")
                .define('P', CreateItems.get("iron_sheet"))
                .define('N', Items.NOTE_BLOCK)
                .define('A', CreateItems.get("andesite_alloy"))
                .define('C', CreateItems.get("cogwheel"))
                .unlockedBy("has_cogwheel", has(CreateItems.get("cogwheel")))
                .save(output, id("crafting/hand_crank_siren"));
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, CivilDefenseRegistry.SEISMIC_DETECTOR_ITEM.get())
                .pattern(" C ")
                .pattern("EAE")
                .pattern("SSS")
                .define('C', Items.COMPASS)
                .define('E', CreateItems.get("electron_tube"))
                .define('A', CreateItems.get("andesite_casing"))
                .define('S', Items.SMOOTH_STONE_SLAB)
                .unlockedBy("has_electron_tube", has(CreateItems.get("electron_tube")))
                .save(output, id("crafting/seismic_detector"));
        blastDoors(output);
    }

    /** Reinforced frames around obsidian, plate leaves, and lead (or depleted uranium) lining for the shielded ones. */
    private void blastDoors(RecipeOutput output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, CivilDefenseRegistry.BLAST_DOOR_FRAME_ITEM.get(), 8)
                .pattern("POP")
                .pattern("O O")
                .pattern("POP")
                .define('P', CreateItems.get("iron_sheet"))
                .define('O', Items.OBSIDIAN)
                .unlockedBy("has_obsidian", has(Items.OBSIDIAN))
                .save(output, id("crafting/blast_door_frame"));
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, CivilDefenseRegistry.STEEL_BLAST_DOOR_ITEM.get(), 2)
                .pattern("PPP")
                .pattern("PBP")
                .pattern("PPP")
                .define('P', CreateItems.get("iron_sheet"))
                .define('B', Items.IRON_BLOCK)
                .unlockedBy("has_blast_door_frame", has(CivilDefenseRegistry.BLAST_DOOR_FRAME_ITEM.get()))
                .save(output, id("crafting/steel_blast_door"));
        Ingredient lining = CompoundIngredient.of(Ingredient.of(LEAD_INGOTS),
                MinEnrichmentIngredient.tier(AllNuclearItems.URANIUM.get(), EnrichmentTier.DEPLETED).toVanilla());
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, CivilDefenseRegistry.LEAD_BLAST_DOOR_ITEM.get(), 2)
                .pattern("LDL")
                .pattern("LDL")
                .define('L', lining)
                .define('D', CivilDefenseRegistry.STEEL_BLAST_DOOR_ITEM.get())
                .unlockedBy("has_steel_blast_door", has(CivilDefenseRegistry.STEEL_BLAST_DOOR_ITEM.get()))
                .save(output, id("crafting/lead_blast_door"));
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, CivilDefenseRegistry.BLAST_DOOR_CONTROLLER_ITEM.get())
                .pattern(" M ")
                .pattern("SFS")
                .pattern(" R ")
                .define('M', CreateItems.get("precision_mechanism"))
                .define('S', CreateItems.get("shaft"))
                .define('F', CivilDefenseRegistry.BLAST_DOOR_FRAME_ITEM.get())
                .define('R', Items.REDSTONE)
                .unlockedBy("has_blast_door_frame", has(CivilDefenseRegistry.BLAST_DOOR_FRAME_ITEM.get()))
                .save(output, id("crafting/blast_door_controller"));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, path);
    }
}
