package org.papiricoh.create_nuclearindustry.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.minecraft.tags.BlockTags;
import org.papiricoh.create_nuclearindustry.missile.silo.SiloRegistry;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockTags;
import org.papiricoh.create_nuclearindustry.data.recipe.KillVehicleRecipes;
import net.minecraft.data.DataProvider;
import net.minecraft.data.CachedOutput;

import java.util.concurrent.CompletableFuture;

/** The kill vehicle's blockstates, models, tags and recipes. */
final class InterceptorSiloData {
    private InterceptorSiloData() {
    }

    static void blockStates(NuclearBlockStateProvider provider) {
    }

    static void itemModels(NuclearItemModelProvider provider) {
    }

    static void blockTags(NuclearBlockTagsProvider provider) {
        provider.tagFor(BlockTags.MINEABLE_WITH_PICKAXE).add(SiloRegistry.KILL_VEHICLE.get());
        provider.tagFor(MultiblockTags.MULTIBLOCK_PARTS).add(SiloRegistry.KILL_VEHICLE.get());
    }

    static void itemTags(NuclearItemTagsProvider provider) {
    }

    static void blockLoot(NuclearBlockLoot loot) {
    }

    static void addProviders(GatherDataEvent event, DataGenerator generator, PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
        generator.addProvider(event.includeServer(), new Named("Kill vehicle crafting recipe", new KillVehicleRecipes(output, lookup)));
    }
    private record Named(String name, DataProvider provider) implements DataProvider {
        @Override public CompletableFuture<?> run(CachedOutput output) { return provider.run(output); }
        @Override public String getName() { return name; }
    }
}
