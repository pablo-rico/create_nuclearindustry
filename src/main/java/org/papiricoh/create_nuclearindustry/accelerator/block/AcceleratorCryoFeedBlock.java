package org.papiricoh.create_nuclearindustry.accelerator.block;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.papiricoh.create_nuclearindustry.accelerator.AcceleratorRegistry;
import org.papiricoh.create_nuclearindustry.accelerator.blockentity.AcceleratorCryoFeedBlockEntity;
import org.papiricoh.create_nuclearindustry.accelerator.multiblock.AttachmentRole;

/**
 * The accelerator's cryogenic feed, beside the beamline: liquid helium piped in (from a cryogenic
 * plant, a helium well or Liquid Helium Cells) goes to the controller's helium loop for the
 * superconducting dipoles and cavities, and the boiled-off helium comes back out. Its coupling faces
 * {@code FACING}; pipes work on every side.
 */
public class AcceleratorCryoFeedBlock extends DirectionalBlock implements IBE<AcceleratorCryoFeedBlockEntity>, AttachmentRole.Block,
        IWrenchable {
    public static final MapCodec<AcceleratorCryoFeedBlock> CODEC = simpleCodec(AcceleratorCryoFeedBlock::new);

    public AcceleratorCryoFeedBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends DirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /** The coupling faces the player, ready for a pipe. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Override
    public AttachmentRole attachmentRole(BlockState state) {
        return AttachmentRole.CRYO;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        IBE.onRemove(state, level, pos, newState);
    }

    @Override
    public Class<AcceleratorCryoFeedBlockEntity> getBlockEntityClass() {
        return AcceleratorCryoFeedBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends AcceleratorCryoFeedBlockEntity> getBlockEntityType() {
        return AcceleratorRegistry.CRYO_FEED_ENTITY.get();
    }
}
