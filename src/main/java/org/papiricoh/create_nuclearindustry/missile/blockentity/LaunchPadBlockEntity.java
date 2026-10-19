package org.papiricoh.create_nuclearindustry.missile.blockentity;

import com.mojang.authlib.GameProfile;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import org.papiricoh.create_nuclearindustry.missile.LaunchCooldowns;
import org.papiricoh.create_nuclearindustry.missile.LaunchRules;
import org.papiricoh.create_nuclearindustry.missile.LaunchWarnings;
import org.papiricoh.create_nuclearindustry.missile.MissileConfig;
import org.papiricoh.create_nuclearindustry.missile.MissileRegistry;
import org.papiricoh.create_nuclearindustry.missile.flight.Flight;
import org.papiricoh.create_nuclearindustry.missile.flight.FlightTracker;
import org.papiricoh.create_nuclearindustry.missile.flight.Warhead;
import org.papiricoh.create_nuclearindustry.missile.item.TargetDesignatorItem;
import org.papiricoh.create_nuclearindustry.missile.physics.MissileDesign;
import org.papiricoh.create_nuclearindustry.missile.physics.MissileFlight;

import java.util.UUID;

/**
 * A launch pad: loaded through its menu and powered with redstone, it counts down and fires its missile
 * into the level's {@link FlightTracker}. Before counting down, and again at lift-off, it checks the
 * server's limits ({@link LaunchRules}); a refused pad says why on its screen and to the player who
 * answers for it: the last one to load it, or else whoever placed it.
 */
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
    /** Whoever placed the pad, if known. */
    @Nullable
    private UUID owner;
    /** The last player to load it through its menu, if any. */
    @Nullable
    private UUID operator;
    /** Why it will not launch, if it is powered and loaded but the server's limits forbid it. */
    private LaunchRules.Refusal refusal = LaunchRules.Refusal.NONE;
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
        // Kept only to let old worlds unload their inventories. Never resume an old countdown.
        if (!legacyChecked && level != null && !level.isClientSide) {
            legacyChecked = true;
            ejectWhatNoLongerFits();
        }
        countdown = -1;
        if (level != null && !level.isClientSide) ThreatRegistry.withdraw(level, worldPosition);
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

    /**
     * Checks the server's limits; a refusal stops any countdown, is shown on the pad and told to its player
     * once. Returns whether the pad is refused.
     */
    private boolean refuse() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }
        LaunchRules.Refusal now = checkLimits(serverLevel);
        if (now != refusal) {
            refusal = now;
            if (now != LaunchRules.Refusal.NONE) {
                tellAnswerable(serverLevel.getServer(), now);
            }
            markDirtyAndSync();
        }
        if (now != LaunchRules.Refusal.NONE && countdown >= 0) {
            countdown = -1;
            ThreatRegistry.withdraw(level, getBlockPos());
            markDirtyAndSync();
        }
        return now != LaunchRules.Refusal.NONE;
    }

    /** What, if anything, the server's limits ({@code missile.*}) say against launching now. Public for tests. */
    public LaunchRules.Refusal checkLimits(ServerLevel level) {
        if (!LaunchRules.dimensionAllowed(level.dimension().location().toString(), MissileConfig.allowedDimensions)) {
            return LaunchRules.Refusal.DIMENSION;
        }
        Vec3 aim = aimPoint();
        if (aim != null) {
            Vec3 start = ShipSpace.toWorld(level, launchPoint());
            if (LaunchRules.outOfRange(start.x, start.z, aim.x, aim.z, MissileConfig.maxRange)) {
                return LaunchRules.Refusal.OUT_OF_RANGE;
            }
            BlockPos spawn = level.getSharedSpawnPos();
            if (level.dimension() == Level.OVERWORLD
                    && LaunchRules.nearSpawn(aim.x, aim.z, spawn.getX() + 0.5, spawn.getZ() + 0.5, MissileConfig.protectedSpawnRadius)) {
                return LaunchRules.Refusal.SPAWN_PROTECTED;
            }
        }
        UUID player = answerable();
        MinecraftServer server = level.getServer();
        if (MissileConfig.launchPermissionLevel > 0 && permissionLevel(server, player) < MissileConfig.launchPermissionLevel) {
            return LaunchRules.Refusal.NO_PERMISSION;
        }
        if (player != null && LaunchCooldowns.get(server).ticksLeft(server, player) > 0) {
            return LaunchRules.Refusal.COOLDOWN;
        }
        return LaunchRules.Refusal.NONE;
    }

    /** The permission level of {@code player}, online or not; 0 for nobody. */
    private static int permissionLevel(MinecraftServer server, @Nullable UUID player) {
        if (player == null) {
            return 0;
        }
        ServerPlayer online = server.getPlayerList().getPlayer(player);
        if (online != null) {
            return server.getProfilePermissions(online.getGameProfile());
        }
        GameProfile profile = server.getProfileCache() == null ? null : server.getProfileCache().get(player).orElse(null);
        return server.getProfilePermissions(profile != null ? profile : new GameProfile(player, ""));
    }

    private void tellAnswerable(MinecraftServer server, LaunchRules.Refusal why) {
        UUID player = answerable();
        ServerPlayer online = player == null ? null : server.getPlayerList().getPlayer(player);
        if (online == null) {
            return;
        }
        Component reason = why == LaunchRules.Refusal.COOLDOWN
                ? Component.translatable(why.key() + ".seconds", Mth.ceil(LaunchCooldowns.get(server).ticksLeft(server, player) / 20.0))
                : Component.translatable(why.key());
        BlockPos pos = getBlockPos();
        online.sendSystemMessage(Component.translatable("message.create_nuclearindustry.launch_refused", pos.getX(), pos.getY(),
                pos.getZ(), reason).withStyle(ChatFormatting.GOLD));
    }

    /** The player who answers for this pad's launches: the last to load it, or whoever placed it. */
    @Nullable
    public UUID answerable() {
        return operator != null ? operator : owner;
    }

    public void setOwner(@Nullable UUID owner) {
        this.owner = owner;
        setChanged();
    }

    /** Called when {@code player} loads the pad through its menu: from now on they answer for it. */
    public void setOperator(UUID player) {
        if (!player.equals(operator)) {
            operator = player;
            setChanged();
        }
    }

    public LaunchRules.Refusal getRefusal() {
        return refusal;
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
        if (refuse()) {
            return;
        }
        Warhead warhead = warhead();
        Vec3 velocity = launchVelocity();
        int flightTicks = predictedFlightTicks();
        UUID player = answerable();
        consumeFuel();
        markDirtyAndSync();

        // A pad aboard a ship launches from where the ship really is.
        Vec3 start = ShipSpace.toWorld(serverLevel, launchPoint());
        FlightTracker.get(serverLevel).launch(serverLevel, MissileDesign.LEGACY_PAD, start, velocity, aim, target, warhead, player);
        if (player != null) {
            LaunchCooldowns.get(serverLevel.getServer()).record(player, LaunchCooldowns.now(serverLevel.getServer()));
        }
        LaunchWarnings.announce(serverLevel, aim, warhead.threatRadius(), flightTicks);
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
        if (owner != null) {
            tag.putUUID("owner", owner);
        }
        if (operator != null) {
            tag.putUUID("operator", operator);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        if (tag.contains("inventory")) {
            inventory.deserializeNBT(provider, tag.getCompound("inventory"));
        }
        countdown = -1;
        owner = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
        operator = tag.hasUUID("operator") ? tag.getUUID("operator") : null;
        // Only in client updates (block entity packets land here too); a save has none and reads as none.
        refusal = LaunchRules.Refusal.byOrdinal(tag.getInt("refusal"));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        CompoundTag tag = super.getUpdateTag(provider);
        tag.put("inventory", inventory.serializeNBT(provider));
        tag.putInt("countdown", countdown);
        tag.putInt("refusal", refusal.ordinal());
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
        countdown = -1;
        refusal = LaunchRules.Refusal.byOrdinal(tag.getInt("refusal"));
    }
}
