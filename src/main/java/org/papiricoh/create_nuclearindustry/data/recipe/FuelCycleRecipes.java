package org.papiricoh.create_nuclearindustry.data.recipe;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.api.data.recipe.MixingRecipeGen;
import com.simibubi.create.api.data.recipe.SequencedAssemblyRecipeGen;
import com.simibubi.create.api.data.recipe.StandardProcessingRecipeGen;
import com.simibubi.create.content.fluids.transfer.EmptyingRecipe;
import com.simibubi.create.content.kinetics.crusher.CrushingRecipe;
import com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe;
import com.simibubi.create.content.kinetics.fan.processing.SplashingRecipe;
import com.simibubi.create.content.kinetics.press.PressingRecipe;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import com.simibubi.create.content.processing.recipe.HeatCondition;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
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
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluids;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.enrichment.EnrichmentTier;
import org.papiricoh.create_nuclearindustry.enrichment.recipe.MinEnrichmentIngredient;
import org.papiricoh.create_nuclearindustry.fuelcycle.FuelCycleRegistry;
import org.papiricoh.create_nuclearindustry.processing.AllNuclearRecipeTypes;
import org.papiricoh.create_nuclearindustry.processing.recipe.AcidLeachingRecipe;
import org.papiricoh.create_nuclearindustry.processing.recipe.CentrifugingRecipe;
import org.papiricoh.create_nuclearindustry.processing.recipe.IrradiationRecipe;
import org.papiricoh.create_nuclearindustry.spentfuel.CoolingGrade;
import org.papiricoh.create_nuclearindustry.spentfuel.SpentFuelGradeIngredient;
import org.papiricoh.create_nuclearindustry.waste.WasteRegistry;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.papiricoh.create_nuclearindustry.data.recipe.RecipeHelpers.enriched;

/**
 * The renewable fuel cycle's recipes: rock, sand, sea water and brine as sources of the base
 * materials, the acid leaching and irradiation fan treatments, decontamination, and the thorium
 * fuel cycle from thorium dioxide to uranium-233 and back.
 *
 * <p>The yields are tuned so that a renewable route costs between one and two times the ore route
 * per unit of product, counting what goes in and the machine time; {@code RenewableCostTest}
 * checks the generated files against that rule. Three recipes replace Create's own (washing sand,
 * red sand and gravel, and crushing tuff), keeping everything Create's gave and adding the new
 * material: two recipes for the same item would leave the choice between them to chance.
 *
 * <p>Like {@link WasteTreatmentRecipes}, this provider runs its generators under a name of its own,
 * since Create names generators after their mod and recipe type.
 */
public final class FuelCycleRecipes implements DataProvider {
    private final List<DataProvider> generators;

    public FuelCycleRecipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        generators = List.of(new Crushing(output, registries), new Washing(output, registries), new Leaching(output, registries),
                new Irradiation(output, registries), new Mixing(output, registries), new Compacting(output, registries),
                new Assembly(output, registries), new Centrifuging(output, registries), new Emptying(output, registries),
                new Crafting(output, registries));
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return CompletableFuture.allOf(generators.stream().map(generator -> generator.run(cache)).toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "Renewable fuel cycle recipes";
    }

    private static ResourceLocation createId(String path) {
        return ResourceLocation.fromNamespaceAndPath("create", path);
    }

    /** Base class of the generators: the recipe type and the mod's namespace. */
    private abstract static class Gen<R extends StandardProcessingRecipe<?>> extends StandardProcessingRecipeGen<R> {
        private final IRecipeTypeInfo type;

        Gen(PackOutput output, CompletableFuture<HolderLookup.Provider> registries, IRecipeTypeInfo type) {
            super(output, registries, Create_NuclearIndustry.MODID);
            this.type = type;
        }

        @Override
        protected IRecipeTypeInfo getRecipeType() {
            return type;
        }
    }

    // ------------------------------------------------------------------ rock

    /**
     * Crushed rock gives fines: a little of everything in the rock. Two fines per three blocks on
     * average, and what the rock already gave (red sand from granite, Create's nuggets from tuff).
     */
    @SuppressWarnings("unused")
    static final class Crushing extends Gen<CrushingRecipe> {
        GeneratedRecipe GRANITE = create("granite", b -> b
                .require(Items.GRANITE)
                .output(FuelCycleRegistry.GRANITE_FINES.get())
                .output(0.5f, FuelCycleRegistry.GRANITE_FINES.get())
                .output(0.25f, Items.RED_SAND)
                .duration(200));

