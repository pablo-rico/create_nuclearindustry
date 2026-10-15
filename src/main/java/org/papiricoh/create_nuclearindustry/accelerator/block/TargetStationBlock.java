package org.papiricoh.create_nuclearindustry.accelerator.block;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.papiricoh.create_nuclearindustry.accelerator.AcceleratorRegistry;
import org.papiricoh.create_nuclearindustry.accelerator.blockentity.TargetStationBlockEntity;
import org.papiricoh.create_nuclearindustry.accelerator.multiblock.BeamElementBlock;
import org.papiricoh.create_nuclearindustry.accelerator.multiblock.BeamElementRole;

/**
 * The Target Station at the end of a beamline. Its {@code FACING} face is the beam window, turned to
 * the incoming beam (towards the player who places it at the end of the line); the antiproton
 * collector sits on the opposite face. It takes the beam from any side, like the beam dump.
 */
public class TargetStationBlock extends HorizontalDirectionalBlock implements IBE<TargetStationBlockEntity>, BeamElementBlock,
        IWrenchable {
    public static final MapCodec<TargetStationBlock> CODEC = simpleCodec(TargetStationBlock::new);

    public TargetStationBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState getRotatedBlockState(BlockState state, Direction targetedFace) {
        return state.setValue(FACING, state.getValue(FACING).getClockWise());
    }

    @Override
    public BeamElementRole beamRole(BlockState state) {
        return BeamElementRole.TARGET;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        IBE.onRemove(state, level, pos, newState);
    }

    @Override
    public Class<TargetStationBlockEntity> getBlockEntityClass() {
        return TargetStationBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends TargetStationBlockEntity> getBlockEntityType() {
        return AcceleratorRegistry.TARGET_STATION_ENTITY.get();
    }
}
