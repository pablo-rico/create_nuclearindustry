package org.papiricoh.create_nuclearindustry.reactor.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.AllNuclearEntities;
import org.papiricoh.create_nuclearindustry.multiblock.PartPlacement;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorRedstonePortBlockEntity;

/**
 * Reactor shell part that puts a reading of the reactor out as redstone on its facing side.
 * The wrench cycles what it reads; sneaking with the wrench turns its output side.
 */
public class ReactorRedstonePortBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final IntegerProperty POWER = BlockStateProperties.POWER;
    public static final EnumProperty<RedstonePortMode> MODE = EnumProperty.create("mode", RedstonePortMode.class);
    public static final MapCodec<ReactorRedstonePortBlock> CODEC = simpleCodec(ReactorRedstonePortBlock::new);
    private static final TagKey<Item> WRENCHES = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "tools/wrench"));

    public ReactorRedstonePortBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(POWER, 0)
                .setValue(MODE, RedstonePortMode.TEMPERATURE));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWER, MODE);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // The panel faces the player placing it, who stands outside the reactor.
        return defaultBlockState().setValue(FACING, PartPlacement.facingPlayer(context));
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hitResult) {
        if (!stack.is(WRENCHES)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            if (player.isShiftKeyDown()) {
                level.setBlock(pos, state.setValue(FACING, nextDirection(state.getValue(FACING))), UPDATE_ALL);
            } else {
                RedstonePortMode mode = state.getValue(MODE).next();
                level.setBlock(pos, state.setValue(MODE, mode), UPDATE_ALL);
                player.displayClientMessage(Component.translatable("create_nuclearindustry.redstone_port.mode",
                        Component.translatable(modeKey(mode))), true);
            }
            if (level.getBlockEntity(pos) instanceof ReactorRedstonePortBlockEntity port) {
                port.refreshSignal();
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    public static String modeKey(RedstonePortMode mode) {
        return "create_nuclearindustry.redstone_port.mode." + mode.getSerializedName();
    }

    private static Direction nextDirection(Direction direction) {
        return switch (direction) {
            case DOWN -> Direction.UP;
            case UP -> Direction.NORTH;
            case NORTH -> Direction.EAST;
            case EAST -> Direction.SOUTH;
            case SOUTH -> Direction.WEST;
            case WEST -> Direction.DOWN;
        };
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ReactorRedstonePortBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == AllNuclearEntities.REACTOR_REDSTONE_PORT.get()
                ? (lvl, pos, blockState, blockEntity) -> {
                    if (blockEntity instanceof ReactorRedstonePortBlockEntity port) {
                        port.tick();
                    }
                }
                : null;
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    /** {@code direction} is the side of the neighbour being powered, so the output side is the facing. */
    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return direction.getOpposite() == state.getValue(FACING) ? state.getValue(POWER) : 0;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
