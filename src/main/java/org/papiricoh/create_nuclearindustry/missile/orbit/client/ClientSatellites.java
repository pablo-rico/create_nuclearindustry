package org.papiricoh.create_nuclearindustry.missile.orbit.client;

import org.papiricoh.create_nuclearindustry.missile.orbit.OrbitSyncPayload;
import org.papiricoh.create_nuclearindustry.missile.orbit.Satellite;

import java.util.List;

/** The satellites over the client's level, as last sent; their places come from their orbits and the level's game time. */
public final class ClientSatellites {
    private static List<Satellite> satellites = List.of();

    private ClientSatellites() {
    }

    public static void accept(OrbitSyncPayload payload) {
        satellites = payload.satellites();
    }

    public static List<Satellite> all() {
        return satellites;
    }

    /** Forgets them all: on leaving a world. */
    public static void clear() {
        satellites = List.of();
    }
}
