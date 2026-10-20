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
        SatelliteSiloLang.add();
    }

    @Override
    void addProviders(net.neoforged.neoforge.data.event.GatherDataEvent event, net.minecraft.data.DataGenerator generator,
            net.minecraft.data.PackOutput output, java.util.concurrent.CompletableFuture<HolderLookup.Provider> lookup,
            net.neoforged.neoforge.common.data.ExistingFileHelper existingFiles) {
        generator.addProvider(event.includeServer(), new Named("Satellite recipes", new org.papiricoh.create_nuclearindustry.data.recipe.OrbitRecipes(output, lookup)));
        generator.addProvider(event.includeServer(), new Named("Satellite assembly", org.papiricoh.create_nuclearindustry.data.recipe.OrbitRecipes.assembly(output, lookup)));
    }

    private record Named(String name, net.minecraft.data.DataProvider provider) implements net.minecraft.data.DataProvider {
        @Override
        public java.util.concurrent.CompletableFuture<?> run(net.minecraft.data.CachedOutput output) {
            return provider.run(output);
        }

        @Override
        public String getName() {
            return name;
        }
    }

    @Override
    void blockTags(NuclearBlockTagsProvider provider, HolderLookup.Provider lookup) {
        provider.tagFor(BlockTags.MINEABLE_WITH_PICKAXE).add(OrbitRegistry.GROUND_STATION.get(), SiloRegistry.SATELLITE_FAIRING.get());
        provider.tagFor(MultiblockTags.MULTIBLOCK_PARTS).add(SiloRegistry.SATELLITE_FAIRING.get());
    }
}
