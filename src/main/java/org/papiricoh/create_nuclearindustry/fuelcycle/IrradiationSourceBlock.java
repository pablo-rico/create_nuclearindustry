package org.papiricoh.create_nuclearindustry.fuelcycle;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A sealed capsule of waste held in an open cage: the catalyst of the irradiation fan treatment.
 * A fan's air current stops at any full block, so this one is mostly open: only the capsule in the
 * middle and four thin corner posts are solid, and air blows past it on every side, whichever way
 * the fan points. It gives off radiation like its item.
 */
public class IrradiationSourceBlock extends Block {
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(5, 1, 5, 11, 15, 11),
            Block.box(0, 0, 0, 2, 16, 2),
            Block.box(14, 0, 0, 16, 16, 2),
            Block.box(0, 0, 14, 2, 16, 16),
            Block.box(14, 0, 14, 16, 16, 16));

    public IrradiationSourceBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }
}
