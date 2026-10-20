package org.papiricoh.create_nuclearindustry.missile.radar;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.create_nuclearindustry.controlroom.MonitorReading;
import org.papiricoh.create_nuclearindustry.controlroom.PlantReadable;
import org.papiricoh.create_nuclearindustry.controlroom.TargetKind;
import org.papiricoh.create_nuclearindustry.controlroom.link.LinkCode;
import org.papiricoh.create_nuclearindustry.integration.sable.ShipSpace;
import org.papiricoh.create_nuclearindustry.missile.MissileConfig;
import org.papiricoh.create_nuclearindustry.missile.physics.V3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * A missile radar: a turning antenna that tracks flights in reach and above its horizon ({@link RadarModel}), shares them with the
 * radars on its link code ({@link RadarNetwork}) and watches a defended zone. Its redstone output is on while a tracked flight is
 * predicted to come down in the zone (any flight at all with no zone set); acknowledging the alarm only silences its klaxon. Its
 * counts reach clients for goggles, screens and scopes, and the control room reads it as a {@link TargetKind#RADAR}.
 */
public class RadarBlockEntity extends KineticBlockEntity implements PlantReadable {
    /** Largest defended zone radius, blocks. */
    public static final int MAX_ZONE = 20000;

    private String code = "";
    private int zoneX, zoneZ, zoneRadius;
    /** Threats acknowledged by an operator; forgotten once they stop being threats. */
    private final Set<UUID> acked = new HashSet<>();
    private boolean signal;
    private List<RadarTrack> tracks = List.of();
    private List<RadarTrack> threats = List.of();
    // What clients are told.
    private int status, trackCount, threatCount, soonest;
    private boolean unacked;

    public RadarBlockEntity(BlockPos pos, BlockState state) {
        super(RadarRegistry.ENTITY.get(), pos, state);
        zoneX = pos.getX();
        zoneZ = pos.getZ();
    }

    public boolean stationary() {
        return !isVirtual() && level != null && level.hasChunkAt(worldPosition) && level.getBlockEntity(worldPosition) == this
                && !ShipSpace.onShip(level, worldPosition);
    }

    public boolean operational() {
        return !isRemoved() && stationary() && Math.abs(getSpeed()) >= MissileConfig.minimumRpm && !isOverStressed();
    }

    /** Whether its redstone output is on: a working radar tracking a threat to its zone. */
    public boolean alarm() {
        return level != null && level.isClientSide ? signal : operational() && signal;
    }

    /** 2 on a ship, 1 working, 0 not turning fast enough; on clients as last sent. */
    public int status() {
        if (level != null && level.isClientSide) return status;
        return !stationary() ? 2 : operational() ? 1 : 0;
    }

    /** Where its antenna sees from. */
    public V3 antenna() {
        return new V3(worldPosition.getX() + 0.5, worldPosition.getY() + 1.5, worldPosition.getZ() + 0.5);
    }

    public String code() { return code; }
    public int zoneX() { return zoneX; }
    public int zoneZ() { return zoneZ; }
    public int zoneRadius() { return zoneRadius; }

    /** Tracks it shows, soonest impact first (server). */
    public List<RadarTrack> tracks() { return tracks; }

    /** Tracks threatening its zone (server). */
    public List<RadarTrack> threats() { return threats; }

    public int trackCount() { return level != null && level.isClientSide ? trackCount : tracks.size(); }
    public int threatCount() { return level != null && level.isClientSide ? threatCount : threats.size(); }

    /** A threat no one has acknowledged: the klaxon sounds. */
    public boolean alarmUnacked() { return level != null && level.isClientSide ? unacked : threats.stream().anyMatch(t -> !acked.contains(t.id())); }

    /** Seconds to the soonest impact it tracks, or 0. */
    public int soonestSeconds() {
        if (level != null && level.isClientSide) return soonest;
        return tracks.isEmpty() ? 0 : (tracks.get(0).eta() + 19) / 20;
    }

    /** Its reach for a bare rocket body at its speed, blocks; 0 when it sees the whole dimension. */
    public double reach() {
        double reach = RadarModel.range(MissileConfig.radarBaseRange, getSpeed(), MissileConfig.minimumRpm, 1);
        return Double.isFinite(reach) ? reach : 0;
    }

    public void setCode(String code) {
        this.code = LinkCode.sanitize(code);
        changed();
    }

    public void setZone(int x, int z, int radius) {
        zoneX = x;
        zoneZ = z;
        zoneRadius = Math.max(0, Math.min(MAX_ZONE, radius));
        changed();
    }

    /** Silences the klaxon for every threat it shows now; the redstone output stays on while they last. */
    public void acknowledge() {
        threats.forEach(t -> acked.add(t.id()));
        changed();
    }

    private void changed() {
        setChanged();
        if (level instanceof ServerLevel) {
            refresh();
            sendData();
        }
    }

    @Override public float calculateStressApplied() { return lastStressApplied = (float) MissileConfig.stressImpact; }

    @Override public void initialize() {
        super.initialize();
        RadarNetwork.join(this);
    }

    @Override public void invalidate() {
        RadarNetwork.leave(this);
        super.invalidate();
    }

    @Override public void tick() {
        super.tick();
        if (!(level instanceof ServerLevel server)) return;
        if (operational()) RadarNetwork.update(server);
        if (level.getGameTime() % 20 == 0 || operational() != (status == 1)) refresh();
    }

    /** Takes its picture from the network, works out the threats and tells clients and neighbours what changed. */
    public void refresh() {
        tracks = RadarNetwork.view(this);
        List<RadarTrack> next = new ArrayList<>();
        for (RadarTrack track : tracks) if (track.threatens(zoneX, zoneZ, zoneRadius)) next.add(track);
        threats = next;
        Set<UUID> threatIds = new HashSet<>();
        threats.forEach(t -> threatIds.add(t.id()));
        acked.retainAll(threatIds);
        boolean nextSignal = operational() && !threats.isEmpty();
        int nextStatus = status();
        boolean nextUnacked = alarmUnacked();
        int nextSoonest = soonestSeconds();
        boolean dirty = nextStatus != status || tracks.size() != trackCount || threats.size() != threatCount || nextUnacked != unacked
                || nextSoonest != soonest || nextSignal != signal;
        status = nextStatus;
        trackCount = tracks.size();
        threatCount = threats.size();
        unacked = nextUnacked;
        soonest = nextSoonest;
        if (nextSignal != signal) {
            signal = nextSignal;
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        }
        if (dirty) sendData();
    }

    @Override public boolean addToGoggleTooltip(List<Component> lines, boolean sneak) {
        super.addToGoggleTooltip(lines, sneak);
        lines.add(Component.literal("    ").append(Component.translatable("block.create_nuclearindustry.missile_radar")).withStyle(ChatFormatting.GOLD));
        int status = status();
        lines.add(Component.literal("     ").append(Component.translatable("radar.create_nuclearindustry." + (status == 2 ? "static_only" : status == 1 ? "online" : "offline")))
                .withStyle(status == 1 ? ChatFormatting.GREEN : ChatFormatting.RED));
        lines.add(Component.literal("     ").append(Component.translatable("radar.create_nuclearindustry.contacts", trackCount())).withStyle(ChatFormatting.GRAY));
        if (threatCount() > 0) lines.add(Component.literal("     ").append(Component.translatable("radar.create_nuclearindustry.threats", threatCount(), soonestSeconds()))
                .withStyle(alarmUnacked() ? ChatFormatting.RED : ChatFormatting.GOLD));
        lines.add(Component.literal("     ").append(LinkCode.complete(code) ? Component.translatable("radar.create_nuclearindustry.network", code)
                : Component.translatable("radar.create_nuclearindustry.no_network")).withStyle(ChatFormatting.DARK_GRAY));
        return true;
    }

    // ------------------------------------------------------------------ control room

    @Override public TargetKind plantKind() { return TargetKind.RADAR; }

    @Override public boolean plantFormed() { return true; }

    @Override public MonitorReading plantSample() {
        float[] v = new float[MonitorReading.RADAR_VALUES];
        v[MonitorReading.D_TRACKS] = trackCount();
        v[MonitorReading.D_THREATS] = threatCount();
        v[MonitorReading.D_ETA] = soonestSeconds();
        v[MonitorReading.D_ONLINE] = status() == 1 ? 1 : 0;
        v[MonitorReading.D_UNACKED] = alarmUnacked() ? 1 : 0;
        v[MonitorReading.D_RANGE] = (float) reach();
        String text = Component.translatable("radar.create_nuclearindustry." + (status() == 2 ? "static_only" : status() == 1 ? "online" : "offline")).getString();
        return new MonitorReading(TargetKind.RADAR, text, v, Math.min(1, trackCount() / 8f), threatCount() > 0 ? 1 : 0);
    }

    // ------------------------------------------------------------------ persistence

    @Override protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putString("Code", code);
        tag.putInt("ZoneX", zoneX);
        tag.putInt("ZoneZ", zoneZ);
        tag.putInt("ZoneRadius", zoneRadius);
        ListTag ack = new ListTag();
        acked.forEach(id -> ack.add(NbtUtils.createUUID(id)));
        tag.put("Acked", ack);
        if (clientPacket) {
            tag.putInt("Status", status);
            tag.putInt("Tracks", trackCount);
            tag.putInt("Threats", threatCount);
            tag.putInt("Soonest", soonest);
            tag.putBoolean("Unacked", unacked);
            tag.putBoolean("Signal", signal);
        }
    }

    @Override protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        code = LinkCode.sanitize(tag.getString("Code"));
        if (tag.contains("ZoneX")) {
            zoneX = tag.getInt("ZoneX");
            zoneZ = tag.getInt("ZoneZ");
        }
        zoneRadius = Math.max(0, Math.min(MAX_ZONE, tag.getInt("ZoneRadius")));
        acked.clear();
        for (Tag id : tag.getList("Acked", Tag.TAG_INT_ARRAY)) acked.add(NbtUtils.loadUUID(id));
        if (clientPacket) {
            status = tag.getInt("Status");
            trackCount = tag.getInt("Tracks");
            threatCount = tag.getInt("Threats");
            soonest = tag.getInt("Soonest");
            unacked = tag.getBoolean("Unacked");
            signal = tag.getBoolean("Signal");
        }
    }
}
