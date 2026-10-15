package org.papiricoh.create_nuclearindustry.data;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.Registry;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.Pools;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.heightproviders.ConstantHeight;
import net.minecraft.world.level.levelgen.structure.BuiltinStructureSets;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.AlwaysTrueTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockAgeProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockRotProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.GravityProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.ProcessorRule;
import net.minecraft.world.level.levelgen.structure.templatesystem.RandomBlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.reactor.pwr.PwrRegistry;
import org.papiricoh.create_nuclearindustry.ruins.NuclearRuinStructure;
import org.papiricoh.create_nuclearindustry.ruins.RuinKind;
import org.papiricoh.create_nuclearindustry.ruins.RuinsLoot;
import org.papiricoh.create_nuclearindustry.ruins.RuinsTags;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Datapack entries of the abandoned nuclear sites: their processors, jigsaw pools, structures and
 * structure sets. Pools are {@code ruins/<structure>/<pool>}; every pool falls back on a terminator
 * piece, so a corridor or road that runs out of depth or room still ends in a wall or a barrier.
 */
public final class RuinsWorldgen {
    public static final ResourceKey<StructureProcessorList> RUIN_DECAY = key(Registries.PROCESSOR_LIST, "ruin_decay");
    public static final ResourceKey<StructureProcessorList> BUNKER_DECAY = key(Registries.PROCESSOR_LIST, "bunker_decay");
    public static final ResourceKey<StructureProcessorList> ROAD_GRAVITY = key(Registries.PROCESSOR_LIST, "road_gravity");

    public static final ResourceKey<Structure> ABANDONED_POWER_PLANT = key(Registries.STRUCTURE, RuinsLoot.PLANT);
    public static final ResourceKey<Structure> CIVIL_SHELTER = key(Registries.STRUCTURE, RuinsLoot.SHELTER);
    public static final ResourceKey<Structure> MILITARY_BUNKER = key(Registries.STRUCTURE, RuinsLoot.BUNKER);
    public static final List<ResourceKey<Structure>> STRUCTURES = List.of(ABANDONED_POWER_PLANT, CIVIL_SHELTER, MILITARY_BUNKER);

    public static final ResourceKey<StructureSet> ABANDONED_POWER_PLANTS = key(Registries.STRUCTURE_SET, "abandoned_power_plants");
    public static final ResourceKey<StructureSet> CIVIL_SHELTERS = key(Registries.STRUCTURE_SET, "civil_shelters");
    public static final ResourceKey<StructureSet> MILITARY_BUNKERS = key(Registries.STRUCTURE_SET, "military_bunkers");
    public static final List<ResourceKey<StructureSet>> SETS = List.of(ABANDONED_POWER_PLANTS, CIVIL_SHELTERS, MILITARY_BUNKERS);

    private RuinsWorldgen() {}

    // ------------------------------------------------------------------ processors

    static void processors(BootstrapContext<StructureProcessorList> context) {
        HolderGetter<Block> blocks = context.lookup(Registries.BLOCK);
        Block concrete = PwrRegistry.CONTAINMENT_CONCRETE.get();
        context.register(RUIN_DECAY, new StructureProcessorList(List.of(
                new RuleProcessor(List.of(
                        forbidden(),
                        rule(concrete, 0.12f, Blocks.COBBLESTONE),
                        rule(concrete, 0.06f, Blocks.GRAVEL),
                        rule(concrete, 0.05f, Blocks.AIR),
                        rule(AllNuclearBlocks.REINFORCED_TURBINE_GLASS.get(), 0.4f, Blocks.AIR),
                        rule(Blocks.GLASS_PANE, 0.5f, Blocks.AIR))),
                new BlockRotProcessor(blocks.getOrThrow(RuinsTags.RUIN_ROTTABLE), 0.85f))));
        context.register(BUNKER_DECAY, new StructureProcessorList(List.of(
                new RuleProcessor(List.of(
                        forbidden(),
                        rule(Blocks.POLISHED_DEEPSLATE, 0.05f, Blocks.COBBLED_DEEPSLATE),
                        rule(Blocks.DEEPSLATE_TILES, 0.12f, Blocks.CRACKED_DEEPSLATE_TILES),
                        rule(Blocks.GRAY_CONCRETE, 0.04f, Blocks.GRAVEL),
                        rule(Blocks.LIGHT_GRAY_CONCRETE, 0.04f, Blocks.GRAVEL),
                        // Cobwebs only in what the bunker dug out of the rock, never in the open air.
                        new ProcessorRule(new RandomBlockMatchTest(Blocks.AIR, 0.012f),
                                new TagMatchTest(net.minecraft.tags.BlockTags.BASE_STONE_OVERWORLD), Blocks.COBWEB.defaultBlockState()))),
                new BlockAgeProcessor(0.35f))));
        context.register(ROAD_GRAVITY, new StructureProcessorList(List.of(
                new RuleProcessor(List.of(
                        forbidden(),
                        rule(Blocks.GRAY_CONCRETE, 0.12f, Blocks.GRAVEL),
                        rule(Blocks.GRAY_CONCRETE, 0.08f, Blocks.COARSE_DIRT),
                        rule(Blocks.YELLOW_CONCRETE, 0.3f, Blocks.GRAY_CONCRETE))),
                new GravityProcessor(Heightmap.Types.WORLD_SURFACE_WG, -1))));
    }

