package org.papiricoh.create_nuclearindustry.maintenance;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import org.papiricoh.create_nuclearindustry.maintenance.incident.IncidentLog;
import org.papiricoh.create_nuclearindustry.maintenance.incident.IncidentType;
import org.papiricoh.create_nuclearindustry.maintenance.physics.ComponentHealth;
import org.papiricoh.create_nuclearindustry.maintenance.physics.InesScale;
import org.papiricoh.create_nuclearindustry.maintenance.physics.WearPart;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * The wearing parts of a fission reactor and its refuelling outage, kept out of the reactor block
 * entity, which only delegates here.
 * <ul>
 *   <li>Control rod drives wear with the power and at every SCRAM; a worn one may jam at a SCRAM,
 *       leaving its channel's rods where they were until it is repaired.</li>
 *   <li>The relief valve wears at every lift and sticks open the more often the more worn.</li>
 *   <li>The outage: a formed reactor, shut down cold with every rod in. Only then does it take fresh
 *       fuel ({@code maintenance.requireRefuellingOutage}) or accept repairs.</li>
 *   <li>The incident log hears of automatic and seismic SCRAMs, stuck rods, a stuck relief valve and
 *       a damaged core.</li>
 * </ul>
 */
public final class ReactorMaintenance {
    public static final String KEY = "create_nuclearindustry.maintenance.";
    /** Core temperature of a cold shutdown, °C. */
    public static final double COLD = 100.0;
    /** Rod insertion that counts as every rod in. */
    public static final double ALL_RODS_IN = 0.99;

    private final ComponentHealth rodDrives = new ComponentHealth();
    private final ComponentHealth reliefValve = new ComponentHealth();
    /** The channel whose drive jammed, or -1, and the insertion it froze at. */
    private int stuckChannel = -1;
    private double stuckAt;
    private boolean scramSeen;
    private boolean coreDamageLogged;
    private boolean reliefStuckLogged;
    /** The next SCRAM was set off by an earthquake. */
    private boolean seismicPending;

    /** What the reactor tells this each tick it is formed. */
    public record Readings(double powerFraction, boolean scram, boolean autoScram, double coreDamage, boolean reliefStuck,
                           double[] channelInsertions) {}

    public ComponentHealth rodDrives() {
        return rodDrives;
    }

    public ComponentHealth reliefValve() {
        return reliefValve;
    }

    public int stuckChannel() {
        return stuckChannel;
    }

    /** One tick of a formed reactor: wear, the SCRAM's rod check and the incident log. */
    public void tick(Level level, BlockPos pos, Readings r, Consumer<Component> alert) {
        if (MaintenanceConfig.wearOn()) {
            rodDrives.wear(MaintenanceConfig.params(WearPart.ROD_DRIVES), Math.max(0, Math.min(1.5, r.powerFraction())), 1);
        }
        if (r.scram() && !scramSeen) {
            onScram(level, pos, r, alert);
        }
        scramSeen = r.scram();
        if (!r.scram()) {
            seismicPending = false;
        }
        if (r.reliefStuck() && !reliefStuckLogged) {
            reliefStuckLogged = true;
            IncidentLog.record(level, pos, IncidentType.RELIEF_VALVE_STUCK);
        } else if (!r.reliefStuck()) {
            reliefStuckLogged = false;
        }
        if (!coreDamageLogged && r.coreDamage() >= InesScale.FUEL_DAMAGE) {
            coreDamageLogged = true;
            IncidentLog.record(level, pos, IncidentType.CORE_DAMAGE, InesScale.core(r.coreDamage(), false, 0),
                    String.format(Locale.ROOT, "%.0f%%", r.coreDamage() * 100));
        }
    }

    private void onScram(Level level, BlockPos pos, Readings r, Consumer<Component> alert) {
        if (seismicPending) {
            IncidentLog.record(level, pos, IncidentType.SEISMIC_SCRAM);
        } else if (r.autoScram()) {
            IncidentLog.record(level, pos, IncidentType.SCRAM);
        }
        ComponentHealth.Params p = MaintenanceConfig.params(WearPart.ROD_DRIVES);
        if (MaintenanceConfig.wearOn()) {
            rodDrives.shock(p, 1.0);
        }
        double[] channels = r.channelInsertions();
        if (MaintenanceConfig.failuresOn() && stuckChannel < 0 && channels != null && channels.length > 0
                && rodDrives.check(p, level.random.nextDouble())) {
            jam(level, pos, channels, level.random.nextInt(channels.length), alert);
        }
    }

    /** A drive jams: its channel keeps the insertion it has now. */
    private void jam(Level level, BlockPos pos, double[] channels, int channel, Consumer<Component> alert) {
        stuckChannel = channel;
        stuckAt = channels[channel];
        rodDrives.fail();
        alert.accept(Component.translatable(KEY + "stuck_rod", channel + 1, Math.round(stuckAt * 100)).withStyle(ChatFormatting.RED));
        IncidentLog.record(level, pos, IncidentType.STUCK_ROD, InesScale.NONE, "#" + (channel + 1));
    }

