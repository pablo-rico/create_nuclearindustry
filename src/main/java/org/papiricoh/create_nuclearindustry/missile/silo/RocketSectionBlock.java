package org.papiricoh.create_nuclearindustry.missile.silo;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.papiricoh.create_nuclearindustry.network.ServerPayloadGuard;

public class RocketSectionBlock extends BaseEntityBlock {
    public final RocketComposition.Section section;
    public RocketSectionBlock(Properties properties, RocketComposition.Section section) { super(properties); this.section = section; }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return simpleCodec(p -> new RocketSectionBlock(p, section)); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new RocketSectionEntity(pos, state); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        if (ServerPayloadGuard.check(player, pos) && level.getBlockEntity(pos) instanceof RocketSectionEntity part) {
            if (player.isShiftKeyDown()) part.unload(player);
            else part.insert(player, hand);
            return ItemInteractionResult.SUCCESS;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && ServerPayloadGuard.check(player, pos) && level.getBlockEntity(pos) instanceof RocketSectionEntity part) {
            if (player.isShiftKeyDown()) part.unload(player);
            else player.displayClientMessage(part.description(), true);
        }
        return InteractionResult.SUCCESS;
    }
    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock()) && level.getBlockEntity(pos) instanceof RocketSectionEntity part) part.dropContents();
        super.onRemove(state, level, pos, next, moving);
    }
}