    /** The safety net: a controller that somehow got into a template becomes a dead casing block. */
    private static ProcessorRule forbidden() {
        return new ProcessorRule(new TagMatchTest(RuinsTags.RUIN_FORBIDDEN), AlwaysTrueTest.INSTANCE,
                AllNuclearBlocks.REACTOR_CASING.get().defaultBlockState());
    }

    private static ProcessorRule rule(Block from, float chance, Block to) {
        return new ProcessorRule(new RandomBlockMatchTest(from, chance), AlwaysTrueTest.INSTANCE, to.defaultBlockState());
    }

    // ------------------------------------------------------------------ pools

    static void pools(BootstrapContext<StructureTemplatePool> context) {
        HolderGetter<StructureProcessorList> lists = context.lookup(Registries.PROCESSOR_LIST);
        HolderGetter<StructureTemplatePool> pools = context.lookup(Registries.TEMPLATE_POOL);
        Holder<StructureProcessorList> ruin = lists.getOrThrow(RUIN_DECAY);
        Holder<StructureProcessorList> bunker = lists.getOrThrow(BUNKER_DECAY);
        Holder<StructureProcessorList> road = lists.getOrThrow(ROAD_GRAVITY);
        Holder<StructureTemplatePool> empty = pools.getOrThrow(Pools.EMPTY);

        String p = RuinsLoot.PLANT;
        pool(context, empty, p, "road_ends", Map.of("road_end", 1), road);
        pool(context, pools.getOrThrow(poolKey(p, "road_ends")), p, "roads", Map.of("road_straight", 4, "road_checkpoint", 1), road);
        Holder<StructureTemplatePool> roadEnds = pools.getOrThrow(poolKey(p, "road_ends"));
        pool(context, roadEnds, p, "start", Map.of("reactor_hall", 1), ruin);
        pool(context, roadEnds, p, "turbine_hall", Map.of("turbine_hall", 1), ruin);
        pool(context, roadEnds, p, "control_building", Map.of("control_building", 1), ruin);
        pool(context, roadEnds, p, "yard", Map.of("waste_yard", 1, "cooling_tower_stub", 1), ruin);

        String s = RuinsLoot.SHELTER;
        pool(context, empty, s, "end_wall", Map.of("end_wall", 1), bunker);
        Holder<StructureTemplatePool> endWall = pools.getOrThrow(poolKey(s, "end_wall"));
        pool(context, endWall, s, "start", Map.of("entrance", 1), bunker);
        pool(context, endWall, s, "stairs", Map.of("stair_corridor", 1), bunker);
        pool(context, endWall, s, "rooms", Map.of("dormitory", 2, "supplies", 2, "vent_room", 1), bunker);

        String b = RuinsLoot.BUNKER;
        pool(context, empty, b, "collapsed_end", Map.of("collapsed_end", 1), bunker);
        Holder<StructureTemplatePool> collapsed = pools.getOrThrow(poolKey(b, "collapsed_end"));
        pool(context, collapsed, b, "shaft_bottom", Map.of("shaft_bottom", 1), bunker);
        Holder<StructureTemplatePool> shaftBottom = pools.getOrThrow(poolKey(b, "shaft_bottom"));
        pool(context, shaftBottom, b, "start", Map.of("guard_house", 1), bunker);
        pool(context, shaftBottom, b, "shaft", Map.of("shaft_segment", 1), bunker);
        pool(context, shaftBottom, b, "shaft_lower", Map.of("shaft_segment", 1, "shaft_bottom", 1), bunker);
        pool(context, collapsed, b, "corridors", Map.of("corridor_straight", 5, "corridor_cross", 1, "corridor_tee", 2,
                "corridor_corner", 2, "corridor_blast_door", 2), bunker);
        pool(context, collapsed, b, "rooms", Map.of("command", 2, "dormitory", 3, "armory", 2, "filtration", 2,
                "waste_store", 2, "lab", 2), bunker);
    }

