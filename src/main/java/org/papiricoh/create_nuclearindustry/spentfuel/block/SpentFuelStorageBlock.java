package org.papiricoh.create_nuclearindustry.spentfuel.block;

import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.item.ItemHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.papiricoh.create_nuclearindustry.spentfuel.SpentFuel;
import org.papiricoh.create_nuclearindustry.spentfuel.SpentFuelText;

/**
 * A block that stores spent assemblies: right-click with assemblies to put them in, empty-handed
 * to take the last one out. Hoppers and funnels work too. Comparators read how full it is.
 */
public abstract class SpentFuelStorageBlock<T extends SpentFuelStorageBlockEntity> extends Block implements IBE<T> {

    protected SpentFuelStorageBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hitResult) {
        if (!SpentFuel.isSpentFuel(stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            withBlockEntityDo(level, pos, store -> {
                SpentFuel.refresh(stack, level.getGameTime());
                if (!store.accepts(stack)) {
                    player.displayClientMessage(Component.translatable(SpentFuelText.PREFIX + "refused", SpentFuelText.grade(SpentFuel.gradeOf(stack))), true);
                    return;
                }
                ItemStack rest = store.insert(stack.copy());
                if (rest.getCount() == stack.getCount()) {
                    player.displayClientMessage(Component.translatable(SpentFuelText.PREFIX + "full"), true);
                }
                player.setItemInHand(hand, rest);
            });
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide) {
            withBlockEntityDo(level, pos, store -> {
                ItemStack taken = store.extractLast();
                if (!taken.isEmpty()) {
                    player.getInventory().placeItemBackInInventory(taken);
                }
            });
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        IBE.onRemove(state, level, pos, newState);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return ItemHelper.calcRedstoneFromBlockEntity(this, level, pos);
    }
}
