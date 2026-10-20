package org.papiricoh.create_nuclearindustry.missile.holo;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.create_nuclearindustry.missile.LaunchRules;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.missile.link.TacticalDevice;
import org.papiricoh.create_nuclearindustry.missile.flight.Flight;
import org.papiricoh.create_nuclearindustry.missile.flight.FlightTracker;
import org.papiricoh.create_nuclearindustry.integration.sable.ShipSpace;
import org.papiricoh.create_nuclearindustry.missile.MissileConfig;
import org.papiricoh.create_nuclearindustry.missile.radar.RadarBlockEntity;
import org.papiricoh.create_nuclearindustry.missile.radar.RadarTrack;
import org.papiricoh.create_nuclearindustry.missile.silo.SiloControllerEntity;
import org.papiricoh.create_nuclearindustry.missile.terrain.TerrainTile;
import org.papiricoh.create_nuclearindustry.missile.terrain.TerrainViewer;

import org.papiricoh.create_nuclearindustry.missile.defense.*;
import org.papiricoh.create_nuclearindustry.missile.defense.battery.InterceptorBatteryEntity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * The heart of a holographic command table, on its centre piece: the radars (up to {@link #MAX_RADARS}) and launch controllers (up to {@link #MAX_SILOS}) linked
 * to it with a Tactical Linker. Every half second it tells players within {@link #SIGHT} blocks what its radars track, where they
 * stand, and the controllers' states ({@link HoloTracksPayload}); the hologram and the screen are drawn from that. From its screen a
 * player can send the silo a new target ({@link #designate}), checked here.
 */
public class HoloTableBlockEntity extends BlockEntity implements TacticalDevice, TerrainViewer {
    public static final int MAX_RADARS = 4, MAX_SILOS = 16, MAX_BATTERIES = 8;
    /** Players this close see the hologram and are sent its picture. */
    public static final double SIGHT = 32;
    /** Players this close may use the table: its screen, its terrain, designating. */
    public static final double REACH = 8;
    /** The widest view the table shows, blocks across; targets must lie within half of it. */
    public static final int VIEW = 16384;

    private static final Map<Level, Set<HoloTableBlockEntity>> LOADED = new WeakHashMap<>();

    /** How close (blocks of table) a look must pass a mark to pick it. */
    public static final double PICK = 0.09;
    /** How long a marked target waits to be confirmed, ticks. */
    public static final int MARK_TICKS = 100;

    private final List<GlobalPos> radars = new ArrayList<>();
    private final List<GlobalPos> silos = new ArrayList<>();
    private final List<GlobalPos> batteries = new ArrayList<>();
    private DefenseSettings defense;
    private byte selectedSilo;
    private int counter;
    /** The zoom chosen: 0 automatic, else an index into {@link HoloView#VIEWS} plus one. Saved. */
    private byte zoom;
    /** The pinned track and the target marked, shared by everyone at the table; not saved. */
    @Nullable
    private UUID selected;
    @Nullable
    private HoloTracksPayload.Mark mark;

    public HoloTableBlockEntity(BlockPos pos, BlockState state) {
        super(HoloRegistry.ENTITY.get(), pos, state);
        defense = DefenseSettings.defaults(pos);
    }

    public List<GlobalPos> radars() { return Collections.unmodifiableList(radars); }
    public List<GlobalPos> silos() { return Collections.unmodifiableList(silos); }
    public List<GlobalPos> batteries() { return Collections.unmodifiableList(batteries); }
    public DefenseSettings defense() { return defense; }
    public void setDefense(DefenseSettings settings) { defense = settings; changed(); }
    public byte selectedSilo() { return selectedSilo; }
    @Nullable public GlobalPos silo() { return selectedSilo >= 0 && selectedSilo < silos.size() ? silos.get(selectedSilo) : null; }

    public boolean selectSilo(BlockPos pos) {
        if (level == null) return false;
        int index = silos.indexOf(GlobalPos.of(level.dimension(), pos));
        if (index < 0) return false;
        selectedSilo = (byte) index;
        mark = null;
        changed();
        return true;
    }

    /** The block entity outlives a change of its block's state: this one belongs to the centre only. */
    @Override
    public boolean isValidBlockState(BlockState state) {
        return super.isValidBlockState(state) && state.getValue(HoloTableBlock.PART) == HoloPart.CENTER;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) LOADED.computeIfAbsent(level, l -> Collections.newSetFromMap(new WeakHashMap<>())).add(this);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null) {
            Set<HoloTableBlockEntity> tables = LOADED.get(level);
            if (tables != null) tables.remove(this);
        }
    }

    /** Loaded tables of {@code level}. */
    public static List<HoloTableBlockEntity> loaded(Level level) {
        Set<HoloTableBlockEntity> tables = LOADED.get(level);
        return tables == null ? List.of() : new ArrayList<>(tables);
    }

    // ------------------------------------------------------------------ linking

    @Nullable
    public GlobalPos getTarget() {
        return silo() != null ? silo() : !radars.isEmpty() ? radars.getFirst() : batteries.isEmpty() ? null : batteries.getFirst();
    }

    /** Compatibility entry point for old callers and saves. */
    public void setTarget(@Nullable GlobalPos target) {
        if (target == null) clearLinks();
        else toggleLink(target);
    }

    @Override
    public Result toggleLink(GlobalPos target) {
        if (links().contains(target)) { unlink(target); return Result.REMOVED; }
        if (level == null || !level.dimension().equals(target.dimension())
                || !ShipSpace.closerThan(level, worldPosition, target.pos(), MissileConfig.tacticalLinkRange)) return Result.OUT_OF_RANGE;
        if (!level.isLoaded(target.pos())) return Result.UNSUPPORTED;
        BlockEntity at = level.getBlockEntity(target.pos());
        List<GlobalPos> list;
        int max;
        if (at instanceof RadarBlockEntity) { list = radars; max = MAX_RADARS; }
        else if (at instanceof SiloControllerEntity) { list = silos; max = MAX_SILOS; }
        else if (at instanceof InterceptorBatteryEntity) { list = batteries; max = MAX_BATTERIES; }
        else return Result.UNSUPPORTED;
        if (list.size() >= max) return Result.FULL;
        list.add(target);
        changed();
        return Result.ADDED;
    }

    @Override
    public boolean unlink(GlobalPos target) {
        GlobalPos selected = silo();
        boolean removed = radars.remove(target) | silos.remove(target) | batteries.remove(target);
        if (removed) {
            selectedSilo = (byte) Math.max(0, selected == null ? 0 : silos.indexOf(selected));
            mark = null;
            changed();
        }
        return removed;
    }

    @Override
    public void clearLinks() {
        radars.clear(); silos.clear(); batteries.clear(); selectedSilo = 0; mark = null;
        changed();
    }

    @Override
    public List<GlobalPos> links() {
        List<GlobalPos> all = new ArrayList<>(radars);
        all.addAll(silos);
        all.addAll(batteries);
        return List.copyOf(all);
    }

    /** Adds an unchecked link for tests of unavailable and other-dimension controllers. */
    public void linkSiloUnchecked(@Nullable GlobalPos silo) {
        if (silo == null) { silos.clear(); selectedSilo = 0; }
        else if (!silos.contains(silo) && silos.size() < MAX_SILOS) silos.add(silo);
        changed();
    }

    private void changed() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    public boolean showsTerrainTo(ServerPlayer player) {
        return near(player) && HoloTable.intact(player.level(), worldPosition);
    }

    public boolean near(Player player) {
        return player.level() == level && player.distanceToSqr(worldPosition.getCenter()) <= REACH * REACH;
    }

    // ------------------------------------------------------------------ the picture

    void serverTick() {
        if (!(level instanceof ServerLevel server)) return;
        ++counter;
        if (defense.auto() && counter % Math.max(1, DefenseConfig.autoIntervalTicks) == 0 && HoloTable.intact(level, worldPosition)) {
            DefenseSettings settings = defense.zoneRadius() == 0 ? defense.withZone(worldPosition.getX(), worldPosition.getZ(), 2000) : defense;
            DefenseCommand.autoEngage(server, tracks(), shooters(-1), settings, this::own, EngagementSource.AUTO_TABLE);
        }
        if (counter % 10 != 0) return;
        List<ServerPlayer> watching = new ArrayList<>();
        for (ServerPlayer player : server.players()) {
            if (player.distanceToSqr(worldPosition.getCenter()) <= SIGHT * SIGHT) watching.add(player);
        }
        if (watching.isEmpty() || !HoloTable.intact(level, worldPosition)) return;
        HoloTracksPayload picture = picture();
        for (ServerPlayer player : watching) PacketDistributor.sendToPlayer(player, picture);
    }

    /** Sends the picture at once to everyone watching: after a click changed what they all see. */
    private void broadcast() {
        if (!(level instanceof ServerLevel server)) return;
        HoloTracksPayload picture = picture();
        for (ServerPlayer player : server.players()) {
            if (player.distanceToSqr(worldPosition.getCenter()) <= SIGHT * SIGHT) PacketDistributor.sendToPlayer(player, picture);
        }
    }

    /** Sends the picture to {@code player} at once (when they open the screen). */
    public void syncTo(Player player) {
        if (player instanceof ServerPlayer server) PacketDistributor.sendToPlayer(server, picture());
    }

    @Nullable
    private RadarBlockEntity radar(GlobalPos pos) {
        return level != null && level.dimension().equals(pos.dimension()) && level.isLoaded(pos.pos())
                && level.getBlockEntity(pos.pos()) instanceof RadarBlockEntity radar ? radar : null;
    }

    @Nullable
    public SiloControllerEntity siloEntity() {
        return siloEntity(silo());
    }

    @Nullable
    private SiloControllerEntity siloEntity(@Nullable GlobalPos pos) {
        return pos != null && level != null && level.dimension().equals(pos.dimension()) && level.isLoaded(pos.pos())
                && level.getBlockEntity(pos.pos()) instanceof SiloControllerEntity controller ? controller : null;
    }

    @Nullable
    private InterceptorBatteryEntity battery(GlobalPos pos) {
        return level != null && level.dimension().equals(pos.dimension()) && level.isLoaded(pos.pos())
                && level.getBlockEntity(pos.pos()) instanceof InterceptorBatteryEntity battery ? battery : null;
    }

    /** The same linked radar picture feeds manual and automatic engagements. */
    public List<RadarTrack> tracks() {
        Map<UUID, RadarTrack> tracks = new LinkedHashMap<>();
        for (GlobalPos link : radars) {
            RadarBlockEntity radar = radar(link);
            if (radar != null) for (RadarTrack track : radar.tracks())
                if (tracks.size() < HoloTracksPayload.MAX_TRACKS || tracks.containsKey(track.id())) tracks.putIfAbsent(track.id(), track);
        }
        return List.copyOf(tracks.values());
    }

    /** Loaded launchers, or the launcher selected by the wire protocol's stable link index. */
    public List<InterceptorShooter> shooters(int choice) {
        List<InterceptorShooter> result = new ArrayList<>();
        for (int i = 0; i < batteries.size(); i++) {
            InterceptorBatteryEntity battery = battery(batteries.get(i));
            if ((choice == -1 || choice == i) && battery != null) result.add(battery);
        }
        for (int i = 0; i < silos.size(); i++) {
            SiloControllerEntity silo = siloEntity(silos.get(i));
            if ((choice == -1 || choice == 100 + i) && silo != null && silo.interceptorMode()) result.add(silo);
        }
        return result;
    }

    public boolean own(Flight flight) {
        for (GlobalPos link : batteries) {
            InterceptorBatteryEntity battery = battery(link);
            if (battery != null && battery.owner() != null && battery.owner().equals(flight.owner())) return true;
        }
        for (GlobalPos link : silos) {
            SiloControllerEntity silo = siloEntity(link);
            if (silo != null && (silo.floor().above().equals(flight.origin())
                    || silo.operator() != null && silo.operator().equals(flight.owner()))) return true;
        }
        return false;
    }

    public boolean ownInterceptor(Flight flight) {
        if (flight.kind() != Flight.Kind.INTERCEPTOR || flight.intercept() == null || level == null) return false;
        BlockPos launcher = flight.intercept().launcher();
        if (launcher == null) return false;
        GlobalPos link = GlobalPos.of(level.dimension(), launcher);
        return batteries.contains(link) || silos.contains(link);
    }

    /** The board has no bulk operation; drain each known target/launcher pair. */
    public void cease() {
        setDefense(defense.withAuto(false));
        if (!(level instanceof ServerLevel server)) return;
        Set<UUID> targets = new java.util.HashSet<>();
        tracks().forEach(track -> targets.add(track.id()));
        FlightTracker.get(server).flights().forEach(flight -> targets.add(flight.id()));
        List<GlobalPos> launchers = new ArrayList<>(batteries);
        launchers.addAll(silos);
        for (UUID target : targets) for (GlobalPos launcher : launchers) {
            if (!launcher.dimension().equals(level.dimension())) continue;
            int count = EngagementBoard.reserved(server, target);
            for (int i = 0; i < count; i++) EngagementBoard.release(server, target, launcher.pos());
        }
    }

    /** What the table shows now. Public for tests. */
    public HoloTracksPayload picture() {
        double cx = worldPosition.getX() + 0.5, cz = worldPosition.getZ() + 0.5;
        Map<BlockPos, UUID> origins = new LinkedHashMap<>();
        FlightTracker tracker = level instanceof ServerLevel server ? FlightTracker.getIfPresent(server) : null;
        if (tracker != null) for (Flight flight : tracker.flights()) {
            if (flight.kind() == Flight.Kind.MISSILE) origins.put(flight.origin(), flight.id());
        }
        Set<UUID> own = new java.util.HashSet<>();
        for (GlobalPos pos : silos) {
            SiloControllerEntity controller = siloEntity(pos);
            if (controller != null && origins.containsKey(controller.floor().above())) own.add(origins.get(controller.floor().above()));
        }
        if (tracker != null) for (Flight flight : tracker.flights()) if (own(flight)) own.add(flight.id());
        Map<UUID, HoloTracksPayload.Track> tracks = new LinkedHashMap<>();
        List<HoloTracksPayload.Radar> radarInfo = new ArrayList<>();
        for (GlobalPos pos : radars) {
            RadarBlockEntity radar = radar(pos);
            if (radar == null) {
                radarInfo.add(new HoloTracksPayload.Radar(pos.pos().getX() - worldPosition.getX(), pos.pos().getY(),
                        pos.pos().getZ() - worldPosition.getZ(), 0, false, ""));
                continue;
            }
            BlockPos at = radar.getBlockPos();
            radarInfo.add(new HoloTracksPayload.Radar(at.getX() - worldPosition.getX(), at.getY(), at.getZ() - worldPosition.getZ(),
                    (float) radar.reach(), radar.status() == 1, radar.code()));
            for (RadarTrack track : radar.tracks()) {
                if (tracks.size() >= HoloTracksPayload.MAX_TRACKS) break;
                boolean threat = radar.threats().contains(track);
                HoloTracksPayload.Track old = tracks.get(track.id());
                if (old != null) {
                    if (threat && !old.threat()) tracks.put(track.id(), track(track, true, own.contains(track.id()), cx, cz));
                    continue;
                }
                tracks.put(track.id(), track(track, threat, own.contains(track.id()), cx, cz));
            }
        }
        List<HoloTracksPayload.Silo> siloInfo = new ArrayList<>();
        for (int i = 0; i < silos.size(); i++) {
            GlobalPos link = silos.get(i);
            SiloControllerEntity controller = siloEntity(link);
            BlockPos at = controller == null ? link.pos() : controller.floor();
            BlockPos target = controller == null ? null : controller.target();
            String error = controller == null ? "unloaded" : controller.error();
            siloInfo.add(new HoloTracksPayload.Silo((byte) i, controller != null,
                    at.getX() - worldPosition.getX(), at.getZ() - worldPosition.getZ(),
                    controller == null ? 0 : (float) controller.reach(), controller == null ? 0 : (float) controller.cep(),
                    controller == null ? (byte) 0 : (byte) controller.state().ordinal(), target != null,
                    target == null ? 0 : target.getX() - worldPosition.getX(), target == null ? 0 : target.getZ() - worldPosition.getZ(),
                    error.substring(0, Math.min(32, error.length())), controller == null ? -1 : controller.countdown(),
                    controller == null ? 0 : controller.launchDelay(), controller == null ? 0 : (float) (controller.flightTicks() / 20),
                    controller == null ? null : origins.get(controller.floor().above())));
        }
        List<HoloTracksPayload.Shooter> shooterInfo = new ArrayList<>();
        for (int i = 0; i < batteries.size(); i++) {
            GlobalPos link = batteries.get(i);
            InterceptorBatteryEntity b = battery(link);
            shooterInfo.add(new HoloTracksPayload.Shooter((byte) shooterInfo.size(), (byte) ShooterType.BATTERY.ordinal(), (byte) i, b != null,
                    link.pos().getX() - worldPosition.getX(), link.pos().getZ() - worldPosition.getZ(),
                    (byte) (b == null ? 0 : b.status().ordinal()), (byte) (b == null ? 0 : b.readyRounds()), (byte) 4,
                    b == null ? 0 : (float) b.envelope(MissileConfig.flightParams()).maxRange(),
                    b == null ? 0 : b.yaw(1), b == null ? 0 : b.pitch(1), b != null && b.settings().auto()));
        }
        for (int i = 0; i < silos.size() && shooterInfo.size() < HoloTracksPayload.MAX_SHOOTERS; i++) {
            SiloControllerEntity silo = siloEntity(silos.get(i));
            if (silo == null || !silo.interceptorMode()) continue;
            shooterInfo.add(new HoloTracksPayload.Shooter((byte) shooterInfo.size(), (byte) ShooterType.SILO.ordinal(), (byte) i, true,
                    silo.floor().getX() - worldPosition.getX(), silo.floor().getZ() - worldPosition.getZ(), (byte) silo.state().ordinal(),
                    (byte) silo.readyRounds(), (byte) 1, (float) silo.envelope(MissileConfig.flightParams()).maxRange(), 0, 90, defense.auto()));
        }
        List<HoloTracksPayload.Intercept> interceptInfo = new ArrayList<>();
        if (tracker != null) for (Flight flight : tracker.interceptors()) {
            Flight.Intercept intercept = flight.intercept();
            if (intercept == null || (!tracks.containsKey(intercept.target()) && !ownInterceptor(flight))) continue;
            if (interceptInfo.size() >= HoloTracksPayload.MAX_INTERCEPTS) break;
            var at = flight.flight().position();
            var velocity = flight.flight().velocity();
            var point = intercept.point();
            interceptInfo.add(new HoloTracksPayload.Intercept(flight.id(), intercept.target(),
                    (float) (at.x() - cx), (float) at.y(), (float) (at.z() - cz),
                    (float) velocity.x(), (float) velocity.y(), (float) velocity.z(),
                    (float) (point.x() - cx), (float) point.y(), (float) (point.z() - cz),
                    intercept.arrivalTick() - flight.flight().tick(), (float) intercept.pk()));
        }
        HoloTracksPayload.Defense defenseInfo = new HoloTracksPayload.Defense(defense.auto(), (byte) defense.doctrine().ordinal(),
                defense.zoneX() - worldPosition.getX(), defense.zoneZ() - worldPosition.getZ(), defense.zoneRadius());
        long now = level == null ? 0 : level.getGameTime();
        List<HoloTracksPayload.Track> list = List.copyOf(tracks.values());
        HoloTracksPayload.Track pinned = null;
        for (HoloTracksPayload.Track t : list) if (t.id().equals(selected)) pinned = t;
        if (pinned == null) selected = null;
        if (mark != null && (now - mark.at() > MARK_TICKS || now < mark.at())) mark = null;
        HoloTracksPayload draft = new HoloTracksPayload(worldPosition, now, (float) MissileConfig.gravity, list, radarInfo, siloInfo,
                HoloTracksPayload.Board.EMPTY);
        int view = zoom <= 0 || zoom > HoloView.VIEWS.length ? HoloView.pick(draft.farthest()) : HoloView.VIEWS[zoom - 1];
        // The view follows a pinned track that has left it, in steps of an eighth of the view so it does not crawl.
        double ox = 0, oz = 0;
        if (pinned != null && (Math.abs(pinned.x()) > view * 0.45 || Math.abs(pinned.z()) > view * 0.45)) {
            double step = view / 8.0;
            ox = Math.round(pinned.x() / step) * step;
            oz = Math.round(pinned.z() / step) * step;
        }
        return new HoloTracksPayload(worldPosition, now, (float) MissileConfig.gravity, list, radarInfo, siloInfo,
                new HoloTracksPayload.Board(view, (float) ox, (float) oz, zoom, selectedSilo, selected, mark), shooterInfo, interceptInfo, defenseInfo);
    }

    private static HoloTracksPayload.Track track(RadarTrack track, boolean threat, boolean own, double cx, double cz) {
        RadarTrack.Snapshot s = track.snapshot(true);
        List<org.papiricoh.create_nuclearindustry.missile.physics.V3> history = s.history();
        int from = Math.max(0, history.size() - HoloTracksPayload.HISTORY);
        // The newest stretch of its history.
        int count = history.size() - from;
        float[] points = new float[count * 3];
        for (int i = 0; i < count; i++) {
            var p = history.get(from + i);
            points[i * 3] = (float) (p.x() - cx);
            points[i * 3 + 1] = (float) p.y();
            points[i * 3 + 2] = (float) (p.z() - cz);
        }
        return new HoloTracksPayload.Track(s.id(), s.designation(), s.phase(), threat, own, s.eta(), (float) (s.position().x() - cx), (float) s.position().y(),
                (float) (s.position().z() - cz), (float) s.velocity().x(), (float) s.velocity().y(), (float) s.velocity().z(),
                (float) (s.impact().x() - cx), (float) (s.impact().z() - cz), s.along(), s.across(), s.heading(),
                (float) (s.firstSeen().x() - cx), (float) s.firstSeen().y(), (float) (s.firstSeen().z() - cz), points);
    }

    // ------------------------------------------------------------------ designating

    /** How a designation went: {@code OK}, or why not (a lang key suffix). Sent by ordinal: new ones go last. */
    public enum Designation { OK, TOO_FAR, BROKEN, NO_SILO, OTHER_DIMENSION, UNLOADED, OUT_OF_REACH, OFF_TABLE, LIMITS }

    /**
     * Sends the linked silo a new target at ({@code x, z}), its height read from the ground there if it is loaded, else foretold
     * from the world's generator. The player must stand at the table, the table be whole, the silo be linked, in this dimension and
     * loaded, and the target lie within the table's view and the silo's reach.
     */
    public Designation designate(ServerPlayer player, int x, int z, boolean airburst) {
        Designation check = check(player, x, z);
        if (check != Designation.OK) return check;
        SiloControllerEntity controller = siloEntity();
        if (controller == null) return Designation.UNLOADED;
        int y = level.hasChunk(x >> 4, z >> 4) ? level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z)
                : ((ServerLevel) level).getChunkSource().getGenerator().getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level,
                ((ServerLevel) level).getChunkSource().randomState());
        BlockPos target = new BlockPos(x, y, z);
        // The ring is drawn for level ground: the silo's own flight check has the last word on hills and valleys.
        if (!controller.canReach(target)) return Designation.OUT_OF_REACH;
        controller.configure(player, target, airburst);
        return Designation.OK;
    }

    /** Whether {@link #designate} would take ({@code x, z}) from {@code player}, changing nothing. */
    public Designation check(ServerPlayer player, int x, int z) {
        if (!near(player)) return Designation.TOO_FAR;
        if (!HoloTable.intact(level, worldPosition)) return Designation.BROKEN;
        GlobalPos silo = silo();
        if (silo == null) return Designation.NO_SILO;
        if (!silo.dimension().equals(level.dimension())) return Designation.OTHER_DIMENSION;
        SiloControllerEntity controller = siloEntity();
        if (controller == null) return Designation.UNLOADED;
        if (Math.abs((long) x - worldPosition.getX()) > VIEW / 2 || Math.abs((long) z - worldPosition.getZ()) > VIEW / 2 || !level.getWorldBorder().isWithinBounds(x, z))
            return Designation.OFF_TABLE;
        BlockPos floor = controller.floor();
        double distance = Math.hypot(x + 0.5 - floor.getX() - 0.5, z + 0.5 - floor.getZ() - 0.5);
        double reach = controller.reach();
        if (reach <= 0 || distance > reach) return Designation.OUT_OF_REACH;
        // The server's limits, as the silo will check them again when it launches.
        if (!LaunchRules.dimensionAllowed(level.dimension().location().toString(), MissileConfig.allowedDimensions)
                || LaunchRules.outOfRange(floor.getX() + 0.5, floor.getZ() + 0.5, x + 0.5, z + 0.5, MissileConfig.maxRange)) return Designation.LIMITS;
        BlockPos spawn = ((ServerLevel) level).getSharedSpawnPos();
        if (level.dimension() == Level.OVERWORLD
                && LaunchRules.nearSpawn(x + 0.5, z + 0.5, spawn.getX() + 0.5, spawn.getZ() + 0.5, MissileConfig.protectedSpawnRadius)) return Designation.LIMITS;
        return Designation.OK;
    }

    // ------------------------------------------------------------------ using it in the world

    /** What a right click on the table did. */
    public enum Click { NONE, TOO_FAR, SELECTED, RELEASED, ZOOM, OPEN, MARKED, CONFIRMED, REFUSED, SILO_SELECTED }

    /** The last designation tried by a click, for the message the block shows. */
    @Nullable
    private Designation lastDesignation;

    private int lastX, lastZ;

    @Nullable
    public Designation lastDesignation() { return lastDesignation; }
    /** Where the last click marked or sent a target. */
    public int lastX() { return lastX; }
    public int lastZ() { return lastZ; }

    @Nullable public UUID selected() { return selected; }
    @Nullable public HoloTracksPayload.Mark mark() { return mark; }
    public byte zoom() { return zoom; }

    /**
     * A right click by {@code player} at {@code hit} (world coordinates) on the table, looking from {@code eye} along {@code look}:
     * on a track's mark it pins or releases it; on the frame it steps the zoom; on bare glass it opens the screen, or, sneaking,
     * marks a target there, which a second sneaking click on the mark within five seconds sends to the silo.
     */
    public Click click(ServerPlayer player, Vec3 hit, Vec3 eye, Vec3 look, boolean sneaking) {
        if (!near(player)) return Click.TOO_FAR;
        if (level == null || !HoloTable.intact(level, worldPosition)) return Click.NONE;
        HoloTracksPayload picture = picture();
        HoloProjection projection = picture.projection();
        double bx = worldPosition.getX(), by = worldPosition.getY(), bz = worldPosition.getZ();
        double[] from = {eye.x - bx, eye.y - by, eye.z - bz}, along = {look.x, look.y, look.z};
        if (!sneaking) {
            List<HoloTracksPayload.Silo> visible = picture.silos().stream().filter(s -> s.loaded() && projection.shows(s.dx() + 0.5, s.dz() + 0.5)).toList();
            double[] siloMarks = new double[visible.size() * 3];
            for (int i = 0; i < visible.size(); i++) {
                siloMarks[i * 3] = projection.x(visible.get(i).dx() + 0.5);
                siloMarks[i * 3 + 1] = HoloProjection.Y + 0.03;
                siloMarks[i * 3 + 2] = projection.z(visible.get(i).dz() + 0.5);
            }
            int siloPick = HoloProjection.pick(from, along, siloMarks, PICK);
            if (siloPick >= 0) {
                selectSilo(silos.get(visible.get(siloPick).index()).pos());
                broadcast();
                return Click.SILO_SELECTED;
            }
            int picked = HoloProjection.pick(from, along, picture.marks(projection, 0), PICK);
            if (picked >= 0) {
                UUID id = picture.tracks().get(picked).id();
                boolean release = id.equals(selected);
                selected = release ? null : id;
                broadcast();
                return release ? Click.RELEASED : Click.SELECTED;
            }
        }
        double lx = hit.x - bx, ly = hit.y - by, lz = hit.z - bz;
        HoloProjection.Region region = HoloProjection.region(lx, ly, lz);
        if (region == HoloProjection.Region.FRAME && !sneaking) {
            zoom = (byte) ((zoom + 1) % (HoloView.VIEWS.length + 1));
            setChanged();
            broadcast();
            return Click.ZOOM;
        }
        if (region != HoloProjection.Region.GLASS) return Click.NONE;
        if (!sneaking) return Click.OPEN;
        long now = level.getGameTime();
        if (mark != null && now - mark.at() <= MARK_TICKS && Math.abs(projection.x(mark.dx()) - lx) <= PICK
                && Math.abs(projection.z(mark.dz()) - lz) <= PICK) {
            int x = worldPosition.getX() + mark.dx(), z = worldPosition.getZ() + mark.dz();
            mark = null;
            lastX = x;
            lastZ = z;
            lastDesignation = designate(player, x, z, false);
            broadcast();
            return lastDesignation == Designation.OK ? Click.CONFIRMED : Click.REFUSED;
        }
        int x = (int) Math.floor(worldPosition.getX() + 0.5 + projection.worldDx(lx)), z = (int) Math.floor(worldPosition.getZ() + 0.5 + projection.worldDz(lz));
        lastX = x;
        lastZ = z;
        lastDesignation = check(player, x, z);
        mark = new HoloTracksPayload.Mark(x - worldPosition.getX(), z - worldPosition.getZ(), (byte) lastDesignation.ordinal(), now);
        broadcast();
        return Click.MARKED;
    }

    /** The hologram spans all nine pieces and its arcs rise above them. */
    public AABB renderBox() {
        return new AABB(worldPosition).inflate(1.5, 0, 1.5).expandTowards(0, 3, 0);
    }

    // ------------------------------------------------------------------ persistence

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag list = new ListTag();
        for (GlobalPos pos : radars) GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, pos).result().ifPresent(list::add);
        tag.put("Radars", list);
        ListTag savedBatteries = new ListTag();
        for (GlobalPos pos : batteries) GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, pos).result().ifPresent(savedBatteries::add);
        tag.put("Batteries", savedBatteries);
        tag.put("Defense", defense.save());
        tag.putByte("Zoom", zoom);
        ListTag savedSilos = new ListTag();
        for (GlobalPos pos : silos) GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, pos).result().ifPresent(savedSilos::add);
        tag.put("Silos", savedSilos);
        tag.putByte("SelectedSilo", selectedSilo);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        batteries.clear();
        for (Tag t : tag.getList("Batteries", Tag.TAG_COMPOUND)) {
            if (batteries.size() < MAX_BATTERIES) GlobalPos.CODEC.parse(NbtOps.INSTANCE, t).result()
                    .filter(p -> !batteries.contains(p)).ifPresent(batteries::add);
        }
        defense = DefenseSettings.load(tag.getCompound("Defense"), DefenseSettings.defaults(worldPosition));
        radars.clear();
        for (Tag t : tag.getList("Radars", Tag.TAG_COMPOUND)) {
            if (radars.size() < MAX_RADARS) GlobalPos.CODEC.parse(NbtOps.INSTANCE, t).result().ifPresent(radars::add);
        }
        zoom = (byte) Math.max(0, Math.min(HoloView.VIEWS.length, tag.getByte("Zoom")));
        silos.clear();
        for (Tag t : tag.getList("Silos", Tag.TAG_COMPOUND)) {
            if (silos.size() < MAX_SILOS) GlobalPos.CODEC.parse(NbtOps.INSTANCE, t).result().filter(p -> !silos.contains(p)).ifPresent(silos::add);
        }
        if (!tag.contains("Silos") && tag.contains("Silo")) GlobalPos.CODEC.parse(NbtOps.INSTANCE, tag.get("Silo")).result().ifPresent(silos::add);
        selectedSilo = (byte) Math.max(0, Math.min(Math.max(0, silos.size() - 1), tag.getByte("SelectedSilo")));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    static boolean withinView(int x, int z, BlockPos centre) {
        return Math.abs(x - centre.getX()) <= TerrainTile.RANGE && Math.abs(z - centre.getZ()) <= TerrainTile.RANGE;
    }
}
