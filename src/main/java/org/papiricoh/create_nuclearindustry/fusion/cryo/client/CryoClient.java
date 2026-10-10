package org.papiricoh.create_nuclearindustry.fusion.cryo.client;

import org.papiricoh.create_nuclearindustry.client.NuclearFeatureClient;

/** Client setup for the cryogenic plant, the tokamak's vacuum pumps and cryogenic ports. */
public final class CryoClient implements NuclearFeatureClient {
    public static final CryoClient INSTANCE = new CryoClient();

    private CryoClient() {}
}
