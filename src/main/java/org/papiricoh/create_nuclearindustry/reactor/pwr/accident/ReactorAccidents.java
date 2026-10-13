package org.papiricoh.create_nuclearindustry.reactor.pwr.accident;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.reactor.pwr.PwrConfig;
import org.papiricoh.create_nuclearindustry.reactor.pwr.containment.ContainmentMonitorBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.pwr.containment.ContainmentRegistry;

/**
 * What one fission reactor carries of the accident chain: the state of its fuel cladding and its
 * vessel, the steam explosion latch, and the containment building it stands in (found again every
 * few seconds). Everything that leaves the reactor (steam from the relief valve or a break,
 * hydrogen from the cladding, radioactivity) goes through here: into the containment if it is
 * sealed, otherwise out into the open, where small leaks are gathered and laid as fallout in
 * batches.
 */
public final class ReactorAccidents {
    private static final int LOOKUP_INTERVAL = 100;
    private static final int OPEN_RELEASE_INTERVAL = 200;
    /** Activity a leak in the open must reach before it is laid as fallout at once. */
    private static final double IMMEDIATE_RELEASE = 1.0;
    /** Activity of each mB of primary coolant from an intact core, and the extra from fully oxidised cladding. */
    private static final double COOLANT_ACTIVITY_PER_MB = 1.0e-5;
    private static final double DAMAGED_COOLANT_ACTIVITY_PER_MB = 1.0e-3;

    private final CladdingOxidationModel cladding = new CladdingOxidationModel();
    private final VesselIntegrity vessel = new VesselIntegrity();
    private boolean refloodLatched;
    private boolean refloodPending;
    @Nullable
    private BlockPos containmentPos;
    private int lookupTimer = LOOKUP_INTERVAL;
    private int releaseTimer;
    private double openActivity;
    @Nullable
    private BlockPos openReleasePos;
    private double hydrogenMade;
    // what the reactor reports, synced to clients
    private float containmentPressure;
    private float containmentHydrogen;

    public CladdingOxidationModel cladding() {
        return cladding;
    }

    public VesselIntegrity vessel() {
        return vessel;
    }

    public static CladdingOxidationModel.Params claddingParams() {
        return new CladdingOxidationModel.Params(PwrConfig.oxidationTemperature, PwrConfig.oxidationRate, PwrConfig.zirconiumPerRod,
                PwrConfig.oxidationHeat);
    }

    /** Activity per mB of coolant that escapes, rising as the cladding fails. */
    public double coolantActivityPerMb(int rods) {
        return COOLANT_ACTIVITY_PER_MB + cladding.damage(rods, PwrConfig.zirconiumPerRod) * DAMAGED_COOLANT_ACTIVITY_PER_MB;
    }

    /**
     * Once a tick on the server: finds the containment again now and then, lays gathered open-air
     * leaks as fallout and refreshes the containment readings.
     */
    public void tick(ServerLevel level, BoundingBox reactor) {
        if (++lookupTimer >= LOOKUP_INTERVAL) {
            lookupTimer = 0;
            ContainmentMonitorBlockEntity found = ContainmentRegistry.enclosing(level, reactor);
            containmentPos = found == null ? null : found.getBlockPos();
        }
        if (++releaseTimer >= OPEN_RELEASE_INTERVAL) {
            releaseTimer = 0;
            flushOpenRelease(level);
        }
        ContainmentMonitorBlockEntity containment = containment(level);
        containmentPressure = containment == null ? 0.0f : containment.getPressure();
        containmentHydrogen = containment == null ? 0.0f : containment.getHydrogen();
    }

    /** The sealed containment the reactor stands in, or null. */
    @Nullable
    public ContainmentMonitorBlockEntity containment(Level level) {
        if (containmentPos == null || !level.isLoaded(containmentPos)) {
            return null;
        }
        return level.getBlockEntity(containmentPos) instanceof ContainmentMonitorBlockEntity monitor && monitor.sealed() ? monitor : null;
    }

