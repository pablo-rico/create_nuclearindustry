package org.papiricoh.create_nuclearindustry.controlroom.client;

import org.papiricoh.create_nuclearindustry.client.NuclearFeatureClient;

/** Client setup for the chart recorder, the annunciator panel and automatic rod control. */
public final class ControlRoom2Client implements NuclearFeatureClient {
    public static final ControlRoom2Client INSTANCE = new ControlRoom2Client();

    private ControlRoom2Client() {}
}