    /** Forces a stuck rod in {@code channel} (commands, tests). */
    public void forceStuckRod(Level level, BlockPos pos, double[] channels, int channel, Consumer<Component> alert) {
        if (channels != null && channels.length > 0) {
            jam(level, pos, channels, Math.floorMod(channel, channels.length), alert);
        }
    }

    /** The channel insertions the core sees: a jammed channel keeps its own, whatever the rods do. */
    public double[] applyStuck(double[] channels) {
        if (stuckChannel < 0 || channels == null || stuckChannel >= channels.length) {
            return channels;
        }
        double[] held = channels.clone();
        held[stuckChannel] = stuckAt;
        return held;
    }

    /** The relief valve lifted. */
    public void onReliefLift() {
        if (MaintenanceConfig.wearOn()) {
            reliefValve.shock(MaintenanceConfig.params(WearPart.RELIEF_VALVE), 1.0);
        }
    }

    /** Multiplier on the relief valve's chance of sticking open. */
    public double reliefStickScale() {
        return MaintenanceConfig.wearOn() ? WearPart.valveStickScale(reliefValve.health()) : 1.0;
    }

    /** An earthquake shook the reactor: its parts take a transient, and a SCRAM it causes is logged as seismic. */
    public void shake(double severity, boolean scrams) {
        if (MaintenanceConfig.wearOn()) {
            rodDrives.shock(MaintenanceConfig.params(WearPart.ROD_DRIVES), severity);
            reliefValve.shock(MaintenanceConfig.params(WearPart.RELIEF_VALVE), severity * 0.5);
        }
        if (scrams) {
            seismicPending = true;
        }
    }

    /** A formed reactor shut down cold with every rod in: a refuelling outage. */
    public static boolean inOutage(boolean formed, boolean running, double coreTemperature, double rodInsertion) {
        return formed && !running && coreTemperature < COLD && rodInsertion >= ALL_RODS_IN;
    }

    /** Whether fresh fuel may go in now: always, unless the config asks for an outage and the reactor is not in one. */
    public static boolean refuelAllowed(boolean inOutage) {
        return inOutage || !MaintenanceConfig.outageRequired();
    }

    /** Maintenance: gives the parts back {@code amount} of health and frees a stuck rod. Returns whether anything needed it. */
    public boolean repair(double amount) {
        boolean needed = rodDrives.health() < 1 || reliefValve.health() < 1 || rodDrives.failed() || stuckChannel >= 0;
        rodDrives.repair(amount);
        reliefValve.repair(amount);
        stuckChannel = -1;
        coreDamageLogged = false;
        return needed;
    }

    /** The worse of the two parts' health, 0-1, for the control room. */
    public double worstHealth() {
        return rodDrives.failed() || reliefValve.failed() ? 0 : Math.min(rodDrives.health(), reliefValve.health());
    }

    /** The radiograph's and the goggles' lines on the parts. */
    public List<Component> lines() {
        List<Component> lines = new ArrayList<>();
        lines.add(part("rod_drives", rodDrives));
        lines.add(part("relief_valve", reliefValve));
        if (stuckChannel >= 0) {
            lines.add(Component.translatable(KEY + "stuck_channel", stuckChannel + 1, Math.round(stuckAt * 100))
                    .withStyle(ChatFormatting.RED));
        }
        return lines;
    }

    public static Component part(String id, ComponentHealth part) {
        ComponentHealth.Condition condition = part.condition();
        ChatFormatting style = switch (condition) {
            case GOOD -> ChatFormatting.GREEN;
            case WORN -> ChatFormatting.YELLOW;
            case POOR -> ChatFormatting.GOLD;
            case FAILED -> ChatFormatting.RED;
        };
        return Component.translatable(KEY + "part", Component.translatable(KEY + "part." + id),
                Component.translatable(KEY + "condition." + condition.id()), Math.round(part.health() * 100)).withStyle(style);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("RodDrives", rodDrives.health());
        tag.putBoolean("RodDrivesFailed", rodDrives.failed());
        tag.putDouble("ReliefValve", reliefValve.health());
        tag.putBoolean("ReliefValveFailed", reliefValve.failed());
        tag.putInt("StuckChannel", stuckChannel);
        tag.putDouble("StuckAt", stuckAt);
        tag.putBoolean("CoreDamageLogged", coreDamageLogged);
        return tag;
    }

    public void load(CompoundTag tag) {
        rodDrives.set(tag.contains("RodDrives") ? tag.getDouble("RodDrives") : 1.0, tag.getBoolean("RodDrivesFailed"));
        reliefValve.set(tag.contains("ReliefValve") ? tag.getDouble("ReliefValve") : 1.0, tag.getBoolean("ReliefValveFailed"));
        stuckChannel = tag.contains("StuckChannel") ? tag.getInt("StuckChannel") : -1;
        stuckAt = tag.getDouble("StuckAt");
        coreDamageLogged = tag.getBoolean("CoreDamageLogged");
    }
}
