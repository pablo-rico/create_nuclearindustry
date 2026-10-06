package org.papiricoh.create_nuclearindustry.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.concurrent.CompletableFuture;

public final class NuclearDataGenerators {

    private NuclearDataGenerators() {}

    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();

        CompletableFuture<HolderLookup.Provider> lookup = event.getLookupProvider();
        ExistingFileHelper existingFiles = event.getExistingFileHelper();

        generator.addProvider(event.includeServer(), new NuclearBlockTagsProvider(output, lookup, existingFiles));

        generator.addProvider(event.includeClient(), NuclearLangProvider.english(output));
        generator.addProvider(event.includeClient(), NuclearLangProvider.spanish(output));
    }
}
