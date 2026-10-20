package org.papiricoh.create_nuclearindustry.missile.orbit;

import net.minecraft.server.level.ServerLevel;
import org.papiricoh.create_nuclearindustry.controlroom.link.LinkCode;
import org.papiricoh.create_nuclearindustry.missile.MissileConfig;

/**
 * What a network's satellites do for it, asked by place: whether one of a kind sees a spot, whether its communications satellites
 * relay between two, and how much they narrow a missile's spread. A satellite sees as far as its horizon over the radars' world
 * ({@link Orbit#footprint} with {@link MissileConfig#radarEarthRadius}).
 */
public final class SatelliteServices {
    private SatelliteServices() {
    }

    /** Whether a satellite of {@code kind} on network {@code code} sees the ground at {@code (x, z)} now. */
    public static boolean covered(ServerLevel level, String code, SatelliteKind kind, double x, double z) {
        if (!LinkCode.complete(code)) return false;
        SatelliteTracker tracker = SatelliteTracker.getIfPresent(level);
        if (tracker == null) return false;
        long now = level.getGameTime();
        for (Satellite s : tracker.ofCode(code))
            if (s.kind() == kind && s.orbit().covers(x, z, now, MissileConfig.radarEarthRadius)) return true;
        return false;
    }

    /** Whether one of network {@code code}'s communications satellites sees both {@code a} and {@code b} now: it relays between them. */
    public static boolean relays(ServerLevel level, String code, double ax, double az, double bx, double bz) {
        if (!LinkCode.complete(code)) return false;
        SatelliteTracker tracker = SatelliteTracker.getIfPresent(level);
        if (tracker == null) return false;
        long now = level.getGameTime();
        for (Satellite s : tracker.ofCode(code)) {
            if (s.kind() != SatelliteKind.COMMS) continue;
            if (s.orbit().covers(ax, az, now, MissileConfig.radarEarthRadius) && s.orbit().covers(bx, bz, now, MissileConfig.radarEarthRadius))
                return true;
        }
        return false;
    }

    /**
     * What a missile launched at {@code (lx, lz)} on network {@code code} and aimed at {@code (tx, tz)} has its spread multiplied by:
     * {@link OrbitConfig#gpsCepFactor} when one of the network's communications satellites sees both, else 1.
     */
    public static double gpsFactor(ServerLevel level, String code, double lx, double lz, double tx, double tz) {
        return relays(level, code, lx, lz, tx, tz) ? OrbitConfig.gpsCepFactor : 1;
    }
}
