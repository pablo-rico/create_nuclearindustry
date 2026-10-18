package org.papiricoh.create_nuclearindustry.missile.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.Clearable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.advancement.NuclearAdvancements;
import org.papiricoh.create_nuclearindustry.advancement.NuclearEvent;
import org.papiricoh.create_nuclearindustry.integration.sable.ShipSpace;
import org.papiricoh.create_nuclearindustry.AllNuclearTags;
import org.papiricoh.create_nuclearindustry.explosive.blockentity.NuclearBombBlockEntity;
import org.papiricoh.create_nuclearindustry.civildefense.ThreatRegistry;
import org.papiricoh.create_nuclearindustry.explosive.DirtyWarheadStats;
import org.papiricoh.create_nuclearindustry.explosive.FissileMaterial;
import org.papiricoh.create_nuclearindustry.explosive.WarheadStats;
import org.papiricoh.create_nuclearindustry.missile.MissileConfig;
import org.papiricoh.create_nuclearindustry.missile.MissileRegistry;
import org.papiricoh.create_nuclearindustry.missile.flight.Flight;
import org.papiricoh.create_nuclearindustry.missile.flight.FlightTracker;
import org.papiricoh.create_nuclearindustry.missile.flight.Warhead;
import org.papiricoh.create_nuclearindustry.missile.item.TargetDesignatorItem;
import org.papiricoh.create_nuclearindustry.missile.physics.MissileDesign;
import org.papiricoh.create_nuclearindustry.missile.physics.MissileFlight;

public class LaunchPadBlockEntity extends BlockEntity implements Clearable {
    public static final int MISSILE_SLOT = 0;
    public static final int DESIGNATOR_SLOT = 1;
    public static final int FIRST_URANIUM_SLOT = 2;
    public static final int URANIUM_SLOTS = 3;
    public static final int SLOT_COUNT = FIRST_URANIUM_SLOT + URANIUM_SLOTS;
    public static final float REQUIRED_ENRICHMENT = WarheadStats.REQUIRED_ENRICHMENT;

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
        protected int getStackLimit(int slot, ItemStack stack) {
            return Math.min(super.getStackLimit(slot, stack), LaunchPadBlockEntity.this.stackLimit(slot, stack));
        }

