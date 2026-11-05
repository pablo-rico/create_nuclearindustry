package org.papiricoh.create_nuclearindustry.dyson.ejector;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.papiricoh.create_nuclearindustry.advancement.NuclearAdvancements;
import org.papiricoh.create_nuclearindustry.advancement.NuclearEvent;
import org.papiricoh.create_nuclearindustry.dyson.DysonConfig;
import org.papiricoh.create_nuclearindustry.dyson.DysonFxHooks;
import org.papiricoh.create_nuclearindustry.dyson.DysonRegistry;
import org.papiricoh.create_nuclearindustry.dyson.swarm.DysonSwarm;
import org.papiricoh.create_nuclearindustry.dyson.swarm.OrbitShape;
import org.papiricoh.create_nuclearindustry.dyson.swarm.SunPath;
import org.papiricoh.create_nuclearindustry.integration.sable.ShipSpace;

import java.util.List;

/**
 * An EM rail ejector: a magnetic launcher, driven by a shaft from below, that charges its rails and throws solar sails one at a
 * time into its orbit of the Dyson swarm. It needs the open sky over it and the sun over the horizon, so it only works by day, and
 * charges faster the faster it turns ({@link DysonConfig#ejectorTicksPerSail} at 256 RPM). Funnels and hoppers can fill its
 * magazine. Each launch is told to every client within {@link EjectorLaunchPayload#RANGE} blocks ({@link DysonFxHooks#ejectorFired}).
 */
public class EmRailEjectorBlockEntity extends KineticBlockEntity {
    public static final int SLOTS = 16;

