package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorFluidPortBlockEntity;

@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class FissionStructureTests {

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void completeReactorForms(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        helper.succeedWhen(() -> helper.assertTrue(FissionTestBuilder.controller(helper).isFormed(),
                "reactor should form"));
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void missingCasingDoesNotForm(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        helper.setBlock(FissionTestBuilder.WALL_CASING, Blocks.AIR);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        reactor.requestStructureRevalidation(null);
        helper.runAfterDelay(5, () -> {
            helper.assertFalse(reactor.isFormed(), "reactor with a hole in its shell must not form");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void fluidPortFeedsController(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        helper.runAfterDelay(5, () -> {
            ReactorFluidPortBlockEntity port = helper.getBlockEntity(FissionTestBuilder.COOLANT_PORT);
            IFluidHandler handler = port.getFluidHandler(Direction.WEST);
            helper.assertTrue(handler != null, "input port exposes a handler on its facing side");
            int filled = handler.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
            helper.assertTrue(filled == 1000, "port should pass water to the controller, filled " + filled);
            helper.assertTrue(FissionTestBuilder.controller(helper).getCoolantAmount() == 1000,
                    "controller coolant tank should hold the water");
            helper.succeed();
        });
    }

    // Non-player block removal is not detected yet; the shared multiblock change handler fixes it.
    @GameTest(template = NuclearGameTests.SMALL_ARENA, required = false)
    public static void removingShellBlockUnforms(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(reactor.isFormed(), "reactor should form before the shell is broken");
            helper.destroyBlock(FissionTestBuilder.WALL_CASING);
        });
        helper.runAfterDelay(10, () -> {
            helper.assertFalse(reactor.isFormed(), "breaking the shell must unform the reactor");
            helper.succeed();
        });
    }
}
