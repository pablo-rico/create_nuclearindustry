package org.papiricoh.create_nuclearindustry.data;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;

import java.util.List;
import java.util.function.Supplier;

/**
 * Datapack registry entries: ore generation, the abandoned nuclear sites ({@link RuinsWorldgen}) and the
 * radiation, plutonium burn and blast wave damage types.
 */
public final class NuclearWorldgen {

    private static final List<Ore> ORES = List.of(
            // Uraninite veins deep in deepslate.
            new Ore("uranium_ore", AllNuclearBlocks.URANIUM_ORE::get, BlockTags.DEEPSLATE_ORE_REPLACEABLES, 12, 6, -64, 0),
            // Borate evaporites in the upper stone layers.
            new Ore("borax_ore", AllNuclearBlocks.BORAX_ORE::get, BlockTags.STONE_ORE_REPLACEABLES, 5, 4, 5, 74),
            // Spodumene pegmatites, mid-depth stone.
            new Ore("spodumene_ore", AllNuclearBlocks.SPODUMENE_ORE::get, BlockTags.STONE_ORE_REPLACEABLES, 7, 5, 0, 64));

    private NuclearWorldgen() {}

    public static RegistrySetBuilder registries() {
        return new RegistrySetBuilder()
                .add(Registries.CONFIGURED_FEATURE, NuclearWorldgen::configuredFeatures)
                .add(Registries.PLACED_FEATURE, NuclearWorldgen::placedFeatures)
                .add(NeoForgeRegistries.Keys.BIOME_MODIFIERS, NuclearWorldgen::biomeModifiers)
                .add(Registries.PROCESSOR_LIST, RuinsWorldgen::processors)
                .add(Registries.TEMPLATE_POOL, RuinsWorldgen::pools)
                .add(Registries.STRUCTURE, RuinsWorldgen::structures)
                .add(Registries.STRUCTURE_SET, RuinsWorldgen::structureSets)
                .add(Registries.DAMAGE_TYPE, context -> {
                    context.register(org.papiricoh.create_nuclearindustry.radiation.AllNuclearRadiation.RADIATION_DAMAGE,
                            new net.minecraft.world.damagesource.DamageType(Create_NuclearIndustry.MODID + ".radiation", 0.0f));
                    context.register(org.papiricoh.create_nuclearindustry.radiation.AllNuclearRadiation.PLUTONIUM_BURN,
                            new net.minecraft.world.damagesource.DamageType(Create_NuclearIndustry.MODID + ".plutonium_burn", 0.1f,
                                    net.minecraft.world.damagesource.DamageEffects.BURNING));
                    context.register(org.papiricoh.create_nuclearindustry.civildefense.CivilDefenseRegistry.BLAST_WAVE_DAMAGE,
                            new net.minecraft.world.damagesource.DamageType(Create_NuclearIndustry.MODID + ".blast_wave",
                                    net.minecraft.world.damagesource.DamageScaling.NEVER, 0.1f));
                    context.register(org.papiricoh.create_nuclearindustry.explosive.ExplosiveRegistry.NUCLEAR_FLASH,
                            new net.minecraft.world.damagesource.DamageType(Create_NuclearIndustry.MODID + ".nuclear_flash", 0.1f,
                                    net.minecraft.world.damagesource.DamageEffects.BURNING));
                    context.register(org.papiricoh.create_nuclearindustry.fuelcycle.fan.NuclearFanTypes.ACID_DAMAGE,
                            new net.minecraft.world.damagesource.DamageType(Create_NuclearIndustry.MODID + ".acid", 0.1f));
                });
    }

    private static void configuredFeatures(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        for (Ore ore : ORES) {
            FeatureUtils.register(context, ore.configured(), Feature.ORE, new OreConfiguration(
                    List.of(OreConfiguration.target(new TagMatchTest(ore.replaces()), ore.block().get().defaultBlockState())),
                    ore.veinSize(), 0.0f));
        }
    }

    private static void placedFeatures(BootstrapContext<PlacedFeature> context) {
        HolderGetter<ConfiguredFeature<?, ?>> configured = context.lookup(Registries.CONFIGURED_FEATURE);
        for (Ore ore : ORES) {
            PlacementUtils.register(context, ore.placed(), configured.getOrThrow(ore.configured()), List.of(
                    CountPlacement.of(ore.perChunk()),
                    InSquarePlacement.spread(),
                    HeightRangePlacement.uniform(VerticalAnchor.absolute(ore.minY()), VerticalAnchor.absolute(ore.maxY())),
                    BiomeFilter.biome()));
        }
    }

    private static void biomeModifiers(BootstrapContext<BiomeModifier> context) {
        HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);
        HolderGetter<PlacedFeature> placed = context.lookup(Registries.PLACED_FEATURE);
        for (Ore ore : ORES) {
            context.register(key(NeoForgeRegistries.Keys.BIOME_MODIFIERS, ore.name()), new BiomeModifiers.AddFeaturesBiomeModifier(
                    biomes.getOrThrow(BiomeTags.IS_OVERWORLD),
                    HolderSet.direct(placed.getOrThrow(ore.placed())),
                    GenerationStep.Decoration.UNDERGROUND_ORES));
        }
    }

    private static <T> ResourceKey<T> key(ResourceKey<? extends net.minecraft.core.Registry<T>> registry, String name) {
        return ResourceKey.create(registry, ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, name));
    }

    private record Ore(String name, Supplier<Block> block, TagKey<Block> replaces, int veinSize, int perChunk, int minY, int maxY) {
        ResourceKey<ConfiguredFeature<?, ?>> configured() {
            return key(Registries.CONFIGURED_FEATURE, name);
        }

        ResourceKey<PlacedFeature> placed() {
            return key(Registries.PLACED_FEATURE, name);
        }
    }
}
