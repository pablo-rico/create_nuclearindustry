package org.papiricoh.create_nuclearindustry.accelerator.block;

import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.IRotate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A kinetic accelerator machine whose shaft goes in at its {@code FACING} face. Placed facing a
 * neighbouring shaft that points at it, or else towards the player (sneak to ignore the shaft).
 */
public abstract class ShaftFacingBlock extends DirectionalKineticBlock {

    protected ShaftFacingBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction shaft = null;
        for (Direction side : Direction.values()) {
            BlockPos neighbourPos = context.getClickedPos().relative(side);
            BlockState neighbour = context.getLevel().getBlockState(neighbourPos);
            if (neighbour.getBlock() instanceof IRotate rotate
                    && rotate.hasShaftTowards(context.getLevel(), neighbourPos, neighbour, side.getOpposite())) {
                if (shaft != null) {
                    shaft = null;
                    break;
                }
                shaft = side;
            }
        }
        Direction facing = shaft != null && (context.getPlayer() == null || !context.getPlayer().isShiftKeyDown())
                ? shaft
                : defaultFacing(context);
        return defaultBlockState().setValue(FACING, facing);
    }

    /** Facing without a shaft next to it. */
    protected Direction defaultFacing(BlockPlaceContext context) {
        return context.getNearestLookingDirection().getOpposite();
    }

    @Override
    public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
        return face == state.getValue(FACING);
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }
}