    /** A rigid pool of one structure's pieces with their weights, in name order so the output is stable. */
    private static void pool(BootstrapContext<StructureTemplatePool> context, Holder<StructureTemplatePool> fallback,
                             String structure, String name, Map<String, Integer> pieces, Holder<StructureProcessorList> processors) {
        List<Pair<java.util.function.Function<StructureTemplatePool.Projection, ? extends StructurePoolElement>, Integer>> elements = new ArrayList<>();
        pieces.keySet().stream().sorted().forEach(piece -> elements.add(Pair.of(
                StructurePoolElement.single(RuinsLoot.template(structure + "/" + piece).toString(), processors), pieces.get(piece))));
        context.register(poolKey(structure, name), new StructureTemplatePool(fallback, elements, StructureTemplatePool.Projection.RIGID));
    }

    public static ResourceKey<StructureTemplatePool> poolKey(String structure, String pool) {
        return key(Registries.TEMPLATE_POOL, "ruins/" + structure + "/" + pool);
    }

    // ------------------------------------------------------------------ structures

    static void structures(BootstrapContext<Structure> context) {
        HolderGetter<net.minecraft.world.level.biome.Biome> biomes = context.lookup(Registries.BIOME);
        HolderGetter<StructureTemplatePool> pools = context.lookup(Registries.TEMPLATE_POOL);
        context.register(ABANDONED_POWER_PLANT, new NuclearRuinStructure(
                new Structure.StructureSettings(biomes.getOrThrow(RuinsTags.HAS_ABANDONED_POWER_PLANT), Map.of(),
                        GenerationStep.Decoration.SURFACE_STRUCTURES, TerrainAdjustment.BEARD_THIN),
                pools.getOrThrow(poolKey(RuinsLoot.PLANT, "start")), 4, ConstantHeight.of(VerticalAnchor.absolute(0)),
                Optional.of(Heightmap.Types.WORLD_SURFACE_WG), 80, RuinKind.POWER_PLANT, 4));
        context.register(CIVIL_SHELTER, new NuclearRuinStructure(
                new Structure.StructureSettings(biomes.getOrThrow(RuinsTags.HAS_CIVIL_SHELTER), Map.of(),
                        GenerationStep.Decoration.SURFACE_STRUCTURES, TerrainAdjustment.BEARD_THIN),
                pools.getOrThrow(poolKey(RuinsLoot.SHELTER, "start")), 3, ConstantHeight.of(VerticalAnchor.absolute(0)),
                Optional.of(Heightmap.Types.WORLD_SURFACE_WG), 40, RuinKind.CIVIL_SHELTER, -1));
        context.register(MILITARY_BUNKER, new NuclearRuinStructure(
                new Structure.StructureSettings(biomes.getOrThrow(RuinsTags.HAS_MILITARY_BUNKER), Map.of(),
                        GenerationStep.Decoration.UNDERGROUND_STRUCTURES, TerrainAdjustment.BEARD_BOX),
                pools.getOrThrow(poolKey(RuinsLoot.BUNKER, "start")), 9, ConstantHeight.of(VerticalAnchor.absolute(0)),
                Optional.of(Heightmap.Types.WORLD_SURFACE_WG), 96, RuinKind.MILITARY_BUNKER, -1));
    }

    static void structureSets(BootstrapContext<StructureSet> context) {
        HolderGetter<Structure> structures = context.lookup(Registries.STRUCTURE);
        HolderGetter<StructureSet> sets = context.lookup(Registries.STRUCTURE_SET);
        // Plants keep 8 chunks away from villages: a village grown round a reactor would look odd.
        context.register(ABANDONED_POWER_PLANTS, new StructureSet(structures.getOrThrow(ABANDONED_POWER_PLANT),
                new RandomSpreadStructurePlacement(Vec3i.ZERO, StructurePlacement.FrequencyReductionMethod.DEFAULT, 1.0f,
                        1418367213, Optional.of(new StructurePlacement.ExclusionZone(sets.getOrThrow(BuiltinStructureSets.VILLAGES), 8)),
                        64, 28, RandomSpreadType.LINEAR)));
        context.register(CIVIL_SHELTERS, new StructureSet(structures.getOrThrow(CIVIL_SHELTER),
                new RandomSpreadStructurePlacement(36, 14, RandomSpreadType.LINEAR, 77109313)));
        context.register(MILITARY_BUNKERS, new StructureSet(structures.getOrThrow(MILITARY_BUNKER),
                new RandomSpreadStructurePlacement(56, 22, RandomSpreadType.LINEAR, 552013901)));
    }

    private static <T> ResourceKey<T> key(ResourceKey<? extends Registry<T>> registry, String name) {
        return ResourceKey.create(registry, ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, name));
    }
}
