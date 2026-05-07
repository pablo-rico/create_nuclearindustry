package org.papiricoh.create_nuclearindustry.reactor.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class HeatExchangerBlock extends Block {

    // Plates
    private static final VoxelShape BOTTOM = Block.box(0, 0, 0, 16, 1, 16);
    private static final VoxelShape TOP    = Block.box(0, 15, 0, 16, 16, 16);

    // Pipes (4×4)
    private static final VoxelShape PIPE_NW = Block.box(4, 1, 4, 6, 15, 6);
    private static final VoxelShape PIPE_NE = Block.box(10, 1, 4, 12, 15, 6);
    private static final VoxelShape PIPE_SW = Block.box(4, 1, 10, 6, 15, 12);
    private static final VoxelShape PIPE_SE = Block.box(10, 1, 10, 12, 15, 12);

    // Union of all parts
    private static final VoxelShape SHAPE = Shapes.or(
            BOTTOM, TOP, PIPE_NW, PIPE_NE, PIPE_SW, PIPE_SE
    ).optimize();

    public HeatExchangerBlock(Properties props) {
        // Not a full cube → avoid neighbor face culling issues
        super(props.noOcclusion());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    // Optional: if you ever saw adjacent blocks disappear due to occlusion
    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }
}