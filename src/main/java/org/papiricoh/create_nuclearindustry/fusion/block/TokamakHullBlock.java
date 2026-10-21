package org.papiricoh.create_nuclearindustry.fusion.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.papiricoh.create_nuclearindustry.fusion.multiblock.TokamakFormedLook;

/**
 * A plain tokamak shell block (coil, blanket, vessel). While its tokamak is formed it hides under
 * the skin the controller draws, but keeps its full collision box.
 */
public class TokamakHullBlock extends Block {
    public TokamakHullBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(TokamakFormedLook.HIDDEN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TokamakFormedLook.HIDDEN);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return state.getValue(TokamakFormedLook.HIDDEN) ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(TokamakFormedLook.HIDDEN) ? Shapes.empty() : super.getOcclusionShape(state, level, pos);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return TokamakFormedLook.isShaped(state);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        TokamakFormedLook.releaseIfOrphaned(state, level, pos);
    }
}
