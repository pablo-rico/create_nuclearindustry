package org.papiricoh.create_nuclearindustry.missile.fx.client;

import net.neoforged.bus.api.IEventBus;
import org.papiricoh.create_nuclearindustry.client.NuclearFeatureClient;
import org.papiricoh.create_nuclearindustry.missile.flight.client.ClientFlights;

/** Client side of {@link org.papiricoh.create_nuclearindustry.missile.fx.RocketFxRegistry}: the rocket effects. */
public final class RocketFxClient implements NuclearFeatureClient {
    public static final RocketFxClient INSTANCE = new RocketFxClient();

    private RocketFxClient() {
    }

    @Override
    public void init(IEventBus modEventBus) {
        // Part of the shared flight contract: keep this call whatever else changes here.
        ClientFlights.init();
    }
}
