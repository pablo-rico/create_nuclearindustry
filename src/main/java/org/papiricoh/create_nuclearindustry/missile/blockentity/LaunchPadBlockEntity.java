package org.papiricoh.create_nuclearindustry.missile.blockentity;

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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.AllNuclearEntities;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.enrichment.item.UraniumItem;
import org.papiricoh.create_nuclearindustry.explosive.WarheadStats;
import org.papiricoh.create_nuclearindustry.missile.entity.MissileEntity;
import org.papiricoh.create_nuclearindustry.missile.item.TargetDesignatorItem;

public class LaunchPadBlockEntity extends BlockEntity implements Clearable {
    public static final int MISSILE_SLOT = 0;
    public static final int DESIGNATOR_SLOT = 1;
    public static final int FIRST_URANIUM_SLOT = 2;
    public static final int URANIUM_SLOTS = 3;
    public static final int SLOT_COUNT = FIRST_URANIUM_SLOT + URANIUM_SLOTS;
    public static final float REQUIRED_ENRICHMENT = WarheadStats.REQUIRED_ENRICHMENT;
    private static final int COUNTDOWN_TICKS = 200;

    private final ItemStackHandler inventory = new ItemStackHandler(SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return LaunchPadBlockEntity.this.isItemValid(slot, stack);
        }

        @Override
        public int getSlotLimit(int slot) {
            return (slot == MISSILE_SLOT || slot == DESIGNATOR_SLOT) ? 1 : super.getSlotLimit(slot);
        }

        @Override
        protected void onContentsChanged(int slot) {
            markDirtyAndSync();
        }
    };
    private int countdown = -1;

    public LaunchPadBlockEntity(BlockPos pos, BlockState state) {
        super(AllNuclearEntities.LAUNCH_PAD.get(), pos, state);
    }

    public boolean isItemValid(int slot, ItemStack stack) {
        if (slot == MISSILE_SLOT) {
            return stack.is(AllNuclearItems.MISSILE.get());
        }
        if (slot == DESIGNATOR_SLOT) {
            return stack.is(AllNuclearItems.TARGET_DESIGNATOR.get());
        }
        return isValidUranium(stack);
    }

    public void tick() {
        if (level == null || level.isClientSide) {
            return;
        }

        boolean powered = level.hasNeighborSignal(getBlockPos());
        if (!powered || !isReadyToLaunch()) {
            if (countdown >= 0) {
                countdown = -1;
                markDirtyAndSync();
            }
            return;
        }

        if (countdown < 0) {
            countdown = COUNTDOWN_TICKS;
            markDirtyAndSync();
            return;
        }

        countdown--;
        if (countdown <= 0) {
            launch();
        } else if (countdown % 20 == 0) {
            markDirtyAndSync();
        }
    }

    public boolean isReadyToLaunch() {
        return hasMissile() && getValidUraniumCount() > 0 && getTarget() != null;
    }

    public boolean hasMissile() {
        return inventory.getStackInSlot(MISSILE_SLOT).is(AllNuclearItems.MISSILE.get());
    }

    @Nullable
    public BlockPos getTarget() {
        return TargetDesignatorItem.getTarget(inventory.getStackInSlot(DESIGNATOR_SLOT));
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
        for (int slot = FIRST_URANIUM_SLOT; slot < SLOT_COUNT; slot++) {
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
        for (int slot = FIRST_URANIUM_SLOT; slot < SLOT_COUNT; slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (isValidUranium(stack)) {
                count += stack.getCount();
                total += UraniumItem.getEnrichment(stack) * stack.getCount();
            }
        }
        return count == 0 ? 0.0f : total / count;
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

    public boolean isValidUranium(ItemStack stack) {
        return stack.is(AllNuclearItems.URANIUM.get()) && UraniumItem.getEnrichment(stack) >= REQUIRED_ENRICHMENT;
    }

    private void launch() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        BlockPos target = getTarget();
        if (target == null || !hasMissile() || getValidUraniumCount() <= 0) {
            countdown = -1;
            return;
        }

        int units = getValidUraniumCount();
        float avgEnrichment = getAverageEnrichment();
        float power = WarheadStats.power(units, avgEnrichment);
        int horizontalRadius = WarheadStats.horizontalRadius(units, avgEnrichment);
        int verticalRadius = WarheadStats.verticalRadius(units, avgEnrichment);
        float intensity = WarheadStats.intensity(avgEnrichment);

        consumeFuel();
        markDirtyAndSync();

        BlockPos pos = getBlockPos();
        Vec3 start = new Vec3(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
        Vec3 targetVec = new Vec3(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5);

        MissileEntity missile = new MissileEntity(AllNuclearEntities.MISSILE.get(), serverLevel);
        missile.setPos(start.x, start.y, start.z);
        missile.configure(start, targetVec, power, horizontalRadius, verticalRadius, intensity);
        serverLevel.addFreshEntity(missile);
    }

    private void consumeFuel() {
        // Consume el cohete y todo el uranio cargado; el designador es reutilizable y se conserva.
        inventory.extractItem(MISSILE_SLOT, 1, false);
        for (int slot = FIRST_URANIUM_SLOT; slot < SLOT_COUNT; slot++) {
            inventory.setStackInSlot(slot, ItemStack.EMPTY);
        }
        countdown = -1;
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