    private final ItemStackHandler magazine = new ItemStackHandler(SLOTS) {
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return stack.is(DysonRegistry.SOLAR_SAIL.get());
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private int orbit;
    /** Share of the next launch charged, 0 to 1. */
    private float charge;
    private EjectorStatus status = EjectorStatus.NO_SAILS;
    private boolean skyClear = true;
    /** Client: the game time of the last launch seen, and the charge when last told. */
    private long lastLaunch = Long.MIN_VALUE;
    private float syncedCharge;
    private long syncedAt;

    public EmRailEjectorBlockEntity(BlockPos pos, BlockState state) {
        super(DysonRegistry.EJECTOR_ENTITY.get(), pos, state);
    }

    public ItemStackHandler magazine() {
        return magazine;
    }

    public int orbit() {
        return orbit;
    }

    public void setOrbit(int orbit) {
        if (!OrbitShape.validIndex(orbit) || orbit == this.orbit) return;
        this.orbit = orbit;
        setChanged();
        sendData();
    }

    public EjectorStatus status() {
        return status;
    }

    /** Sails in its magazine. */
    public int sailsLoaded() {
        int sails = 0;
        for (int i = 0; i < SLOTS; i++) sails += magazine.getStackInSlot(i).getCount();
        return sails;
    }

    /** Charge per tick at its present speed. */
    public float chargeRate() {
        return Math.abs(getSpeed()) / 256f / Math.max(1, DysonConfig.ejectorTicksPerSail);
    }

    /** Ticks between launches at its present speed, or -1 standing still. */
    public int ticksPerLaunch() {
        float rate = chargeRate();
        return rate <= 0 ? -1 : (int) Math.ceil(1 / rate);
    }

    /**
     * The rails' charge, 0 to 1, {@code partial} ticks into this one. On the client it runs on from what the server last said at
     * the present speed, so the gauge and the rails' glow fill smoothly between updates.
     */
    public float charge(float partial) {
        if (level == null || !level.isClientSide) return charge;
        if (status != EjectorStatus.CHARGING) return syncedCharge;
        return Math.min(1, syncedCharge + chargeRate() * (float) (level.getGameTime() + partial - syncedAt));
    }

    /** Client: the game time of the last launch this client saw, or {@link Long#MIN_VALUE}. */
    public long lastLaunch() {
        return lastLaunch;
    }

    @Override
    public float calculateStressApplied() {
        return lastStressApplied = (float) DysonConfig.ejectorStress;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level instanceof ServerLevel server) || isVirtual()) return;
        if (server.getGameTime() % 20 == 0) skyClear = server.canSeeSky(ShipSpace.worldBlock(server, worldPosition).above());
        EjectorStatus now = evaluate(server);
        if (now != status) {
            status = now;
            sendData();
        }
        if (status != EjectorStatus.CHARGING) return;
        charge += chargeRate();
        if (charge < 1) return;
        DysonSwarm swarm = DysonSwarm.get(server);
        long before = swarm.totalSails();
        if (!swarm.launch(orbit, server.getGameTime())) return;
        charge = Math.max(0, charge - 1);
        magazine.extractItem(firstLoaded(), 1, false);
        EjectorLaunchPayload.send(server, worldPosition, orbit);
        NuclearAdvancements.awardNearby(server, worldPosition, NuclearAdvancements.WITNESS_RADIUS, NuclearEvent.SAIL_LAUNCHED);
        long after = before + 1;
        if (before < DysonSwarm.MILESTONE_SMALL && after >= DysonSwarm.MILESTONE_SMALL)
            NuclearAdvancements.awardNearby(server, worldPosition, NuclearAdvancements.WITNESS_RADIUS, NuclearEvent.SWARM_HUNDRED);
        if (before < DysonSwarm.MILESTONE_LARGE && after >= DysonSwarm.MILESTONE_LARGE)
            NuclearAdvancements.awardNearby(server, worldPosition, NuclearAdvancements.WITNESS_RADIUS, NuclearEvent.SWARM_THOUSAND);
        setChanged();
        sendData();
    }

    /** What it would do now: charge, or the first thing stopping it. */
    private EjectorStatus evaluate(ServerLevel server) {
        if (!DysonSwarm.underSwarm(server)) return EjectorStatus.WRONG_DIMENSION;
        if (isOverStressed()) return EjectorStatus.OVERSTRESSED;
        if (Math.abs(getSpeed()) < DysonConfig.ejectorMinRpm) return EjectorStatus.TOO_SLOW;
        if (!skyClear) return EjectorStatus.NO_SKY;
        if (SunPath.sinElevation(server.getTimeOfDay(1)) < DysonConfig.minSinElevation()) return EjectorStatus.SUN_DOWN;
        DysonSwarm swarm = DysonSwarm.get(server);
        if (!swarm.shape(orbit).enabled()) return EjectorStatus.ORBIT_CLOSED;
        if (swarm.full()) return EjectorStatus.SWARM_FULL;
        if (firstLoaded() < 0) return EjectorStatus.NO_SAILS;
        return EjectorStatus.CHARGING;
    }

    private int firstLoaded() {
        for (int i = 0; i < SLOTS; i++) if (!magazine.getStackInSlot(i).isEmpty()) return i;
        return -1;
    }

    /** Client: it launched a sail into {@code orbit} this tick. */
    public void launched(int orbit) {
        if (level == null || !level.isClientSide) return;
        lastLaunch = level.getGameTime();
        syncedCharge = 0;
        syncedAt = lastLaunch;
        DysonFxHooks.ejectorFired.accept(this, orbit);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean sneaking) {
        super.addToGoggleTooltip(tooltip, sneaking);
        tooltip.add(Component.literal("    ").append(getBlockState().getBlock().getName()).withStyle(ChatFormatting.GOLD));
        for (Component line : statusLines())
            tooltip.add(Component.literal("     ").append(line).withStyle(ChatFormatting.GRAY));
        return true;
    }

    /** Its state in words: what it is doing, its orbit, sails loaded and launch rate. */
    public List<Component> statusLines() {
        Component state = status == EjectorStatus.TOO_SLOW ? Component.translatable(status.key(), DysonConfig.ejectorMinRpm)
                : Component.translatable(status.key());
        int every = ticksPerLaunch();
        return List.of(state,
                Component.translatable("create_nuclearindustry.ejector.orbit", orbit + 1),
                Component.translatable("create_nuclearindustry.ejector.loaded", sailsLoaded()),
                every < 0 ? Component.translatable("create_nuclearindustry.ejector.idle")
                        : Component.translatable("create_nuclearindustry.ejector.rate", String.format(java.util.Locale.ROOT, "%.1f", every / 20f)));
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put("Magazine", magazine.serializeNBT(registries));
        tag.putInt("Orbit", orbit);
        tag.putFloat("Charge", charge);
        if (clientPacket) tag.putInt("Status", status.ordinal());
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        magazine.deserializeNBT(registries, tag.getCompound("Magazine"));
        orbit = OrbitShape.validIndex(tag.getInt("Orbit")) ? tag.getInt("Orbit") : 0;
        charge = Math.max(0, Math.min(1, tag.getFloat("Charge")));
        if (clientPacket) {
            status = EjectorStatus.byOrdinal(tag.getInt("Status"));
            syncedCharge = charge;
            syncedAt = level != null ? level.getGameTime() : 0;
        }
    }
}
