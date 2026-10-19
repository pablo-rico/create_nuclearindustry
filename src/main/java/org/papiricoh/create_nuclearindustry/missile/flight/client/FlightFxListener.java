package org.papiricoh.create_nuclearindustry.missile.flight.client;

import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.missile.flight.FlightFxEvent;

/** Hears each flight moment the server sends ({@link ClientFlights#addListener}). */
public interface FlightFxListener {
    /**
     * @param flight the flight as the client knows it, or null if it never heard of it (out of range until now); an IMPACT or
     *               ABORT flight is dropped right after its listeners hear it
     * @param flags  {@link org.papiricoh.create_nuclearindustry.missile.flight.FlightFxState} flags at the moment
     */
    void onEvent(@Nullable ClientFlight flight, FlightFxEvent event, int stage, Vec3 pos, Vec3 vel, int flags);
}
