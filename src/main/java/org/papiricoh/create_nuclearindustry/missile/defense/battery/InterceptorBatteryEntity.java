package org.papiricoh.create_nuclearindustry.missile.defense.battery;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.RangedWrapper;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.controlroom.link.LinkCode;
import org.papiricoh.create_nuclearindustry.missile.defense.DefenseConfig;
import org.papiricoh.create_nuclearindustry.missile.defense.DefenseRegistry;
import org.papiricoh.create_nuclearindustry.missile.defense.DefenseSettings;
import org.papiricoh.create_nuclearindustry.missile.defense.EngagementSource;
import org.papiricoh.create_nuclearindustry.missile.defense.FireResult;
import org.papiricoh.create_nuclearindustry.missile.defense.InterceptorDesigns;
import org.papiricoh.create_nuclearindustry.missile.defense.InterceptorShooter;
import org.papiricoh.create_nuclearindustry.missile.defense.ShooterType;
import org.papiricoh.create_nuclearindustry.missile.physics.BallisticModel;
import org.papiricoh.create_nuclearindustry.missile.physics.InterceptorEnvelope;
import org.papiricoh.create_nuclearindustry.missile.physics.V3;
import org.papiricoh.create_nuclearindustry.integration.sable.ShipSpace;
import org.papiricoh.create_nuclearindustry.missile.MissileConfig;
import org.papiricoh.create_nuclearindustry.missile.flight.Flight;
import org.papiricoh.create_nuclearindustry.missile.flight.FlightTracker;
import org.papiricoh.create_nuclearindustry.missile.radar.RadarNetwork;
import org.papiricoh.create_nuclearindustry.missile.defense.DefenseCommand;
import org.papiricoh.create_nuclearindustry.missile.defense.EngagementBoard;
import org.papiricoh.create_nuclearindustry.missile.physics.InterceptSolution;

import java.util.Locale;
import java.util.UUID;
import java.util.ArrayDeque;
import java.util.Optional;

/**
 * An interceptor battery: a turret on a 2×2×2 base that turns its four-tube canister to the missiles it is ordered to meet and
 * launches at them. It hears of missiles from the radars on its link code or from the command tables it is linked to.
 */
public class InterceptorBatteryEntity extends KineticBlockEntity implements InterceptorShooter {
    public static final int TUBES = 4;

    /** What it is doing, for its screen, goggles and table; sent to clients by name. */
    public enum Status { UNPOWERED, IDLE, TRACKING, SLEWING, RELOADING, EMPTY, HOLD, ON_SHIP }

    protected final ItemStackHandler tubes = new ItemStackHandler(TUBES) {
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            return stack.is(DefenseRegistry.INTERCEPTOR.get());
        }

        @Override public int getSlotLimit(int slot) { return 1; }

