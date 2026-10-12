package org.papiricoh.create_nuclearindustry.coolingtower.block;

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
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.AllNuclearEntities;
import org.papiricoh.create_nuclearindustry.coolingtower.blockentity.CoolingTowerPortBlockEntity;
import org.papiricoh.create_nuclearindustry.multiblock.PartPlacement;
import org.papiricoh.create_nuclearindustry.reactor.block.ReactorFluidPortMode;

/** INPUT takes steam into the tower, OUTPUT gives the condensed water. Wrench toggles the mode. */
public class CoolingTowerFluidPortBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final EnumProperty<ReactorFluidPortMode> MODE = EnumProperty.create("mode", ReactorFluidPortMode.class);
    public static final MapCodec<CoolingTowerFluidPortBlock> CODEC = simpleCodec(CoolingTowerFluidPortBlock::new);
    private static final TagKey<Item> WRENCHES = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "tools/wrench"));

    public CoolingTowerFluidPortBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(MODE, ReactorFluidPortMode.INPUT));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, MODE);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, PartPlacement.facingPlayer(context));
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hitResult) {
        if (!stack.is(WRENCHES)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            BlockState updated = player.isShiftKeyDown()
                    ? state.setValue(FACING, Direction.values()[(state.getValue(FACING).ordinal() + 1) % 6])
                    : state.setValue(MODE, state.getValue(MODE).next());
            level.setBlock(pos, updated, UPDATE_ALL);
            if (level.getBlockEntity(pos) instanceof CoolingTowerPortBlockEntity port) {
                port.refreshLink();
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CoolingTowerPortBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == AllNuclearEntities.COOLING_TOWER_FLUID_PORT.get()
                ? (lvl, pos, blockState, blockEntity) -> {
                    if (blockEntity instanceof CoolingTowerPortBlockEntity port) {
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
