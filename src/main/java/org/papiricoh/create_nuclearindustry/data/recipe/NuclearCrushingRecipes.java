package org.papiricoh.create_nuclearindustry.data.recipe;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.api.data.recipe.StandardProcessingRecipeGen;
import com.simibubi.create.content.kinetics.crusher.CrushingRecipe;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Items;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.AllNuclearTags;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;

import java.util.concurrent.CompletableFuture;

@SuppressWarnings("unused")
public class NuclearCrushingRecipes extends StandardProcessingRecipeGen<CrushingRecipe> {

    GeneratedRecipe URANIUM_ORE = create("uranium_ore", b -> b
            .require(AllNuclearBlocks.URANIUM_ORE.get())
            .output(AllNuclearItems.CRUSHED_RAW_URANIUM.get())
            .output(0.75f, AllNuclearItems.CRUSHED_RAW_URANIUM.get())
            .output(0.6f, AllNuclearItems.FLUORITE.get())
            .output(0.75f, CreateItems.get("experience_nugget"))
            .output(0.125f, Items.COBBLED_DEEPSLATE)
            .duration(350));

    // Uraninite veins are hydrothermal: fluorite gangue comes up with the ore.
    GeneratedRecipe RAW_URANIUM = create("raw_uranium", b -> b
            .require(AllNuclearTags.Items.RAW_URANIUM)
            .output(AllNuclearItems.CRUSHED_RAW_URANIUM.get())
            .output(0.6f, AllNuclearItems.FLUORITE.get())
            .output(0.75f, CreateItems.get("experience_nugget"))
            .duration(400));

    GeneratedRecipe SPODUMENE_ORE = create("spodumene_ore", b -> b
            .require(AllNuclearBlocks.SPODUMENE_ORE.get())
            .output(AllNuclearItems.CRUSHED_SPODUMENE.get())
            .output(0.75f, AllNuclearItems.CRUSHED_SPODUMENE.get())
            .output(0.25f, AllNuclearItems.FLUORITE.get())
            .output(0.75f, CreateItems.get("experience_nugget"))
            .output(0.125f, Items.COBBLESTONE)
            .duration(300));

    // Pegmatites carry fluorite alongside the lithium minerals.
    GeneratedRecipe RAW_SPODUMENE = create("raw_spodumene", b -> b
            .require(AllNuclearTags.Items.C_RAW_LITHIUM)
            .output(AllNuclearItems.CRUSHED_SPODUMENE.get())
            .output(0.25f, AllNuclearItems.FLUORITE.get())
            .output(0.75f, CreateItems.get("experience_nugget"))
            .duration(300));

    // Keeps the metal's enrichment: a shapeless un-craft would turn it back into natural uranium.
    GeneratedRecipe DEPLETED_URANIUM_BLOCK = create("depleted_uranium_block", b -> b
            .require(AllNuclearBlocks.DEPLETED_URANIUM_BLOCK.get())
            .output(RecipeHelpers.enriched(AllNuclearItems.URANIUM.get(), org.papiricoh.create_nuclearindustry.enrichment.EnrichmentTier.DEPLETED, 9))
            .duration(250));

    // Shearing spent fuel for reprocessing: the fragments keep the tier the fuel was made at.
    GeneratedRecipe LOW_SPENT_FUEL = spentFuel(org.papiricoh.create_nuclearindustry.enrichment.EnrichmentTier.LOW);
    GeneratedRecipe HIGH_SPENT_FUEL = spentFuel(org.papiricoh.create_nuclearindustry.enrichment.EnrichmentTier.HIGH);

    // Spent MOX is mostly depleted uranium: its fragments are of the depleted tier.
    GeneratedRecipe MOX_SPENT_FUEL = create("mox_spent_fuel_fragments", b -> b
            .require(AllNuclearItems.DEPLETED_MOX_REACTOR_FUEL.get())
            .output(RecipeHelpers.enriched(AllNuclearItems.SPENT_FUEL_FRAGMENTS.get(),
                    org.papiricoh.create_nuclearindustry.enrichment.EnrichmentTier.DEPLETED))
            .output(0.5f, Items.IRON_NUGGET)
            .duration(200));

    private GeneratedRecipe spentFuel(org.papiricoh.create_nuclearindustry.enrichment.EnrichmentTier tier) {
        return create(tier.id() + "_spent_fuel_fragments", b -> b
                .require(org.papiricoh.create_nuclearindustry.enrichment.recipe.MinEnrichmentIngredient.tier(
                        AllNuclearItems.DEPLETED_URANIUM_REACTOR_FUEL.get(), tier))
                .output(RecipeHelpers.enriched(AllNuclearItems.SPENT_FUEL_FRAGMENTS.get(), tier))
                .output(0.5f, Items.IRON_NUGGET)
                .duration(200));
    }

    GeneratedRecipe SPENT_DT_PELLET = create("spent_dt_pellet", b -> b
            .require(AllNuclearItems.SPENT_DT_PELLET.get())
            .output(Items.IRON_NUGGET, 3)
            .duration(100));

    GeneratedRecipe BORAX_ORE = create("borax_ore", b -> b
            .require(AllNuclearBlocks.BORAX_ORE.get())
            .output(AllNuclearItems.BORAX_SALT.get(), 3)
            .output(0.5f, AllNuclearItems.BORAX_SALT.get())
            .output(0.125f, Items.COBBLESTONE)
            .duration(250));

    public NuclearCrushingRecipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, Create_NuclearIndustry.MODID);
    }

    @Override
    protected IRecipeTypeInfo getRecipeType() {
        return AllRecipeTypes.CRUSHING;
    }
}
