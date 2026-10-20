package org.papiricoh.create_nuclearindustry.missile.holo;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * What a command table shows, sent every half second to players near it: the tracks of its linked radars (each with up to
 * {@link #HISTORY} plotted points, positions relative to the table), the radars' own places and reach, up to sixteen silos, and its
 * missile defence: the launchers that fire interceptors, the interceptors in flight and how it defends; and its satellites
 * ({@link Orbital}). Bounded: {@link #MAX_TRACKS} tracks, {@link #MAX_RADARS} radars, {@link #MAX_SHOOTERS} launchers,
 * {@link #MAX_INTERCEPTS} interceptors, {@link #MAX_STATIONS} ground stations, {@link #MAX_SATELLITES} satellites.
 */
public record HoloTracksPayload(BlockPos centre, long sample, float gravity, List<Track> tracks, List<Radar> radars, List<Silo> silos, Board board,
                                List<Shooter> shooters, List<Intercept> intercepts, Defense defense, Orbital orbital)
        implements CustomPacketPayload {
    public static final int MAX_TRACKS = 32, HISTORY = 24, MAX_RADARS = 4, MAX_SHOOTERS = 24, MAX_INTERCEPTS = 32, MAX_STATIONS = 2,
            MAX_SATELLITES = 32;

    /** A picture without satellites. */
    public HoloTracksPayload(BlockPos centre, long sample, float gravity, List<Track> tracks, List<Radar> radars, List<Silo> silos, Board board,
                             List<Shooter> shooters, List<Intercept> intercepts, Defense defense) {
        this(centre, sample, gravity, tracks, radars, silos, board, shooters, intercepts, defense, Orbital.NONE);
    }

    /** A picture without missile defence. */
    public HoloTracksPayload(BlockPos centre, long sample, float gravity, List<Track> tracks, List<Radar> radars, List<Silo> silos, Board board) {
        this(centre, sample, gravity, tracks, radars, silos, board, List.of(), List.of(), Defense.OFF);
    }
    public static final Type<HoloTracksPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "holo_tracks"));

    /**
     * A track: where it is and goes, where it comes down and how sure, relative to the table's centre; its history oldest first,
     * as {@code x, y, z} triples (y absolute).
     */
    public record Track(UUID id, String designation, byte phase, boolean threat, boolean own, int eta, float x, float y, float z, float vx, float vy, float vz,
                        float ix, float iz, float along, float across, float heading, float fx, float fy, float fz, float[] history) {
        /** Where it is {@code since} ticks (at most a second) after the picture, flown on in free fall. */
        public double[] at(double since, float gravity) {
            double s = Math.max(0, Math.min(20, since));
            return HoloProjection.fly(x, y, z, vx, vy, vz, gravity, s);
        }

        /** Its highest point: what it reached, or will in free fall. */
        public double apex(float gravity) {
            double top = y;
            for (int i = 1; i < history.length; i += 3) top = Math.max(top, history[i]);
            if (vy > 0 && gravity > 0) top = Math.max(top, y + vy * vy / (2 * gravity));
            return top;
        }
    }

    /** A linked radar: where, how far it sees a bare rocket body (0 for everything), whether it works and its network's code. */
    public record Radar(int dx, int dy, int dz, float reach, boolean online, String code) {
    }

    /**
     * What everyone at the table shares: the view (blocks across) and where it is centred, the zoom chosen (0 automatic, else an
     * index into {@link HoloView#VIEWS} plus one), the track pinned and the target marked but not yet confirmed.
     */
    public record Board(int view, float ox, float oz, byte zoom, byte selectedSilo, @Nullable UUID selected, @Nullable Mark mark) {
        public static final Board EMPTY = new Board(HoloView.VIEWS[0], 0, 0, (byte) 0, (byte) 0, null, null);
    }

    /** A target marked on the glass: where (relative to the table), how designating there would go (a {@code Designation} ordinal) and since when. */
    public record Mark(int dx, int dz, byte result, long at) {
    }

    /** The linked silo: where, how far its rocket reaches, its spread, its state (ordinal) and its target if it has one. */
    public record Silo(byte index, boolean loaded, int dx, int dz, float reach, float cep, byte state, boolean targeted, int tx, int tz,
                       String error, int countdown, int delay, float flightSeconds, @Nullable UUID flight) {
    }

    /**
     * A launcher that fires interceptors for the table: a battery or a launch controller topped by a kill vehicle.
     *
     * @param type     a {@code ShooterType} ordinal
     * @param link     its index in the table's battery list, or in its silo list for a launch controller
     * @param state    a battery's {@code Status} ordinal, or a launch controller's {@code State} ordinal
     * @param ready    interceptors it could launch now
     * @param capacity most it holds
     * @param range    furthest over the ground it meets a missile
     * @param yaw      where a battery's turret points, degrees from north
     */
    public record Shooter(byte index, byte type, byte link, boolean loaded, int dx, int dz, byte state, byte ready, byte capacity, float range,
                          float yaw, float pitch, boolean auto) {
    }

    /**
     * An interceptor in flight for the table, relative to it (y absolute): where it is and goes, the point it is to meet its target
     * at, ticks until then and its chance to kill.
     */
    public record Intercept(UUID interceptor, UUID target, float x, float y, float z, float vx, float vy, float vz, float px, float py, float pz,
                            int tti, float pk) {
    }

    /** How the table defends: automatically or not, its doctrine (ordinal) and its zone, relative to it (radius 0 the default). */
    public record Defense(boolean auto, byte doctrine, int zoneDx, int zoneDz, int zoneRadius) {
        public static final Defense OFF = new Defense(false, (byte) 0, 0, 0, 0);
    }

    /**
     * The table's satellites: its network's code (from its ground stations; empty without one), the stations, the satellites it
     * knows of (its own, then the foreign ones its radars have catalogued) and its launch controllers topped by a satellite fairing.
     */
    public record Orbital(String code, List<Station> stations, List<Sat> satellites, List<SatLauncher> launchers) {
        public static final Orbital NONE = new Orbital("", List.of(), List.of(), List.of());

        public Orbital {
            stations = List.copyOf(stations);
            satellites = List.copyOf(satellites);
            launchers = List.copyOf(launchers);
        }
    }

    /** A linked ground station: where (relative to the table, y absolute), whether it works and its network's code. */
    public record Station(int dx, int dy, int dz, boolean online, String code) {
    }

    /**
     * A satellite the table knows of, where it is at the picture's sample tick: relative to the table (y absolute), its ground
     * velocity, how far it sees, the ticks until it next sees the table (0 now, -1 not within the look-ahead), the ticks left
     * before it decays (-1 for good) and the chance an interceptor on its way kills it (-1 none).
     *
     * @param kind  a {@code SatelliteKind} ordinal
     * @param orbit an {@code OrbitType} ordinal
     */
    public record Sat(UUID id, String designation, byte kind, byte orbit, boolean own, float x, float y, float z, float vx, float vz,
                      float footprint, int nextPass, int life, float pk) {
    }

    /**
     * A linked launch controller topped by a satellite fairing: {@code link} its index in the silo list, the satellite it carries
     * (a {@code SatelliteKind} ordinal, -1 an empty fairing), the highest orbit it reaches (an {@code OrbitType} ordinal, -1 none)
     * and its state (a {@code State} ordinal).
     */
    public record SatLauncher(byte link, byte kind, byte reach, byte state) {
    }

    @Nullable
    public Silo silo() {
        for (Silo silo : silos) if (silo.index == board.selectedSilo) return silo;
        return null;
    }

    /** How far (along x or z) from the table the farthest thing worth showing lies: tracks, where they fly from and to, radars, the silo and its target. */
    public double farthest() {
        double far = 0;
        for (Track t : tracks) {
            far = Math.max(far, Math.max(Math.abs(t.x), Math.abs(t.z)));
            far = Math.max(far, Math.max(Math.abs(t.ix), Math.abs(t.iz)));
            far = Math.max(far, Math.max(Math.abs(t.fx), Math.abs(t.fz)));
        }
        for (Radar r : radars) far = Math.max(far, Math.max(Math.abs(r.dx), Math.abs(r.dz)));
        for (Silo silo : silos) {
            far = Math.max(far, Math.max(Math.abs(silo.dx), Math.abs(silo.dz)));
            if (silo.targeted) far = Math.max(far, Math.max(Math.abs(silo.tx), Math.abs(silo.tz)));
        }
        return far;
    }

    /** The highest any track flies, or will, above sea level; at least 50. */
    public double top() {
        double top = 50;
        for (Track t : tracks) top = Math.max(top, t.apex(gravity) - HoloProjection.SEA_LEVEL);
        return top;
    }

    /** How this picture lies on the glass. */
    public HoloProjection projection() {
        return HoloProjection.of(board.view, board.ox, board.oz, top());
    }

    /** Where each track is {@code since} ticks after the picture, on the table ({@code x, y, z} triples, table coordinates; NaN off the glass). */
    public double[] marks(HoloProjection p, double since) {
        double[] out = new double[tracks.size() * 3];
        for (int i = 0; i < tracks.size(); i++) {
            double[] at = tracks.get(i).at(since, gravity);
            out[i * 3] = p.x(at[0]);
            out[i * 3 + 1] = p.y(at[1]);
            out[i * 3 + 2] = p.z(at[2]);
            // Off the glass it is not drawn, so it cannot be picked either.
            if (!HoloProjection.onGlass(out[i * 3], out[i * 3 + 2])) out[i * 3] = out[i * 3 + 1] = out[i * 3 + 2] = Double.NaN;
        }
        return out;
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, HoloTracksPayload> CODEC = StreamCodec.of(HoloTracksPayload::write, HoloTracksPayload::read);

    private static void write(RegistryFriendlyByteBuf b, HoloTracksPayload p) {
        b.writeBlockPos(p.centre);
        b.writeLong(p.sample);
        b.writeFloat(p.gravity);
        int count = Math.min(MAX_TRACKS, p.tracks.size());
        b.writeVarInt(count);
        for (int i = 0; i < count; i++) {
            Track t = p.tracks.get(i);
            UUIDUtil.STREAM_CODEC.encode(b, t.id);
            b.writeUtf(t.designation, 16);
            b.writeByte(t.phase);
            b.writeBoolean(t.threat);
            b.writeBoolean(t.own);
            b.writeVarInt(t.eta);
            for (float f : new float[]{t.x, t.y, t.z, t.vx, t.vy, t.vz, t.ix, t.iz, t.along, t.across, t.heading, t.fx, t.fy, t.fz}) b.writeFloat(f);
            int points = Math.min(HISTORY, t.history.length / 3);
            b.writeByte(points);
            for (int k = t.history.length / 3 - points; k < t.history.length / 3; k++) {
                b.writeFloat(t.history[k * 3]);
                b.writeFloat(t.history[k * 3 + 1]);
                b.writeFloat(t.history[k * 3 + 2]);
            }
        }
        int radars = Math.min(MAX_RADARS, p.radars.size());
        b.writeVarInt(radars);
        for (int i = 0; i < radars; i++) {
            Radar r = p.radars.get(i);
            b.writeVarInt(r.dx);
            b.writeVarInt(r.dy);
            b.writeVarInt(r.dz);
            b.writeFloat(r.reach);
            b.writeBoolean(r.online);
            b.writeUtf(r.code, 8);
        }
        if (p.silos.size() > HoloTableBlockEntity.MAX_SILOS) throw new IllegalArgumentException("Too many holo silos");
        b.writeVarInt(p.silos.size());
        for (Silo s : p.silos) {
            b.writeByte(s.index); b.writeBoolean(s.loaded);
            b.writeVarInt(s.dx); b.writeVarInt(s.dz);
            b.writeFloat(s.reach); b.writeFloat(s.cep);
            b.writeByte(s.state); b.writeBoolean(s.targeted);
            b.writeVarInt(s.tx); b.writeVarInt(s.tz);
            b.writeUtf(s.error, 32); b.writeVarInt(s.countdown); b.writeVarInt(s.delay); b.writeFloat(s.flightSeconds);
            b.writeBoolean(s.flight != null);
            if (s.flight != null) b.writeUUID(s.flight);
        }
        Board board = p.board;
        b.writeVarInt(board.view);
        b.writeFloat(board.ox);
        b.writeFloat(board.oz);
        b.writeByte(board.zoom);
        b.writeByte(board.selectedSilo);
        b.writeBoolean(board.selected != null);
        if (board.selected != null) UUIDUtil.STREAM_CODEC.encode(b, board.selected);
        b.writeBoolean(board.mark != null);
        if (board.mark != null) {
            b.writeVarInt(board.mark.dx);
            b.writeVarInt(board.mark.dz);
            b.writeByte(board.mark.result);
            b.writeLong(board.mark.at);
        }
        if (p.shooters.size() > MAX_SHOOTERS) throw new IllegalArgumentException("Too many holo shooters");
        b.writeVarInt(p.shooters.size());
        for (Shooter s : p.shooters) {
            b.writeByte(s.index); b.writeByte(s.type); b.writeByte(s.link); b.writeBoolean(s.loaded);
            b.writeVarInt(s.dx); b.writeVarInt(s.dz);
            b.writeByte(s.state); b.writeByte(s.ready); b.writeByte(s.capacity);
            b.writeFloat(s.range); b.writeFloat(s.yaw); b.writeFloat(s.pitch); b.writeBoolean(s.auto);
        }
        int intercepts = Math.min(MAX_INTERCEPTS, p.intercepts.size());
        b.writeVarInt(intercepts);
        for (int i = 0; i < intercepts; i++) {
            Intercept x = p.intercepts.get(i);
            UUIDUtil.STREAM_CODEC.encode(b, x.interceptor);
            UUIDUtil.STREAM_CODEC.encode(b, x.target);
            for (float f : new float[]{x.x, x.y, x.z, x.vx, x.vy, x.vz, x.px, x.py, x.pz}) b.writeFloat(f);
            b.writeVarInt(x.tti);
            b.writeFloat(x.pk);
        }
        Defense d = p.defense;
        b.writeBoolean(d.auto); b.writeByte(d.doctrine);
        b.writeVarInt(d.zoneDx); b.writeVarInt(d.zoneDz); b.writeVarInt(d.zoneRadius);
        Orbital o = p.orbital;
        b.writeUtf(o.code, 8);
        int stations = Math.min(MAX_STATIONS, o.stations.size());
        b.writeVarInt(stations);
        for (int i = 0; i < stations; i++) {
            Station st = o.stations.get(i);
            b.writeVarInt(st.dx); b.writeVarInt(st.dy); b.writeVarInt(st.dz); b.writeBoolean(st.online); b.writeUtf(st.code, 8);
        }
        int sats = Math.min(MAX_SATELLITES, o.satellites.size());
        b.writeVarInt(sats);
        for (int i = 0; i < sats; i++) {
            Sat s = o.satellites.get(i);
            UUIDUtil.STREAM_CODEC.encode(b, s.id);
            b.writeUtf(s.designation, 12);
            b.writeByte(s.kind); b.writeByte(s.orbit); b.writeBoolean(s.own);
            for (float f : new float[]{s.x, s.y, s.z, s.vx, s.vz, s.footprint}) b.writeFloat(f);
            b.writeVarInt(s.nextPass); b.writeVarInt(s.life); b.writeFloat(s.pk);
        }
        if (o.launchers.size() > HoloTableBlockEntity.MAX_SILOS) throw new IllegalArgumentException("Too many holo satellite launchers");
        b.writeVarInt(o.launchers.size());
        for (SatLauncher l : o.launchers) { b.writeByte(l.link); b.writeByte(l.kind); b.writeByte(l.reach); b.writeByte(l.state); }
    }

    private static HoloTracksPayload read(RegistryFriendlyByteBuf b) {
        BlockPos centre = b.readBlockPos();
        long sample = b.readLong();
        float gravity = b.readFloat();
        int count = b.readVarInt();
        if (count < 0 || count > MAX_TRACKS) throw new IllegalArgumentException("Too many holo tracks");
        List<Track> tracks = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            UUID id = UUIDUtil.STREAM_CODEC.decode(b);
            String designation = b.readUtf(16);
            byte phase = b.readByte();
            boolean threat = b.readBoolean();
            boolean own = b.readBoolean();
            int eta = b.readVarInt();
            float[] f = new float[14];
            for (int k = 0; k < f.length; k++) f[k] = b.readFloat();
            int points = b.readUnsignedByte();
            if (points > HISTORY) throw new IllegalArgumentException("Too much holo history");
            float[] history = new float[points * 3];
            for (int k = 0; k < history.length; k++) history[k] = b.readFloat();
            tracks.add(new Track(id, designation, phase, threat, own, eta, f[0], f[1], f[2], f[3], f[4], f[5], f[6], f[7], f[8], f[9], f[10], f[11], f[12], f[13], history));
        }
        int radarCount = b.readVarInt();
        if (radarCount < 0 || radarCount > MAX_RADARS) throw new IllegalArgumentException("Too many holo radars");
        List<Radar> radars = new ArrayList<>(radarCount);
        for (int i = 0; i < radarCount; i++) radars.add(new Radar(b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readFloat(), b.readBoolean(), b.readUtf(8)));
        int siloCount = b.readVarInt();
        if (siloCount < 0 || siloCount > HoloTableBlockEntity.MAX_SILOS) throw new IllegalArgumentException("Too many holo silos");
        List<Silo> silos = new ArrayList<>(siloCount);
        for (int i = 0; i < siloCount; i++) {
            byte index = b.readByte();
            if (index != i) throw new IllegalArgumentException("Unknown holo silo index");
            silos.add(new Silo(index, b.readBoolean(), b.readVarInt(), b.readVarInt(), b.readFloat(), b.readFloat(),
                    b.readByte(), b.readBoolean(), b.readVarInt(), b.readVarInt(), b.readUtf(32), b.readVarInt(), b.readVarInt(),
                    b.readFloat(), b.readBoolean() ? b.readUUID() : null));
        }
        int view = b.readVarInt();
        boolean known = false;
        for (int v : HoloView.VIEWS) known |= v == view;
        if (!known) throw new IllegalArgumentException("Unknown holo view");
        float ox = b.readFloat(), oz = b.readFloat();
        byte zoom = b.readByte();
        byte selectedSilo = b.readByte();
        if (selectedSilo < 0 || selectedSilo >= Math.max(1, siloCount)) throw new IllegalArgumentException("Unknown selected silo");
        UUID selected = b.readBoolean() ? UUIDUtil.STREAM_CODEC.decode(b) : null;
        Mark mark = b.readBoolean() ? new Mark(b.readVarInt(), b.readVarInt(), b.readByte(), b.readLong()) : null;
        int shooterCount = b.readVarInt();
        if (shooterCount < 0 || shooterCount > MAX_SHOOTERS) throw new IllegalArgumentException("Too many holo shooters");
        List<Shooter> shooters = new ArrayList<>(shooterCount);
        for (int i = 0; i < shooterCount; i++) {
            byte index = b.readByte();
            if (index != i) throw new IllegalArgumentException("Unknown holo shooter index");
            shooters.add(new Shooter(index, b.readByte(), b.readByte(), b.readBoolean(), b.readVarInt(), b.readVarInt(), b.readByte(),
                    b.readByte(), b.readByte(), b.readFloat(), b.readFloat(), b.readFloat(), b.readBoolean()));
        }
        int interceptCount = b.readVarInt();
        if (interceptCount < 0 || interceptCount > MAX_INTERCEPTS) throw new IllegalArgumentException("Too many holo intercepts");
        List<Intercept> intercepts = new ArrayList<>(interceptCount);
        for (int i = 0; i < interceptCount; i++) {
            UUID interceptor = UUIDUtil.STREAM_CODEC.decode(b);
            UUID target = UUIDUtil.STREAM_CODEC.decode(b);
            float[] f = new float[9];
            for (int k = 0; k < f.length; k++) f[k] = b.readFloat();
            intercepts.add(new Intercept(interceptor, target, f[0], f[1], f[2], f[3], f[4], f[5], f[6], f[7], f[8], b.readVarInt(), b.readFloat()));
        }
        Defense defense = new Defense(b.readBoolean(), b.readByte(), b.readVarInt(), b.readVarInt(), b.readVarInt());
        String code = b.readUtf(8);
        int stationCount = b.readVarInt();
        if (stationCount < 0 || stationCount > MAX_STATIONS) throw new IllegalArgumentException("Too many holo stations");
        List<Station> stations = new ArrayList<>(stationCount);
        for (int i = 0; i < stationCount; i++) stations.add(new Station(b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readBoolean(), b.readUtf(8)));
        int satCount = b.readVarInt();
        if (satCount < 0 || satCount > MAX_SATELLITES) throw new IllegalArgumentException("Too many holo satellites");
        List<Sat> sats = new ArrayList<>(satCount);
        for (int i = 0; i < satCount; i++) {
            UUID id = UUIDUtil.STREAM_CODEC.decode(b);
            String designation = b.readUtf(12);
            byte kind = b.readByte(), orbit = b.readByte();
            boolean own = b.readBoolean();
            float[] f = new float[6];
            for (int k = 0; k < f.length; k++) f[k] = b.readFloat();
            sats.add(new Sat(id, designation, kind, orbit, own, f[0], f[1], f[2], f[3], f[4], f[5], b.readVarInt(), b.readVarInt(), b.readFloat()));
        }
        int launcherCount = b.readVarInt();
        if (launcherCount < 0 || launcherCount > HoloTableBlockEntity.MAX_SILOS) throw new IllegalArgumentException("Too many holo satellite launchers");
        List<SatLauncher> launchers = new ArrayList<>(launcherCount);
        for (int i = 0; i < launcherCount; i++) {
            SatLauncher l = new SatLauncher(b.readByte(), b.readByte(), b.readByte(), b.readByte());
            if (l.link < 0 || l.link >= siloCount) throw new IllegalArgumentException("Unknown holo satellite launcher");
            launchers.add(l);
        }
        return new HoloTracksPayload(centre, sample, gravity, List.copyOf(tracks), List.copyOf(radars), List.copyOf(silos),
                new Board(view, ox, oz, zoom, selectedSilo, selected, mark), List.copyOf(shooters), List.copyOf(intercepts), defense,
                new Orbital(code, stations, sats, launchers));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
