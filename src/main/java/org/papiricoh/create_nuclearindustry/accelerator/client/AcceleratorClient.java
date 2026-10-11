package org.papiricoh.create_nuclearindustry.accelerator.client;

import org.papiricoh.create_nuclearindustry.client.NuclearFeatureClient;

/** Client setup for the particle accelerator: the beam renderer, screens and sounds. */
public final class AcceleratorClient implements NuclearFeatureClient {
    public static final AcceleratorClient INSTANCE = new AcceleratorClient();

    private AcceleratorClient() {}
}
