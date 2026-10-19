package org.papiricoh.create_nuclearindustry.missile.flight;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Server side moments of each flight, fired by the {@link FlightTracker} as they happen. They go to the clients in range first
 * ({@link FlightFxBroadcaster}), then to whatever listens here (debris, radars, tests).
 */
public final class FlightEvents {

    public interface Listener {
        void onFlightEvent(ServerLevel level, Flight flight, FlightFxEvent event, int stage, Vec3 pos, Vec3 vel);
    }

    private static final List<Listener> LISTENERS = new CopyOnWriteArrayList<>();

    private FlightEvents() {
    }

    public static void addListener(Listener listener) {
        LISTENERS.add(listener);
    }

    public static void removeListener(Listener listener) {
        LISTENERS.remove(listener);
    }

    static void fire(ServerLevel level, Flight flight, FlightFxEvent event, int stage) {
        fire(level, flight, event, stage, flight.position(), flight.velocity());
    }

    static void fire(ServerLevel level, Flight flight, FlightFxEvent event, int stage, Vec3 pos, Vec3 vel) {
        FlightFxBroadcaster.event(level, flight, event, stage, pos, vel);
        for (Listener listener : LISTENERS) listener.onFlightEvent(level, flight, event, stage, pos, vel);
    }

    /** For things that fall from a flight (spent stages): a moment of theirs, sent to the clients like the flight's own. */
    public static void fireDebris(ServerLevel level, Flight flight, FlightFxEvent event, int stage, Vec3 pos, Vec3 vel) {
        fire(level, flight, event, stage, pos, vel);
    }
}