    /** Looks for the containment right now, for events that cannot wait for the next lookup. */
    @Nullable
    public ContainmentMonitorBlockEntity findContainment(ServerLevel level, BoundingBox reactor) {
        ContainmentMonitorBlockEntity found = ContainmentRegistry.enclosing(level, reactor);
        containmentPos = found == null ? null : found.getBlockPos();
        return found;
    }

    /** Coolant flashing to steam out of the primary: into the containment, or out with its activity. */
    public void releaseCoolant(ServerLevel level, BlockPos at, int mb, double temperatureK, double activityPerMb) {
        if (mb <= 0) {
            return;
        }
        ContainmentMonitorBlockEntity containment = containment(level);
        if (containment != null) {
            containment.addSteam(mb * PwrConfig.molesPerMb, temperatureK);
        }
        releaseActivity(level, at, mb * activityPerMb);
    }

    /** Hydrogen from the cladding: into the containment, or lost to the open air. */
    public void releaseHydrogen(ServerLevel level, double moles, double temperatureK) {
        if (moles <= 0) {
            return;
        }
        hydrogenMade += moles;
        ContainmentMonitorBlockEntity containment = containment(level);
        if (containment != null) {
            containment.addHydrogen(moles, temperatureK);
        }
    }

    /** Radioactivity escaping the reactor: kept airborne in the containment, or laid as fallout around {@code at}. */
    public void releaseActivity(ServerLevel level, BlockPos at, double activity) {
        if (!(activity > 0)) {
            return;
        }
        ContainmentMonitorBlockEntity containment = containment(level);
        if (containment != null) {
            containment.addActivity(activity);
            return;
        }
        openActivity += activity;
        openReleasePos = at.immutable();
        if (openActivity >= IMMEDIATE_RELEASE) {
            flushOpenRelease(level);
        }
    }

    private void flushOpenRelease(ServerLevel level) {
        if (openActivity > 0 && openReleasePos != null) {
            ReleasePath.UNFILTERED.release(level, openReleasePos, openActivity);
        }
        openActivity = 0;
    }

    /**
     * Water pumped into a core hotter than the steam explosion temperature while it was dried out.
     * Latches until the core cools well below that, so one reflooding explodes once.
     */
    public void noteReflood(double coreTemperature, boolean wasDry) {
        if (PwrConfig.steamExplosions && wasDry && !refloodLatched && coreTemperature > PwrConfig.steamExplosionTemperature) {
            refloodLatched = true;
            refloodPending = true;
        }
    }

    /** Whether a reflood explosion is due; clears it. Also releases the latch once the core has cooled. */
    public boolean takeReflood(double coreTemperature) {
        if (refloodLatched && !refloodPending && coreTemperature < PwrConfig.steamExplosionTemperature - 300.0) {
            refloodLatched = false;
        }
        boolean due = refloodPending;
        refloodPending = false;
        return due;
    }

    /** Pressure of the containment around the reactor, bar, or 0 without one. */
    public float containmentPressure() {
        return containmentPressure;
    }

    public float containmentHydrogen() {
        return containmentHydrogen;
    }

    /** Hydrogen the cladding has made since the reactor was built, mol. */
    public double hydrogenMade() {
        return hydrogenMade;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("Oxidized", cladding.oxidized());
        tag.putBoolean("Breached", vessel.breached());
        tag.putInt("Overpressure", vessel.overpressureTicks());
        tag.putBoolean("RefloodLatched", refloodLatched);
        tag.putDouble("Hydrogen", hydrogenMade);
        tag.putFloat("ContainmentPressure", containmentPressure);
        tag.putFloat("ContainmentHydrogen", containmentHydrogen);
        return tag;
    }

    public void load(CompoundTag tag) {
        cladding.restore(tag.getDouble("Oxidized"));
        vessel.restore(tag.getBoolean("Breached"), tag.getInt("Overpressure"));
        refloodLatched = tag.getBoolean("RefloodLatched");
        hydrogenMade = Math.max(0, tag.getDouble("Hydrogen"));
        containmentPressure = tag.getFloat("ContainmentPressure");
        containmentHydrogen = tag.getFloat("ContainmentHydrogen");
    }
}
