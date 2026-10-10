package org.papiricoh.create_nuclearindustry.reactor.pwr.client;

import org.papiricoh.create_nuclearindustry.client.NuclearFeatureClient;

/** Client setup for the pressurized water loop, steam generator, containment and the accident chain. */
public final class PwrClient implements NuclearFeatureClient {
    public static final PwrClient INSTANCE = new PwrClient();

    private PwrClient() {}
}
