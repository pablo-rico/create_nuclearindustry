package org.papiricoh.create_nuclearindustry.missile.radar;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.create_nuclearindustry.missile.MissileConfig;
import org.papiricoh.create_nuclearindustry.missile.flight.FlightTracker;
import java.util.*;

/** One immutable snapshot per dimension and second. No terrain or entity queries. */
public final class RadarContacts {
    public record Contact(UUID id, BlockPos origin, Vec3 position, Vec3 velocity, Vec3 impact, int ticks) {}
    private record Snapshot(long second, List<Contact> contacts) {}
    private static final Map<ServerLevel, Snapshot> CACHE = new WeakHashMap<>();
    public static List<Contact> get(ServerLevel level) {
        long second = level.getGameTime() / 20;
        Snapshot prior = CACHE.get(level);
        if (prior != null && prior.second == second) return prior.contacts;
        FlightTracker tracker = FlightTracker.getIfPresent(level);
        List<Contact> contacts = tracker == null ? List.of() : tracker.flights().stream().map(f -> new Contact(f.id(), f.origin(),
                f.position(), f.velocity(), f.aim(), (int) Math.ceil(f.flight().ticksToImpact(MissileConfig.flightParams())))).toList();
        CACHE.put(level, new Snapshot(second, contacts));
        return contacts;
    }
    private RadarContacts() {}
}
