package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.accelerator.AcceleratorRegistry;
import org.papiricoh.create_nuclearindustry.accelerator.api.AcceleratorSupply;
import org.papiricoh.create_nuclearindustry.accelerator.blockentity.AcceleratorControllerBlockEntity;
import org.papiricoh.create_nuclearindustry.accelerator.blockentity.BeamDumpBlockEntity;
import org.papiricoh.create_nuclearindustry.accelerator.blockentity.IonSourceBlockEntity;

/**
 * Builds a straight linac in {@link NuclearGameTests#LONG_ARENA} (11 wide, 20 long): the controller
 * at the north end facing south, the ion source in front of it with its shaft up, then the beamline
 * along z and a beam dump at the south end. Every fourth element is a quadrupole; the first ones are
 * RF cavities. Machines go beside the line on demand.
 */
public final class AcceleratorTestBuilder {
    public static final BlockPos CONTROLLER = new BlockPos(5, 2, 1);
    public static final BlockPos SOURCE = CONTROLLER.south();
    /** First beamline block after the source. */
    public static final BlockPos FIRST = SOURCE.south();
    /** Beamline blocks between the source and the dump. */
    public static final int LENGTH = 15;
    public static final BlockPos DUMP = FIRST.south(LENGTH);
    /** Supply that runs any linac this builder makes, for tests that do not care about the machines. */
    public static final AcceleratorSupply PLENTY = new AcceleratorSupply(100, 1000, 10, 1, 1, 1, 1);

    private AcceleratorTestBuilder() {}

    /** The {@code i}-th beamline block after the source, 0-based. */
    public static BlockPos cell(int i) {
        return FIRST.south(i);
    }

    /** The {@code i}-th beamline block of a linac whose controller stands at {@code controller}. */
    public static BlockPos cell(BlockPos controller, int i) {
        return controller.south(2 + i);
    }

    /** The beam dump at the end of a linac whose controller stands at {@code controller}. */
    public static BlockPos dump(BlockPos controller) {
        return cell(controller, LENGTH);
    }

    /**
     * A linac of {@link #LENGTH} blocks: {@code cavities} RF cavities, quadrupoles every fourth block
     * when {@code focused}, beam pipe for the rest, and a dump at the end.
     */
    public static void linac(GameTestHelper helper, int cavities, boolean focused) {
        linac(BlockPlacer.of(helper), CONTROLLER, cavities, focused);
    }

    /** The same linac running south from {@code controller} of {@code placer}. */
    public static void linac(BlockPlacer helper, BlockPos controller, int cavities, boolean focused) {
        helper.set(controller, AcceleratorRegistry.CONTROLLER.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
        helper.set(controller.south(), AcceleratorRegistry.ION_SOURCE.get().defaultBlockState()
                .setValue(BlockStateProperties.FACING, Direction.UP));
        int placedCavities = 0;
        for (int i = 0; i < LENGTH; i++) {
            Block block;
            if (focused && i % 4 == 0) {
                block = AcceleratorRegistry.QUADRUPOLE.get();
            } else if (placedCavities < cavities) {
                block = AcceleratorRegistry.RF_CAVITY.get();
                placedCavities++;
            } else {
                block = AcceleratorRegistry.BEAM_PIPE.get();
            }
            helper.set(cell(controller, i), along(block, Direction.Axis.Z));
        }
        helper.set(dump(controller), AcceleratorRegistry.BEAM_DUMP.get().defaultBlockState());
    }

    /** A beamline block turned along {@code axis}. */
    public static BlockState along(Block block, Direction.Axis axis) {
        BlockState state = block.defaultBlockState();
        return state.hasProperty(RotatedPillarBlock.AXIS) ? state.setValue(RotatedPillarBlock.AXIS, axis) : state;
    }

    /** A klystron, magnet supply and pump beside the line, shafts facing away from it, no motors. */
    public static void machines(GameTestHelper helper) {
        machines(BlockPlacer.of(helper), CONTROLLER);
    }

    public static void machines(BlockPlacer helper, BlockPos controller) {
        helper.set(cell(controller, 1).east(), AcceleratorRegistry.KLYSTRON.get().defaultBlockState()
                .setValue(BlockStateProperties.FACING, Direction.EAST));
        helper.set(cell(controller, 2).east(), AcceleratorRegistry.MAGNET_POWER_SUPPLY.get().defaultBlockState()
                .setValue(BlockStateProperties.FACING, Direction.EAST));
        helper.set(cell(controller, 3).east(), AcceleratorRegistry.VACUUM_PUMP.get().defaultBlockState()
                .setValue(BlockStateProperties.FACING, Direction.EAST));
    }

    /** Fills the ion source with hydrogen. */
    public static void fillGas(GameTestHelper helper, int millibuckets) {
        IonSourceBlockEntity source = entity(helper, SOURCE, IonSourceBlockEntity.class);
        if (source != null) {
            source.fill(new FluidStack(AllNuclearFluids.HYDROGEN.get(), millibuckets));
        }
    }

    @Nullable
    public static AcceleratorControllerBlockEntity controller(GameTestHelper helper) {
        return entity(helper, CONTROLLER, AcceleratorControllerBlockEntity.class);
    }

    @Nullable
    public static BeamDumpBlockEntity dump(GameTestHelper helper) {
        return entity(helper, DUMP, BeamDumpBlockEntity.class);
    }

    /** Forms the linac, gives it plenty of supply, gas and vacuum, and starts it. */
    @Nullable
    public static AcceleratorControllerBlockEntity startReady(GameTestHelper helper, int cavities) {
        linac(helper, cavities, true);
        fillGas(helper, 4000);
        AcceleratorControllerBlockEntity controller = controller(helper);
        if (controller == null) {
            return null;
        }
        controller.attemptFormation(null);
        controller.setSupplyOverride(PLENTY);
        controller.primeVacuum();
        controller.requestStart();
        return controller;
    }

    /** The block entity at {@code pos} if it is of this type; never throws inside a test callback. */
    @Nullable
    public static <T> T entity(GameTestHelper helper, BlockPos pos, Class<T> type) {
        BlockEntity blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        return type.isInstance(blockEntity) ? type.cast(blockEntity) : null;
    }
}
