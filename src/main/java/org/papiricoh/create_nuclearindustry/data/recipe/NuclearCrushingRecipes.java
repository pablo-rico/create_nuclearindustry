package org.papiricoh.create_nuclearindustry.data.recipe;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.api.data.recipe.StandardProcessingRecipeGen;
import com.simibubi.create.content.kinetics.crusher.CrushingRecipe;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.AllNuclearTags;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.spentfuel.CoolingGrade;
import org.papiricoh.create_nuclearindustry.spentfuel.SpentFuelGradeIngredient;

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

    // Shearing spent fuel for reprocessing: the fragments keep the tier the fuel was made at. Fuel left to
    // cool in a pool rack or a dry cask first gives more fragments and less waste. Each recipe takes its
    // own cooling grades, so whichever one Create finds first is the one meant for the fuel.
    GeneratedRecipe LOW_SPENT_FUEL = spentFuel(org.papiricoh.create_nuclearindustry.enrichment.EnrichmentTier.LOW, SpentGrade.FRESH);
    GeneratedRecipe LOW_SPENT_FUEL_COOLED = spentFuel(org.papiricoh.create_nuclearindustry.enrichment.EnrichmentTier.LOW, SpentGrade.COOLED);
    GeneratedRecipe LOW_SPENT_FUEL_AGED = spentFuel(org.papiricoh.create_nuclearindustry.enrichment.EnrichmentTier.LOW, SpentGrade.AGED);
    GeneratedRecipe HIGH_SPENT_FUEL = spentFuel(org.papiricoh.create_nuclearindustry.enrichment.EnrichmentTier.HIGH, SpentGrade.FRESH);
    GeneratedRecipe HIGH_SPENT_FUEL_COOLED = spentFuel(org.papiricoh.create_nuclearindustry.enrichment.EnrichmentTier.HIGH, SpentGrade.COOLED);
    GeneratedRecipe HIGH_SPENT_FUEL_AGED = spentFuel(org.papiricoh.create_nuclearindustry.enrichment.EnrichmentTier.HIGH, SpentGrade.AGED);

    // Spent MOX is mostly depleted uranium: its fragments are of the depleted tier.
    GeneratedRecipe MOX_SPENT_FUEL = moxSpentFuel(SpentGrade.FRESH);
    GeneratedRecipe MOX_SPENT_FUEL_COOLED = moxSpentFuel(SpentGrade.COOLED);
    GeneratedRecipe MOX_SPENT_FUEL_AGED = moxSpentFuel(SpentGrade.AGED);

    /**
     * Cooling grades one spent fuel recipe takes, and what cooling adds: a chance of a second fragment
     * and a smaller chance of waste. {@code FRESH} keeps the old recipe ids and takes fuel without a history.
     */
    private enum SpentGrade {
        FRESH(CoolingGrade.HOT, CoolingGrade.WARM, "", 0.0f, 0.25f),
        COOLED(CoolingGrade.COOLED, CoolingGrade.COOLED, "_cooled", 0.25f, 0.1f),
        AGED(CoolingGrade.AGED, CoolingGrade.AGED, "_aged", 0.5f, 0.0f);

        final CoolingGrade min;
        final CoolingGrade max;
        final String suffix;
        final float extraFragment;
        final float waste;

        SpentGrade(CoolingGrade min, CoolingGrade max, String suffix, float extraFragment, float waste) {
            this.min = min;
            this.max = max;
            this.suffix = suffix;
            this.extraFragment = extraFragment;
            this.waste = waste;
        }
    }

    private GeneratedRecipe spentFuel(org.papiricoh.create_nuclearindustry.enrichment.EnrichmentTier tier, SpentGrade grade) {
        return spentFuel(tier.id() + "_spent_fuel_fragments" + grade.suffix, grade,
                org.papiricoh.create_nuclearindustry.enrichment.recipe.MinEnrichmentIngredient.tier(
                        AllNuclearItems.DEPLETED_URANIUM_REACTOR_FUEL.get(), tier).toVanilla(), tier);
    }

    private GeneratedRecipe moxSpentFuel(SpentGrade grade) {
        return spentFuel("mox_spent_fuel_fragments" + grade.suffix, grade,
                Ingredient.of(AllNuclearItems.DEPLETED_MOX_REACTOR_FUEL.get()),
                org.papiricoh.create_nuclearindustry.enrichment.EnrichmentTier.DEPLETED);
    }

    private GeneratedRecipe spentFuel(String name, SpentGrade grade, Ingredient fuel,
                                      org.papiricoh.create_nuclearindustry.enrichment.EnrichmentTier fragmentTier) {
        return create(name, b -> {
            b.require(SpentFuelGradeIngredient.of(fuel, grade.min, grade.max))
                    .output(RecipeHelpers.enriched(AllNuclearItems.SPENT_FUEL_FRAGMENTS.get(), fragmentTier));
            if (grade.extraFragment > 0) {
                b.output(grade.extraFragment, RecipeHelpers.enriched(AllNuclearItems.SPENT_FUEL_FRAGMENTS.get(), fragmentTier));
            }
            b.output(0.5f, Items.IRON_NUGGET);
            if (grade.waste > 0) {
                b.output(grade.waste, AllNuclearItems.RADIOACTIVE_WASTE.get());
            }
            return b.duration(200);
        });
    }

    // A spent RTG capsule gives back its iron and a little residue.
    GeneratedRecipe DEPLETED_RTG_PELLET = create("depleted_rtg_pellet", b -> b
            .require(AllNuclearItems.DEPLETED_RTG_PELLET.get())
            .output(Items.IRON_NUGGET, 3)
            .output(0.25f, AllNuclearItems.RADIOACTIVE_WASTE.get())
            .duration(100));

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
