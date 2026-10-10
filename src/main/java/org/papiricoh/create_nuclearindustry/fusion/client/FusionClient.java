package org.papiricoh.create_nuclearindustry.fusion.client;

import org.papiricoh.create_nuclearindustry.client.NuclearFeatureClient;

/** Client setup for the tokamak core: plasma physics, startup sequence, controller and its fluids. */
public final class FusionClient implements NuclearFeatureClient {
    public static final FusionClient INSTANCE = new FusionClient();

    private FusionClient() {}
}
