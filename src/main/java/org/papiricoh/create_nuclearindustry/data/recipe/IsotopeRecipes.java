package org.papiricoh.create_nuclearindustry.data.recipe;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.api.data.recipe.MixingRecipeGen;
import com.simibubi.create.api.data.recipe.StandardProcessingRecipeGen;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import com.simibubi.create.content.processing.recipe.HeatCondition;
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
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.CompoundIngredient;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.enrichment.EnrichmentTier;
import org.papiricoh.create_nuclearindustry.enrichment.recipe.MinEnrichmentIngredient;
import org.papiricoh.create_nuclearindustry.isotope.AgedPlutoniumIngredient;
import org.papiricoh.create_nuclearindustry.isotope.IsotopeRegistry;
import org.papiricoh.create_nuclearindustry.processing.AllNuclearRecipeTypes;
import org.papiricoh.create_nuclearindustry.processing.recipe.TargetIrradiationRecipe;
import org.papiricoh.create_nuclearindustry.waste.WasteRegistry;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Recipes for isotopes and nuclear medicine. Cobalt comes from heated mixing of crushed iron and
 * copper ore (it hides in both, as in real nickel-copper ores); cobalt-60 from the reactor's
 * irradiation channel (no recipe: see {@code IrradiationChannelBlockEntity}); strontium-90 from
 * PUREX high-level waste; americium-241 from plutonium aged long enough; the technetium generator
 * from depleted uranium in a Target Station (photofission of U-238). Shielding is lead, or depleted uranium without
 * a lead mod, like the rest of the mod. Every recipe id is the mod's own.
 */
