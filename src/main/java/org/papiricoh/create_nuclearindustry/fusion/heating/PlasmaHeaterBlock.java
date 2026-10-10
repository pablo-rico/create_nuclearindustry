package org.papiricoh.create_nuclearindustry.fusion.heating;

import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.create_nuclearindustry.fusion.multiblock.TokamakShellBlock;
import org.papiricoh.create_nuclearindustry.fusion.multiblock.TokamakShellRole;

/**
 * A heater in the tokamak shell. {@code FACING} is its outer face, where the drive shaft goes in;
 * the opposite face looks into the plasma chamber. Placed facing the player, or towards a
 * neighbouring shaft that points at it.
 */
public abstract class PlasmaHeaterBlock<T extends PlasmaHeaterBlockEntity> extends DirectionalKineticBlock
        implements IBE<T>, TokamakShellBlock {

    protected PlasmaHeaterBlock(Properties properties) {
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
                : context.getNearestLookingDirection().getOpposite();
        return defaultBlockState().setValue(FACING, facing);
    }

    @Override
    public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
        return face == state.getValue(FACING);
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    public abstract TokamakShellRole tokamakRole(BlockState state);
}
