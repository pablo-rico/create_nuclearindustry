package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.chemistry.electrolyzer.ElectrolyzerBlockEntity;
import org.papiricoh.create_nuclearindustry.processing.KineticProcessingBlockEntity;

@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class ElectrolyzerTests {
    private static final BlockPos ELECTROLYZER = new BlockPos(2, 1, 2);

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 200)
    public static void waterConcentratesIntoHeavyWater(GameTestHelper helper) {
        KineticTestHelper.placePoweredMachine(helper, ELECTROLYZER, AllNuclearBlocks.ELECTROLYZER.get(), 128);
        ElectrolyzerBlockEntity electrolyzer = helper.getBlockEntity(ELECTROLYZER);
        helper.runAfterDelay(2, () -> electrolyzer.getFluidHandler(Direction.UP)
                .fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE));
        helper.succeedWhen(() -> {
            FluidStack product = electrolyzer.getProductFluid();
            helper.assertTrue(product.is(AllNuclearFluids.HEAVY_WATER.get()) && product.getAmount() == 10,
                    "expected 10 mB heavy water, got " + product.getAmount());
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 200)
    public static void carbonateReducesToLithium(GameTestHelper helper) {
        KineticTestHelper.placePoweredMachine(helper, ELECTROLYZER, AllNuclearBlocks.ELECTROLYZER.get(), 128);
        ElectrolyzerBlockEntity electrolyzer = helper.getBlockEntity(ELECTROLYZER);
        helper.runAfterDelay(2, () -> electrolyzer.getItemHandler(Direction.UP)
                .insertItem(0, new ItemStack(AllNuclearItems.LITHIUM_CARBONATE.get()), false));
        helper.succeedWhen(() -> {
            ItemStack out = electrolyzer.getItem(KineticProcessingBlockEntity.FIRST_OUTPUT_SLOT);
            helper.assertTrue(out.is(AllNuclearItems.LITHIUM.get()), "expected lithium, got " + out);
        });
    }
}
