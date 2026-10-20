package org.papiricoh.create_nuclearindustry.missile.radar;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import org.papiricoh.create_nuclearindustry.missile.physics.MissileFlight;
import org.papiricoh.create_nuclearindustry.missile.physics.V3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

/**
 * One flight as the radars of a level have tracked it: what they last measured, where they predict it comes down and the
 * positions they plotted, one a second, up to {@link #HISTORY}. Kept server side per level ({@link RadarNetwork}); radars and
 * their screens are sent {@link Snapshot}s.
 */
public final class RadarTrack {
    /** Plotted positions kept: a minute of flight. */
    public static final int HISTORY = 64;

    private final UUID id;
    private final String designation;
    private final V3 firstSeen;
    private final long firstSeenTick;
    private V3 position;
    private V3 velocity = V3.ZERO;
    private MissileFlight.Phase phase = MissileFlight.Phase.BOOST;
    private V3 impact = V3.ZERO;
    private RadarModel.Ellipse ellipse = new RadarModel.Ellipse(0, 0, 0);
    private int eta;
    private double apogee;
    private long lastSeenTick;
    private final Deque<V3> history = new ArrayDeque<>();

    RadarTrack(UUID id, V3 position, long tick) {
        this.id = id;
        this.designation = RadarModel.designation(id);
        this.firstSeen = position;
        this.firstSeenTick = tick;
        this.position = position;
        this.lastSeenTick = tick;
        this.apogee = position.y();
    }

    /** A fresh measurement: what the radars see now and what they make of it. */
    void measure(V3 position, V3 velocity, MissileFlight.Phase phase, double gravity, double groundY, long tick) {
        this.position = position;
        this.velocity = velocity;
        this.phase = phase;
        this.lastSeenTick = tick;
        this.apogee = Math.max(apogee, position.y());
        double ticks = RadarModel.timeToGround(position, velocity, gravity, groundY);
        this.eta = Double.isFinite(ticks) ? (int) Math.ceil(ticks) : 0;
        this.impact = RadarModel.impactPoint(position, velocity, gravity, groundY);
        this.ellipse = RadarModel.ellipse(velocity, ticks, phase == MissileFlight.Phase.BOOST);
        history.addLast(position);
        while (history.size() > HISTORY) history.removeFirst();
    }

    public UUID id() { return id; }
    public String designation() { return designation; }
    public V3 position() { return position; }
    public V3 velocity() { return velocity; }
    public MissileFlight.Phase phase() { return phase; }
    public V3 impact() { return impact; }
    public RadarModel.Ellipse ellipse() { return ellipse; }
    public int eta() { return eta; }
    public long lastSeenTick() { return lastSeenTick; }
    public long firstSeenTick() { return firstSeenTick; }
    public V3 firstSeen() { return firstSeen; }

    /** Whether its predicted impact (with its error) reaches a zone {@code radius} round {@code (x, z)}; radius 0 is any zone. */
    public boolean threatens(double x, double z, double radius) {
        return radius <= 0 || ellipse.touches(impact.x(), impact.z(), x, z, radius);
    }

    public Snapshot snapshot(boolean withHistory) {
        return new Snapshot(id, designation, position, velocity, (byte) phase.ordinal(), impact, (float) ellipse.along(), (float) ellipse.across(),
                (float) ellipse.heading(), eta, firstSeen, (float) apogee, withHistory ? List.copyOf(history) : List.of(position));
    }

    /**
     * What a screen is told of one track. {@code history} holds every plotted position the first time a screen hears of it,
     * then only the newest, which the screen appends to what it has.
     */
    public record Snapshot(UUID id, String designation, V3 position, V3 velocity, byte phase, V3 impact, float along, float across,
                           float heading, int eta, V3 firstSeen, float apogee, List<V3> history) {
        public MissileFlight.Phase phaseEnum() {
            MissileFlight.Phase[] phases = MissileFlight.Phase.values();
            return phases[Math.max(0, Math.min(phases.length - 1, phase))];
        }

        public RadarModel.Ellipse ellipse() {
            return new RadarModel.Ellipse(along, across, heading);
        }

        public boolean threatens(double x, double z, double radius) {
            return radius <= 0 || ellipse().touches(impact.x(), impact.z(), x, z, radius);
        }

        public void write(ByteBuf buf) {
            UUIDUtil.STREAM_CODEC.encode(buf, id);
            byte[] name = designation.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            buf.writeByte(Math.min(16, name.length));
            buf.writeBytes(name, 0, Math.min(16, name.length));
            vec(buf, position);
            fvec(buf, velocity);
            buf.writeByte(phase);
            vec(buf, impact);
            buf.writeFloat(along);
            buf.writeFloat(across);
            buf.writeFloat(heading);
            buf.writeInt(eta);
            vec(buf, firstSeen);
            buf.writeFloat(apogee);
            int count = Math.min(HISTORY, history.size());
            buf.writeByte(count);
            for (int i = history.size() - count; i < history.size(); i++) fvecRelative(buf, history.get(i), position);
        }

        public static Snapshot read(ByteBuf buf) {
            UUID id = UUIDUtil.STREAM_CODEC.decode(buf);
            int length = Math.min(16, buf.readUnsignedByte());
            byte[] name = new byte[length];
            buf.readBytes(name);
            V3 position = vec(buf);
            V3 velocity = fvec(buf);
            byte phase = buf.readByte();
            V3 impact = vec(buf);
            float along = buf.readFloat(), across = buf.readFloat(), heading = buf.readFloat();
            int eta = buf.readInt();
            V3 first = vec(buf);
            float apogee = buf.readFloat();
            int count = Math.min(HISTORY, buf.readUnsignedByte());
            List<V3> history = new ArrayList<>(count);
            for (int i = 0; i < count; i++) history.add(fvecRelative(buf, position));
            return new Snapshot(id, new String(name, java.nio.charset.StandardCharsets.UTF_8), position, velocity, phase, impact, along, across, heading,
                    eta, first, apogee, history);
        }

        private static void vec(ByteBuf buf, V3 v) {
            buf.writeDouble(v.x());
            buf.writeDouble(v.y());
            buf.writeDouble(v.z());
        }

        private static V3 vec(ByteBuf buf) {
            return new V3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        }

        private static void fvec(ByteBuf buf, V3 v) {
            buf.writeFloat((float) v.x());
            buf.writeFloat((float) v.y());
            buf.writeFloat((float) v.z());
        }

        private static V3 fvec(ByteBuf buf) {
            return new V3(buf.readFloat(), buf.readFloat(), buf.readFloat());
        }

        /** History points relative to the track, as floats: a few kilometres of trail stay exact to the block. */
        private static void fvecRelative(ByteBuf buf, V3 v, V3 origin) {
            fvec(buf, v.sub(origin));
        }

        private static V3 fvecRelative(ByteBuf buf, V3 origin) {
            return fvec(buf).add(origin);
        }
    }
}
