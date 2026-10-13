package org.papiricoh.create_nuclearindustry.waste.client;

import org.papiricoh.create_nuclearindustry.client.NuclearFeatureClient;

/** Client setup for waste classes, treatments and the geological repository. */
public final class WasteClient implements NuclearFeatureClient {
    public static final WasteClient INSTANCE = new WasteClient();

    private WasteClient() {}
}
