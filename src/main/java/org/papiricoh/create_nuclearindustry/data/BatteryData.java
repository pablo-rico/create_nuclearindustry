package org.papiricoh.create_nuclearindustry.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import org.papiricoh.create_nuclearindustry.missile.defense.DefenseRegistry;
import org.papiricoh.create_nuclearindustry.data.recipe.DefenseRecipes;
import net.minecraft.data.DataProvider;
import net.minecraft.data.CachedOutput;

import java.util.concurrent.CompletableFuture;

/** The interceptor battery's blockstates, models, tags, loot and recipes. */
final class BatteryData {
    private BatteryData() {
    }

    static void blockStates(NuclearBlockStateProvider provider) {
    }

    static void itemModels(NuclearItemModelProvider provider) {
        provider.basicItem(DefenseRegistry.INTERCEPTOR.get());
        provider.basicItem(DefenseRegistry.INCOMPLETE_INTERCEPTOR.get());
    }

    static void blockTags(NuclearBlockTagsProvider provider) {
        provider.tagFor(BlockTags.MINEABLE_WITH_PICKAXE).add(DefenseRegistry.BATTERY.get(), DefenseRegistry.BATTERY_PART.get());
        provider.tagFor(TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("create", "non_movable")))
                .add(DefenseRegistry.BATTERY.get(), DefenseRegistry.BATTERY_PART.get());
    }

    static void itemTags(NuclearItemTagsProvider provider) {
    }

    static void blockLoot(NuclearBlockLoot loot) {
    }

    static void addProviders(GatherDataEvent event, DataGenerator generator, PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
        generator.addProvider(event.includeServer(), new Named("Defense battery recipes", new DefenseRecipes(output, lookup)));
        generator.addProvider(event.includeServer(), new Named("Defense interceptor assembly", DefenseRecipes.assembly(output, lookup)));
    }
    private record Named(String name, DataProvider provider) implements DataProvider {
        @Override public CompletableFuture<?> run(CachedOutput output) { return provider.run(output); }
        @Override public String getName() { return name; }
    }
}
