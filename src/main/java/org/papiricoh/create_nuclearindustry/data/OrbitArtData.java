package org.papiricoh.create_nuclearindustry.data;

import net.neoforged.neoforge.client.model.generators.ModelFile;
import org.papiricoh.create_nuclearindustry.missile.orbit.OrbitRegistry;
import org.papiricoh.create_nuclearindustry.missile.silo.SiloRegistry;

/**
 * The satellites' blockstates and item models. The block models are hand-written (textures in {@code tex_orbit.py}): the fairing's
 * ogive with its seam, and the ground station's plinth, whose mount and dish are partials drawn by its renderer; the station's item
 * puts the three together ({@code models/item/ground_station.json}).
 */
final class OrbitArtData {
    private OrbitArtData() {
    }

    static void blockStates(NuclearBlockStateProvider provider) {
        ModelFile fairing = provider.models().getExistingFile(provider.modLoc("block/rocket_satellite_fairing"));
        provider.simpleBlock(SiloRegistry.SATELLITE_FAIRING.get(), fairing);
        provider.simpleBlockItem(SiloRegistry.SATELLITE_FAIRING.get(), fairing);
        provider.simpleBlock(OrbitRegistry.GROUND_STATION.get(), provider.models().getExistingFile(provider.modLoc("block/ground_station")));
    }

    static void itemModels(NuclearItemModelProvider provider) {
        provider.basicItem(OrbitRegistry.EARLY_WARNING_SATELLITE.get());
        provider.basicItem(OrbitRegistry.COMMS_SATELLITE.get());
        provider.basicItem(OrbitRegistry.INCOMPLETE_SATELLITE.get());
    }
}
