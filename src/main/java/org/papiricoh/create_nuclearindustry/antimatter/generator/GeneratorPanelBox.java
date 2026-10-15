package org.papiricoh.create_nuclearindustry.antimatter.generator;

import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Where the mode's value box sits: on the bevelled panel in the back half of the generator's top,
 * away from the vial socket over the shaft. Laid out for a generator facing south and turned with it.
 */
final class GeneratorPanelBox extends ValueBoxTransform.Sided {

    GeneratorPanelBox() {
        fromSide(Direction.UP);
    }

    /** On the top face, (8, 16 - 11) = z 5 of a south-facing block, which the panel covers. */
    @Override
    protected Vec3 getSouthLocation() {
        return VecHelper.voxelSpace(8, 11, 15.5);
    }

    @Override
    public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
        return rotateHorizontally(state, super.getLocalOffset(level, pos, state));
    }

    @Override
    protected boolean isSideActive(BlockState state, Direction direction) {
        return direction == Direction.UP;
    }
}
