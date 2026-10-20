package org.papiricoh.create_nuclearindustry.missile.orbit;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import org.papiricoh.create_nuclearindustry.missile.physics.V3;

/**
 * A satellite's orbit over a flat world bounded by its world border, simplified so a player can read it off a map. Pure: no game
 * classes but NBT, no config.
 * <ul>
 *   <li>{@link OrbitType#GEO}: parked {@code altitude} above sea level over {@code (anchorX, anchorZ)}.</li>
 *   <li>{@link OrbitType#LEO}: flies straight at {@code speed} blocks per tick along {@code heading} (radians, 0 is +Z, as a
 *   flight's) and, leaving its {@link Bounds} (the world border when it was launched) on one side, comes back in on the other, as
 *   if the world were round. Unless it flies square to the border its ground track shifts every crossing, so over time it passes
 *   over all of the world. At {@code epoch} (a game tick) it is over its anchor.</li>
 * </ul>
 *
 * @param altitude blocks above sea level ({@link #SEA_LEVEL})
 */
public record Orbit(OrbitType type, double anchorX, double anchorZ, double heading, double altitude, long epoch, double speed, Bounds bounds) {
    /** Sea level, as the radar's horizon measures it. */
    public static final double SEA_LEVEL = 63;

    /** The square a low orbit wraps round: the world border, or a part of it when the border is too large to cross in useful time. */
    public record Bounds(double minX, double minZ, double maxX, double maxZ) {
        public static final Bounds UNBOUNDED = new Bounds(-3.0E7, -3.0E7, 3.0E7, 3.0E7);

        public Bounds {
            if (maxX - minX < 1) maxX = minX + 1;
            if (maxZ - minZ < 1) maxZ = minZ + 1;
        }

        public double width() {
            return maxX - minX;
        }

        public double depth() {
            return maxZ - minZ;
        }

        public boolean contains(double x, double z) {
            return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
        }

        public double clampX(double x) {
            return Math.max(minX, Math.min(maxX, x));
        }

        public double clampZ(double z) {
            return Math.max(minZ, Math.min(maxZ, z));
        }

        double wrapX(double x) {
            return minX + floorMod(x - minX, width());
        }

        double wrapZ(double z) {
            return minZ + floorMod(z - minZ, depth());
        }

        private static double floorMod(double a, double m) {
            return a - Math.floor(a / m) * m;
        }
    }

    public Orbit {
        altitude = Math.max(0, altitude);
        speed = Math.max(0, speed);
        anchorX = bounds.clampX(anchorX);
        anchorZ = bounds.clampZ(anchorZ);
    }

    public static Orbit geo(double x, double z, double altitude, long epoch, Bounds bounds) {
        return new Orbit(OrbitType.GEO, x, z, 0, altitude, epoch, 0, bounds);
    }

    public static Orbit leo(double x, double z, double heading, double altitude, long epoch, double speed, Bounds bounds) {
        return new Orbit(OrbitType.LEO, x, z, heading, altitude, epoch, speed, bounds);
    }

    public boolean geostationary() {
        return type == OrbitType.GEO;
    }

    /** Height above the world's origin, y. */
    public double y() {
        return SEA_LEVEL + altitude;
    }

    /** Ground speed, blocks per tick; 0 for a parked one. */
    public double groundSpeed() {
        return geostationary() ? 0 : speed;
    }

    /** Where it is at game tick {@code tick} (fractions allowed): always inside its bounds. */
    public V3 position(double tick) {
        if (geostationary()) return new V3(anchorX, y(), anchorZ);
        double along = (tick - epoch) * speed;
        return new V3(bounds.wrapX(anchorX + Math.sin(heading) * along), y(), bounds.wrapZ(anchorZ + Math.cos(heading) * along));
    }

