package org.papiricoh.create_nuclearindustry.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.BiomeTagsProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.ruins.RuinsTags;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Where each abandoned site generates. Plants want open, flat land (their own slope check throws out
 * the rough parts of these biomes); shelters also turn up in cherry groves; military bunkers, dug
 * deep, also in badlands and windswept hills.
 */
final class RuinsBiomeTagsProvider extends BiomeTagsProvider {
    private static final List<ResourceKey<Biome>> FLAT = List.of(Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS, Biomes.SNOWY_PLAINS,
            Biomes.DESERT, Biomes.SAVANNA, Biomes.MEADOW, Biomes.FOREST, Biomes.BIRCH_FOREST, Biomes.TAIGA, Biomes.SNOWY_TAIGA);

    RuinsBiomeTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper existingFiles) {
        super(output, lookup, Create_NuclearIndustry.MODID, existingFiles);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        var plant = tag(RuinsTags.HAS_ABANDONED_POWER_PLANT);
        FLAT.forEach(plant::add);
        var shelter = tag(RuinsTags.HAS_CIVIL_SHELTER);
        FLAT.forEach(shelter::add);
        shelter.add(Biomes.CHERRY_GROVE, Biomes.DARK_FOREST, Biomes.OLD_GROWTH_PINE_TAIGA);
        var bunker = tag(RuinsTags.HAS_MILITARY_BUNKER);
        FLAT.forEach(bunker::add);
        bunker.add(Biomes.BADLANDS, Biomes.WOODED_BADLANDS, Biomes.ERODED_BADLANDS, Biomes.WINDSWEPT_HILLS,
                Biomes.WINDSWEPT_FOREST, Biomes.WINDSWEPT_GRAVELLY_HILLS, Biomes.WINDSWEPT_SAVANNA, Biomes.SAVANNA_PLATEAU);
    }

    @Override
    public String getName() {
        return "Ruins biome tags";
    }
}
