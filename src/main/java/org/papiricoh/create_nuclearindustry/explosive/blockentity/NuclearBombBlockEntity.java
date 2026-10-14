package org.papiricoh.create_nuclearindustry.explosive.blockentity;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Clearable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.AllNuclearEntities;
import org.papiricoh.create_nuclearindustry.civildefense.ThreatRegistry;
import org.papiricoh.create_nuclearindustry.explosive.DirtyWarheadStats;
import org.papiricoh.create_nuclearindustry.explosive.FissileMaterial;
import org.papiricoh.create_nuclearindustry.explosive.NuclearDetonation;
import org.papiricoh.create_nuclearindustry.explosive.WarheadLoad;
import org.papiricoh.create_nuclearindustry.explosive.WarheadStats;
import org.slf4j.Logger;

/**
 * A Nuclear Bomb or a Dirty Bomb: loaded through its screen, it counts down while powered and goes off
 * at zero. A nuclear bomb takes weapons-grade cores, two to a slot, so six make a full warhead; a dirty
 * one takes anything radioactive that is not weapons material. Subclasses (the thermonuclear bomb) bring
 * their own slots, countdown and detonation through the hooks.
 * <p>
 * Bombs placed before warheads took only cores may hold loose metal: on their first tick whatever no
 * longer fits is dropped beside the bomb and any countdown stops.
 */
public class NuclearBombBlockEntity extends BlockEntity implements Clearable {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final int SLOT_COUNT = 3;
    public static final int CORES_PER_SLOT = 2;
    public static final int MAX_CORES = SLOT_COUNT * CORES_PER_SLOT;
    public static final float REQUIRED_ENRICHMENT = WarheadStats.REQUIRED_ENRICHMENT;
    public static final int MAX_URANIUM_UNITS = MAX_CORES * WarheadLoad.UNITS_PER_CORE;
    private static final int COUNTDOWN_TICKS = 200;

    protected final ItemStackHandler inventory;
    private int countdown = -1;
    /** Whether what it holds has been checked against what it takes now, once after loading. */
    private boolean legacyChecked;

    public NuclearBombBlockEntity(BlockPos pos, BlockState state) {
        this(AllNuclearEntities.NUCLEAR_BOMB.get(), pos, state, SLOT_COUNT);
    }

    protected NuclearBombBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int slots) {
        super(type, pos, state);
        inventory = new ItemStackHandler(slots) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return isValidInSlot(slot, stack);
            }

            @Override
            protected int getStackLimit(int slot, ItemStack stack) {
                return Math.min(super.getStackLimit(slot, stack), stackLimit(slot, stack));
            }

            @Override
            protected void onContentsChanged(int slot) {
                markDirtyAndSync();
            }
        };
    }

    public void tick() {
        if (level == null || level.isClientSide) {
            return;
        }
        if (!legacyChecked) {
            legacyChecked = true;
            ejectWhatNoLongerFits();
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
            countdown = countdownTicks();
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

    /** Drops beside the bomb what it no longer takes, or past what a slot now holds, and stops its countdown. */
    private void ejectWhatNoLongerFits() {
        boolean ejected = false;
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            int keep = isValidInSlot(slot, stack) ? Math.min(stack.getCount(), stackLimit(slot, stack)) : 0;
            if (keep < stack.getCount()) {
                ItemStack out = stack.copyWithCount(stack.getCount() - keep);
                inventory.setStackInSlot(slot, stack.copyWithCount(keep));
                Block.popResource(level, getBlockPos().above(), out);
                ejected = true;
            }
        }
        if (ejected) {
            LOGGER.info("Ejected the loose warhead material of the bomb at {}: warheads now take weapons-grade cores", getBlockPos());
            if (countdown >= 0) {
                countdown = -1;
                ThreatRegistry.withdraw(level, getBlockPos());
            }
            markDirtyAndSync();
        }
    }

    /** Warns the sirens around while counting down; reported every tick, so it survives a reload. */
    private void reportThreat() {
        ThreatRegistry.report(level, getBlockPos(), getBlockPos(), threatRadius(), countdown);
    }

    /** How far round the bomb the sirens warn of. */
    protected int threatRadius() {
        return isDirty() ? DirtyWarheadStats.falloutRadius(getRadiologicalLoad())
                : NuclearDetonation.falloutRadius(getEstimatedRadius());
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
        return countdownTicks();
    }

    /** Ticks from powering it to the detonation. */
    protected int countdownTicks() {
        return COUNTDOWN_TICKS;
    }

    /** Warhead units loaded: eight per core. */
    public int getValidUraniumCount() {
        return FissileMaterial.units(inventory, 0, inventory.getSlots());
    }

    /** Cores loaded. */
    public int getCoreCount() {
        return getValidUraniumCount() / WarheadLoad.UNITS_PER_CORE;
    }

    public float getAverageEnrichment() {
        return FissileMaterial.averageEnrichment(inventory, 0, inventory.getSlots());
    }

    public float getEstimatedPower() {
        return WarheadStats.power(getValidUraniumCount(), getAverageEnrichment());
    }

    public int getEstimatedRadius() {
        return WarheadStats.horizontalRadius(getValidUraniumCount(), getAverageEnrichment());
    }

    public int getEstimatedCraterRadius() {
        return WarheadStats.craterRadius(getValidUraniumCount(), getAverageEnrichment());
    }

    /** A weapons-grade core. */
    public boolean isValidFissile(ItemStack stack) {
        return FissileMaterial.isWeaponsFissile(stack);
    }

    /** What this bomb takes: cores, or for a dirty bomb anything else radioactive. */
    public boolean isValidCharge(ItemStack stack) {
        return isDirty() ? DirtyWarheadStats.isRadiological(stack) : isValidFissile(stack);
    }

    /** Kept for callers from before plutonium: same as {@link #isValidCharge}. */
    public boolean isValidUranium(ItemStack stack) {
        return isValidCharge(stack);
    }

    /** What {@code slot} takes. */
    public boolean isValidInSlot(int slot, ItemStack stack) {
        return isValidCharge(stack);
    }

    /** How many of {@code stack} {@code slot} holds at most. */
    public int stackLimit(int slot, ItemStack stack) {
        return isDirty() ? stack.getMaxStackSize() : CORES_PER_SLOT;
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
        return DirtyWarheadStats.load(inventory, 0, inventory.getSlots());
    }

    private void detonate() {
        Level level = getLevel();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        BlockPos center = getBlockPos();
        ThreatRegistry.withdraw(level, center);
        explode(serverLevel, center);
    }

    /** Clears the bomb, takes its block away and sets off its warhead. */
    protected void explode(ServerLevel level, BlockPos center) {
        if (isDirty()) {
            float load = getRadiologicalLoad();
            clearContent();
            level.removeBlock(center, false);
            NuclearDetonation.detonateDirty(level, center.getCenter(), load);
            return;
        }
        float power = getEstimatedPower();
        int horizontalRadius = getEstimatedRadius();
        float intensity = WarheadStats.intensity(getAverageEnrichment());
        clearContent();
        level.removeBlock(center, false);
        NuclearDetonation.detonate(level, center.getCenter(), power, horizontalRadius, intensity, 0.0f);
    }

    protected void markDirtyAndSync() {
        setChanged();
        Level level = getLevel();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(getBlockPos(), state, state, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public void clearContent() {
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
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
