package org.papiricoh.create_nuclearindustry.data;

import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.data.event.GatherDataEvent;

public final class NuclearDataGenerators {

    private NuclearDataGenerators() {}

    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();

        generator.addProvider(event.includeClient(), NuclearLangProvider.english(output));
        generator.addProvider(event.includeClient(), NuclearLangProvider.spanish(output));
    }
}
