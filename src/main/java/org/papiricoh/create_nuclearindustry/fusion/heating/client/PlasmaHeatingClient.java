package org.papiricoh.create_nuclearindustry.fusion.heating.client;

import org.papiricoh.create_nuclearindustry.client.NuclearFeatureClient;

/** Client setup for the tokamak's neutral beam injectors, radio-frequency antennas, viewports, plasma rendering and sounds. */
public final class PlasmaHeatingClient implements NuclearFeatureClient {
    public static final PlasmaHeatingClient INSTANCE = new PlasmaHeatingClient();

    private PlasmaHeatingClient() {}
}
