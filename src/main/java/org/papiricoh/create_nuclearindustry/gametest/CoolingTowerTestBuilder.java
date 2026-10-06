package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.coolingtower.CoolingTowerGeometry;
import org.papiricoh.create_nuclearindustry.coolingtower.block.CoolingTowerFanBlock;
import org.papiricoh.create_nuclearindustry.coolingtower.block.CoolingTowerFluidPortBlock;
import org.papiricoh.create_nuclearindustry.coolingtower.blockentity.CoolingTowerControllerBlockEntity;
import org.papiricoh.create_nuclearindustry.coolingtower.blockentity.CoolingTowerPortBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.block.ReactorFluidPortMode;

/**
 * Builds a cooling tower from {@link CoolingTowerGeometry} around a floor centre: controller on
 * the west of the bottom wall layer facing west, a steam input in the floor on the south edge
 * and a water output in the floor on the north edge.
 */
final class CoolingTowerTestBuilder {
    final BlockPos center;
    final CoolingTowerGeometry geometry;

    private CoolingTowerTestBuilder(BlockPos center, CoolingTowerGeometry geometry) {
        this.center = center;
        this.geometry = geometry;
    }

    static CoolingTowerTestBuilder build(GameTestHelper helper, BlockPos center, int baseRadius, int height) {
        CoolingTowerTestBuilder tower = new CoolingTowerTestBuilder(center, CoolingTowerGeometry.of(baseRadius, height));
        BlockState casing = AllNuclearBlocks.COOLING_TOWER_CASING.get().defaultBlockState();
        for (int y = 0; y < height; y++) {
            for (int dx = -baseRadius; dx <= baseRadius; dx++) {
                for (int dz = -baseRadius; dz <= baseRadius; dz++) {
                    CoolingTowerGeometry.Cell cell = tower.geometry.classify(dx, y, dz);
                    if (cell == CoolingTowerGeometry.Cell.FLOOR || cell == CoolingTowerGeometry.Cell.WALL) {
                        helper.setBlock(center.offset(dx, y, dz), casing);
                    }
                }
            }
        }
        helper.setBlock(tower.controllerPos(), AllNuclearBlocks.COOLING_TOWER_CONTROLLER.get().defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST));
        helper.setBlock(tower.inputPos(), port(ReactorFluidPortMode.INPUT));
        helper.setBlock(tower.outputPos(), port(ReactorFluidPortMode.OUTPUT));
        return tower;
    }

    BlockPos controllerPos() {
        return center.offset(-geometry.radius(1), 1, 0);
    }

    BlockPos inputPos() {
        return center.offset(0, 0, geometry.baseRadius());
    }

    BlockPos outputPos() {
        return center.offset(0, 0, -geometry.baseRadius());
    }

    /** A wall cell of layer {@code y} on the east axis. */
    BlockPos eastWall(int y) {
        return center.offset(geometry.radius(y), y, 0);
    }

    CoolingTowerControllerBlockEntity controller(GameTestHelper helper) {
        return helper.getBlockEntity(controllerPos());
    }

    CoolingTowerPortBlockEntity input(GameTestHelper helper) {
        return helper.getBlockEntity(inputPos());
    }

    /** Puts a fan in the east wall of layer 1, facing out, driven by a creative motor at {@code rpm}. */
    BlockPos addEastFan(GameTestHelper helper, int rpm) {
        BlockPos fan = eastWall(1);
        helper.setBlock(fan, AllNuclearBlocks.COOLING_TOWER_FAN.get().defaultBlockState()
                .setValue(CoolingTowerFanBlock.FACING, Direction.EAST));
        BlockPos motorPos = fan.east();
        Block motor = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", "creative_motor"));
        helper.setBlock(motorPos, motor.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.WEST));
        CreativeMotorBlockEntity motorEntity = helper.getBlockEntity(motorPos);
        motorEntity.generatedSpeed.setValue(rpm);
        return fan;
    }

    void clearInside(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, Blocks.AIR);
    }

    private static BlockState port(ReactorFluidPortMode mode) {
        return AllNuclearBlocks.COOLING_TOWER_FLUID_PORT.get().defaultBlockState()
                .setValue(CoolingTowerFluidPortBlock.FACING, Direction.DOWN)
                .setValue(CoolingTowerFluidPortBlock.MODE, mode);
    }
}
