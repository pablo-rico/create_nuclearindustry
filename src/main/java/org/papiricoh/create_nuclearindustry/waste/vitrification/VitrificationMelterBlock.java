package org.papiricoh.create_nuclearindustry.waste.vitrification;

import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.papiricoh.create_nuclearindustry.waste.WasteRegistry;

/**
 * Vitrification melter: sits on a Blaze Burner like a basin. Right-click with an ingredient to
 * load it, empty-handed to take the canisters out; funnels and hoppers work too.
 */
public class VitrificationMelterBlock extends Block implements IBE<VitrificationMelterBlockEntity> {

    public VitrificationMelterBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hitResult) {
        VitrificationMelterBlockEntity melter = getBlockEntity(level, pos);
        if (melter == null || !accepts(melter, stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            ItemStack rest = stack;
            for (int slot = 0; slot < VitrificationMelterBlockEntity.INPUT_SLOTS && !rest.isEmpty(); slot++) {
                rest = melter.getAutomationHandler().insertItem(slot, rest, false);
            }
            player.setItemInHand(hand, rest);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    private static boolean accepts(VitrificationMelterBlockEntity melter, ItemStack stack) {
        for (int slot = 0; slot < VitrificationMelterBlockEntity.INPUT_SLOTS; slot++) {
            if (melter.getAutomationHandler().insertItem(slot, stack, true).getCount() < stack.getCount()) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide) {
            withBlockEntityDo(level, pos, melter -> {
                ItemStack taken = melter.getAutomationHandler().extractItem(VitrificationMelterBlockEntity.OUTPUT_SLOT, 64, false);
                // Sneaking with nothing to collect takes an ingredient back out, so a wrong load can be undone.
                for (int slot = 0; taken.isEmpty() && player.isShiftKeyDown() && slot < VitrificationMelterBlockEntity.INPUT_SLOTS; slot++) {
                    taken = melter.getInventory().extractItem(slot, 64, false);
                }
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
    public Class<VitrificationMelterBlockEntity> getBlockEntityClass() {
        return VitrificationMelterBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends VitrificationMelterBlockEntity> getBlockEntityType() {
        return WasteRegistry.VITRIFICATION_MELTER_ENTITY.get();
    }
}