    /**
     * Where it is at {@code tick} without coming round: the straight line it flies, which leaves the bounds. For aiming at it over
     * a short look-ahead: an interceptor does not wrap round the world.
     */
    public V3 unwrapped(double tick, double fromTick) {
        V3 from = position(fromTick);
        if (geostationary()) return from;
        double along = (tick - fromTick) * speed;
        return new V3(from.x() + Math.sin(heading) * along, y(), from.z() + Math.cos(heading) * along);
    }

    /** Its velocity, blocks per tick: along its track. */
    public V3 velocity(double tick) {
        if (geostationary()) return V3.ZERO;
        return new V3(Math.sin(heading), 0, Math.cos(heading)).scale(speed);
    }

    /**
     * How far over the ground it sees, radius around the point under it: its horizon over a world of radius {@code earthRadius}
     * (0 or less for no horizon: it sees everything).
     */
    public double footprint(double earthRadius) {
        if (earthRadius <= 0) return Double.POSITIVE_INFINITY;
        return Math.sqrt(2 * earthRadius * Math.max(1, altitude));
    }

    /** Whether at {@code tick} it sees the ground at {@code (x, z)}. */
    public boolean covers(double x, double z, double tick, double earthRadius) {
        V3 at = position(tick);
        double dx = at.x() - x, dz = at.z() - z;
        double reach = footprint(earthRadius);
        return dx * dx + dz * dz <= reach * reach;
    }

    /**
     * The first tick from {@code tick} (inclusive) within {@code horizon} ticks it sees {@code (x, z)}, checked every {@code step}
     * ticks, or -1 if it does not.
     */
    public long nextPass(double x, double z, long tick, long horizon, double earthRadius, int step) {
        int stride = Math.max(1, step);
        for (long t = tick; t <= tick + horizon; t += stride) if (covers(x, z, t, earthRadius)) return t;
        return -1;
    }

    /** The same orbit, lower or higher: what decay does. */
    public Orbit withAltitude(double newAltitude) {
        return new Orbit(type, anchorX, anchorZ, heading, newAltitude, epoch, speed, bounds);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Type", type.name().toLowerCase(java.util.Locale.ROOT));
        tag.putDouble("AnchorX", anchorX);
        tag.putDouble("AnchorZ", anchorZ);
        tag.putDouble("Heading", heading);
        tag.putDouble("Altitude", altitude);
        tag.putLong("Epoch", epoch);
        tag.putDouble("Speed", speed);
        tag.putDouble("MinX", bounds.minX);
        tag.putDouble("MinZ", bounds.minZ);
        tag.putDouble("MaxX", bounds.maxX);
        tag.putDouble("MaxZ", bounds.maxZ);
        return tag;
    }

    public static Orbit load(CompoundTag tag) {
        Bounds bounds = tag.contains("MinX") ? new Bounds(tag.getDouble("MinX"), tag.getDouble("MinZ"), tag.getDouble("MaxX"), tag.getDouble("MaxZ"))
                : Bounds.UNBOUNDED;
        return new Orbit(OrbitType.byName(tag.getString("Type")), tag.getDouble("AnchorX"), tag.getDouble("AnchorZ"), tag.getDouble("Heading"),
                tag.getDouble("Altitude"), tag.getLong("Epoch"), tag.getDouble("Speed"), bounds);
    }

    public void write(ByteBuf b) {
        b.writeByte(type.ordinal());
        b.writeDouble(anchorX);
        b.writeDouble(anchorZ);
        b.writeFloat((float) heading);
        b.writeFloat((float) altitude);
        b.writeLong(epoch);
        b.writeFloat((float) speed);
        b.writeDouble(bounds.minX);
        b.writeDouble(bounds.minZ);
        b.writeDouble(bounds.maxX);
        b.writeDouble(bounds.maxZ);
    }

    public static Orbit read(ByteBuf b) {
        return new Orbit(OrbitType.byOrdinal(b.readByte()), b.readDouble(), b.readDouble(), b.readFloat(), b.readFloat(), b.readLong(),
                b.readFloat(), new Bounds(b.readDouble(), b.readDouble(), b.readDouble(), b.readDouble()));
    }
}
