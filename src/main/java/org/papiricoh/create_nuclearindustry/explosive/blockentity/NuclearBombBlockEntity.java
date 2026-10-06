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
import org.papiricoh.create_nuclearindustry.explosive.FissileMaterial;
import org.papiricoh.create_nuclearindustry.explosive.NuclearDetonation;
import org.papiricoh.create_nuclearindustry.explosive.WarheadStats;

public class NuclearBombBlockEntity extends BlockEntity implements Clearable {
    public static final int SLOT_COUNT = 3;
    public static final float REQUIRED_ENRICHMENT = WarheadStats.REQUIRED_ENRICHMENT;
    public static final int MAX_URANIUM_UNITS = SLOT_COUNT * 16;
    private static final int COUNTDOWN_TICKS = 200;

    private final ItemStackHandler inventory = new ItemStackHandler(SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return isValidFissile(stack);
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

    /** Warhead units loaded: one per weapons-grade uranium, two per plutonium. */
    public int getValidUraniumCount() {
        return FissileMaterial.units(inventory, 0, SLOT_COUNT);
    }

    public float getAverageEnrichment() {
        return FissileMaterial.averageEnrichment(inventory, 0, SLOT_COUNT);
    }

    public float getEstimatedPower() {
        return WarheadStats.power(getValidUraniumCount(), getAverageEnrichment());
    }

    public int getEstimatedRadius() {
        return WarheadStats.horizontalRadius(getValidUraniumCount(), getAverageEnrichment());
    }

    public int getEstimatedVerticalRadius() {
        return WarheadStats.verticalRadius(getValidUraniumCount(), getAverageEnrichment());
    }

    /** Weapons-grade uranium or plutonium. */
    public boolean isValidFissile(ItemStack stack) {
        return FissileMaterial.isWeaponsFissile(stack);
    }

    /** Kept for callers from before plutonium: same as {@link #isValidFissile}. */
    public boolean isValidUranium(ItemStack stack) {
        return isValidFissile(stack);
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
        float intensity = WarheadStats.intensity(getAverageEnrichment());
        clearContent();
        level.removeBlock(center, false);
        NuclearDetonation.detonate(serverLevel, center.getCenter(),
                power, horizontalRadius, verticalRadius, intensity);
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
