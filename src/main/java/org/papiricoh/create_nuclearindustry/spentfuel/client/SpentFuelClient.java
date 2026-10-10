package org.papiricoh.create_nuclearindustry.spentfuel.client;

import org.papiricoh.create_nuclearindustry.client.NuclearFeatureClient;

/** Client setup for spent fuel cooling: pool racks and dry casks. */
public final class SpentFuelClient implements NuclearFeatureClient {
    public static final SpentFuelClient INSTANCE = new SpentFuelClient();

    private SpentFuelClient() {}
}
