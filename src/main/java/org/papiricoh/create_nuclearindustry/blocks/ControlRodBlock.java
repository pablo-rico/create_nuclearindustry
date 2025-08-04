package org.papiricoh.create_nuclearindustry.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ControlRodBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 16, 15);

    public ControlRodBlock(Properties props) {
        // Not a full cube → avoid culling adjacent blocks
        super(props.noOcclusion());
    }

    // Selection / outline shape (the black highlight)
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    // Optional: make entity collision match the model
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    // Prevent neighbor-face culling by returning an empty occlusion shape
    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }
}
