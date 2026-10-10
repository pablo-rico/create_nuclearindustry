package org.papiricoh.create_nuclearindustry.reactor.pwr;

import org.papiricoh.create_nuclearindustry.NuclearFeature;

/** Content for the pressurized water loop, steam generator, containment and the accident chain: blocks, items and block entities register here. */
public final class PwrRegistry implements NuclearFeature {
    public static final PwrRegistry INSTANCE = new PwrRegistry();

    private PwrRegistry() {}
}
