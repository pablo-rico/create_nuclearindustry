package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.reactor.block.ReactorFluidPortMode;
import org.papiricoh.create_nuclearindustry.reactor.block.TurbineFluidPortBlock;
import org.papiricoh.create_nuclearindustry.reactor.block.TurbineRotorBlock;
import org.papiricoh.create_nuclearindustry.turbine.AbstractTurbineControllerBlockEntity;
import org.papiricoh.create_nuclearindustry.turbine.TurbinePortBlockEntity;
import org.papiricoh.create_nuclearindustry.turbine.TurbineStructure;
import org.papiricoh.create_nuclearindustry.turbine.TurbineTier;

@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class TurbineControllerTests {
    private static final BlockPos CONTROLLER = new BlockPos(6, 6, 1);

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void formingHidesTheRotorBlocks(GameTestHelper helper) {
        TurbineStructure s = TurbineTestBuilder.build(helper, TurbineTier.STEAM, CONTROLLER, 3, 3, 5);
        AbstractTurbineControllerBlockEntity turbine = helper.getBlockEntity(CONTROLLER);
        helper.succeedWhen(() -> {
            helper.assertTrue(turbine.getStructure().isPresent(), "turbine should form: " + turbine.getDisplay().message());
            for (int a = 1; a < s.length - 1; a++) {
                helper.assertBlockProperty(s.at(a, 0, 0), TurbineRotorBlock.FORMED, true);
            }
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void breakingTheCasingUnformsAndShowsRotors(GameTestHelper helper) {
        TurbineStructure s = TurbineTestBuilder.build(helper, TurbineTier.STEAM, CONTROLLER, 3, 3, 5);
        AbstractTurbineControllerBlockEntity turbine = helper.getBlockEntity(CONTROLLER);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(turbine.getStructure().isPresent(), "turbine should form first");
            helper.destroyBlock(s.at(2, 0, s.hv));
        });
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(turbine.getStructure().isEmpty(), "a hole in the wall must unform the turbine");
            helper.assertBlockProperty(s.at(2, 0, 0), TurbineRotorBlock.FORMED, false);
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void oneRotorExpandsEightyPerTick(GameTestHelper helper) {
        runAndExpectUse(helper, TurbineTier.STEAM, 3, 3, 3, false, 80);
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void fourRotorsWithTwoPortPairsExpandFull(GameTestHelper helper) {
        runAndExpectUse(helper, TurbineTier.STEAM, 5, 5, 6, true, 320);
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void onePortPairCapsALongTurbine(GameTestHelper helper) {
        runAndExpectUse(helper, TurbineTier.STEAM, 3, 3, 10, false, 320);
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void plasmaRotorsExpandFortyEach(GameTestHelper helper) {
        runAndExpectUse(helper, TurbineTier.PLASMA, 3, 3, 6, false, 160);
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void runningTurbineSpinsAtSixtyFourRpm(GameTestHelper helper) {
        TurbineStructure s = TurbineTestBuilder.build(helper, TurbineTier.STEAM, CONTROLLER, 3, 3, 4);
        AbstractTurbineControllerBlockEntity turbine = helper.getBlockEntity(CONTROLLER);
        helper.runAfterDelay(3, () -> feed(helper, TurbineTier.STEAM, s));
        helper.succeedWhen(() -> {
            helper.assertTrue(Math.abs(turbine.getSpeed()) == 64f, "speed should be 64 RPM, got " + turbine.getSpeed());
            helper.assertTrue(turbine.calculateAddedStressCapacity() > 0, "a running turbine provides stress capacity");
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void oldThreeBlockLineAsksToRebuild(GameTestHelper helper) {
        BlockPos controller = new BlockPos(6, 2, 2);
        helper.setBlock(controller.south(), AllNuclearBlocks.TURBINE_ROTOR.get().defaultBlockState()
                .setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z));
        helper.setBlock(controller.south(2), AllNuclearBlocks.TURBINE_CASING.get());
        helper.setBlock(controller, AllNuclearBlocks.TURBINE_OUTPUT.get().defaultBlockState()
                .setValue(BlockStateProperties.FACING, Direction.NORTH));
        AbstractTurbineControllerBlockEntity turbine = helper.getBlockEntity(controller);
        helper.succeedWhen(() -> {
            helper.assertTrue(turbine.getStructure().isEmpty(), "the old line must not form");
            helper.assertTrue(turbine.getValidationMessage().contains("Rebuild"), "got: " + turbine.getValidationMessage());
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void oldSteamTurbineKeepsItsFluids(GameTestHelper helper) {
        migrate(helper, TurbineTier.STEAM, "steam", AllNuclearFluids.STEAM.get());
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void oldPlasmaTurbineKeepsItsFluids(GameTestHelper helper) {
        migrate(helper, TurbineTier.PLASMA, "plasmaSteam", AllNuclearFluids.PLASMA_STEAM.get());
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void wrenchingAPortRevalidates(GameTestHelper helper) {
        TurbineStructure s = TurbineTestBuilder.build(helper, TurbineTier.STEAM, CONTROLLER, 3, 3, 4);
        AbstractTurbineControllerBlockEntity turbine = helper.getBlockEntity(CONTROLLER);
        BlockPos output = s.at(1, -s.hu, 0);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(turbine.getStructure().isPresent(), "turbine should form first");
            // What the wrench does: a state-only change, then the port refreshes its link.
            helper.setBlock(output, helper.getBlockState(output).setValue(TurbineFluidPortBlock.MODE, ReactorFluidPortMode.INPUT));
            TurbinePortBlockEntity port = helper.getBlockEntity(output);
            port.refreshLink();
        });
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(turbine.getStructure().isEmpty(), "without an output port the turbine must unform");
            helper.assertTrue(turbine.getValidationMessage().startsWith("Missing condensate output"), "got: " + turbine.getValidationMessage());
            helper.succeed();
        });
    }

    private static void runAndExpectUse(GameTestHelper helper, TurbineTier tier, int width, int height, int length,
                                        boolean extraPortPair, int expected) {
        TurbineStructure s = TurbineTestBuilder.build(helper, tier, CONTROLLER, width, height, length);
        if (extraPortPair) {
            TurbineTestBuilder.port(helper, tier, s.at(2, s.hu, 0), Direction.UP, ReactorFluidPortMode.INPUT);
            TurbineTestBuilder.port(helper, tier, s.at(2, -s.hu, 0), Direction.DOWN, ReactorFluidPortMode.OUTPUT);
        }
        AbstractTurbineControllerBlockEntity turbine = helper.getBlockEntity(CONTROLLER);
        helper.runAfterDelay(3, () -> feed(helper, tier, s));
        helper.runAfterDelay(8, () -> {
            helper.assertTrue(turbine.getStructure().isPresent(), "turbine should form: " + turbine.getDisplay().message());
            helper.assertTrue(turbine.getCurrentUse() == expected,
                    "expected " + expected + " mB/t, got " + turbine.getCurrentUse() + " (limit " + turbine.getStructure().get().throughputLimit() + ")");
            helper.succeed();
        });
    }

    private static void feed(GameTestHelper helper, TurbineTier tier, TurbineStructure s) {
        TurbinePortBlockEntity input = helper.getBlockEntity(s.at(1, s.hu, 0));
        var fluid = tier == TurbineTier.STEAM ? AllNuclearFluids.STEAM.get() : AllNuclearFluids.PLASMA_STEAM.get();
        int filled = input.getFluidHandler(Direction.UP).fill(new FluidStack(fluid, 16_000), IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(filled > 0, "the input port should feed the controller");
    }

    private static void migrate(GameTestHelper helper, TurbineTier tier, String steamKey, net.minecraft.world.level.material.Fluid fluid) {
        helper.setBlock(CONTROLLER, TurbineTestBuilder.controller(tier).defaultBlockState().setValue(BlockStateProperties.FACING, Direction.NORTH));
        AbstractTurbineControllerBlockEntity turbine = helper.getBlockEntity(CONTROLLER);
        var registries = helper.getLevel().registryAccess();
        FluidTank steam = new FluidTank(tier.baseTank);
        steam.fill(new FluidStack(fluid, 5_000), IFluidHandler.FluidAction.EXECUTE);
        FluidTank condensate = new FluidTank(tier.baseTank);
        condensate.fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER, 3_000), IFluidHandler.FluidAction.EXECUTE);
        CompoundTag legacy = new CompoundTag();
        legacy.put(steamKey, steam.writeToNBT(registries, new CompoundTag()));
        legacy.put("condensate", condensate.writeToNBT(registries, new CompoundTag()));
        legacy.putBoolean("formed", true);
        turbine.loadCustomOnly(legacy, registries);
        helper.assertTrue(turbine.getSteamTank().getFluidAmount() == 5_000, "working fluid kept, got " + turbine.getSteamTank().getFluidAmount());
        helper.assertTrue(turbine.getCondensateTank().getFluidAmount() == 3_000, "condensate kept");
        helper.assertTrue(turbine.saveWithoutMetadata(registries).getInt("TurbineVersion") == 2, "saved in the new format");
        helper.succeed();
    }
}
