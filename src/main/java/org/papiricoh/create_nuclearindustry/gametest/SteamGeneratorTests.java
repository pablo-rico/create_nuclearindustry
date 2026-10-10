package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.controlroom.MonitorReading;
import org.papiricoh.create_nuclearindustry.controlroom.TargetKind;
import org.papiricoh.create_nuclearindustry.fluids.NuclearFluidHelper;
import org.papiricoh.create_nuclearindustry.reactor.pwr.PwrRegistry;
import org.papiricoh.create_nuclearindustry.reactor.pwr.steamgen.SteamGeneratorControllerBlockEntity;

/** The steam generator: forming, boiling feedwater with hot primary coolant, and drying out. */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class SteamGeneratorTests {
    private static final BlockPos CENTER = new BlockPos(6, 1, 6);

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void formsWithATubeColumnAndEveryPort(GameTestHelper helper) {
        SteamGeneratorTestBuilder.build(helper, CENTER, 6);
        SteamGeneratorControllerBlockEntity controller = helper.getBlockEntity(SteamGeneratorTestBuilder.controller(CENTER));
        helper.succeedWhen(() -> {
            helper.assertTrue(controller.getStructure().isPresent(), "the steam generator should form");
            helper.assertTrue(controller.getStructure().get().tubes == 4, "a 6-high generator holds 4 tube bundles");
            MonitorReading reading = controller.plantSample();
            helper.assertTrue(reading.kind() == TargetKind.STEAM_GENERATOR && reading.flag(MonitorReading.S_FORMED),
                    "monitors read it as a formed steam generator");
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void needsAPortOfEachRole(GameTestHelper helper) {
        SteamGeneratorTestBuilder.build(helper, CENTER, 4);
        helper.setBlock(SteamGeneratorTestBuilder.steamOut(CENTER, 4), PwrRegistry.STEAM_GENERATOR_CASING.get());
        SteamGeneratorControllerBlockEntity controller = helper.getBlockEntity(SteamGeneratorTestBuilder.controller(CENTER));
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(controller.getStructure().isEmpty(), "without a steam outlet it must not form");
            SteamGeneratorTestBuilder.port(helper, SteamGeneratorTestBuilder.steamOut(CENTER, 4),
                    org.papiricoh.create_nuclearindustry.reactor.pwr.steamgen.SteamGeneratorPortRole.STEAM_OUT);
        });
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(controller.getStructure().isPresent(), "adding the port forms it");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 200)
    public static void boilsFeedwaterWithHotPrimary(GameTestHelper helper) {
        SteamGeneratorTestBuilder.build(helper, CENTER, 4);
        SteamGeneratorControllerBlockEntity controller = helper.getBlockEntity(SteamGeneratorTestBuilder.controller(CENTER));
        int[] filled = new int[2];
        helper.runAfterDelay(5, () -> {
            filled[0] = fill(helper, SteamGeneratorTestBuilder.primaryIn(CENTER), new FluidStack(AllNuclearFluids.HOT_PRIMARY_COOLANT.get(), 8_000));
            filled[1] = fill(helper, SteamGeneratorTestBuilder.feedIn(CENTER), new FluidStack(Fluids.WATER, 16_000));
        });
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(filled[0] == 8_000 && filled[1] == 16_000, "the ports take hot primary and feedwater, got "
                    + filled[0] + " and " + filled[1]);
            helper.assertTrue(controller.getTotalSteam() > 100, "it makes steam, made " + controller.getTotalSteam());
            helper.assertTrue(controller.getTotalCooled() > controller.getTotalSteam(),
                    "a mB of primary carries less heat than a mB of steam takes");
            double heatOut = controller.getTotalSteam() * 0.225;
            double heatIn = controller.getTotalCooled() * 0.12;
            helper.assertTrue(Math.abs(heatIn - heatOut) <= 0.5, "the heat balances: " + heatIn + " in, " + heatOut + " out");
            IFluidHandler steam = handler(helper, SteamGeneratorTestBuilder.steamOut(CENTER, 4));
            IFluidHandler cooled = handler(helper, SteamGeneratorTestBuilder.primaryOut(CENTER));
            helper.assertTrue(steam != null && NuclearFluidHelper.isSteam(steam.drain(100, IFluidHandler.FluidAction.SIMULATE)),
                    "steam comes out of the steam port");
            helper.assertTrue(cooled != null && NuclearFluidHelper.isPrimaryCoolant(cooled.drain(100, IFluidHandler.FluidAction.SIMULATE)),
                    "cooled primary comes out of the primary outlet");
            helper.assertTrue(controller.getPrimaryDrop() > 0, "the primary cools crossing the tubes");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 300)
    public static void driesOutWithoutFeedwater(GameTestHelper helper) {
        SteamGeneratorTestBuilder.build(helper, CENTER, 4);
        SteamGeneratorControllerBlockEntity controller = helper.getBlockEntity(SteamGeneratorTestBuilder.controller(CENTER));
        long[] cooled = new long[1];
        helper.runAfterDelay(5, () -> {
            controller.getHotTank().fill(new FluidStack(AllNuclearFluids.HOT_PRIMARY_COOLANT.get(), 8_000), IFluidHandler.FluidAction.EXECUTE);
            controller.getFeedTank().fill(new FluidStack(Fluids.WATER, 300), IFluidHandler.FluidAction.EXECUTE);
        });
        helper.runAfterDelay(150, () -> {
            helper.assertTrue(controller.getTotalSteam() <= 300, "it cannot make more steam than its water");
            helper.assertTrue(controller.getTotalSteam() > 0, "it boiled what water it had");
            cooled[0] = controller.getTotalCooled();
        });
        helper.runAfterDelay(200, () -> {
            helper.assertTrue(controller.getTotalCooled() == cooled[0], "dry tubes stop cooling the primary");
            helper.assertTrue(controller.getHotTank().getFluidAmount() > 0, "hot primary is left in the tubes");
            helper.assertTrue(controller.isDry(), "it reports itself dry");
            helper.succeed();
        });
    }

    private static int fill(GameTestHelper helper, BlockPos port, FluidStack stack) {
        IFluidHandler handler = handler(helper, port);
        return handler == null ? -1 : handler.fill(stack, IFluidHandler.FluidAction.EXECUTE);
    }

    private static IFluidHandler handler(GameTestHelper helper, BlockPos port) {
        return helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(port), Direction.UP);
    }
}
