package org.papiricoh.create_nuclearindustry.accelerator.interlock;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.papiricoh.create_nuclearindustry.accelerator.AcceleratorRegistry;
import org.papiricoh.create_nuclearindustry.accelerator.multiblock.AttachmentRole;

/**
 * The Beam Interlock: a key panel beside the beamline, by the door into the vault. Right-click to
 * search and secure the area (the lamp turns green), sneak-right-click to open it again. While any
 * panel of an accelerator is open its beam cannot start, and a running beam trips. {@code SECURED}
 * is the panel's state: the block entity only adds what the beam is doing.
 */
public class BeamInterlockBlock extends HorizontalDirectionalBlock implements IBE<BeamInterlockBlockEntity>, AttachmentRole.Block,
        IWrenchable {
    public static final BooleanProperty SECURED = BooleanProperty.create("secured");
    public static final MapCodec<BeamInterlockBlock> CODEC = simpleCodec(BeamInterlockBlock::new);

    public BeamInterlockBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(SECURED, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, SECURED);
    }

    /** The panel faces the player who places it. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState getRotatedBlockState(BlockState state, Direction targetedFace) {
        return state.setValue(FACING, state.getValue(FACING).getClockWise());
    }

    @Override
    public AttachmentRole attachmentRole(BlockState state) {
        return AttachmentRole.INTERLOCK;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof BeamInterlockBlockEntity interlock) {
            if (player.isShiftKeyDown()) {
                interlock.open(player);
            } else {
                interlock.secure(player);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    /**
     * Secured panels give a redstone signal out of their back only, for a warning light behind them:
     * on every side it would open the very door the panel guards, which breaks the search.
     */
    @Override
    protected int getSignal(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(SECURED) && direction == state.getValue(FACING) ? 15 : 0;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        IBE.onRemove(state, level, pos, newState);
    }

    @Override
    public Class<BeamInterlockBlockEntity> getBlockEntityClass() {
        return BeamInterlockBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends BeamInterlockBlockEntity> getBlockEntityType() {
        return AcceleratorRegistry.BEAM_INTERLOCK_ENTITY.get();
    }
}
