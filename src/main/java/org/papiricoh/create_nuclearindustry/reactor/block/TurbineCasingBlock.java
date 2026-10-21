package org.papiricoh.create_nuclearindustry.reactor.block;

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
import org.papiricoh.create_nuclearindustry.turbine.block.TurbineFormedLook;

/**
 * Turbine wall. In a formed turbine it becomes thin plates on its outer faces, or hides behind
 * the controller's bezel in the front slice ({@link TurbineFormedLook}).
 */
public class TurbineCasingBlock extends Block {
    public TurbineCasingBlock(Properties properties) {
        super(properties);
        registerDefaultState(TurbineFormedLook.withoutPanels(defaultBlockState()).setValue(TurbineFormedLook.HIDDEN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TurbineFormedLook.HIDDEN);
        TurbineFormedLook.addPanels(builder);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return state.getValue(TurbineFormedLook.HIDDEN) ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(TurbineFormedLook.HIDDEN) ? Shapes.empty() : super.getOcclusionShape(state, level, pos);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return TurbineFormedLook.isShaped(state);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        TurbineFormedLook.releaseIfOrphaned(state, level, pos);
    }
}
