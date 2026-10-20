package org.papiricoh.create_nuclearindustry.missile.holo.client;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.missile.holo.HoloTracksPayload;
import org.papiricoh.create_nuclearindustry.missile.orbit.Orbit;
import org.papiricoh.create_nuclearindustry.missile.orbit.Satellite;
import org.papiricoh.create_nuclearindustry.missile.orbit.client.ClientSatellites;
import org.papiricoh.create_nuclearindustry.missile.physics.V3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Where the command table's satellites are, for its screen and its hologram alike: from their orbits as the client last got them
 * ({@link ClientSatellites}), so they fly on smoothly between pictures and their ground tracks come round the world border as the
 * orbits do; from the picture's own place and ground speed for one the client has not been sent. Offsets are from the centre of
 * the table's centre piece, as the picture's.
 */
final class SatelliteViews {
    /** How far along its ground track, ahead and behind, a low satellite's track is drawn: blocks. */
    static final double TRACK_REACH = 3000;

    private SatelliteViews() {
    }

    @Nullable
    static Orbit orbit(UUID id) {
        for (Satellite satellite : ClientSatellites.all()) if (satellite.id().equals(id)) return satellite.orbit();
        return null;
    }

    /** Its ground offset from the table at game time {@code time}, {@code since} ticks after the picture: {@code dx, dz}. */
    static double[] offset(HoloTracksPayload.Sat sat, BlockPos centre, double time, double since) {
        Orbit orbit = orbit(sat.id());
        if (orbit != null) {
            V3 at = orbit.position(time);
            return new double[]{at.x() - centre.getX() - 0.5, at.z() - centre.getZ() - 0.5};
        }
        return new double[]{sat.x() + sat.vx() * since, sat.z() + sat.vz() * since};
    }

    /**
     * Its ground track {@link #TRACK_REACH} blocks either side of where it is, as segments ({@code dx0, dz0, dx1, dz1} offsets
     * from the table) every other one left out so it draws dashed; the segment where it comes round the border is dropped.
     * Empty for a parked satellite.
     */
    static List<double[]> track(HoloTracksPayload.Sat sat, BlockPos centre, double time, double since, int steps) {
        List<double[]> out = new ArrayList<>();
        Orbit orbit = orbit(sat.id());
        double ox = centre.getX() + 0.5, oz = centre.getZ() + 0.5;
        if (orbit != null) {
            double speed = orbit.groundSpeed();
            if (speed <= 1e-6) return out;
            double span = TRACK_REACH / speed, step = span / steps, longest = speed * step * 1.5 + 1;
            for (int i = -steps; i < steps; i += 2) {
                V3 a = orbit.position(time + step * i), b = orbit.position(time + step * (i + 1));
                if (Math.hypot(b.x() - a.x(), b.z() - a.z()) > longest) continue;
                out.add(new double[]{a.x() - ox, a.z() - oz, b.x() - ox, b.z() - oz});
            }
            return out;
        }
        double speed = Math.hypot(sat.vx(), sat.vz());
        if (speed <= 1e-6) return out;
        double ux = sat.vx() / speed, uz = sat.vz() / speed, x = sat.x() + sat.vx() * since, z = sat.z() + sat.vz() * since;
        double step = TRACK_REACH / steps;
        for (int i = -steps; i < steps; i += 2)
            out.add(new double[]{x + ux * step * i, z + uz * step * i, x + ux * step * (i + 1), z + uz * step * (i + 1)});
        return out;
    }

    /** Whether it sees anything worth a ring: a finite footprint. */
    static boolean hasFootprint(HoloTracksPayload.Sat sat) {
        return Float.isFinite(sat.footprint()) && sat.footprint() > 0 && sat.footprint() < 1.0E6f;
    }

    /** Ticks as {@code m:ss}. */
    static String clock(int ticks) {
        int seconds = Math.max(0, ticks) / 20;
        return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
    }

    /** Blocks above sea level. */
    static long altitude(HoloTracksPayload.Sat sat) {
        return Math.round(sat.y() - Orbit.SEA_LEVEL);
    }
}
