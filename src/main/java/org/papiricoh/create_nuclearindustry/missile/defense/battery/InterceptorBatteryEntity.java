package org.papiricoh.create_nuclearindustry.missile.defense.battery;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
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

import java.util.Locale;
import java.util.UUID;

/**
 * An interceptor battery: a turret on a 2×2×2 base that turns its four-tube canister to the missiles it is ordered to meet and
 * launches at them. It hears of missiles from the radars on its link code or from the command tables it is linked to.
 */
public class InterceptorBatteryEntity extends KineticBlockEntity implements InterceptorShooter {
    public static final int TUBES = 4;

    /** What it is doing, for its screen, goggles and table; sent to clients by name. */
    public enum Status { UNPOWERED, IDLE, TRACKING, SLEWING, RELOADING, EMPTY, HOLD, ON_SHIP }

    protected final ItemStackHandler tubes = new ItemStackHandler(TUBES) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    /** Ticks each tube still needs before its round is ready. */
    protected final int[] loading = new int[TUBES];
    protected String code = "";
    protected DefenseSettings settings;
    @Nullable
    protected UUID owner;
    protected float yaw, pitch = 45, lastYaw, lastPitch = 45;
    protected long lastFired = Long.MIN_VALUE;
    protected Status status = Status.UNPOWERED;

    public InterceptorBatteryEntity(BlockPos pos, BlockState state) {
        super(DefenseRegistry.BATTERY_ENTITY.get(), pos, state);
        settings = DefenseSettings.defaults(pos);
    }

    public IItemHandler tubes() { return tubes; }

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

    /** Game tick of its last launch. */
    public long lastFired() { return lastFired; }

    public Status status() { return status; }

    protected void changed() {
        setChanged();
        if (level instanceof ServerLevel) sendData();
    }

    @Override public float calculateStressApplied() { return lastStressApplied = (float) DefenseConfig.batteryStressImpact; }

    @Override public void tick() {
        super.tick();
        lastYaw = yaw;
        lastPitch = pitch;
    }

    // ------------------------------------------------------------------ InterceptorShooter

    @Override public BlockPos shooterPos() { return worldPosition; }

    @Override public ShooterType shooterType() { return ShooterType.BATTERY; }

    @Nullable @Override public UUID shooterOwner() { return owner; }

    @Override public boolean operational() { return false; }

    @Override public int readyRounds() { return 0; }

    @Override public int reactionTicks(V3 direction) { return 0; }

    @Override public V3 launchPoint() {
        return new V3(worldPosition.getX() + 1.0, worldPosition.getY() + 2.0, worldPosition.getZ() + 1.0);
    }

    @Override public InterceptorEnvelope envelope(BallisticModel.Params params) {
        double y = worldPosition.getY();
        return new InterceptorEnvelope(InterceptorDesigns.battery(DefenseConfig.batteryDeltaV, DefenseConfig.batteryAcceleration), 0,
                DefenseConfig.batteryEjectSpeed, DefenseConfig.batteryRange, y + DefenseConfig.batteryMinAltitude,
                Math.min(params.atmosphereTop(), y + DefenseConfig.batteryCeiling), false, DefenseConfig.batteryBasePk, 20, 600);
    }

    @Override public double seekerFactor() { return 1.0; }

    @Override public FireResult order(ServerLevel level, UUID target, EngagementSource source) { return FireResult.OFFLINE; }

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
        }
    }

    @Override protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        code = LinkCode.sanitize(tag.getString("Code"));
        settings = DefenseSettings.load(tag.getCompound("Defense"), DefenseSettings.defaults(worldPosition));
        ItemStackHandler saved = new ItemStackHandler(TUBES);
        saved.deserializeNBT(registries, tag.getCompound("Tubes"));
        for (int i = 0; i < TUBES; i++) tubes.setStackInSlot(i, i < saved.getSlots() ? saved.getStackInSlot(i) : net.minecraft.world.item.ItemStack.EMPTY);
        int[] savedLoading = tag.getIntArray("Loading");
        for (int i = 0; i < TUBES; i++) loading[i] = i < savedLoading.length ? Math.max(0, savedLoading[i]) : 0;
        yaw = tag.getFloat("Yaw");
        pitch = tag.contains("Pitch") ? tag.getFloat("Pitch") : 45;
        if (clientPacket) {
            try {
                status = Status.valueOf(tag.getString("Status").toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                status = Status.UNPOWERED;
            }
            lastFired = tag.getLong("LastFired");
        }
    }
}
