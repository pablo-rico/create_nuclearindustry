package org.papiricoh.create_nuclearindustry.data;

import org.papiricoh.create_nuclearindustry.missile.orbit.OrbitRegistry;

/**
 * The satellites' blockstates and item models. The ground station's block model is hand-written (textures in {@code tex_orbit.py}):
 * its plinth, whose mount and dish are partials drawn by its renderer; the station's item puts the three together
 * ({@code models/item/ground_station.json}). The satellite fairing is a rocket section, in {@link MissileArtData}.
 */
final class OrbitArtData {
    private OrbitArtData() {
    }

    static void blockStates(NuclearBlockStateProvider provider) {
        provider.simpleBlock(OrbitRegistry.GROUND_STATION.get(), provider.models().getExistingFile(provider.modLoc("block/ground_station")));
    }

    static void itemModels(NuclearItemModelProvider provider) {
        provider.basicItem(OrbitRegistry.EARLY_WARNING_SATELLITE.get());
        provider.basicItem(OrbitRegistry.COMMS_SATELLITE.get());
        provider.basicItem(OrbitRegistry.INCOMPLETE_SATELLITE.get());
    }
}
