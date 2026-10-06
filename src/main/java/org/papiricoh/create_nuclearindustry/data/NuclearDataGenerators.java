package org.papiricoh.create_nuclearindustry.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.data.recipe.*;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class NuclearDataGenerators {

    private NuclearDataGenerators() {}

    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        ExistingFileHelper existingFiles = event.getExistingFileHelper();

        DatapackBuiltinEntriesProvider datapackEntries = generator.addProvider(event.includeServer(),
                new DatapackBuiltinEntriesProvider(output, event.getLookupProvider(), NuclearWorldgen.registries(),
                        Set.of(Create_NuclearIndustry.MODID)));
        CompletableFuture<HolderLookup.Provider> lookup = datapackEntries.getRegistryProvider();

        NuclearBlockTagsProvider blockTags = generator.addProvider(event.includeServer(),
                new NuclearBlockTagsProvider(output, lookup, existingFiles));
        generator.addProvider(event.includeServer(),
                new NuclearItemTagsProvider(output, lookup, blockTags.contentsGetter(), existingFiles));
        generator.addProvider(event.includeServer(), new LootTableProvider(output, Set.of(),
                List.of(new LootTableProvider.SubProviderEntry(NuclearBlockLoot::new, LootContextParamSets.BLOCK)), lookup));

        generator.addProvider(event.includeServer(), new NuclearCraftingRecipes(output, lookup));
        generator.addProvider(event.includeServer(), new NuclearCrushingRecipes(output, lookup));
        generator.addProvider(event.includeServer(), new NuclearWashingRecipes(output, lookup));
        generator.addProvider(event.includeServer(), new NuclearMixingRecipes(output, lookup));
        generator.addProvider(event.includeServer(), new NuclearPressingRecipes(output, lookup));
        generator.addProvider(event.includeServer(), new NuclearSequencedAssemblyRecipes(output, lookup));
        generator.addProvider(event.includeServer(), new NuclearCentrifugingRecipes(output, lookup));

        generator.addProvider(event.includeClient(), new NuclearBlockStateProvider(output, existingFiles));
        generator.addProvider(event.includeClient(), new NuclearItemModelProvider(output, existingFiles));
        generator.addProvider(event.includeClient(), NuclearLangProvider.english(output));
        generator.addProvider(event.includeClient(), NuclearLangProvider.spanish(output));
    }
}
