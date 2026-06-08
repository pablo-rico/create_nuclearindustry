package org.papiricoh.create_nuclearindustry.explosive.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Clearable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.papiricoh.create_nuclearindustry.AllNuclearEntities;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.enrichment.item.UraniumItem;
import org.papiricoh.create_nuclearindustry.explosive.NuclearBlastManager;

public class NuclearBombBlockEntity extends BlockEntity implements Clearable {
    public static final int SLOT_COUNT = 3;
    public static final float REQUIRED_ENRICHMENT = 90.0f;
    public static final int MAX_URANIUM_UNITS = SLOT_COUNT * 16;
    private static final int COUNTDOWN_TICKS = 200;
    private static final int MAX_HORIZONTAL_RADIUS = 250;
    private static final int MIN_HORIZONTAL_RADIUS = 80;

    private final ItemStackHandler inventory = new ItemStackHandler(SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return isValidUranium(stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            markDirtyAndSync();
        }
    };
    private int countdown = -1;

    public NuclearBombBlockEntity(BlockPos pos, BlockState state) {
        super(AllNuclearEntities.NUCLEAR_BOMB.get(), pos, state);
    }

    public void tick() {
        if (level == null || level.isClientSide) {
            return;
        }

        boolean powered = level.hasNeighborSignal(getBlockPos());
        if (!powered) {
            if (countdown >= 0) {
                countdown = -1;
                markDirtyAndSync();
            }
            return;
        }

        if (getValidUraniumCount() <= 0) {
            countdown = -1;
            return;
        }

        if (countdown < 0) {
            countdown = COUNTDOWN_TICKS;
            markDirtyAndSync();
            return;
        }

        countdown--;
        if (countdown <= 0) {
            detonate();
        } else if (countdown % 20 == 0) {
            markDirtyAndSync();
        }
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    public int getCountdown() {
        return countdown;
    }

    public int getCountdownDuration() {
        return COUNTDOWN_TICKS;
    }

    public int getValidUraniumCount() {
        int count = 0;
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (isValidUranium(stack)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    public float getAverageEnrichment() {
        int count = 0;
        float total = 0.0f;
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (isValidUranium(stack)) {
                count += stack.getCount();
                total += UraniumItem.getEnrichment(stack) * stack.getCount();
            }
        }
        return count == 0 ? 0.0f : total / count;
    }

    public float getEstimatedPower() {
        int units = getValidUraniumCount();
        if (units <= 0) {
            return 0.0f;
        }
        float enrichmentFactor = Math.max(1.0f, getAverageEnrichment() / REQUIRED_ENRICHMENT);
        return Math.min(160.0f, 36.0f + units * 2.6f * enrichmentFactor);
    }

    public int getEstimatedRadius() {
        int units = getValidUraniumCount();
        if (units <= 0) {
            return 0;
        }
        float enrichmentFactor = Math.max(1.0f, getAverageEnrichment() / REQUIRED_ENRICHMENT);
        float fuelRatio = Math.min(1.0f, units / (float) MAX_URANIUM_UNITS);
        return Math.min(MAX_HORIZONTAL_RADIUS, Math.round(MIN_HORIZONTAL_RADIUS + (MAX_HORIZONTAL_RADIUS - MIN_HORIZONTAL_RADIUS) * fuelRatio * enrichmentFactor));
    }

    public int getEstimatedVerticalRadius() {
        return Math.max(28, Math.round(getEstimatedRadius() * 0.34f));
    }

    public boolean isValidUranium(ItemStack stack) {
        return stack.is(AllNuclearItems.URANIUM.get()) && UraniumItem.getEnrichment(stack) >= REQUIRED_ENRICHMENT;
    }

    private void detonate() {
        Level level = getLevel();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        BlockPos center = getBlockPos();
        float power = getEstimatedPower();
        int horizontalRadius = getEstimatedRadius();
        int verticalRadius = getEstimatedVerticalRadius();
        float intensity = Math.max(1.0f, getAverageEnrichment() / REQUIRED_ENRICHMENT);
        clearContent();
        level.removeBlock(center, false);
        level.explode(null, center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5,
                Math.min(64.0f, power * 0.35f), Level.ExplosionInteraction.BLOCK);
        NuclearBlastManager.start(serverLevel, center, horizontalRadius, verticalRadius, intensity);
    }

    private void markDirtyAndSync() {
        setChanged();
        Level level = getLevel();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(getBlockPos(), state, state, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public void clearContent() {
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            inventory.setStackInSlot(slot, ItemStack.EMPTY);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.put("inventory", inventory.serializeNBT(provider));
        tag.putInt("countdown", countdown);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        if (tag.contains("inventory")) {
            inventory.deserializeNBT(provider, tag.getCompound("inventory"));
        }
        countdown = tag.contains("countdown") ? tag.getInt("countdown") : -1;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        CompoundTag tag = super.getUpdateTag(provider);
        tag.put("inventory", inventory.serializeNBT(provider));
        tag.putInt("countdown", countdown);
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider provider) {
        super.handleUpdateTag(tag, provider);
        if (tag.contains("inventory")) {
            inventory.deserializeNBT(provider, tag.getCompound("inventory"));
        }
        countdown = tag.contains("countdown") ? tag.getInt("countdown") : -1;
    }
}