        GeneratedRecipe DEEPSLATE = deepslate("deepslate", Items.DEEPSLATE);
        GeneratedRecipe COBBLED_DEEPSLATE = deepslate("cobbled_deepslate", Items.COBBLED_DEEPSLATE);

        // Create already crushes tuff into a few nuggets; its two recipes are replaced by ones that also give the fines.
        GeneratedRecipe TUFF = tuff(createId("tuff"), Ingredient.of(Items.TUFF));
        GeneratedRecipe TUFF_RECYCLING = tuff(createId("tuff_recycling"),
                Ingredient.of(TagKey.create(Registries.ITEM, createId("stone_types/tuff"))));

        // Spent thorium fuel is sheared like the other fuels, and gives more for having cooled.
        GeneratedRecipe SPENT_THORIUM = spentThorium("", CoolingGrade.HOT, CoolingGrade.WARM, 0.0f, 0.25f);
        GeneratedRecipe SPENT_THORIUM_COOLED = spentThorium("_cooled", CoolingGrade.COOLED, CoolingGrade.COOLED, 0.25f, 0.1f);
        GeneratedRecipe SPENT_THORIUM_AGED = spentThorium("_aged", CoolingGrade.AGED, CoolingGrade.AGED, 0.5f, 0.0f);

        Crushing(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries, AllRecipeTypes.CRUSHING);
        }

        private GeneratedRecipe deepslate(String name, ItemLike rock) {
            return create(name, b -> b
                    .require(rock)
                    .output(FuelCycleRegistry.DEEPSLATE_FINES.get())
                    .output(0.5f, FuelCycleRegistry.DEEPSLATE_FINES.get())
                    .output(0.1f, Items.FLINT)
                    .duration(250));
        }

        private GeneratedRecipe tuff(ResourceLocation id, Ingredient rock) {
            return create(id, b -> b
                    .require(rock)
                    .output(FuelCycleRegistry.TUFF_FINES.get())
                    .output(0.5f, FuelCycleRegistry.TUFF_FINES.get())
                    .output(0.25f, Items.FLINT)
                    .output(0.1f, Items.GOLD_NUGGET)
                    .output(0.1f, CreateItems.get("copper_nugget"))
                    .output(0.1f, CreateItems.get("zinc_nugget"))
                    .output(0.1f, Items.IRON_NUGGET)
                    .duration(350));
        }