        @Override
        protected void onContentsChanged(int slot) {
            // A round just loaded has to be seated before it can fly; an emptied tube has nothing to wait for. Only the server
            // times it: a client's copy of the tubes is refilled by every sync, and is told the time left with the rest.
            if (level == null || level.isClientSide) return;
            loading[slot] = getStackInSlot(slot).isEmpty() ? 0 : DefenseConfig.batteryReloadSeconds * 20;
            changed();
        }
    };
    /** What funnels and mechanical arms see: rounds go in, never come out. */
    private final IItemHandler insertOnlyTubes = new RangedWrapper(tubes, 0, TUBES) {
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { return ItemStack.EMPTY; }
    };
    /** Ticks each tube still needs before its round is ready. */
    protected final int[] loading = new int[TUBES];
    protected String code = "";
    protected DefenseSettings settings;
    @Nullable
    protected UUID owner;
    /** Where the turret points now and is turning to; the client turns it too, from the last angles it was sent. */
    protected float yaw, pitch = 45, lastYaw, lastPitch = 45, targetYaw, targetPitch = 45;
    protected long lastFired = Long.MIN_VALUE;
    protected Status status = Status.UNPOWERED;
    /** Orders to engage, oldest first; not saved, a reload drops them and automatic defence gives them again. */
    private final ArrayDeque<Order> orders = new ArrayDeque<>();
    private long lastSolve = Long.MIN_VALUE;
    private boolean aimed;

    private record Order(UUID target, EngagementSource source, long expiry) {}

    /** Ticks between fresh courses for the order it is turning to. */
    private static final int RESOLVE_TICKS = 5;
    /** Ticks between launches, so two rounds never leave the same instant. */
    private static final int LAUNCH_SPACING = 4;
    /** Degrees off its course it may still launch at: the round's own guidance makes up the rest. */
    private static final float ALIGNED = 2;

    public InterceptorBatteryEntity(BlockPos pos, BlockState state) {
        super(DefenseRegistry.BATTERY_ENTITY.get(), pos, state);
        settings = DefenseSettings.defaults(pos);
    }

    /** Automation may load tubes but must not pull live rounds out. */
    public IItemHandler tubes() { return insertOnlyTubes; }

    /** Its tubes themselves, for its screen and its block's drops. */
    public ItemStackHandler tubeInventory() { return tubes; }

    /** Loads one round from {@code held} into the first empty tube; whether it found one. */
    public boolean loadOne(ItemStack held) {
        if (!held.is(DefenseRegistry.INTERCEPTOR.get())) return false;
        for (int i = 0; i < TUBES; i++) if (tubes.getStackInSlot(i).isEmpty()) {
            tubes.setStackInSlot(i, held.copyWithCount(1));
            held.shrink(1);
            return true;
        }
        return false;
    }

    /** Takes a ready round out of its tube, or nothing. */
    public ItemStack takeReady() {
        for (int i = 0; i < TUBES; i++) if (ready(i)) {
            ItemStack result = tubes.getStackInSlot(i);
            tubes.setStackInSlot(i, ItemStack.EMPTY);
            return result;
        }
        return ItemStack.EMPTY;
    }

    /** Turns the turret to rest facing {@code facing}, as placed. */
    public void restFacing(Direction facing) {
        yaw = lastYaw = targetYaw = TurretAim.wrap(facing.toYRot() - 180);
        changed();
    }

    private boolean ready(int tube) {
        return !tubes.getStackInSlot(tube).isEmpty() && loading[tube] == 0;
    }

    public int loadingTicks(int tube) { return tube >= 0 && tube < TUBES ? loading[tube] : 0; }

    public String code() { return code; }

    public void setCode(String code) {
        this.code = LinkCode.sanitize(code);
        changed();
    }

    public DefenseSettings settings() { return settings; }

    public void setSettings(DefenseSettings settings) {
        this.settings = settings;
        changed();
    }

    @Nullable
    public UUID owner() { return owner; }

    public void setOwner(@Nullable UUID owner) {
        this.owner = owner;
        changed();
    }

    /** Degrees the turret has turned from north, clockwise, at {@code partial} of this tick. */
    public float yaw(float partial) { return Mth.rotLerp(partial, lastYaw, yaw); }

    /** Degrees the canister is raised above the horizon at {@code partial} of this tick. */
    public float pitch(float partial) { return Mth.lerp(partial, lastPitch, pitch); }

    /** Where it is turning to, degrees clockwise from north: the bearing of the missile it was ordered at. */
    public float targetYaw() { return targetYaw; }

    /** Game tick of its last launch. */
    public long lastFired() { return lastFired; }

    public Status status() { return status; }

    /** Orders it is still to launch. */
    public int queuedOrders() { return orders.size(); }

    @Override public boolean addToGoggleTooltip(java.util.List<Component> lines, boolean sneak) {
        super.addToGoggleTooltip(lines, sneak);
        int reloading = 0, ready = 0;
        for (int i = 0; i < TUBES; i++) {
            if (ready(i)) ready++;
            else if (loading[i] > 0) reloading++;
        }
        lines.add(Component.literal("    ").append(Component.translatable("block.create_nuclearindustry.interceptor_battery")).withStyle(ChatFormatting.GOLD));
        lines.add(Component.literal("     ").append(Component.translatable("battery.create_nuclearindustry." + status.name().toLowerCase(Locale.ROOT)))
                .withStyle(status == Status.UNPOWERED || status == Status.ON_SHIP || status == Status.EMPTY ? ChatFormatting.RED
                        : status == Status.HOLD ? ChatFormatting.GOLD : ChatFormatting.GREEN));
        lines.add(Component.literal("     ").append(Component.translatable("battery.create_nuclearindustry.rounds", ready, reloading)).withStyle(ChatFormatting.GRAY));
        lines.add(Component.literal("     ").append(LinkCode.complete(code) ? Component.translatable("battery.create_nuclearindustry.network", code)
                : Component.translatable("battery.create_nuclearindustry.no_network")).withStyle(ChatFormatting.DARK_GRAY));
        lines.add(Component.literal("     ").append(Component.translatable(settings.auto() ? "battery.create_nuclearindustry.goggles.auto"
                        : "battery.create_nuclearindustry.goggles.manual",
                Component.translatable("battery.create_nuclearindustry.doctrine." + settings.doctrine().name().toLowerCase(Locale.ROOT))))
                .withStyle(settings.auto() ? ChatFormatting.AQUA : ChatFormatting.GRAY));
        lines.add(Component.literal("     ").append(settings.zoneRadius() == 0
                ? Component.translatable("battery.create_nuclearindustry.goggles.zone_default", Math.round(DefenseConfig.batteryRange))
                : Component.translatable("battery.create_nuclearindustry.goggles.zone", settings.zoneRadius(), settings.zoneX(), settings.zoneZ()))
                .withStyle(ChatFormatting.GRAY));
        return true;
    }

    protected void changed() {
        setChanged();
        if (level instanceof ServerLevel) sendData();
    }

    @Override public float calculateStressApplied() { return lastStressApplied = (float) DefenseConfig.batteryStressImpact; }

    /** Degrees it turns a tick at its speed. */
    private float slewRate() {
        return (float) (DefenseConfig.batterySlewDegreesPerRpm * Math.abs(getSpeed()));
    }

    @Override public void tick() {
        super.tick();
        lastYaw = yaw;
        lastPitch = pitch;
        if (level == null) return;
        if (level.isClientSide) {
            // Turns between the angles it is sent, at the rate the server does, so it never jumps.
            float rate = slewRate();
            yaw = TurretAim.yawStep(yaw, targetYaw, rate);
            pitch = TurretAim.pitchStep(pitch, targetPitch, rate);
            return;
        }
        ServerLevel server = (ServerLevel) level;
        boolean dirty = false;
        for (int i = 0; i < TUBES; i++) if (loading[i] > 0 && --loading[i] == 0) dirty = true;
        Status next;
        if (!operational()) {
            next = onShip() ? Status.ON_SHIP : Status.UNPOWERED;
        } else {
            boolean hold = redstone();
            if (!hold && settings.auto() && LinkCode.complete(code) && server.getGameTime() % DefenseConfig.autoIntervalTicks == 0)
                DefenseCommand.autoEngage(server, RadarNetwork.networkTracks(server, code, worldPosition), java.util.List.of(this), settings,
                        flight -> owner != null && owner.equals(flight.owner()), EngagementSource.AUTO_BATTERY);
            expire(server);
            float before = targetYaw;
            if (!orders.isEmpty()) next = engage(server, orders.peek());
            else next = hold ? Status.HOLD : readyRounds() > 0 ? Status.IDLE : hasLoading() ? Status.RELOADING : Status.EMPTY;
            dirty |= Math.abs(TurretAim.wrap(targetYaw - before)) > 0.5f;
            float rate = slewRate();
            yaw = TurretAim.yawStep(yaw, targetYaw, rate);
            pitch = TurretAim.pitchStep(pitch, targetPitch, rate);
        }
        if (next != status) {
            status = next;
            dirty = true;
        }
        if (dirty) changed();
    }

    private boolean onShip() { return level == null || ShipSpace.onShip(level, worldPosition); }

    /** A signal into any of its eight blocks holds its automatic fire. */
    private boolean redstone() {
        if (level == null) return false;
        for (int x = 0; x < 2; x++) for (int y = 0; y < 2; y++) for (int z = 0; z < 2; z++)
            if (level.hasNeighborSignal(worldPosition.offset(x, y, z))) return true;
        return false;
    }

    /** Whether redstone holds its automatic fire. */
    public boolean held() { return redstone(); }

    private boolean hasLoading() { for (int tick : loading) if (tick > 0) return true; return false; }

    private void expire(ServerLevel level) {
        while (!orders.isEmpty() && (orders.peek().expiry < level.getGameTime() || FlightTracker.get(level).get(orders.peek().target) == null))
            drop(level);
    }

    private void drop(ServerLevel level) {
        EngagementBoard.release(level, orders.remove().target, worldPosition);
        aimed = false;
    }

    /** Turns to the head order's course, worked out again every few ticks, and launches once on it. */
    private Status engage(ServerLevel level, Order order) {
        Flight target = FlightTracker.get(level).get(order.target);
        if (target == null) { drop(level); return Status.TRACKING; }
        if (!aimed || level.getGameTime() - lastSolve >= RESOLVE_TICKS) {
            lastSolve = level.getGameTime();
            Optional<InterceptSolution> course = DefenseCommand.solve(level, target, this, 0);
            if (course.isEmpty()) { drop(level); return Status.TRACKING; }
            TurretAim.Angles aim = TurretAim.fromDirection(course.get().point().sub(launchPoint()));
            targetYaw = aim.yaw();
            targetPitch = aim.pitch();
            aimed = true;
        }
        if (!TurretAim.aligned(yaw, pitch, new TurretAim.Angles(targetYaw, targetPitch), ALIGNED)) return Status.SLEWING;
        // Compared this way round: lastFired starts at the smallest long, and subtracting from it would wrap.
        if (readyTube() < 0 || level.getGameTime() < lastFired + LAUNCH_SPACING) return Status.TRACKING;
        // On its course: the exact one from its tube's mouth, now.
        Optional<InterceptSolution> course = DefenseCommand.solve(level, target, this, 0);
        if (course.isEmpty()) { drop(level); return Status.TRACKING; }
        int tube = readyTube();
        if (DefenseCommand.launch(level, this, target, course.get(), envelope(MissileConfig.flightParams()).design(),
                InterceptorDesigns.BATTERY_SHAPE, null) != null) {
            orders.remove();
            aimed = false;
            tubes.setStackInSlot(tube, ItemStack.EMPTY);
            lastFired = level.getGameTime();
            changed();
        }
        return Status.TRACKING;
    }

    /** The tube its next round leaves from, or -1. */
    private int readyTube() {
        for (int i = 0; i < TUBES; i++) if (ready(i)) return i;
        return -1;
    }

    // ------------------------------------------------------------------ InterceptorShooter

    @Override public BlockPos shooterPos() { return worldPosition; }

    @Override public ShooterType shooterType() { return ShooterType.BATTERY; }

    @Nullable @Override public UUID shooterOwner() { return owner; }

    @Override public boolean operational() {
        return !isRemoved() && !onShip() && Math.abs(getSpeed()) >= MissileConfig.minimumRpm && !isOverStressed();
    }

    /** Ready rounds not yet promised to an order. */
    @Override public int readyRounds() {
        int ready = 0;
        for (int i = 0; i < TUBES; i++) if (ready(i)) ready++;
        return Math.max(0, ready - orders.size());
    }

    @Override public int reactionTicks(V3 direction) {
        float rate = slewRate();
        if (rate <= 0) return 100_000;
        TurretAim.Angles aim = TurretAim.fromDirection(direction);
        return (int) Math.ceil(TurretAim.error(yaw, pitch, aim) / rate) + 2;
    }

    /** The mouth of the tube its next round leaves from, where the canister points now. */
    @Override public V3 launchPoint() {
        int tube = Math.max(0, readyTube());
        float[] offset = BatteryGeometry.TUBE_OFFSETS[tube];
        V3 mouth = TurretAim.rotate(new V3(offset[0], offset[1], -BatteryGeometry.TUBE_MOUTH), yaw, pitch);
        float[] pivot = BatteryGeometry.CANISTER_PIVOT;
        return new V3(worldPosition.getX() + pivot[0], worldPosition.getY() + pivot[1], worldPosition.getZ() + pivot[2]).add(mouth);
    }

    @Override public InterceptorEnvelope envelope(BallisticModel.Params params) {
        double y = worldPosition.getY();
        return new InterceptorEnvelope(InterceptorDesigns.battery(DefenseConfig.batteryDeltaV, DefenseConfig.batteryAcceleration), 0,
                DefenseConfig.batteryEjectSpeed, DefenseConfig.batteryRange, y + DefenseConfig.batteryMinAltitude,
                Math.min(params.atmosphereTop(), y + DefenseConfig.batteryCeiling), false, DefenseConfig.batteryBasePk, 20, 600);
    }

    @Override public double seekerFactor() { return 1.0; }

    @Override public FireResult order(ServerLevel level, UUID target, EngagementSource source) {
        if (!operational()) return FireResult.OFFLINE;
        if (source == EngagementSource.AUTO_BATTERY && redstone()) return FireResult.HOLD;
        if (readyRounds() <= 0) return FireResult.NO_AMMO;
        orders.add(new Order(target, source, level.getGameTime() + DefenseConfig.orderExpireTicks));
        return FireResult.QUEUED;
    }

    // ------------------------------------------------------------------ persistence

    @Override protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        if (owner != null) tag.putUUID("Owner", owner);
        tag.putString("Code", code);
        tag.put("Defense", settings.save());
        tag.put("Tubes", tubes.serializeNBT(registries));
        tag.putIntArray("Loading", loading);
        tag.putFloat("Yaw", yaw);
        tag.putFloat("Pitch", pitch);
        if (clientPacket) {
            tag.putString("Status", status.name().toLowerCase(Locale.ROOT));
            tag.putLong("LastFired", lastFired);
            tag.putFloat("TargetYaw", targetYaw);
            tag.putFloat("TargetPitch", targetPitch);
        }
    }

    @Override protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        code = LinkCode.sanitize(tag.getString("Code"));
        settings = DefenseSettings.load(tag.getCompound("Defense"), DefenseSettings.defaults(worldPosition));
        // Read into a spare handler: deserialising into the tubes would resize them to whatever was saved.
        ItemStackHandler saved = new ItemStackHandler(TUBES);
        saved.deserializeNBT(registries, tag.getCompound("Tubes"));
        for (int i = 0; i < TUBES; i++) tubes.setStackInSlot(i, i < saved.getSlots() ? saved.getStackInSlot(i) : ItemStack.EMPTY);
        int[] savedLoading = tag.getIntArray("Loading");
        for (int i = 0; i < TUBES; i++) loading[i] = i < savedLoading.length ? Math.max(0, savedLoading[i]) : 0;
        float savedYaw = tag.getFloat("Yaw"), savedPitch = tag.contains("Pitch") ? tag.getFloat("Pitch") : 45;
        if (clientPacket) {
            try {
                status = Status.valueOf(tag.getString("Status").toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                status = Status.UNPOWERED;
            }
            lastFired = tag.getLong("LastFired");
            targetYaw = tag.getFloat("TargetYaw");
            targetPitch = tag.contains("TargetPitch") ? tag.getFloat("TargetPitch") : savedPitch;
            // The client keeps turning on its own: only a big disagreement snaps it to the server's angles.
            if (Math.abs(TurretAim.wrap(savedYaw - yaw)) > 10 || Math.abs(savedPitch - pitch) > 10) {
                yaw = lastYaw = savedYaw;
                pitch = lastPitch = savedPitch;
            }
        } else {
            yaw = lastYaw = targetYaw = savedYaw;
            pitch = lastPitch = targetPitch = savedPitch;
        }
    }
}
