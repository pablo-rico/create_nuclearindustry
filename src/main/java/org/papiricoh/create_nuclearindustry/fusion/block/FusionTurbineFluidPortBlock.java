package org.papiricoh.create_nuclearindustry.fusion.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.AllNuclearEntities;
import org.papiricoh.create_nuclearindustry.fusion.blockentity.FusionTurbineFluidPortBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.block.ReactorFluidPortMode;

public class FusionTurbineFluidPortBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final EnumProperty<ReactorFluidPortMode> MODE = EnumProperty.create("mode", ReactorFluidPortMode.class);
    public static final MapCodec<FusionTurbineFluidPortBlock> CODEC = simpleCodec(FusionTurbineFluidPortBlock::new);
    private static final TagKey<Item> WRENCHES = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "tools/wrench"));

    public FusionTurbineFluidPortBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(MODE, ReactorFluidPortMode.INPUT));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING, MODE);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(FACING, context.getClickedFace())
                .setValue(MODE, ReactorFluidPortMode.INPUT);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hitResult) {
        if (!stack.is(WRENCHES)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (!level.isClientSide) {
            BlockState updated = player.isShiftKeyDown()
                    ? state.setValue(FACING, nextDirection(state.getValue(FACING)))
                    : state.setValue(MODE, state.getValue(MODE).next());
            level.setBlock(pos, updated, UPDATE_ALL);
            if (level.getBlockEntity(pos) instanceof FusionTurbineFluidPortBlockEntity port) {
                port.refreshLink();
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
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
        return new FusionTurbineFluidPortBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == AllNuclearEntities.FUSION_TURBINE_FLUID_PORT.get()
                ? (lvl, pos, blockState, blockEntity) -> {
                    if (blockEntity instanceof FusionTurbineFluidPortBlockEntity port) {
                        port.tick();
                    }
                }
                : null;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