        private GeneratedRecipe spentThorium(String suffix, CoolingGrade min, CoolingGrade max, float extraFragment, float waste) {
            return create("depleted_thorium_reactor_fuel" + suffix, b -> {
                b.require(SpentFuelGradeIngredient.of(Ingredient.of(FuelCycleRegistry.DEPLETED_THORIUM_REACTOR_FUEL.get()), min, max))
                        .output(FuelCycleRegistry.THORIUM_SPENT_FRAGMENTS.get());
                if (extraFragment > 0) {
                    b.output(extraFragment, FuelCycleRegistry.THORIUM_SPENT_FRAGMENTS.get());
                }
                b.output(0.5f, Items.IRON_NUGGET);
                if (waste > 0) {
                    b.output(waste, AllNuclearItems.RADIOACTIVE_WASTE.get());
                }
                return b.duration(200);
            });
        }
    }

    // ------------------------------------------------------------------ sand

    /**
     * Washing sand, red sand and gravel pans out heavy mineral sand (monazite and zircon) next to
     * what Create's washing gave. These replace Create's recipes of the same name.
     */
    @SuppressWarnings("unused")
    static final class Washing extends Gen<SplashingRecipe> {
        GeneratedRecipe SAND = create(createId("sand"), b -> b
                .require(Items.SAND)
                .output(0.25f, Items.CLAY_BALL)
                .output(0.2f, FuelCycleRegistry.HEAVY_MINERAL_SAND.get()));

        GeneratedRecipe RED_SAND = create(createId("red_sand"), b -> b
                .require(Items.RED_SAND)
                .output(0.125f, Items.GOLD_NUGGET, 3)
                .output(0.05f, Items.DEAD_BUSH)
                .output(0.25f, FuelCycleRegistry.HEAVY_MINERAL_SAND.get()));

        GeneratedRecipe GRAVEL = create(createId("gravel"), b -> b
                .require(Items.GRAVEL)
                .output(0.25f, Items.FLINT)
                .output(0.125f, Items.IRON_NUGGET)
                .output(0.15f, FuelCycleRegistry.HEAVY_MINERAL_SAND.get()));

        Washing(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries, AllRecipeTypes.SPLASHING);
        }
    }

    // ------------------------------------------------------------------ acid leaching

    /** A fan blowing through sulfuric acid: traces out of fines and sands, uranium off the adsorbent, contamination off waste. */
    @SuppressWarnings("unused")
    static final class Leaching extends Gen<AcidLeachingRecipe> {
        // Granite carries a little uraninite and thorite.
        GeneratedRecipe GRANITE_FINES = create("granite_fines", b -> b
                .require(FuelCycleRegistry.GRANITE_FINES.get())
                .output(0.3f, AllNuclearItems.URANIUM_CONCENTRATE.get())
                .output(0.05f, FuelCycleRegistry.THORIUM_DIOXIDE.get()));

        // Volcanic tuff weathers into lithium clays.
        GeneratedRecipe TUFF_FINES = create("tuff_fines", b -> b
                .require(FuelCycleRegistry.TUFF_FINES.get())
                .output(0.35f, AllNuclearItems.SPODUMENE_CONCENTRATE.get()));

        // Deep rock: uranium in black shale, fluorite in its veins.
        GeneratedRecipe DEEPSLATE_FINES = create("deepslate_fines", b -> b
                .require(FuelCycleRegistry.DEEPSLATE_FINES.get())
                .output(0.3f, AllNuclearItems.URANIUM_CONCENTRATE.get())
                .output(0.1f, AllNuclearItems.FLUORITE.get()));

        // Monazite is the world's thorium ore; its sands also carry a little uranium and beryl.
        GeneratedRecipe HEAVY_MINERAL_SAND = create("heavy_mineral_sand", b -> b
                .require(FuelCycleRegistry.HEAVY_MINERAL_SAND.get())
                .output(FuelCycleRegistry.THORIUM_DIOXIDE.get())
                .output(0.1f, AllNuclearItems.URANIUM_CONCENTRATE.get())
                .output(0.1f, AllNuclearItems.BERYLLIUM.get()));

        // Stripping the uranium off the adsorbent wears it: half of it can be loaded again.
        GeneratedRecipe LOADED_ADSORBENT = create("loaded_adsorbent", b -> b
                .require(FuelCycleRegistry.LOADED_ADSORBENT.get())
                .output(0.3f, AllNuclearItems.URANIUM_CONCENTRATE.get())
                .output(0.5f, FuelCycleRegistry.AMIDOXIME_ADSORBENT.get()));

        // Decontamination: the metal comes back clean, the activity ends up in the sludge.
        GeneratedRecipe LOW_LEVEL_WASTE = create("low_level_waste", b -> b
                .require(WasteRegistry.LOW_LEVEL_WASTE.get())
                .output(Items.IRON_NUGGET, 2)
                .output(FuelCycleRegistry.RADIOACTIVE_SLUDGE.get()));

        GeneratedRecipe SPENT_DT_PELLET = create("spent_dt_pellet", b -> b
                .require(AllNuclearItems.SPENT_DT_PELLET.get())
                .output(Items.IRON_NUGGET, 3)
                .output(0.25f, FuelCycleRegistry.RADIOACTIVE_SLUDGE.get()));

        GeneratedRecipe DEPLETED_RTG_PELLET = create("depleted_rtg_pellet", b -> b
                .require(AllNuclearItems.DEPLETED_RTG_PELLET.get())
                .output(Items.IRON_NUGGET, 3)
                .output(FuelCycleRegistry.RADIOACTIVE_SLUDGE.get()));

        Leaching(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries, AllNuclearRecipeTypes.ACID_LEACHING);
        }
    }

    // ------------------------------------------------------------------ irradiation

    /** A fan blowing past a radioactive block: neutron captures in fertile material. */
    @SuppressWarnings("unused")
    static final class Irradiation extends Gen<IrradiationRecipe> {
        // Thorium-232 captures a neutron and decays through protactinium-233 towards uranium-233.
        GeneratedRecipe THORIUM_DIOXIDE = create("thorium_dioxide", b -> b
                .require(FuelCycleRegistry.THORIUM_DIOXIDE.get())
                .output(FuelCycleRegistry.IRRADIATED_THORIUM.get()));

        // Uranium-238 captures a neutron and decays towards plutonium-239.
        GeneratedRecipe DEPLETED_URANIUM_DIOXIDE = create("depleted_uranium_dioxide", b -> b
                .require(MinEnrichmentIngredient.tier(AllNuclearItems.URANIUM_DIOXIDE.get(), EnrichmentTier.DEPLETED))
                .output(FuelCycleRegistry.IRRADIATED_DEPLETED_URANIUM.get()));

        GeneratedRecipe DEPLETED_URANIUM = create("depleted_uranium", b -> b
                .require(MinEnrichmentIngredient.tier(AllNuclearItems.URANIUM.get(), EnrichmentTier.DEPLETED))
                .output(FuelCycleRegistry.IRRADIATED_DEPLETED_URANIUM.get()));

        Irradiation(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries, AllNuclearRecipeTypes.IRRADIATION);
        }
    }

    // ------------------------------------------------------------------ mixing

    @SuppressWarnings("unused")
    static final class Mixing extends MixingRecipeGen {
        // Boiling sea water down: lithium and boron salts stay behind in the brine.
        GeneratedRecipe BRINE = create("brine", b -> b
                .require(Fluids.WATER, 1000)
                .output(AllNuclearFluids.BRINE.get(), 250)
                .requiresHeat(HeatCondition.HEATED));

        // Acrylic fibre (string) treated with a nitrogen source into an amidoxime adsorbent.
        GeneratedRecipe AMIDOXIME_ADSORBENT = create("amidoxime_adsorbent", b -> b
                .require(Items.STRING)
                .require(Items.BONE_MEAL)
                .require(Fluids.WATER, 250)
                .output(FuelCycleRegistry.AMIDOXIME_ADSORBENT.get(), 2));

        // Soaked in sea water the fibre picks up uranyl ions; brine is richer, so less of it does.
        GeneratedRecipe LOADED_ADSORBENT = create("loaded_adsorbent", b -> b
                .require(FuelCycleRegistry.AMIDOXIME_ADSORBENT.get())
                .require(Fluids.WATER, 1000)
                .output(FuelCycleRegistry.LOADED_ADSORBENT.get()));

        GeneratedRecipe LOADED_ADSORBENT_FROM_BRINE = create("loaded_adsorbent_from_brine", b -> b
                .require(FuelCycleRegistry.AMIDOXIME_ADSORBENT.get())
                .require(AllNuclearFluids.BRINE.get(), 100)
                .output(FuelCycleRegistry.LOADED_ADSORBENT.get()));

        // Irradiated thorium dissolved in nitric acid: a little uranium-233 separates, most thorium comes back.
        GeneratedRecipe IRRADIATED_THORIUM = create("irradiated_thorium", b -> b
                .require(FuelCycleRegistry.IRRADIATED_THORIUM.get())
                .require(AllNuclearFluids.NITRIC_ACID.get(), 100)
                .output(0.8f, FuelCycleRegistry.THORIUM_DIOXIDE.get())
                .output(0.12f, FuelCycleRegistry.URANIUM_233_DIOXIDE.get())
                .requiresHeat(HeatCondition.HEATED));

        // Irradiated depleted uranium joins the PUREX chain: traces of plutonium go into the spent fuel solution.
        GeneratedRecipe IRRADIATED_DEPLETED_URANIUM = create("irradiated_depleted_uranium", b -> b
                .require(FuelCycleRegistry.IRRADIATED_DEPLETED_URANIUM.get())
                .require(AllNuclearFluids.NITRIC_ACID.get(), 250)
                .output(enriched(AllNuclearItems.URANYL_NITRATE.get(), EnrichmentTier.DEPLETED))
                .output(AllNuclearFluids.SPENT_FUEL_SOLUTION.get(), 25)
                .requiresHeat(HeatCondition.HEATED));

        // THOREX: spent thorium fuel dissolves in nitric acid (real plants add a little fluoride).
        GeneratedRecipe DISSOLVE_THORIUM_SPENT_FUEL = create("dissolve_thorium_spent_fuel", b -> b
                .require(FuelCycleRegistry.THORIUM_SPENT_FRAGMENTS.get())
                .require(AllNuclearFluids.NITRIC_ACID.get(), 250)
                .output(AllNuclearFluids.THORIUM_FUEL_SOLUTION.get(), 500)
                .requiresHeat(HeatCondition.HEATED));

        // Uranium-233 nitrate denitrated to oxide; part of the acid is recovered.
        GeneratedRecipe URANIUM_233_DIOXIDE = create("uranium_233_dioxide", b -> b
                .require(AllNuclearFluids.URANIUM_233_NITRATE.get(), 250)
                .output(FuelCycleRegistry.URANIUM_233_DIOXIDE.get())
                .output(AllNuclearFluids.NITRIC_ACID.get(), 100)
                .requiresHeat(HeatCondition.HEATED));

        Mixing(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries, Create_NuclearIndustry.MODID);
        }
    }

    // ------------------------------------------------------------------ thorium fuel

    /** Thorium fuel pellets: three parts thorium dioxide to one of a fissile seed. */
    @SuppressWarnings("unused")
    static final class Compacting extends Gen<BasinRecipe> {
        GeneratedRecipe LEU_SEED = pellets("thorium_fuel_pellet", MinEnrichmentIngredient.tier(
                AllNuclearItems.URANIUM_DIOXIDE.get(), EnrichmentTier.LOW).toVanilla());
        GeneratedRecipe URANIUM_233_SEED = pellets("thorium_fuel_pellet_from_uranium_233",
                Ingredient.of(FuelCycleRegistry.URANIUM_233_DIOXIDE.get()));

        Compacting(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries, AllRecipeTypes.COMPACTING);
        }

        private GeneratedRecipe pellets(String name, Ingredient seed) {
            Item thoria = FuelCycleRegistry.THORIUM_DIOXIDE.get();
            return create(name, b -> b
                    .require(thoria).require(thoria).require(thoria)
                    .require(seed)
                    .output(FuelCycleRegistry.THORIUM_FUEL_PELLET.get(), 4));
        }
    }

    /** Thorium assemblies are built like the uranium and MOX ones: two pellets into an iron sheet, pressed shut. */
    static final class Assembly extends SequencedAssemblyRecipeGen {
        {
            Ingredient pellet = Ingredient.of(FuelCycleRegistry.THORIUM_FUEL_PELLET.get());
            create("thorium_reactor_fuel", b -> b
                    .require(CreateItems.get("iron_sheet"))
                    .transitionTo(FuelCycleRegistry.INCOMPLETE_THORIUM_REACTOR_FUEL.get())
                    .addOutput(FuelCycleRegistry.THORIUM_REACTOR_FUEL.get(), 1)
                    .loops(1)
                    .addStep(DeployerApplicationRecipe::new, step -> step.require(pellet))
                    .addStep(DeployerApplicationRecipe::new, step -> step.require(pellet))
                    .addStep(PressingRecipe::new, step -> step));
        }

        Assembly(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries, Create_NuclearIndustry.MODID);
        }
    }

    // ------------------------------------------------------------------ centrifuge

    @SuppressWarnings("unused")
    static final class Centrifuging extends Gen<CentrifugingRecipe> {
        // Brine spun down: lithium carbonate and borax settle out, fresh water is left.
        GeneratedRecipe BRINE = create("brine", b -> b
                .require(AllNuclearFluids.BRINE.get(), 1000)
                .output(0.6f, AllNuclearItems.LITHIUM_CARBONATE.get())
                .output(AllNuclearItems.BORAX_SALT.get(), 2)
                .output(Fluids.WATER, 500)
                .duration(200));

        // THOREX extraction: uranium-233 leaves in the product, the acid is recovered as tails,
        // thorium comes back as oxide and the fission products settle out as high-level waste.
        GeneratedRecipe THORIUM_FUEL_SOLUTION = create("thorium_fuel_solution", b -> b
                .require(AllNuclearFluids.THORIUM_FUEL_SOLUTION.get(), 1000)
                .output(AllNuclearFluids.URANIUM_233_NITRATE.get(), 250)
                .output(AllNuclearFluids.NITRIC_ACID.get(), 250)
                .output(FuelCycleRegistry.THORIUM_DIOXIDE.get(), 2)
                .output(WasteRegistry.HIGH_LEVEL_WASTE.get())
                .duration(300));

        Centrifuging(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries, AllNuclearRecipeTypes.CENTRIFUGING);
        }
    }

    /** An item drain pours the sludge out as contaminated water, which a centrifuge cleans. */
    @SuppressWarnings("unused")
    static final class Emptying extends Gen<EmptyingRecipe> {
        GeneratedRecipe RADIOACTIVE_SLUDGE = create("radioactive_sludge", b -> b
                .require(FuelCycleRegistry.RADIOACTIVE_SLUDGE.get())
                .output(AllNuclearFluids.CONTAMINATED_WATER.get(), 500));

        Emptying(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries, AllRecipeTypes.EMPTYING);
        }
    }

    /** The irradiation source: a capsule of waste in an open iron cage. */
    static final class Crafting extends RecipeProvider {
        Crafting(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries);
        }

        @Override
        protected void buildRecipes(RecipeOutput output) {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, FuelCycleRegistry.IRRADIATION_SOURCE_ITEM.get())
                    .pattern("PBP")
                    .pattern("BWB")
                    .pattern("PBP")
                    .define('P', CreateItems.get("iron_sheet"))
                    .define('B', Items.IRON_BARS)
                    .define('W', AllNuclearItems.RADIOACTIVE_WASTE.get())
                    .unlockedBy("has_radioactive_waste", has(AllNuclearItems.RADIOACTIVE_WASTE.get()))
                    .save(output, ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "crafting/irradiation_source"));
        }
    }
}
