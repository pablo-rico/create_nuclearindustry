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
import org.papiricoh.create_nuclearindustry.controlroom.ControlRoomDevice;
import org.papiricoh.create_nuclearindustry.missile.MissileConfig;
import org.papiricoh.create_nuclearindustry.missile.radar.RadarBlockEntity;
import org.papiricoh.create_nuclearindustry.missile.radar.RadarTrack;
import org.papiricoh.create_nuclearindustry.missile.silo.SiloControllerEntity;
import org.papiricoh.create_nuclearindustry.missile.terrain.TerrainTile;
import org.papiricoh.create_nuclearindustry.missile.terrain.TerrainViewer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * The heart of a holographic command table, on its centre piece: the radars (up to {@link #MAX_RADARS}) and the silo it is linked
 * to with a Control Room Linker. Every half second it tells players within {@link #SIGHT} blocks what its radars track, where they
 * stand, and its silo's state ({@link HoloTracksPayload}); the hologram and the screen are drawn from that. From its screen a
 * player can send the silo a new target ({@link #designate}), checked here.
 */
public class HoloTableBlockEntity extends BlockEntity implements ControlRoomDevice, TerrainViewer {
    public static final int MAX_RADARS = 4;
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
    @Nullable
    private GlobalPos silo;
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
    }

    public List<GlobalPos> radars() { return Collections.unmodifiableList(radars); }
    @Nullable public GlobalPos silo() { return silo; }

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

    @Override
    @Nullable
    public GlobalPos getTarget() {
        return silo != null ? silo : radars.isEmpty() ? null : radars.get(0);
    }

    /**
     * A linker's click: a radar is added (or taken off if it is already linked), a silo controller becomes the table's silo (or is
     * unlinked if it already was), anything else is ignored. A null target unlinks everything.
     */
    @Override
    public void setTarget(@Nullable GlobalPos target) {
        if (target == null) {
            radars.clear();
            silo = null;
        } else if (level != null && level.dimension().equals(target.dimension()) && level.isLoaded(target.pos())) {
            BlockEntity at = level.getBlockEntity(target.pos());
            if (at instanceof RadarBlockEntity) {
                if (!radars.remove(target) && radars.size() < MAX_RADARS) radars.add(target);
            } else if (at instanceof SiloControllerEntity) {
                silo = target.equals(silo) ? null : target;
            }
        }
        changed();
    }

    /** Links {@code silo} as it is, unchecked: for tests of what designating refuses. */
    public void linkSiloUnchecked(@Nullable GlobalPos silo) {
        this.silo = silo;
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
        if (!(level instanceof ServerLevel server) || ++counter % 10 != 0) return;
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
        return silo != null && level != null && level.dimension().equals(silo.dimension()) && level.isLoaded(silo.pos())
                && level.getBlockEntity(silo.pos()) instanceof SiloControllerEntity controller ? controller : null;
    }

    /** What the table shows now. Public for tests. */
    public HoloTracksPayload picture() {
        double cx = worldPosition.getX() + 0.5, cz = worldPosition.getZ() + 0.5;
        Map<UUID, HoloTracksPayload.Track> tracks = new LinkedHashMap<>();
        List<HoloTracksPayload.Radar> radarInfo = new ArrayList<>();
        for (GlobalPos pos : radars) {
            RadarBlockEntity radar = radar(pos);
            if (radar == null) continue;
            BlockPos at = radar.getBlockPos();
            radarInfo.add(new HoloTracksPayload.Radar(at.getX() - worldPosition.getX(), at.getY(), at.getZ() - worldPosition.getZ(),
                    (float) radar.reach(), radar.status() == 1, radar.code()));
            for (RadarTrack track : radar.tracks()) {
                if (tracks.size() >= HoloTracksPayload.MAX_TRACKS) break;
                boolean threat = radar.threats().contains(track);
                HoloTracksPayload.Track old = tracks.get(track.id());
                if (old != null) {
                    if (threat && !old.threat()) tracks.put(track.id(), track(track, true, cx, cz));
                    continue;
                }
                tracks.put(track.id(), track(track, threat, cx, cz));
            }
        }
        HoloTracksPayload.Silo siloInfo = null;
        SiloControllerEntity controller = siloEntity();
        if (controller != null) {
            BlockPos at = controller.floor();
            BlockPos target = controller.target();
            siloInfo = new HoloTracksPayload.Silo(at.getX() - worldPosition.getX(), at.getZ() - worldPosition.getZ(), (float) controller.reach(),
                    (float) controller.cep(), (byte) controller.state().ordinal(), target != null,
                    target == null ? 0 : target.getX() - worldPosition.getX(), target == null ? 0 : target.getZ() - worldPosition.getZ());
        }
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
                new HoloTracksPayload.Board(view, (float) ox, (float) oz, zoom, selected, mark));
    }

    private static HoloTracksPayload.Track track(RadarTrack track, boolean threat, double cx, double cz) {
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
        return new HoloTracksPayload.Track(s.id(), s.designation(), s.phase(), threat, s.eta(), (float) (s.position().x() - cx), (float) s.position().y(),
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
     * loaded, and the target lie within the table's view and the silo's reach (with a 5% margin).
     */
    public Designation designate(ServerPlayer player, int x, int z, boolean airburst) {
        Designation check = check(player, x, z);
        if (check != Designation.OK) return check;
        SiloControllerEntity controller = siloEntity();
        if (controller == null) return Designation.UNLOADED;
        int y = level.hasChunk(x >> 4, z >> 4) ? level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z)
                : ((ServerLevel) level).getChunkSource().getGenerator().getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level,
                ((ServerLevel) level).getChunkSource().randomState());
        controller.configure(player, new BlockPos(x, y, z), airburst);
        return Designation.OK;
    }

    /** Whether {@link #designate} would take ({@code x, z}) from {@code player}, changing nothing. */
    public Designation check(ServerPlayer player, int x, int z) {
        if (!near(player)) return Designation.TOO_FAR;
        if (!HoloTable.intact(level, worldPosition)) return Designation.BROKEN;
        if (silo == null) return Designation.NO_SILO;
        if (!silo.dimension().equals(level.dimension())) return Designation.OTHER_DIMENSION;
        SiloControllerEntity controller = siloEntity();
        if (controller == null) return Designation.UNLOADED;
        if (Math.abs(x - worldPosition.getX()) > VIEW / 2 || Math.abs(z - worldPosition.getZ()) > VIEW / 2 || !level.getWorldBorder().isWithinBounds(x, z))
            return Designation.OFF_TABLE;
        BlockPos floor = controller.floor();
        double distance = Math.hypot(x + 0.5 - floor.getX() - 0.5, z + 0.5 - floor.getZ() - 0.5);
        double reach = controller.reach();
        if (reach <= 0 || distance > reach * 1.05) return Designation.OUT_OF_REACH;
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
    public enum Click { NONE, TOO_FAR, SELECTED, RELEASED, ZOOM, OPEN, MARKED, CONFIRMED, REFUSED }

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
        tag.putByte("Zoom", zoom);
        if (silo != null) GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, silo).result().ifPresent(t -> tag.put("Silo", t));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        radars.clear();
        for (Tag t : tag.getList("Radars", Tag.TAG_COMPOUND)) {
            if (radars.size() < MAX_RADARS) GlobalPos.CODEC.parse(NbtOps.INSTANCE, t).result().ifPresent(radars::add);
        }
        zoom = (byte) Math.max(0, Math.min(HoloView.VIEWS.length, tag.getByte("Zoom")));
        silo = tag.contains("Silo") ? GlobalPos.CODEC.parse(NbtOps.INSTANCE, tag.get("Silo")).result().orElse(null) : null;
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
