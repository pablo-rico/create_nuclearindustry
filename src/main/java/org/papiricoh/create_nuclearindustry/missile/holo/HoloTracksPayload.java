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
 * {@link #HISTORY} plotted points, positions relative to the table), the radars' own places and reach, and up to sixteen silos. Bounded:
 * {@link #MAX_TRACKS} tracks, {@link #MAX_RADARS} radars.
 */
public record HoloTracksPayload(BlockPos centre, long sample, float gravity, List<Track> tracks, List<Radar> radars, List<Silo> silos, Board board)
        implements CustomPacketPayload {
    public static final int MAX_TRACKS = 32, HISTORY = 24, MAX_RADARS = 4;
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
        return new HoloTracksPayload(centre, sample, gravity, List.copyOf(tracks), List.copyOf(radars), List.copyOf(silos), new Board(view, ox, oz, zoom, selectedSilo, selected, mark));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
