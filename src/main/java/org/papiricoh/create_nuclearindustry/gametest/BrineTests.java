package org.papiricoh.create_nuclearindustry.gametest;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import com.simibubi.create.content.processing.recipe.HeatCondition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
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
import org.papiricoh.create_nuclearindustry.fuelcycle.FuelCycleRegistry;
import org.papiricoh.create_nuclearindustry.processing.KineticProcessingBlockEntity;

import java.util.List;
import java.util.Optional;

/** Sea water: brine boiled down from water, spun into lithium and borax, and the adsorbent that pulls uranium out of it. */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class BrineTests {
    private static final BlockPos CENTRIFUGE = new BlockPos(2, 1, 2);

    private static final List<String> CHAIN = List.of(
            "mixing/brine",
            "centrifuging/brine",
            "mixing/amidoxime_adsorbent",
            "mixing/loaded_adsorbent",
            "mixing/loaded_adsorbent_from_brine",
            "acid_leaching/loaded_adsorbent",
            "acid_leaching/low_level_waste",
            "emptying/radioactive_sludge");

    private static Optional<RecipeHolder<?>> recipe(GameTestHelper helper, String path) {
        return helper.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, path));
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void everySeaWaterRecipeIsLoaded(GameTestHelper helper) {
        for (String path : CHAIN) {
            helper.assertTrue(recipe(helper, path).isPresent(), "missing recipe " + path);
        }
        Optional<RecipeHolder<?>> brine = recipe(helper, "mixing/brine");
        helper.assertTrue(brine.isPresent() && brine.get().value() instanceof BasinRecipe basin
                        && basin.getRequiredHeat() == HeatCondition.HEATED,
                "brine is boiled down in a heated basin");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 400)
    public static void centrifugeSplitsBrineIntoSaltsAndWater(GameTestHelper helper) {
        KineticTestHelper.placePoweredMachine(helper, CENTRIFUGE, AllNuclearBlocks.CENTRIFUGE.get(), 64);
        CentrifugeBlockEntity centrifuge = helper.getBlockEntity(CENTRIFUGE);
        helper.runAfterDelay(2, () -> {
            IFluidHandler feed = centrifuge.getFluidHandler(Direction.UP);
            int filled = feed.fill(new FluidStack(AllNuclearFluids.BRINE.get(), 1000), IFluidHandler.FluidAction.EXECUTE);
            helper.assertTrue(filled == 1000, "the centrifuge should take brine, took " + filled);
        });
        helper.succeedWhen(() -> {
            FluidStack water = centrifuge.getProductFluid();
            helper.assertTrue(water.is(Fluids.WATER) && water.getAmount() == 500, "expected 500 mB of water, got " + water.getAmount());
            boolean borax = false;
            for (int slot = KineticProcessingBlockEntity.FIRST_OUTPUT_SLOT;
                 slot < KineticProcessingBlockEntity.FIRST_OUTPUT_SLOT + KineticProcessingBlockEntity.OUTPUT_SLOTS; slot++) {
                ItemStack stack = centrifuge.getItem(slot);
                borax |= stack.is(AllNuclearItems.BORAX_SALT.get()) && stack.getCount() == 2;
            }
            helper.assertTrue(borax, "two borax should settle out of a bucket of brine");
            helper.assertTrue(centrifuge.getInputFluid().isEmpty(), "the brine should be used up");
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void loadedAdsorbentIsLeachedForUranium(GameTestHelper helper) {
        ItemStack loaded = new ItemStack(FuelCycleRegistry.LOADED_ADSORBENT.get());
        net.minecraft.world.item.crafting.RecipeType<Recipe<SingleRecipeInput>> type =
                org.papiricoh.create_nuclearindustry.processing.AllNuclearRecipeTypes.ACID_LEACHING.getType();
        Optional<RecipeHolder<Recipe<SingleRecipeInput>>> leaching = helper.getLevel().getRecipeManager()
                .getRecipeFor(type, new SingleRecipeInput(loaded), helper.getLevel());
        helper.assertTrue(leaching.isPresent(), "acid leaching should take the loaded adsorbent");
        Optional<RecipeHolder<Recipe<SingleRecipeInput>>> emptying = AllRecipeTypes.EMPTYING.find(
                new SingleRecipeInput(new ItemStack(FuelCycleRegistry.RADIOACTIVE_SLUDGE.get())), helper.getLevel());
        helper.assertTrue(emptying.isPresent(), "an item drain should pour out radioactive sludge");
        if (emptying.isPresent() && emptying.get().value() instanceof com.simibubi.create.content.fluids.transfer.EmptyingRecipe recipe) {
            helper.assertTrue(recipe.getResultingFluid().is(AllNuclearFluids.CONTAMINATED_WATER.get()),
                    "radioactive sludge drains into contaminated water");
        }
        helper.succeed();
    }
}
