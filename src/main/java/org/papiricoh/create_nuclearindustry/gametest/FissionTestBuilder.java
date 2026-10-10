package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.reactor.block.ReactorFluidPortBlock;
import org.papiricoh.create_nuclearindustry.reactor.block.ReactorFluidPortMode;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.pwr.PwrRegistry;

/**
 * Builds the smallest complete fission reactor: a 5x5x5 casing shell with uranium,
 * heat exchanger and control rod columns, plus one coolant input and one steam output port.
 * The {@code origin} overloads build it anywhere in a bigger arena.
 */
public final class FissionTestBuilder {
    public static final BlockPos ORIGIN = new BlockPos(1, 1, 1);
    public static final int SIZE = 5;
    public static final BlockPos CONTROLLER = controllerAt(ORIGIN);
    public static final BlockPos COOLANT_PORT = coolantPortAt(ORIGIN);
    public static final BlockPos STEAM_PORT = steamPortAt(ORIGIN);
    public static final BlockPos WALL_CASING = ORIGIN.offset(2, 2, 0);

    private FissionTestBuilder() {}

    public static void build(GameTestHelper helper) {
        build(helper, true);
    }

    /** With {@code controlRods} false the control column is left empty and open at the top: 0% insertion. */
    public static void build(GameTestHelper helper, boolean controlRods) {
        build(helper, ORIGIN, controlRods);
    }

    /** The same reactor with its lowest corner at {@code origin}. */
    public static void build(GameTestHelper helper, BlockPos origin, boolean controlRods) {
        BlockState casing = AllNuclearBlocks.REACTOR_CASING.get().defaultBlockState();
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                for (int z = 0; z < SIZE; z++) {
                    boolean shell = x == 0 || y == 0 || z == 0 || x == SIZE - 1 || y == SIZE - 1 || z == SIZE - 1;
                    if (shell) {
                        helper.setBlock(origin.offset(x, y, z), casing);
                    }
                }
            }
        }

        for (int y = 1; y < SIZE - 1; y++) {
            helper.setBlock(origin.offset(1, y, 1), AllNuclearBlocks.URANIUM_ROD.get());
            helper.setBlock(origin.offset(3, y, 3), AllNuclearBlocks.URANIUM_ROD.get());
            helper.setBlock(origin.offset(2, y, 2), AllNuclearBlocks.HEAT_EXCHANGER.get());
            if (controlRods) {
                helper.setBlock(origin.offset(1, y, 3), AllNuclearBlocks.CONTROL_ROD.get());
            }
        }
        if (!controlRods) {
            helper.setBlock(origin.offset(1, SIZE - 1, 3), net.minecraft.world.level.block.Blocks.AIR);
        }

        helper.setBlock(coolantPortAt(origin), port(Direction.WEST, ReactorFluidPortMode.INPUT));
        helper.setBlock(steamPortAt(origin), port(Direction.EAST, ReactorFluidPortMode.OUTPUT));
        helper.setBlock(controllerAt(origin), AllNuclearBlocks.REACTOR_CONTROLLER.get());
    }

    public static BlockPos controllerAt(BlockPos origin) {
        return origin.offset(2, SIZE - 1, 2);
    }

    /** The coolant input port, facing west: the cold leg of a pressurized loop. */
    public static BlockPos coolantPortAt(BlockPos origin) {
        return origin.offset(0, 2, 2);
    }

    /** The steam output port, facing east: the hot leg of a pressurized loop. */
    public static BlockPos steamPortAt(BlockPos origin) {
        return origin.offset(SIZE - 1, 2, 2);
    }

    /**
     * Stacks a pressurizer on the lid, over the free interior column (3, 1): {@code heaters} heater
     * ports at the bottom, plain pressurizer blocks up to {@code segments}, then a relief valve.
     * Returns the relief valve's position.
     */
    public static BlockPos pressurizer(GameTestHelper helper, BlockPos origin, int segments, int heaters) {
        BlockPos base = origin.offset(3, SIZE, 1);
        for (int i = 0; i < segments; i++) {
            helper.setBlock(base.above(i), i < heaters ? PwrRegistry.PRESSURIZER_HEATER_PORT.get() : PwrRegistry.PRESSURIZER.get());
        }
        BlockPos valve = base.above(segments);
        helper.setBlock(valve, PwrRegistry.RELIEF_VALVE.get());
        return valve;
    }

    /**
     * Fills a free interior column with {@code block}. The built core uses (1,1) and (3,3) for fuel,
     * (2,2) for the heat exchanger and (1,3) for control rods; (1,2), (2,1), (2,3), (3,1) and (3,2) are free.
     */
    public static void column(GameTestHelper helper, int x, int z, net.minecraft.world.level.block.Block block) {
        for (int y = 1; y < SIZE - 1; y++) {
            helper.setBlock(ORIGIN.offset(x, y, z), block);
        }
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
