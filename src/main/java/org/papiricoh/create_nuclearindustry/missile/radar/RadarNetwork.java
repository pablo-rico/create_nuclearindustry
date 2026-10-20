package org.papiricoh.create_nuclearindustry.missile.radar;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.papiricoh.create_nuclearindustry.controlroom.link.LinkCode;
import org.papiricoh.create_nuclearindustry.missile.MissileConfig;
import org.papiricoh.create_nuclearindustry.missile.flight.Flight;
import org.papiricoh.create_nuclearindustry.missile.flight.FlightTracker;
import org.papiricoh.create_nuclearindustry.missile.physics.BallisticModel;
import org.papiricoh.create_nuclearindustry.missile.physics.MissileFlight;
import org.papiricoh.create_nuclearindustry.missile.physics.V3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * The radar picture of a level, worked out once a second: which loaded, working radar sees which flight ({@link RadarModel}), and
 * the tracks they keep between them ({@link RadarTrack}). Radars that share a four-character link code and stand within
 * {@code missile.radar.networkRange} of each other see what any of them sees; a radar without a code sees only its own. Only
 * numbers: no chunk is loaded and no terrain read. Nothing is saved: after a reload the radars plot again.
 */
public final class RadarNetwork {
    /** Seconds a track lost by every radar is kept before it is dropped. */
    static final int LOST_SECONDS = 5;

    private static final Map<Level, Set<RadarBlockEntity>> RADARS = new WeakHashMap<>();
    private static final Map<ServerLevel, Plot> PLOTS = new WeakHashMap<>();

    private static final class Plot {
        long second = Long.MIN_VALUE;
        final Map<UUID, RadarTrack> tracks = new LinkedHashMap<>();
        final Map<RadarBlockEntity, Set<UUID>> seen = new HashMap<>();
    }

    private RadarNetwork() {
    }

    static void join(RadarBlockEntity radar) {
        if (radar.getLevel() != null && !radar.getLevel().isClientSide)
            RADARS.computeIfAbsent(radar.getLevel(), level -> Collections.newSetFromMap(new WeakHashMap<>())).add(radar);
    }

    static void leave(RadarBlockEntity radar) {
        if (radar.getLevel() == null) return;
        Set<RadarBlockEntity> radars = RADARS.get(radar.getLevel());
        if (radars != null) radars.remove(radar);
    }

    /** The radars of {@code level} known to the network: loaded, whether working or not. */
    public static List<RadarBlockEntity> radars(Level level) {
        Set<RadarBlockEntity> radars = RADARS.get(level);
        return radars == null ? List.of() : new ArrayList<>(radars);
    }

    /** Brings {@code level}'s radar picture up to this second; every call after the first in a second is free. */
    public static void update(ServerLevel level) {
        Plot plot = PLOTS.computeIfAbsent(level, l -> new Plot());
        long second = level.getGameTime() / 20;
        if (plot.second == second) return;
        plot.second = second;
        plot(level, plot);
    }

    /** Works the picture out again now, whenever it was last; for tests that change the config. */
    public static void refresh(ServerLevel level) {
        Plot plot = PLOTS.computeIfAbsent(level, l -> new Plot());
        plot.second = level.getGameTime() / 20;
        plot(level, plot);
    }

    private static void plot(ServerLevel level, Plot plot) {
        plot.seen.clear();
        FlightTracker tracker = FlightTracker.getIfPresent(level);
        Collection<Flight> flights = tracker == null ? List.of() : tracker.flights();
        BallisticModel.Params params = MissileConfig.flightParams();
        long tick = level.getGameTime();
        Set<UUID> detected = new HashSet<>();
        for (RadarBlockEntity radar : radars(level)) {
            if (!radar.operational()) continue;
            V3 at = radar.antenna();
            Set<UUID> sees = new HashSet<>();
            for (Flight flight : flights) {
                MissileFlight.Phase phase = flight.flight().phase(params);
                double reach = RadarModel.range(MissileConfig.radarBaseRange, radar.getSpeed(), MissileConfig.minimumRpm, RadarModel.signature(phase));
                if (RadarModel.detects(at, Flight.v3(flight.position()), reach, MissileConfig.radarEarthRadius)) sees.add(flight.id());
            }
            plot.seen.put(radar, sees);
            detected.addAll(sees);
        }
        Map<UUID, Flight> byId = new HashMap<>();
        flights.forEach(flight -> byId.put(flight.id(), flight));
        for (UUID id : detected) {
            Flight flight = byId.get(id);
            RadarTrack track = plot.tracks.computeIfAbsent(id, key -> new RadarTrack(key, Flight.v3(flight.position()), tick));
            // Impacts are predicted onto sea level, or onto the ground it was first seen leaving if that lies lower.
            double groundY = Math.min(RadarModel.SEA_LEVEL, track.firstSeen().y());
            track.measure(Flight.v3(flight.position()), Flight.v3(flight.velocity()), flight.flight().phase(params), params.gravity(), groundY, tick);
        }
        plot.tracks.values().removeIf(track -> !byId.containsKey(track.id()) || tick - track.lastSeenTick() > LOST_SECONDS * 20L);
    }

    /**
     * What {@code radar} shows: every track one of its network sees now (itself included), soonest impact first. Empty while it is
     * not working.
     */
    public static List<RadarTrack> view(RadarBlockEntity radar) {
        if (!(radar.getLevel() instanceof ServerLevel level) || !radar.operational()) return List.of();
        Plot plot = PLOTS.get(level);
        if (plot == null) return List.of();
        Set<UUID> ids = new HashSet<>();
        for (Map.Entry<RadarBlockEntity, Set<UUID>> entry : plot.seen.entrySet()) {
            if (entry.getKey() == radar || peers(radar, entry.getKey())) ids.addAll(entry.getValue());
        }
        List<RadarTrack> tracks = new ArrayList<>();
        for (UUID id : ids) {
            RadarTrack track = plot.tracks.get(id);
            if (track != null) tracks.add(track);
        }
        tracks.sort(Comparator.comparingInt(RadarTrack::eta).thenComparing(RadarTrack::designation));
        return tracks;
    }

    /** Whether two radars share their picture: the same complete link code and close enough. */
    public static boolean peers(RadarBlockEntity a, RadarBlockEntity b) {
        if (!LinkCode.complete(a.code()) || !a.code().equals(b.code()) || a.getLevel() != b.getLevel()) return false;
        double range = MissileConfig.radarNetworkRange;
        if (range <= 0) return true;
        double dx = a.getBlockPos().getX() - b.getBlockPos().getX(), dz = a.getBlockPos().getZ() - b.getBlockPos().getZ();
        return dx * dx + dz * dz <= range * range;
    }

    /** Every track in {@code level}, by flight; for the command table and tests. */
    public static Map<UUID, RadarTrack> tracks(ServerLevel level) {
        Plot plot = PLOTS.get(level);
        return plot == null ? Map.of() : Collections.unmodifiableMap(plot.tracks);
    }
}
