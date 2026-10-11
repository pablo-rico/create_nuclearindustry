package org.papiricoh.create_nuclearindustry.fuelcycle.client;

import org.papiricoh.create_nuclearindustry.client.NuclearFeatureClient;

/** Client setup for the renewable fuel cycle and its fan treatments. */
public final class FuelCycleClient implements NuclearFeatureClient {
    public static final FuelCycleClient INSTANCE = new FuelCycleClient();

    private FuelCycleClient() {}
}
