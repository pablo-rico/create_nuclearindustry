package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.coolingtower.block.CoolingTowerFluidPortBlock;
import org.papiricoh.create_nuclearindustry.coolingtower.blockentity.CoolingTowerControllerBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.block.ReactorFluidPortMode;

@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class CoolingTowerTests {
    private static final BlockPos SMALL_CENTER = new BlockPos(6, 1, 6);

    private static CoolingTowerTestBuilder smallest(GameTestHelper helper) {
        return CoolingTowerTestBuilder.build(helper, SMALL_CENTER, 4, 12);
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void smallestTowerForms(GameTestHelper helper) {
        CoolingTowerTestBuilder tower = smallest(helper);
        CoolingTowerControllerBlockEntity controller = tower.controller(helper);
        helper.succeedWhen(() -> {
            helper.assertTrue(controller.getStructure().isPresent(), "tower should form: " + controller.getValidationMessage());
            helper.assertTrue(controller.getStructure().get().baseRadius() == 4 && controller.getStructure().get().height() == 12,
                    "tower should measure radius 4, 12 high");
            helper.assertTrue(controller.getCapacity() == 24, "passive capacity is half radius x height, got " + controller.getCapacity());
        });
    }

    @GameTest(template = "gametest/cooling_tower_ponder")
    public static void ponderSchematicForms(GameTestHelper helper) {
        CoolingTowerControllerBlockEntity controller = helper.getBlockEntity(new BlockPos(1, 3, 5));
        helper.succeedWhen(() -> helper.assertTrue(controller.getStructure().isPresent(),
                "Ponder tower should form: " + controller.getValidationMessage()));
    }

    @GameTest(template = NuclearGameTests.TOWER_ARENA, timeoutTicks = 200)
    public static void largerTowerForms(GameTestHelper helper) {
        CoolingTowerTestBuilder tower = CoolingTowerTestBuilder.build(helper, new BlockPos(9, 1, 9), 7, 20);
        CoolingTowerControllerBlockEntity controller = tower.controller(helper);
        helper.succeedWhen(() -> {
            helper.assertTrue(controller.getStructure().isPresent(), "a radius 7, 20 high tower should form: "
                    + controller.getValidationMessage());
            helper.assertTrue(controller.getCapacity() == 70, "capacity 7 x 20 / 2, got " + controller.getCapacity());
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void holeInWallUnforms(GameTestHelper helper) {
        CoolingTowerTestBuilder tower = smallest(helper);
        CoolingTowerControllerBlockEntity controller = tower.controller(helper);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(controller.getStructure().isPresent(), "tower should form first");
            helper.destroyBlock(tower.eastWall(6));
        });
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(controller.getStructure().isEmpty(), "a hole in the wall must unform the tower");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void blockedInteriorIsReported(GameTestHelper helper) {
        CoolingTowerTestBuilder tower = smallest(helper);
        helper.setBlock(SMALL_CENTER.above(5), Blocks.STONE);
        CoolingTowerControllerBlockEntity controller = tower.controller(helper);
        helper.succeedWhen(() -> {
            helper.assertTrue(controller.getStructure().isEmpty(), "a blocked tower must not form");
            helper.assertTrue(controller.getValidationMessage().contains("interior"), "got " + controller.getValidationMessage());
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void offCentreControllerDoesNotForm(GameTestHelper helper) {
        CoolingTowerTestBuilder tower = smallest(helper);
        BlockPos shifted = tower.controllerPos().south();
        helper.setBlock(tower.controllerPos(), AllNuclearBlocks.COOLING_TOWER_CASING.get());
        helper.setBlock(shifted, AllNuclearBlocks.COOLING_TOWER_CONTROLLER.get().defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST));
        CoolingTowerControllerBlockEntity controller = helper.getBlockEntity(shifted);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(controller.getStructure().isEmpty(), "an off-centre controller must not form the tower");
            helper.assertFalse(controller.getValidationMessage().isEmpty(), "it should say why");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void portAboveLowerLayersRejected(GameTestHelper helper) {
        CoolingTowerTestBuilder tower = smallest(helper);
        helper.setBlock(tower.eastWall(5), AllNuclearBlocks.COOLING_TOWER_FLUID_PORT.get().defaultBlockState()
                .setValue(CoolingTowerFluidPortBlock.MODE, ReactorFluidPortMode.INPUT));
        CoolingTowerControllerBlockEntity controller = tower.controller(helper);
        helper.succeedWhen(() -> helper.assertTrue(controller.getValidationMessage().contains("Fluid ports go in"),
                "got " + controller.getValidationMessage()));
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 100)
    public static void steamCondensesAtPassiveRate(GameTestHelper helper) {
        CoolingTowerTestBuilder tower = smallest(helper);
        CoolingTowerControllerBlockEntity controller = tower.controller(helper);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(controller.getStructure().isPresent(), "tower should form");
            int filled = tower.input(helper).getFluidHandler(Direction.DOWN)
                    .fill(new FluidStack(AllNuclearFluids.STEAM.get(), 10000), IFluidHandler.FluidAction.EXECUTE);
            helper.assertTrue(filled == 10000, "input port should take steam, took " + filled);
        });
        helper.runAfterDelay(25, () -> {
            int water = controller.getWaterTank().getFluidAmount();
            helper.assertTrue(water >= 456 && water <= 504, "20 ticks at 24 mB/t should give about 480 mB, got " + water);
            helper.assertTrue(controller.getWaterTank().getFluid().is(net.minecraft.world.level.material.Fluids.WATER),
                    "steam condenses to water");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 100)
    public static void heavySteamBecomesHeavyWater(GameTestHelper helper) {
        CoolingTowerTestBuilder tower = smallest(helper);
        CoolingTowerControllerBlockEntity controller = tower.controller(helper);
        helper.runAfterDelay(5, () -> tower.input(helper).getFluidHandler(Direction.DOWN)
                .fill(new FluidStack(AllNuclearFluids.HEAVY_STEAM.get(), 1000), IFluidHandler.FluidAction.EXECUTE));
        helper.succeedWhen(() -> helper.assertTrue(controller.getWaterTank().getFluid().is(AllNuclearFluids.HEAVY_WATER.get()),
                "heavy steam should condense to heavy water"));
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 100)
    public static void fanBoostsCapacity(GameTestHelper helper) {
        CoolingTowerTestBuilder tower = smallest(helper);
        tower.addEastFan(helper, 256);
        CoolingTowerControllerBlockEntity controller = tower.controller(helper);
        helper.succeedWhen(() -> {
            controller.refreshFans();
            helper.assertTrue(controller.getCapacity() == 30, "a fan at 256 RPM adds 25%, got " + controller.getCapacity());
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 60)
    public static void slowFanGivesNoBoost(GameTestHelper helper) {
        CoolingTowerTestBuilder tower = smallest(helper);
        tower.addEastFan(helper, 8);
        CoolingTowerControllerBlockEntity controller = tower.controller(helper);
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(controller.getStructure().isPresent(), "tower with a fan should form: " + controller.getValidationMessage());
            controller.refreshFans();
            helper.assertTrue(controller.getCapacity() == 24, "a fan under 16 RPM adds nothing, got " + controller.getCapacity());
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void fanFacingInwardIsRejected(GameTestHelper helper) {
        CoolingTowerTestBuilder tower = smallest(helper);
        helper.setBlock(tower.eastWall(1), AllNuclearBlocks.COOLING_TOWER_FAN.get().defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST));
        CoolingTowerControllerBlockEntity controller = tower.controller(helper);
        helper.succeedWhen(() -> helper.assertTrue(controller.getValidationMessage().contains("face out"),
                "got " + controller.getValidationMessage()));
    }
}
