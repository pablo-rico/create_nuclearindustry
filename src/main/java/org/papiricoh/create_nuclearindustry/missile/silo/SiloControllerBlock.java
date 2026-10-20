package org.papiricoh.create_nuclearindustry.missile.silo;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import org.papiricoh.create_nuclearindustry.missile.item.TargetDesignatorItem;
import org.papiricoh.create_nuclearindustry.network.ServerPayloadGuard;

/** The launch controller: a rack cabinet that faces its operator, driven by a shaft from below. */
public class SiloControllerBlock extends KineticBlock implements IBE<SiloControllerEntity> {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public SiloControllerBlock(Properties properties) { super(properties); registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH)); }
    @Override protected MapCodec<? extends SiloControllerBlock> codec() { return simpleCodec(SiloControllerBlock::new); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { super.createBlockStateDefinition(b); b.add(FACING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext c) { return defaultBlockState().setValue(FACING, c.getHorizontalDirection().getOpposite()); }
    /** Driven from below, through the generator in its plinth; {@link #FACING} is only where its front and its base lie. */
    @Override public Direction.Axis getRotationAxis(BlockState s) { return Direction.Axis.Y; }
    @Override public boolean hasShaftTowards(LevelReader l, BlockPos p, BlockState s, Direction d) { return d == Direction.DOWN; }
    @Override public Class<SiloControllerEntity> getBlockEntityClass() { return SiloControllerEntity.class; }
    @Override public BlockEntityType<? extends SiloControllerEntity> getBlockEntityType() { return SiloRegistry.CONTROLLER_ENTITY.get(); }
    @Override protected InteractionResult useWithoutItem(BlockState s, Level level, BlockPos p, Player player, BlockHitResult hit) {
        if (!level.isClientSide && ServerPayloadGuard.check(player, p) && level.getBlockEntity(p) instanceof SiloControllerEntity controller) {
            controller.sendData();
            player.openMenu(new SimpleMenuProvider((id, inv, who) -> new SiloMenu(id, inv, p), Component.translatable(getDescriptionId())), buf -> buf.writeBlockPos(p));
        }
        return InteractionResult.SUCCESS;
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState s, Level level, BlockPos p, Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof TargetDesignatorItem) {
            if (!level.isClientSide && ServerPayloadGuard.check(player, p) && level.getBlockEntity(p) instanceof SiloControllerEntity c)
                c.configure(player, TargetDesignatorItem.getTarget(stack), TargetDesignatorItem.isAirburst(stack));
            return ItemInteractionResult.SUCCESS;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
}
