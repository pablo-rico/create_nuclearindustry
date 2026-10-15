package org.papiricoh.create_nuclearindustry.maintenance.incident;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.papiricoh.create_nuclearindustry.maintenance.MaintenanceConfig;
import org.papiricoh.create_nuclearindustry.maintenance.physics.InesScale;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The server's log of plant incidents, newest first, rated on the INES scale: SCRAMs, stuck rods,
 * tube ruptures, damaged and melted cores, earthquakes, detonations and drills. Kept with the
 * overworld's saved data, so it covers every dimension. Players near an incident of INES 1 or more
 * are told at once. Off with {@code maintenance.incidentLog.enabled}.
 */
public final class IncidentLog extends SavedData {
    private static final String NAME = "create_nuclearindustry_incidents";
    /** Blocks within which players hear of a new incident. */
    public static final double NOTICE_RADIUS = 128.0;

    private final List<Incident> incidents = new ArrayList<>();

    public static IncidentLog get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(IncidentLog::new, IncidentLog::load, null), NAME);
    }

    /** Records an incident at {@code pos}: its kind's own INES level, or {@code ines} if higher. */
    public static void record(Level level, BlockPos pos, IncidentType type, int ines, String detail) {
        if (!(level instanceof ServerLevel server) || !MaintenanceConfig.incidentLogOn()) {
            return;
        }
        int rated = Math.max(type.ines(), ines);
        // Logged where it happened in the world, also aboard a ship.
        pos = org.papiricoh.create_nuclearindustry.integration.sable.ShipSpace.worldBlock(level, pos);
        Incident incident = new Incident(server.getServer().overworld().getGameTime(), server.dimension().location().toString(),
                pos.immutable(), type, rated, detail == null ? "" : detail);
        get(server.getServer()).add(incident);
        if (rated >= 1 || type == IncidentType.DRILL || type == IncidentType.EARTHQUAKE) {
            Component line = incident.line();
            for (ServerPlayer player : server.players()) {
                if (player.blockPosition().distSqr(pos) <= NOTICE_RADIUS * NOTICE_RADIUS) {
                    player.sendSystemMessage(line);
                }
            }
        }
    }

    public static void record(Level level, BlockPos pos, IncidentType type) {
        record(level, pos, type, InesScale.NONE, "");
    }

    private void add(Incident incident) {
        incidents.add(0, incident);
        while (incidents.size() > Math.max(1, MaintenanceConfig.incidentLogSize)) {
            incidents.remove(incidents.size() - 1);
        }
        setDirty();
    }

    /** Newest first. */
    public List<Incident> incidents() {
        return Collections.unmodifiableList(incidents);
    }

    /** The worst INES level among the newest {@code count} incidents, or {@link InesScale#NONE}. */
    public int worst(int count) {
        int worst = InesScale.NONE;
        for (int i = 0; i < Math.min(count, incidents.size()); i++) {
            worst = Math.max(worst, incidents.get(i).ines());
        }
        return worst;
    }

    /** Game tests: empties the log. */
    public void clear() {
        incidents.clear();
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Incident incident : incidents) {
            list.add(incident.save());
        }
        tag.put("Incidents", list);
        return tag;
    }

    private static IncidentLog load(CompoundTag tag, HolderLookup.Provider registries) {
        IncidentLog log = new IncidentLog();
        for (Tag entry : tag.getList("Incidents", Tag.TAG_COMPOUND)) {
            log.incidents.add(Incident.load((CompoundTag) entry));
        }
        return log;
    }
}
