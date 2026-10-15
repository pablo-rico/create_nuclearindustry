package org.papiricoh.create_nuclearindustry.gametest;

import com.simibubi.create.AllRecipeTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.locale.Language;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.NuclearFeatures;
import org.papiricoh.create_nuclearindustry.controlroom.ControlRoom2Registry;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ModuleType;
import org.papiricoh.create_nuclearindustry.creativetab.SectionedOutput;
import org.papiricoh.create_nuclearindustry.integration.jei.PlantJeiInfo;
import org.papiricoh.create_nuclearindustry.spentfuel.CoolingGrade;
import org.papiricoh.create_nuclearindustry.spentfuel.SpentFuel;
import org.papiricoh.create_nuclearindustry.spentfuel.SpentFuelData;
import org.papiricoh.create_nuclearindustry.spentfuel.SpentFuelGradeIngredient;
import org.papiricoh.create_nuclearindustry.spentfuel.SpentFuelRegistry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * What recipe viewers show of the power plant areas: every item has an info page with its text,
 * and the spent fuel crushing recipes show example assemblies that carry the cooling grade each
 * recipe takes, so a player can tell the Hot/Warm, Cooled and Aged recipes apart.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class PlantJeiTests {
    /** Their own batch: they build nothing, and must not shift where the world-building tests stand. */
    private static final String BATCH = "plant_jei";

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void plantInfoPagesHaveText(GameTestHelper helper) {
        Language language = Language.getInstance();
        Set<String> ids = new HashSet<>();
        for (PlantJeiInfo.Page page : PlantJeiInfo.pages()) {
            helper.assertTrue(ids.add(page.id()), "info page " + page.id() + " is listed twice");
            helper.assertTrue(language.has(page.key()), "no text for " + page.key());
            helper.assertTrue(!page.items().isEmpty() || !page.fluids().isEmpty(), "info page " + page.id() + " is shown for nothing");
            page.items().forEach(stack -> helper.assertFalse(stack.isEmpty(), "empty stack on info page " + page.id()));
            page.fluids().forEach(fluid -> helper.assertFalse(fluid.isEmpty(), "empty fluid on info page " + page.id()));
        }
        helper.assertTrue(language.has("jei.create_nuclearindustry.category.vitrification"), "no title for the vitrification category");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void everyPlantItemHasAnInfoPage(GameTestHelper helper) {
        Set<Item> covered = new HashSet<>();
        PlantJeiInfo.pages().forEach(page -> page.items().forEach(stack -> covered.add(stack.getItem())));

        List<ItemStack> shown = new ArrayList<>();
        SectionedOutput output = new SectionedOutput((stack, visibility) -> shown.add(stack));
        NuclearFeatures.fillCreativeTab(null, output);
        output.finish();
        // Not in the creative tab: torn off a recorder, and the module comes with the other desk modules.
        shown.add(new ItemStack(ControlRoom2Registry.CHART_ROLL.get()));
        shown.add(new ItemStack(AllNuclearItems.CONTROL_MODULES.get(ModuleType.CONTROLLER).get()));

        helper.assertFalse(shown.isEmpty(), "the plant areas put nothing in the creative tab");
        for (ItemStack stack : shown) {
            helper.assertTrue(covered.contains(stack.getItem()),
                    BuiltInRegistries.ITEM.getKey(stack.getItem()) + " has no JEI info page");
        }
        helper.succeed();
    }

    /** The safe shutdown page follows boric acid and the water bled from a core; the building hydrogen page the recombiner. */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void shutdownPagesFollowTheirFluids(GameTestHelper helper) {
        PlantJeiInfo.Page shutdown = PlantJeiInfo.pages().stream().filter(page -> page.id().equals("safe_shutdown")).findFirst().orElse(null);
        helper.assertTrue(shutdown != null, "a safe shutdown page");
        if (shutdown == null) {
            return;
        }
        for (var fluid : List.of(AllNuclearFluids.BORIC_ACID.get(), AllNuclearFluids.HOT_WATER.get(), AllNuclearFluids.HOT_HEAVY_WATER.get())) {
            helper.assertTrue(shutdown.fluids().stream().anyMatch(stack -> stack.getFluid() == fluid), "shown for " + fluid);
        }
        PlantJeiInfo.Page hydrogen = PlantJeiInfo.pages().stream().filter(page -> page.id().equals("reactor_building_hydrogen"))
                .findFirst().orElse(null);
        helper.assertTrue(hydrogen != null && hydrogen.items().stream()
                        .anyMatch(stack -> stack.is(org.papiricoh.create_nuclearindustry.reactor.pwr.PwrRegistry.RECOMBINER_ITEM.get())),
                "a building hydrogen page on the recombiner");
        Language language = Language.getInstance();
        helper.assertTrue(language.has(shutdown.key()) && hydrogen != null && language.has(hydrogen.key()), "both with their text");
        helper.succeed();
    }

    /**
     * Every example a recipe viewer shows for a spent fuel crushing recipe carries a cooling grade
     * inside that recipe's range, is accepted by it and by no other spent fuel recipe; and every
     * grade of every fuel shown has exactly one recipe.
     */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = BATCH)
    public static void spentFuelRecipesShowTheirGrade(GameTestHelper helper) {
        List<SpentFuelGradeIngredient> graded = new ArrayList<>();
        List<Ingredient> ingredients = new ArrayList<>();
        for (RecipeHolder<?> holder : helper.getLevel().getRecipeManager().getRecipes()) {
            Recipe<?> recipe = holder.value();
            if (!AllRecipeTypes.CRUSHING.getId().equals(BuiltInRegistries.RECIPE_TYPE.getKey(recipe.getType()))
                    || recipe.getIngredients().isEmpty()) {
                continue;
            }
            Ingredient ingredient = recipe.getIngredients().get(0);
            if (ingredient.getCustomIngredient() instanceof SpentFuelGradeIngredient grade) {
                graded.add(grade);
                ingredients.add(ingredient);
            }
        }
        // Uranium (low and high enriched) and MOX, each in three grade ranges.
        helper.assertTrue(graded.size() >= 9, "expected at least 9 spent fuel crushing recipes, found " + graded.size());

        for (int i = 0; i < graded.size(); i++) {
            SpentFuelGradeIngredient grade = graded.get(i);
            ItemStack[] examples = ingredients.get(i).getItems();
            helper.assertTrue(examples.length > 0, "a spent fuel recipe shows no examples");
            for (ItemStack example : examples) {
                SpentFuelData data = example.get(SpentFuelRegistry.SPENT_FUEL.get());
                helper.assertTrue(data != null, "example " + example + " carries no cooling grade");
                if (data == null) {
                    continue;
                }
                helper.assertTrue(grade.acceptsGrade(data.grade()), "example graded " + data.grade() + " outside its recipe's range");
                helper.assertTrue(ingredients.get(i).test(example), "a recipe refuses its own example " + data.grade());
                for (CoolingGrade other : CoolingGrade.values()) {
                    ItemStack regraded = example.copy();
                    regraded.set(SpentFuelRegistry.SPENT_FUEL.get(), SpentFuelData.undated(other));
                    long takers = ingredients.stream().filter(ingredient -> ingredient.test(regraded)).count();
                    helper.assertTrue(takers == 1, BuiltInRegistries.ITEM.getKey(example.getItem()) + " graded " + other
                            + " matches " + takers + " spent fuel recipes, expected one");
                }
            }
        }
        // Fuel from before cooling existed has no grade and counts as Warm: still exactly one recipe.
        ItemStack legacy = new ItemStack(AllNuclearItems.DEPLETED_MOX_REACTOR_FUEL.get());
        helper.assertTrue(SpentFuel.gradeOf(legacy) == CoolingGrade.WARM, "ungraded spent fuel should count as Warm");
        helper.assertTrue(ingredients.stream().filter(ingredient -> ingredient.test(legacy)).count() == 1,
                "ungraded spent MOX should match exactly one recipe");
        helper.succeed();
    }
}
