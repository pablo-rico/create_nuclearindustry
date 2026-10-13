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
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.civildefense.ThreatRegistry;
import org.papiricoh.create_nuclearindustry.explosive.DirtyWarheadStats;
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
            return isValidCharge(stack);
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
                ThreatRegistry.withdraw(level, getBlockPos());
                markDirtyAndSync();
            }
            return;
        }

        if (!hasCharge()) {
            if (countdown >= 0) {
                ThreatRegistry.withdraw(level, getBlockPos());
            }
            countdown = -1;
            return;
        }

        if (countdown < 0) {
            countdown = COUNTDOWN_TICKS;
            reportThreat();
            markDirtyAndSync();
            return;
        }

        countdown--;
        if (countdown <= 0) {
            detonate();
        } else {
            reportThreat();
            if (countdown % 20 == 0) {
                markDirtyAndSync();
            }
        }
    }

    /** Warns the sirens around while counting down; reported every tick, so it survives a reload. */
    private void reportThreat() {
        int radius = isDirty() ? DirtyWarheadStats.falloutRadius(getRadiologicalLoad())
                : NuclearDetonation.falloutRadius(getEstimatedRadius());
        ThreatRegistry.report(level, getBlockPos(), getBlockPos(), radius, countdown);
    }

    @Override
    public void setRemoved() {
        if (level != null) {
            ThreatRegistry.withdraw(level, getBlockPos());
        }
        super.setRemoved();
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

    /** What this bomb takes: fissile material, or for a dirty bomb anything else radioactive. */
    public boolean isValidCharge(ItemStack stack) {
        return isDirty() ? DirtyWarheadStats.isRadiological(stack) : isValidFissile(stack);
    }

    /** Kept for callers from before plutonium: same as {@link #isValidCharge}. */
    public boolean isValidUranium(ItemStack stack) {
        return isValidCharge(stack);
    }

    /** A Dirty Bomb block rather than a Nuclear Bomb. */
    public boolean isDirty() {
        return getBlockState().is(AllNuclearBlocks.DIRTY_BOMB.get());
    }

    public boolean hasCharge() {
        return isDirty() ? getRadiologicalLoad() > 0 : getValidUraniumCount() > 0;
    }

    /** Radioactivity of a dirty bomb's load, in rad/s. */
    public float getRadiologicalLoad() {
        return DirtyWarheadStats.load(inventory, 0, SLOT_COUNT);
    }

    private void detonate() {
        Level level = getLevel();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        BlockPos center = getBlockPos();
        ThreatRegistry.withdraw(level, center);
        if (isDirty()) {
            float load = getRadiologicalLoad();
            clearContent();
            level.removeBlock(center, false);
            NuclearDetonation.detonateDirty(serverLevel, center.getCenter(), load);
            return;
        }
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
