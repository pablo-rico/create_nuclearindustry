package org.papiricoh.create_nuclearindustry.enrichment.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.papiricoh.create_nuclearindustry.AllNuclearEntities;
import org.papiricoh.create_nuclearindustry.processing.AllNuclearRecipeTypes;
import org.papiricoh.create_nuclearindustry.processing.KineticProcessingBlockEntity;

import java.util.ArrayList;
import java.util.List;

/** One stage of a gas centrifuge cascade. */
public class CentrifugeBlockEntity extends KineticProcessingBlockEntity {

    public CentrifugeBlockEntity(BlockPos pos, BlockState state) {
        super(AllNuclearEntities.CENTRIFUGE.get(), pos, state);
    }

    @Override
    protected AllNuclearRecipeTypes recipeType() {
        return AllNuclearRecipeTypes.CENTRIFUGING;
    }

    @Override
    protected float minimumSpeed() {
        return 32.0f;
    }

    @Override
    protected float stressImpact() {
        return 16.0f;
    }

    @Override
    protected Component machineName() {
        return Component.translatable("block.create_nuclearindustry.centrifuge");
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if (!clientPacket && tag.contains("inventory")) {
            migrateItemCentrifuge(tag, registries);
        }
    }

    /**
     * Centrifuges placed before the cascade rewrite enriched solid uranium in place. Their
     * items are moved to the output slots (and the input slot if needed) so they can be
     * taken out; the new recipes no longer accept them.
     */
    private void migrateItemCentrifuge(CompoundTag tag, HolderLookup.Provider registries) {
        ItemStackHandler legacy = new ItemStackHandler(2);
        legacy.deserializeNBT(registries, tag.getCompound("inventory"));
        List<ItemStack> recovered = new ArrayList<>();
        for (int slot = 0; slot < legacy.getSlots(); slot++) {
            recovered.add(legacy.getStackInSlot(slot));
        }
        if (tag.contains("processing")) {
            recovered.add(ItemStack.parseOptional(registries, tag.getCompound("processing")));
        }
        int[] targets = {FIRST_OUTPUT_SLOT, FIRST_OUTPUT_SLOT + 1, INPUT_SLOT};
        int next = 0;
        for (ItemStack stack : recovered) {
            if (stack.isEmpty()) {
                continue;
            }
            while (next < targets.length && !items.getStackInSlot(targets[next]).isEmpty()) {
                next++;
            }
            if (next >= targets.length) {
                break;
            }
            items.setStackInSlot(targets[next], stack);
        }
    }
}
