package org.papiricoh.create_nuclearindustry.missile.defense.battery;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleMenuProvider;
import org.papiricoh.create_nuclearindustry.network.ServerPayloadGuard;
import org.papiricoh.create_nuclearindustry.integration.sable.ShipAssembly;
import org.papiricoh.create_nuclearindustry.missile.defense.DefenseRegistry;

/**
 * The interceptor battery's master block: the corner of its 2×2×2 footprint with the lowest coordinates, driven by a shaft from
 * below. Its block entity holds the four tubes and does the turning and launching; the other seven blocks are
 * {@link InterceptorBatteryPartBlock}s.
 */
public class InterceptorBatteryBlock extends KineticBlock implements EntityBlock {
    public static final MapCodec<InterceptorBatteryBlock> CODEC = simpleCodec(InterceptorBatteryBlock::new);
    /** The way the battery faced when placed: where its turret rests. */
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public InterceptorBatteryBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Override protected MapCodec<? extends InterceptorBatteryBlock> codec() { return CODEC; }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder.add(FACING));
    }

    @Override public Direction.Axis getRotationAxis(BlockState state) { return Direction.Axis.Y; }

    @Override public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) { return face == Direction.DOWN; }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new InterceptorBatteryEntity(pos, state); }

    @Override protected boolean hasAnalogOutputSignal(BlockState state) { return true; }
    @Override protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof InterceptorBatteryEntity battery ? battery.readyRounds() * 15 / InterceptorBatteryEntity.TUBES : 0;
    }

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown() && !player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()) return InteractionResult.PASS;
        if (player.isShiftKeyDown() && level.getBlockEntity(pos) instanceof InterceptorBatteryEntity battery) {
            if (!level.isClientSide) {
                ItemStack round = battery.takeReady();
                if (!round.isEmpty() && !player.getInventory().add(round)) player.drop(round, false);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof InterceptorBatteryEntity battery && ServerPayloadGuard.check(player, pos))
            player.openMenu(new SimpleMenuProvider((id, inv, who) -> new InterceptorBatteryMenu(id, inv, battery, pos), Component.translatable(getDescriptionId())), b -> b.writeBlockPos(pos));
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof InterceptorBatteryEntity battery && stack.is(DefenseRegistry.INTERCEPTOR.get())) {
            if (level.isClientSide || battery.loadOne(stack)) return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!moving && !ShipAssembly.isMoving() && state.getBlock() != next.getBlock()) BatteryBlocks.remove(level, pos, true);
        super.onRemove(state, level, pos, next, moving);
    }

    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == DefenseRegistry.BATTERY_ENTITY.get() ? (l, p, s, be) -> ((InterceptorBatteryEntity) be).tick() : null;
    }
}
