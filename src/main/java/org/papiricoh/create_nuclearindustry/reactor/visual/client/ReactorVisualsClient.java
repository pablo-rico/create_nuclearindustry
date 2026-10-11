package org.papiricoh.create_nuclearindustry.reactor.visual.client;

import org.papiricoh.create_nuclearindustry.client.NuclearFeatureClient;

/** Client setup for the visible and audible fission reactor: its sounds and effects. */
public final class ReactorVisualsClient implements NuclearFeatureClient {
    public static final ReactorVisualsClient INSTANCE = new ReactorVisualsClient();

    private ReactorVisualsClient() {}
}