        @Override
        protected void onContentsChanged(int slot) {
            markDirtyAndSync();
        }
    };
    private int countdown = -1;
    /** Whether what it holds has been checked against what it takes now, once after loading. */
    private boolean legacyChecked;

    public LaunchPadBlockEntity(BlockPos pos, BlockState state) {
        super(MissileRegistry.LAUNCH_PAD_ENTITY.get(), pos, state);
    }

    public boolean isItemValid(int slot, ItemStack stack) {
        if (slot == MISSILE_SLOT) {
            return stack.is(MissileRegistry.MISSILE_ITEM.get());
        }
        if (slot == DESIGNATOR_SLOT) {
            return stack.is(MissileRegistry.TARGET_DESIGNATOR.get());
        }
        if (stack.is(AllNuclearTags.Items.THERMONUCLEAR_COMPONENTS)) {
            // A missile is too small for a thermonuclear warhead.
            return false;
        }
        return isValidFissile(stack) || DirtyWarheadStats.isRadiological(stack);
    }

    /** How many of {@code stack} {@code slot} holds at most: cores go two to a slot, as in a bomb. */
    public int stackLimit(int slot, ItemStack stack) {
        if (slot == MISSILE_SLOT || slot == DESIGNATOR_SLOT) {
            return 1;
        }
        return FissileMaterial.isWarheadCore(stack) ? NuclearBombBlockEntity.CORES_PER_SLOT : stack.getMaxStackSize();
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
        if (!powered || !isReadyToLaunch()) {
            if (countdown >= 0) {
                countdown = -1;
                ThreatRegistry.withdraw(level, getBlockPos());
                markDirtyAndSync();
            }
            return;
        }

        if (countdown < 0) {
            countdown = MissileConfig.launchCountdownTicks();
            reportThreat();
            markDirtyAndSync();
            return;
        }

        countdown--;
        if (countdown <= 0) {
            // The missile reports itself from its first tick of flight.
            ThreatRegistry.withdraw(level, getBlockPos());
            launch();
        } else {
            reportThreat();
            if (countdown % 20 == 0) {
                markDirtyAndSync();
            }
        }
    }

    /**
     * Pads loaded before warheads took only cores may hold loose metal: it is dropped beside the pad,
     * with anything past what a slot now holds, and the countdown stops.
     */
    private void ejectWhatNoLongerFits() {
        boolean ejected = false;
        for (int slot = FIRST_URANIUM_SLOT; slot < SLOT_COUNT; slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            int keep = isItemValid(slot, stack) ? Math.min(stack.getCount(), stackLimit(slot, stack)) : 0;
            if (keep < stack.getCount()) {
                ItemStack out = stack.copyWithCount(stack.getCount() - keep);
                inventory.setStackInSlot(slot, stack.copyWithCount(keep));
                Block.popResource(level, getBlockPos().above(), out);
                ejected = true;
            }
        }
        if (ejected && countdown >= 0) {
            countdown = -1;
            ThreatRegistry.withdraw(level, getBlockPos());
        }
    }

    /**
     * Warns the sirens around the target while counting down, ahead of the launch: the impact is the
     * rest of the countdown plus the flight. Reported every tick, so it survives a reload.
     */
    private void reportThreat() {
        BlockPos target = getTarget();
        Vec3 aim = aimPoint();
        if (target == null || aim == null) {
            return;
        }
        ThreatRegistry.report(level, getBlockPos(), BlockPos.containing(aim), warhead().threatRadius(), countdown + predictedFlightTicks());
    }

    private Vec3 launchPoint() {
        BlockPos pos = getBlockPos();
        return new Vec3(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
    }

    /** The point the missile comes down on: its target, or above it when the designator is set to airburst. */
    @Nullable
    public Vec3 aimPoint() {
        BlockPos target = getTarget();
        if (target == null) {
            return null;
        }
        Vec3 aim = Vec3.atCenterOf(target);
        Warhead warhead = warhead();
        if (warhead.nuclear() && TargetDesignatorItem.isAirburst(inventory.getStackInSlot(DESIGNATOR_SLOT))) {
            // Set to airburst: it goes off above the target, clear of its own fireball.
            aim = aim.add(0, TargetDesignatorItem.airburstHeight(warhead.destructionRadius()), 0);
        }
        return aim;
    }

    /** Ticks the loaded missile would fly to its aim point, from lift-off to impact; 0 without a target. */
    public int predictedFlightTicks() {
        Vec3 aim = aimPoint();
        if (aim == null) {
            return 0;
        }
        Vec3 start = level == null ? launchPoint() : ShipSpace.toWorld(level, launchPoint());
        MissileFlight.Prediction flight = MissileFlight.predict(MissileDesign.LEGACY_PAD, Flight.v3(start), Flight.v3(aim),
                Flight.v3(launchVelocity()), MissileConfig.flightParams());
        return Mth.ceil(flight.ticks());
    }

    /** A pad aboard a ship hands its missile the ship's own speed, blocks per tick. */
    private Vec3 launchVelocity() {
        if (level == null) {
            return Vec3.ZERO;
        }
        ShipSpace.Ship ship = ShipSpace.shipAt(level, getBlockPos());
        return ship == null ? Vec3.ZERO : ship.velocity(ShipSpace.toWorld(level, launchPoint())).scale(1.0 / 20.0);
    }

    /** What the loaded warhead slots make. */
    public Warhead warhead() {
        if (isDirtyWarhead()) {
            return Warhead.dirty(getRadiologicalLoad());
        }
        int units = getValidUraniumCount();
        float enrichment = getAverageEnrichment();
        return Warhead.fission(WarheadStats.power(units, enrichment), WarheadStats.horizontalRadius(units, enrichment),
                WarheadStats.intensity(enrichment), getRadiologicalLoad());
    }

    @Override
    public void setRemoved() {
        if (level != null) {
            ThreatRegistry.withdraw(level, getBlockPos());
        }
        super.setRemoved();
    }

    public boolean isReadyToLaunch() {
        return hasMissile() && (getValidUraniumCount() > 0 || getRadiologicalLoad() > 0) && getTarget() != null;
    }

    /**
     * Radioactivity of the non-fissile radioactive material in the warhead slots, in rad/s. With
     * fissile material too it salts a nuclear warhead; on its own it makes a dirty one.
     */
    public float getRadiologicalLoad() {
        return DirtyWarheadStats.load(inventory, FIRST_URANIUM_SLOT, SLOT_COUNT);
    }

    /** No fissile material, only radioactive: the missile carries a dirty warhead. */
    public boolean isDirtyWarhead() {
        return getValidUraniumCount() <= 0 && getRadiologicalLoad() > 0;
    }

    public boolean hasMissile() {
        return inventory.getStackInSlot(MISSILE_SLOT).is(MissileRegistry.MISSILE_ITEM.get());
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
        return MissileConfig.launchCountdownTicks();
    }

    /** Warhead units loaded: eight per core. */
    public int getValidUraniumCount() {
        return FissileMaterial.units(inventory, FIRST_URANIUM_SLOT, SLOT_COUNT);
    }

    public float getAverageEnrichment() {
        return FissileMaterial.averageEnrichment(inventory, FIRST_URANIUM_SLOT, SLOT_COUNT);
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

    /** Kept for callers from before plutonium: same as {@link #isValidFissile}. */
    public boolean isValidUranium(ItemStack stack) {
        return isValidFissile(stack);
    }

    private void launch() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        BlockPos target = getTarget();
        Vec3 aim = aimPoint();
        if (target == null || aim == null || !isReadyToLaunch()) {
            countdown = -1;
            return;
        }
        Warhead warhead = warhead();
        Vec3 velocity = launchVelocity();
        consumeFuel();
        markDirtyAndSync();

        // A pad aboard a ship launches from where the ship really is.
        Vec3 start = ShipSpace.toWorld(serverLevel, launchPoint());
        FlightTracker.get(serverLevel).launch(serverLevel, MissileDesign.LEGACY_PAD, start, velocity, aim, target, warhead, null);
        NuclearAdvancements.awardNearby(serverLevel, worldPosition, NuclearAdvancements.WITNESS_RADIUS, NuclearEvent.MISSILE_LAUNCHED);
    }

    private void consumeFuel() {
        // The missile and the whole warhead go; the designator is kept for the next launch.
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
