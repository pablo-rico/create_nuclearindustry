package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.reactor.block.ReactorFluidPortBlock;
import org.papiricoh.create_nuclearindustry.reactor.block.ReactorFluidPortMode;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorBlockEntity;

/**
 * Builds the smallest complete fission reactor: a 5x5x5 casing shell with uranium,
 * heat exchanger and control rod columns, plus one coolant input and one steam output port.
 */
public final class FissionTestBuilder {
    public static final BlockPos ORIGIN = new BlockPos(1, 1, 1);
    public static final int SIZE = 5;
    public static final BlockPos CONTROLLER = ORIGIN.offset(2, SIZE - 1, 2);
    public static final BlockPos COOLANT_PORT = ORIGIN.offset(0, 2, 2);
    public static final BlockPos STEAM_PORT = ORIGIN.offset(SIZE - 1, 2, 2);
    public static final BlockPos WALL_CASING = ORIGIN.offset(2, 2, 0);

    private FissionTestBuilder() {}

    public static void build(GameTestHelper helper) {
        build(helper, true);
    }

    /** With {@code controlRods} false the control column is left empty and open at the top: 0% insertion. */
    public static void build(GameTestHelper helper, boolean controlRods) {
        BlockState casing = AllNuclearBlocks.REACTOR_CASING.get().defaultBlockState();
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                for (int z = 0; z < SIZE; z++) {
                    boolean shell = x == 0 || y == 0 || z == 0 || x == SIZE - 1 || y == SIZE - 1 || z == SIZE - 1;
                    if (shell) {
                        helper.setBlock(ORIGIN.offset(x, y, z), casing);
                    }
                }
            }
        }

        for (int y = 1; y < SIZE - 1; y++) {
            helper.setBlock(ORIGIN.offset(1, y, 1), AllNuclearBlocks.URANIUM_ROD.get());
            helper.setBlock(ORIGIN.offset(3, y, 3), AllNuclearBlocks.URANIUM_ROD.get());
            helper.setBlock(ORIGIN.offset(2, y, 2), AllNuclearBlocks.HEAT_EXCHANGER.get());
            if (controlRods) {
                helper.setBlock(ORIGIN.offset(1, y, 3), AllNuclearBlocks.CONTROL_ROD.get());
            }
        }
        if (!controlRods) {
            helper.setBlock(ORIGIN.offset(1, SIZE - 1, 3), net.minecraft.world.level.block.Blocks.AIR);
        }

        helper.setBlock(COOLANT_PORT, port(Direction.WEST, ReactorFluidPortMode.INPUT));
        helper.setBlock(STEAM_PORT, port(Direction.EAST, ReactorFluidPortMode.OUTPUT));
        helper.setBlock(CONTROLLER, AllNuclearBlocks.REACTOR_CONTROLLER.get());
    }

    public static ReactorBlockEntity controller(GameTestHelper helper) {
        return helper.getBlockEntity(CONTROLLER);
    }

    private static BlockState port(Direction facing, ReactorFluidPortMode mode) {
        return AllNuclearBlocks.REACTOR_FLUID_PORT.get().defaultBlockState()
                .setValue(ReactorFluidPortBlock.FACING, facing)
                .setValue(ReactorFluidPortBlock.MODE, mode);
    }
}
