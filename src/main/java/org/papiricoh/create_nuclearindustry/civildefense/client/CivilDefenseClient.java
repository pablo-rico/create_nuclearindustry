package org.papiricoh.create_nuclearindustry.civildefense.client;

import org.papiricoh.create_nuclearindustry.client.NuclearFeatureClient;

/** Client setup for sirens, the seismic detector, blast doors, the blast wave and shelters. */
public final class CivilDefenseClient implements NuclearFeatureClient {
    public static final CivilDefenseClient INSTANCE = new CivilDefenseClient();

    private CivilDefenseClient() {}
}
