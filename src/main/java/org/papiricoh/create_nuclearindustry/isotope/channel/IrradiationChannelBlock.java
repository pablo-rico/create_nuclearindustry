package org.papiricoh.create_nuclearindustry.isotope.channel;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.isotope.IsotopeRegistry;
import org.papiricoh.create_nuclearindustry.multiblock.PartPlacement;

/**
 * The Reactor Irradiation Channel: a sample tube through a fission reactor's wall. It takes the
 * place of a casing block; targets put into its mouth soak up the core's neutrons and come out
 * activated. Right-click with cobalt to load it, with an empty hand to take out what is ready.
 */
public class IrradiationChannelBlock extends BaseEntityBlock implements IBE<IrradiationChannelBlockEntity> {
    public static final MapCodec<IrradiationChannelBlock> CODEC = simpleCodec(IrradiationChannelBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final EnumProperty<ChannelStatus> STATUS = EnumProperty.create("status", ChannelStatus.class);

    public IrradiationChannelBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(STATUS, ChannelStatus.EMPTY));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, STATUS);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, PartPlacement.facingPlayer(context));
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!IrradiationChannelBlockEntity.isTarget(stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            withBlockEntityDo(level, pos, channel -> channel.load(player, hand));
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            withBlockEntityDo(level, pos, channel -> channel.unload(player));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock())) {
            withBlockEntityDo(level, pos, IrradiationChannelBlockEntity::dropContents);
        }
        IBE.onRemove(state, level, pos, newState);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return getBlockEntityOptional(level, pos).map(IrradiationChannelBlockEntity::comparatorSignal).orElse(0);
    }

    @Override
    public Class<IrradiationChannelBlockEntity> getBlockEntityClass() {
        return IrradiationChannelBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends IrradiationChannelBlockEntity> getBlockEntityType() {
        return IsotopeRegistry.IRRADIATION_CHANNEL_ENTITY.get();
    }
}
