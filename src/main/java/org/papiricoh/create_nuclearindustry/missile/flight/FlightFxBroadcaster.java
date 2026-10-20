package org.papiricoh.create_nuclearindustry.missile.flight;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.papiricoh.create_nuclearindustry.missile.MissileConfig;
import org.papiricoh.create_nuclearindustry.missile.physics.BallisticModel;
import org.papiricoh.create_nuclearindustry.missile.physics.MissileFlight;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Tells players what flies within {@code missile.fx.range} of them, well past the distance entities are tracked at, so a launch
 * a kilometre off still draws its plume and trail. Near flights go every tick, far ones every {@code missile.fx.farUpdateTicks};
 * a flight's shape only the first time a player hears of it. Positions only: never a target.
 */
public final class FlightFxBroadcaster {
    /** Within this many blocks a flight's state is sent every tick. */
    static final double NEAR = 256;

    /** The flights each player has been sent the shape of. */
    private static final Map<ServerPlayer, Set<UUID>> known = new WeakHashMap<>();

    private FlightFxBroadcaster() {
    }

    /** Sends this tick's states. The tracker calls it once a tick, after flying everything. */
    static void tick(ServerLevel level, Iterable<Flight> flights) {
        double range = MissileConfig.fxRange;
        long time = level.getGameTime();
        boolean farTick = time % Math.max(1, MissileConfig.fxFarUpdateTicks) == 0;
        BallisticModel.Params params = MissileConfig.flightParams();
        for (ServerPlayer player : level.players()) {
            // Forgotten once out of range, so a player coming back is sent the shape again.
            Set<UUID> sent = known.computeIfAbsent(player, p -> new HashSet<>());
            Set<UUID> within = new HashSet<>();
            List<Flight> near = new ArrayList<>();
            for (Flight flight : flights) {
                double d = horizontal(player.position(), flight.position());
                if (d > range) continue;
                within.add(flight.id());
                if (farTick || d <= NEAR) near.add(flight);
            }
            sent.retainAll(within);
            if (near.isEmpty()) continue;
            near.sort(Comparator.comparingDouble(f -> horizontal(player.position(), f.position())));
            List<FlightFxState> states = new ArrayList<>();
            for (Flight flight : near.subList(0, Math.min(FlightFxStatePayload.MAX_FLIGHTS, near.size()))) {
                boolean first = sent.add(flight.id());
                states.add(state(flight, params, first));
            }
            PacketDistributor.sendToPlayer(player, new FlightFxStatePayload(time, (float) params.gravity(), states));
        }
    }

    static FlightFxState state(Flight flight, BallisticModel.Params params, boolean withShape) {
        MissileFlight f = flight.flight();
        Vec3 pos = flight.position(), vel = flight.velocity();
        MissileFlight.Phase phase = f.phase(params);
        if (flight.kind() == Flight.Kind.SATELLITE && f.burnedOut() && !f.impacted()) phase = MissileFlight.Phase.MIDCOURSE;
        int flags = 0;
        if (phase == MissileFlight.Phase.BOOST) flags |= FlightFxState.BURNING;
        if (f.rising(params)) flags |= FlightFxState.RISING;
        if (flight.warhead().nuclear()) flags |= FlightFxState.NUCLEAR_WARHEAD;
        if (flight.puppet != null) flags |= FlightFxState.PUPPET;
        if (f.burnedOut()) flags |= FlightFxState.BURNED_OUT;
        if (flight.kind() == Flight.Kind.INTERCEPTOR) {
            flags |= FlightFxState.INTERCEPTOR;
            if (flight.intercept() != null && flight.intercept().launcherType() == org.papiricoh.create_nuclearindustry.missile.defense.ShooterType.BATTERY)
                flags |= FlightFxState.CANISTER;
        }
        return new FlightFxState(flight.id(), pos.x, pos.y, pos.z, (float) vel.x, (float) vel.y, (float) vel.z, (byte) phase.ordinal(),
                (byte) f.stage(), (byte) flags, flight.heading(), withShape ? flight.shape() : null);
    }

    /** Sends a moment of {@code flight} to everyone in range of where it happened. */
    static void event(ServerLevel level, Flight flight, FlightFxEvent event, int stage, Vec3 pos, Vec3 vel) {
        double range = MissileConfig.fxRange;
        int flags = state(flight, MissileConfig.flightParams(), false).flags();
        FlightFxEventPayload payload = new FlightFxEventPayload(flight.id(), event.id(), (byte) stage, (byte) flags, pos.x, pos.y, pos.z,
                (float) vel.x, (float) vel.y, (float) vel.z);
        for (ServerPlayer player : level.players()) {
            if (horizontal(player.position(), pos) <= range) PacketDistributor.sendToPlayer(player, payload);
        }
    }

    /**
     * Sends a moment that belongs to no flight, a satellite's, to everyone in range of where it happened: {@code id} is the
     * satellite's.
     */
    public static void event(ServerLevel level, java.util.UUID id, FlightFxEvent event, int flags, Vec3 pos, Vec3 vel) {
        double range = MissileConfig.fxRange;
        FlightFxEventPayload payload = new FlightFxEventPayload(id, event.id(), (byte) 0, (byte) flags, pos.x, pos.y, pos.z,
                (float) vel.x, (float) vel.y, (float) vel.z);
        for (ServerPlayer player : level.players()) {
            if (horizontal(player.position(), pos) <= range) PacketDistributor.sendToPlayer(player, payload);
        }
    }

    private static double horizontal(Vec3 a, Vec3 b) {
        double dx = a.x - b.x, dz = a.z - b.z;
        return Math.sqrt(dx * dx + dz * dz);
    }
}
