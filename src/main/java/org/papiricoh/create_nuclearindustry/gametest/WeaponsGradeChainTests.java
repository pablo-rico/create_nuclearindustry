package org.papiricoh.create_nuclearindustry.gametest;

import com.simibubi.create.content.processing.recipe.HeatCondition;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.enrichment.blockentity.CentrifugeBlockEntity;
import org.papiricoh.create_nuclearindustry.enrichment.item.UraniumItem;
import org.papiricoh.create_nuclearindustry.enrichment.recipe.WeaponsGradeUraniumCoreRecipe;
import org.papiricoh.create_nuclearindustry.explosive.blockentity.NuclearBombBlockEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Uranium goes past 20%: real centrifuges take LEU to HEU and HEU to weapons grade, and each
 * later step accepts exactly what the step before it makes, down to the bomb.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class WeaponsGradeChainTests {
    private static final BlockPos CENTRIFUGE = new BlockPos(2, 1, 2);

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 1200)
    public static void centrifugeTakesLowEnrichedToHighEnriched(GameTestHelper helper) {
        centrifugeStage(helper, AllNuclearFluids.LOW_ENRICHED_URANIUM_HEXAFLUORIDE.get(),
                AllNuclearFluids.HIGH_ENRICHED_URANIUM_HEXAFLUORIDE.get(), AllNuclearFluids.URANIUM_HEXAFLUORIDE.get());
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 1200)
    public static void centrifugeTakesHighEnrichedToWeaponsGrade(GameTestHelper helper) {
        centrifugeStage(helper, AllNuclearFluids.HIGH_ENRICHED_URANIUM_HEXAFLUORIDE.get(),
                AllNuclearFluids.WEAPONS_GRADE_URANIUM_HEXAFLUORIDE.get(), AllNuclearFluids.LOW_ENRICHED_URANIUM_HEXAFLUORIDE.get());
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void weaponsGradeGasEndsInTheBomb(GameTestHelper helper) {
        // Weapons-grade UF6 + water, heated -> 90% uranium dioxide.
        ProcessingRecipe<?, ?> deconversion = processing(helper, "mixing/weapons_uranium_dioxide");
        helper.assertTrue(deconversion.getRequiredHeat() == HeatCondition.HEATED, "deconversion should need a heated basin");
        helper.assertTrue(deconversion.getFluidIngredients().stream().anyMatch(ingredient ->
                        ingredient.test(new FluidStack(AllNuclearFluids.WEAPONS_GRADE_URANIUM_HEXAFLUORIDE.get(), 1000))),
                "deconversion should take the centrifuge's weapons-grade UF6");
        helper.assertTrue(deconversion.getFluidIngredients().stream().anyMatch(ingredient ->
                ingredient.test(new FluidStack(Fluids.WATER, 1000))), "deconversion should take water");
        ItemStack oxide = deconversion.getRollableResultsAsItemStacks().get(0);
        helper.assertTrue(oxide.is(AllNuclearItems.URANIUM_DIOXIDE.get()) && UraniumItem.getEnrichment(oxide) == 90f,
                "deconversion should give 90% dioxide, got " + oxide + " at " + UraniumItem.getEnrichment(oxide));

        // 90% dioxide + bone meal, superheated -> 90% uranium metal. 20% oxide must not pass.
        ProcessingRecipe<?, ?> reduction = processing(helper, "mixing/weapons_uranium_metal");
        helper.assertTrue(reduction.getRequiredHeat() == HeatCondition.SUPERHEATED, "reduction should need a superheated basin");
        helper.assertTrue(reduction.getIngredients().get(0).test(oxide), "reduction should take the 90% dioxide");
        helper.assertTrue(reduction.getIngredients().get(1).test(new ItemStack(Items.BONE_MEAL)), "reduction should take bone meal");
        helper.assertFalse(reduction.getIngredients().get(0).test(
                UraniumItem.withEnrichment(new ItemStack(AllNuclearItems.URANIUM_DIOXIDE.get()), 20f)),
                "20% dioxide must not reduce to weapons-grade metal");
        ItemStack metal = reduction.getRollableResultsAsItemStacks().get(0);
        helper.assertTrue(metal.is(AllNuclearItems.URANIUM.get()) && UraniumItem.getEnrichment(metal) == 90f,
                "reduction should give 90% uranium, got " + metal + " at " + UraniumItem.getEnrichment(metal));

        // The bomb arms with it; 20% uranium is not enough.
        BlockPos bombPos = new BlockPos(2, 1, 2);
        helper.setBlock(bombPos, AllNuclearBlocks.NUCLEAR_BOMB.get());
        NuclearBombBlockEntity bomb = helper.getBlockEntity(bombPos);
        helper.assertTrue(bomb.isValidUranium(metal), "the bomb should accept 90% uranium");
        helper.assertFalse(bomb.isValidUranium(UraniumItem.withEnrichment(new ItemStack(AllNuclearItems.URANIUM.get()), 20f)),
                "the bomb must refuse 20% uranium");

        // Eight of them craft a warhead core.
        Recipe<?> core = helper.getLevel().getRecipeManager()
                .byKey(id("crafting/weapons_grade_uranium_core")).orElseThrow().value();
        List<ItemStack> grid = new ArrayList<>(Collections.nCopies(8, metal.copyWithCount(1)));
        grid.add(ItemStack.EMPTY);
        CraftingInput eight = CraftingInput.of(3, 3, grid);
        helper.assertTrue(((CraftingRecipe) core).matches(eight, helper.getLevel()),
                "eight 90% uranium should craft a weapons-grade core");
        helper.assertTrue(WeaponsGradeUraniumCoreRecipe.REQUIRED_URANIUM == 8, "core recipe size changed");
        helper.succeed();
    }

    private static void centrifugeStage(GameTestHelper helper, Fluid feed, Fluid product, Fluid tails) {
        KineticTestHelper.placePoweredMachine(helper, CENTRIFUGE, AllNuclearBlocks.CENTRIFUGE.get(), 256);
        CentrifugeBlockEntity centrifuge = helper.getBlockEntity(CENTRIFUGE);
        helper.runAfterDelay(2, () -> {
            int filled = centrifuge.getFluidHandler(Direction.UP)
                    .fill(new FluidStack(feed, 1000), IFluidHandler.FluidAction.EXECUTE);
            helper.assertTrue(filled == 1000, "feed tank should accept " + feed + ", accepted " + filled);
        });
        helper.succeedWhen(() -> {
            FluidStack out = centrifuge.getProductFluid();
            FluidStack rest = centrifuge.getTailsFluid();
            helper.assertTrue(out.is(product) && out.getAmount() == 400,
                    "expected 400 mB " + product + ", got " + out.getAmount() + " " + out.getHoverName().getString());
            helper.assertTrue(rest.is(tails) && rest.getAmount() == 600,
                    "expected 600 mB tails " + tails + ", got " + rest.getAmount() + " " + rest.getHoverName().getString());
        });
    }

    private static ProcessingRecipe<?, ?> processing(GameTestHelper helper, String path) {
        return (ProcessingRecipe<?, ?>) helper.getLevel().getRecipeManager().byKey(id(path)).orElseThrow().value();
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, path);
    }
}