public final class IsotopeRecipes implements DataProvider {
    private static final TagKey<Item> LEAD_INGOTS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "ingots/lead"));

    private final List<DataProvider> generators;

    public IsotopeRecipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        generators = List.of(new Mixing(output, registries), new Compacting(output, registries), new Target(output, registries),
                new Crafting(output, registries));
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return CompletableFuture.allOf(generators.stream().map(generator -> generator.run(cache)).toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "Isotope recipes";
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, path);
    }

    /** Lead, or depleted uranium. */
    private static Ingredient shielding() {
        return CompoundIngredient.of(Ingredient.of(LEAD_INGOTS),
                MinEnrichmentIngredient.tier(AllNuclearItems.URANIUM.get(), EnrichmentTier.DEPLETED).toVanilla());
    }

    @SuppressWarnings("unused")
    static final class Mixing extends MixingRecipeGen {
        // Cobalt travels with iron and copper in their ores; melting both together frees a little of it.
        GeneratedRecipe COBALT = create("cobalt_nugget", b -> b
                .require(CreateItems.get("crushed_raw_iron"))
                .require(CreateItems.get("crushed_raw_copper"))
                .output(IsotopeRegistry.COBALT_NUGGET.get(), 2)
                .output(CreateItems.get("copper_nugget"), 3)
                .output(Items.IRON_NUGGET, 3)
                .requiresHeat(HeatCondition.HEATED));

        // Strontium-90 extracted from the fission products PUREX leaves in its high-level waste.
        GeneratedRecipe STRONTIUM_90 = create("strontium_90", b -> b
                .require(WasteRegistry.HIGH_LEVEL_WASTE.get())
                .require(AllNuclearFluids.NITRIC_ACID.get(), 250)
                .output(IsotopeRegistry.STRONTIUM_90.get())
                .output(0.25f, WasteRegistry.LOW_LEVEL_WASTE.get())
                .requiresHeat(HeatCondition.HEATED));

        // Americium-241 grows in stored plutonium as its plutonium-241 decays; nitric acid takes it back out.
        GeneratedRecipe AMERICIUM_241 = create("americium_241", b -> b
                .require(new AgedPlutoniumIngredient(Ingredient.of(AllNuclearItems.PLUTONIUM.get())).toVanilla())
                .require(AllNuclearFluids.NITRIC_ACID.get(), 250)
                .output(AllNuclearItems.PLUTONIUM.get())
                .output(IsotopeRegistry.AMERICIUM_241.get(), 2)
                .requiresHeat(HeatCondition.HEATED));

        Mixing(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries, Create_NuclearIndustry.MODID);
        }
    }

    @SuppressWarnings("unused")
    static final class Compacting extends StandardProcessingRecipeGen<BasinRecipe> {
        // Strontium titanate sealed in an iron capsule, like the plutonium pellet.
        GeneratedRecipe STRONTIUM_RTG_PELLET = create("strontium_rtg_pellet", b -> b
                .require(IsotopeRegistry.STRONTIUM_90.get()).require(IsotopeRegistry.STRONTIUM_90.get())
                .require(CreateItems.get("iron_sheet"))
                .output(IsotopeRegistry.STRONTIUM_RTG_PELLET.get()));

        Compacting(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries, Create_NuclearIndustry.MODID);
        }

        @Override
        protected IRecipeTypeInfo getRecipeType() {
            return AllRecipeTypes.COMPACTING;
        }
    }

    /** Targets for the Target Station's beam below the antiproton threshold. */
    @SuppressWarnings("unused")
    static final class Target extends StandardProcessingRecipeGen<TargetIrradiationRecipe> {
        // Photofission of uranium-238 in the beam's shower: molybdenum-99 is one of its commonest products.
        GeneratedRecipe TECHNETIUM_GENERATOR = create("technetium_generator", b -> b
                .require(MinEnrichmentIngredient.tier(AllNuclearItems.URANIUM.get(), EnrichmentTier.DEPLETED).toVanilla())
                .output(IsotopeRegistry.TECHNETIUM_GENERATOR.get())
                .duration(60));

        Target(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries, Create_NuclearIndustry.MODID);
        }

        @Override
        protected IRecipeTypeInfo getRecipeType() {
            return AllNuclearRecipeTypes.TARGET_IRRADIATION;
        }
    }

    static final class Crafting extends RecipeProvider {
        Crafting(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries);
        }

        @Override
        protected void buildRecipes(RecipeOutput output) {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, IsotopeRegistry.COBALT_INGOT.get())
                    .pattern("NNN").pattern("NNN").pattern("NNN")
                    .define('N', IsotopeRegistry.COBALT_NUGGET.get())
                    .unlockedBy("has_cobalt_nugget", has(IsotopeRegistry.COBALT_NUGGET.get()))
                    .save(output, id("crafting/cobalt_ingot"));
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, IsotopeRegistry.COBALT_NUGGET.get(), 9)
                    .requires(IsotopeRegistry.COBALT_INGOT.get())
                    .unlockedBy("has_cobalt_ingot", has(IsotopeRegistry.COBALT_INGOT.get()))
                    .save(output, id("crafting/cobalt_nugget_from_ingot"));
            // A sample tube through a casing block, to the reactor core.
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, IsotopeRegistry.IRRADIATION_CHANNEL_ITEM.get())
                    .pattern(" S ").pattern("PCP").pattern(" S ")
                    .define('S', CreateItems.get("iron_sheet"))
                    .define('P', CreateItems.get("fluid_pipe"))
                    .define('C', AllNuclearItems.REACTOR_CASING.get())
                    .unlockedBy("has_reactor_casing", has(AllNuclearItems.REACTOR_CASING.get()))
                    .save(output, id("crafting/reactor_irradiation_channel"));
            // A shielded head with a shutter, on four legs.
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, IsotopeRegistry.GAMMA_IRRADIATOR_ITEM.get())
                    .pattern("LLL").pattern("LPL").pattern("I I")
                    .define('L', shielding())
                    .define('P', CreateItems.get("precision_mechanism"))
                    .define('I', CreateItems.get("iron_sheet"))
                    .unlockedBy("has_cobalt_60", has(IsotopeRegistry.COBALT_60.get()))
                    .save(output, id("crafting/gamma_irradiator"));
            // An ionisation chamber with a speck of americium, a sounder and its battery.
            ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, IsotopeRegistry.SMOKE_DETECTOR_ITEM.get(), 4)
                    .pattern("NAN").pattern("NRN")
                    .define('N', Items.IRON_NUGGET)
                    .define('A', IsotopeRegistry.AMERICIUM_241.get())
                    .define('R', Items.REDSTONE)
                    .unlockedBy("has_americium_241", has(IsotopeRegistry.AMERICIUM_241.get()))
                    .save(output, id("crafting/smoke_detector"));
            // A shielded source holder with a collimator, and a film cassette.
            ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, IsotopeRegistry.RADIOGRAPHY_CAMERA.get())
                    .pattern("SLS").pattern("LGL").pattern("SPS")
                    .define('S', CreateItems.get("iron_sheet"))
                    .define('L', shielding())
                    .define('G', Items.GLASS_PANE)
                    .define('P', Items.PAPER)
                    .unlockedBy("has_cobalt_60", has(IsotopeRegistry.COBALT_60.get()))
                    .save(output, id("crafting/radiography_camera"));
            // A gamma probe and its case: the doses go in separately.
            ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, IsotopeRegistry.MEDICAL_ISOTOPE_KIT.get())
                    .pattern("LPL").pattern("BGB").pattern("LLL")
                    .define('L', Items.LEATHER)
                    .define('P', Items.PAPER)
                    .define('B', Items.GLASS_BOTTLE)
                    .define('G', AllNuclearItems.GEIGER_COUNTER.get())
                    .unlockedBy("has_technetium_99m", has(IsotopeRegistry.TECHNETIUM_99M.get()))
                    .save(output, id("crafting/medical_isotope_kit"));
        }
    }
}
