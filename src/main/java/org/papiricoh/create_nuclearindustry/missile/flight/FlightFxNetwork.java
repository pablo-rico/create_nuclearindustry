package org.papiricoh.create_nuclearindustry.missile.flight;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Registers the flight effect packets; their client side is {@code missile.flight.client.ClientFlights}. */
public final class FlightFxNetwork {

    private FlightFxNetwork() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(RegisterPayloadHandlersEvent.class, event -> event.registrar("1")
                .playToClient(FlightFxStatePayload.TYPE, FlightFxStatePayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(
                        () -> org.papiricoh.create_nuclearindustry.missile.flight.client.ClientFlights.handleState(payload)))
                .playToClient(FlightFxEventPayload.TYPE, FlightFxEventPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(
                        () -> org.papiricoh.create_nuclearindustry.missile.flight.client.ClientFlights.handleEvent(payload))));
    }
}
