package org.papiricoh.create_nuclearindustry.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.tags.BlockTags;
import org.papiricoh.create_nuclearindustry.missile.orbit.OrbitRegistry;
import org.papiricoh.create_nuclearindustry.missile.silo.SiloRegistry;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockTags;

/** Datagen of satellites: the ground station, the satellite fairing and the satellites, and their lang. */
final class OrbitDataGen extends AreaDataGen {
    static final OrbitDataGen INSTANCE = new OrbitDataGen();

    private OrbitDataGen() {
    }

    @Override
    void lang() {
        OrbitLang.add();
    }

    @Override
    void blockTags(NuclearBlockTagsProvider provider, HolderLookup.Provider lookup) {
        provider.tagFor(BlockTags.MINEABLE_WITH_PICKAXE).add(OrbitRegistry.GROUND_STATION.get(), SiloRegistry.SATELLITE_FAIRING.get());
        provider.tagFor(MultiblockTags.MULTIBLOCK_PARTS).add(SiloRegistry.SATELLITE_FAIRING.get());
    }
}
