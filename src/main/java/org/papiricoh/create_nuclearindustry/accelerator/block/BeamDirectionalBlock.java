package org.papiricoh.create_nuclearindustry.accelerator.block;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.papiricoh.create_nuclearindustry.accelerator.multiblock.BeamElementBlock;
import org.papiricoh.create_nuclearindustry.accelerator.multiblock.BeamElementRole;

/**
 * A beamline block with a horizontal {@code FACING} that says where the beam goes next: a bending
 * magnet (its exit), an injection septum (the ring's centre) or an extraction septum (the extraction
 * line). Placed facing the way the player looks; the wrench turns it.
 */
public class BeamDirectionalBlock extends HorizontalDirectionalBlock implements BeamElementBlock, IWrenchable {
    private final BeamElementRole role;
    private final MapCodec<BeamDirectionalBlock> codec;

    public BeamDirectionalBlock(BeamElementRole role, Properties properties) {
        super(properties);
        this.role = role;
        this.codec = simpleCodec(p -> new BeamDirectionalBlock(role, p));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return codec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    public BlockState getRotatedBlockState(BlockState state, Direction targetedFace) {
        return state.setValue(FACING, state.getValue(FACING).getClockWise());
    }

    @Override
    public BeamElementRole beamRole(BlockState state) {
        return role;
    }

    @Override
    public Direction beamExit(BlockState state) {
        return state.getValue(FACING);
    }
}
