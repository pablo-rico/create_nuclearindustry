package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.JigsawReplacementProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.AllNuclearDataComponents;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.data.RuinsWorldgen;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockController;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockRegistry;
import org.papiricoh.create_nuclearindustry.ruins.NuclearRuinStructure;
import org.papiricoh.create_nuclearindustry.ruins.RuinKind;
import org.papiricoh.create_nuclearindustry.ruins.RuinsConfig;
import org.papiricoh.create_nuclearindustry.ruins.RuinsLoot;
import org.papiricoh.create_nuclearindustry.ruins.RuinsTags;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The abandoned nuclear sites: their templates load and hold no machine, their jigsaws connect, their
 * chests never give anything enriched or weapons-grade, and their config switches work.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class RuinsTests {
    private static final String EMPTY = "minecraft:empty";
    /** Item ids a ruin's chest must never give (besides anything carrying an enrichment). */
    private static final List<String> FORBIDDEN_LOOT = List.of("plutonium", "weapons_grade", "enriched", "lens", "firing_set",
            "implosion", "initiator", "bomb", "thermonuclear", "deuteride", "antimatter", "missile", "cannon_shell", "tritium",
            "uranium_233", "mox");

    // ------------------------------------------------------------------ templates

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void everyTemplateLoads(GameTestHelper helper) {
        StructureTemplateManager manager = helper.getLevel().getStructureManager();
        for (String piece : RuinsLoot.PIECES) {
            Optional<StructureTemplate> template = manager.get(RuinsLoot.template(piece));
            helper.assertTrue(template.isPresent(), "template " + piece + " is missing");
            Vec3i size = template.get().getSize();
            helper.assertTrue(size.getX() > 0 && size.getY() > 0 && size.getZ() > 0, "template " + piece + " is empty");
        }
        Set<String> files = helper.getLevel().getServer().getResourceManager()
                .listResources("structure/ruins", id -> id.getPath().endsWith(".nbt")).keySet().stream()
                .filter(id -> id.getNamespace().equals(Create_NuclearIndustry.MODID))
                .map(id -> id.getPath().substring("structure/ruins/".length(), id.getPath().length() - ".nbt".length()))
                .collect(Collectors.toSet());
        helper.assertTrue(files.equals(new HashSet<>(RuinsLoot.PIECES)),
                "RuinsLoot.PIECES must list exactly the templates under structure/ruins, files are " + files);
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void noTemplateHoldsAMachine(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        Block barrel = AllNuclearBlocks.RADIOACTIVE_WASTE_BARREL.get();
        for (String piece : RuinsLoot.PIECES) {
            CompoundTag nbt = read(server, piece);
            helper.assertTrue(nbt != null, "cannot read template " + piece);
            List<Block> palette = palette(nbt);
            for (Block block : palette) {
                helper.assertFalse(block.defaultBlockState().is(RuinsTags.RUIN_FORBIDDEN),
                        piece + " holds " + BuiltInRegistries.BLOCK.getKey(block) + ", which no ruin may hold");
                helper.assertFalse(ownsMultiblock(block), piece + " holds a multiblock controller, " + BuiltInRegistries.BLOCK.getKey(block));
            }
            long barrels = nbt.getList("blocks", Tag.TAG_COMPOUND).stream()
                    .filter(entry -> palette.get(((CompoundTag) entry).getInt("state")) == barrel).count();
            helper.assertTrue(barrels <= 6, piece + " has " + barrels + " leaking barrels, at most 6");
        }
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void forbiddenTagCoversEveryController(GameTestHelper helper) {
        for (Block block : BuiltInRegistries.BLOCK) {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
            if (id.getNamespace().equals(Create_NuclearIndustry.MODID) && ownsMultiblock(block)) {
                helper.assertTrue(block.defaultBlockState().is(RuinsTags.RUIN_FORBIDDEN),
                        id + " owns a multiblock: it belongs in the ruin_forbidden tag");
            }
        }
        helper.succeed();
    }

    // ------------------------------------------------------------------ jigsaws and pools

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void jigsawsPointAtPoolsThatAnswer(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        StructureTemplateManager manager = level.getStructureManager();
        Registry<StructureTemplatePool> pools = level.registryAccess().registryOrThrow(Registries.TEMPLATE_POOL);
        RandomSource random = RandomSource.create(42);
        int checked = 0;
        for (Map.Entry<net.minecraft.resources.ResourceKey<StructureTemplatePool>, StructureTemplatePool> entry : pools.entrySet()) {
            ResourceLocation poolId = entry.getKey().location();
            if (!poolId.getNamespace().equals(Create_NuclearIndustry.MODID) || !poolId.getPath().startsWith("ruins/")) {
                continue;
            }
            helper.assertTrue(!entry.getValue().getFallback().is(net.minecraft.data.worldgen.Pools.EMPTY)
                            || isTerminator(entry.getValue(), manager, random),
                    poolId + " needs a terminator to fall back on");
            for (StructurePoolElement element : entry.getValue().getShuffledTemplates(random)) {
                Vec3i size = element.getSize(manager, Rotation.NONE);
                helper.assertTrue(size.getX() > 0 && size.getY() > 0 && size.getZ() > 0, poolId + " has an empty element " + element);
                for (StructureTemplate.StructureBlockInfo jigsaw : element.getShuffledJigsawBlocks(manager, BlockPos.ZERO, Rotation.NONE, random)) {
                    CompoundTag nbt = jigsaw.nbt();
                    helper.assertTrue(nbt != null, poolId + ": a jigsaw without data in " + element);
                    String pool = nbt.getString("pool");
                    String target = nbt.getString("target");
                    if (pool.equals(EMPTY)) {
                        continue;
                    }
                    ResourceLocation targetPool = ResourceLocation.parse(pool);
                    helper.assertTrue(pools.containsKey(targetPool), element + " grows from missing pool " + pool);
                    helper.assertTrue(names(pools.get(targetPool), manager, random).contains(target),
                            element + " targets " + target + ", which no piece of " + pool + " answers to");
                    checked++;
                }
            }
        }
        helper.assertTrue(checked > 20, "only " + checked + " jigsaws checked");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void structuresAndSetsAreRegistered(GameTestHelper helper) {
        var access = helper.getLevel().registryAccess();
        Registry<Structure> structures = access.registryOrThrow(Registries.STRUCTURE);
        Registry<StructureSet> sets = access.registryOrThrow(Registries.STRUCTURE_SET);
        for (var key : RuinsWorldgen.STRUCTURES) {
            helper.assertTrue(structures.get(key) instanceof NuclearRuinStructure, key.location() + " is not registered as a nuclear ruin");
        }
        for (var key : RuinsWorldgen.SETS) {
            helper.assertTrue(sets.containsKey(key), "structure set " + key.location() + " is missing");
        }
        helper.succeed();
    }

    /** Every ruin, laid out high in the air of the test level, grows beyond its start piece. */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 200)
    public static void ruinsLayOutTheirPieces(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Map<net.minecraft.resources.ResourceKey<Structure>, Integer> least = Map.of(
                RuinsWorldgen.ABANDONED_POWER_PLANT, 5, RuinsWorldgen.CIVIL_SHELTER, 3, RuinsWorldgen.MILITARY_BUNKER, 5);
        for (var entry : least.entrySet()) {
            Structure structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(entry.getKey());
            helper.assertTrue(structure instanceof NuclearRuinStructure, entry.getKey().location() + " is missing");
            NuclearRuinStructure ruin = (NuclearRuinStructure) structure;
            for (int i = 0; i < 4; i++) {
                ChunkPos chunk = new ChunkPos(1000 + 37 * i, -800 + 53 * i);
                Optional<Structure.GenerationStub> stub = ruin.layout(context(level, chunk, 1000 + i),
                        new BlockPos(chunk.getMinBlockX(), 200, chunk.getMinBlockZ()), Optional.empty());
                helper.assertTrue(stub.isPresent(), entry.getKey().location() + " laid nothing out");
                int pieces = stub.get().getPiecesBuilder().build().pieces().size();
                helper.assertTrue(pieces >= entry.getValue(),
                        entry.getKey().location() + " laid out " + pieces + " pieces, expected at least " + entry.getValue());
            }
        }
        helper.succeed();
    }

    // ------------------------------------------------------------------ loot

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 200)
    public static void chestLootIsNeverEmptyNorDangerous(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Vec3 origin = helper.absoluteVec(new Vec3(6.5, 2, 6.5));
        for (var key : RuinsLoot.TABLES) {
            LootTable table = level.getServer().reloadableRegistries().getLootTable(key);
            helper.assertTrue(table != LootTable.EMPTY, "loot table " + key.location() + " is missing");
            for (int roll = 0; roll < 100; roll++) {
                LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, origin)
                        .create(LootContextParamSets.CHEST);
                List<ItemStack> items = table.getRandomItems(params);
                helper.assertTrue(!items.isEmpty(), key.location() + " gave nothing");
                for (ItemStack stack : items) {
                    ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
                    helper.assertFalse(stack.has(AllNuclearDataComponents.ENRICHMENT.get()), key.location() + " gave enriched " + id);
                    boolean forbidden = id.getNamespace().equals(Create_NuclearIndustry.MODID)
                            && FORBIDDEN_LOOT.stream().anyMatch(id.getPath()::contains);
                    helper.assertFalse(forbidden, key.location() + " gave " + id + ", which no ruin may hold");
                }
            }
        }
        helper.succeed();
    }

    // ------------------------------------------------------------------ placement and config

    /** The reactor hall, decay processors and all, forms no machine: casings, rods and generators stay dead. */
    @GameTest(template = NuclearGameTests.PLANT_ARENA, timeoutTicks = 120)
    public static void reactorHallPlacesWithoutFormingMachines(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Optional<StructureTemplate> template = level.getStructureManager().get(RuinsLoot.template(RuinsLoot.PLANT + "/reactor_hall"));
        helper.assertTrue(template.isPresent(), "the reactor hall template is missing");
        Holder<StructureProcessorList> decay = level.registryAccess().registryOrThrow(Registries.PROCESSOR_LIST)
                .getHolderOrThrow(RuinsWorldgen.RUIN_DECAY);
        StructurePlaceSettings settings = new StructurePlaceSettings().setKnownShape(true)
                .addProcessor(JigsawReplacementProcessor.INSTANCE);
        decay.value().list().forEach(settings::addProcessor);
        BlockPos origin = helper.absolutePos(new BlockPos(1, 1, 1));
        template.get().placeInWorld(level, origin, origin, settings, RandomSource.create(7), 2);
        Vec3i size = template.get().getSize();
        BoundingBox box = BoundingBox.fromCorners(origin, origin.offset(size).offset(-1, -1, -1));
        helper.runAfterDelay(60, () -> {
            int barrels = 0;
            for (BlockPos pos : BlockPos.betweenClosed(origin, origin.offset(size).offset(-1, -1, -1))) {
                BlockState state = level.getBlockState(pos);
                helper.assertFalse(state.is(RuinsTags.RUIN_FORBIDDEN), "a forbidden block at " + pos);
                helper.assertFalse(state.is(net.minecraft.world.level.block.Blocks.JIGSAW), "a jigsaw block left at " + pos);
                BlockEntity entity = level.getBlockEntity(pos);
                helper.assertFalse(entity instanceof MultiblockController, "a multiblock controller at " + pos);
                if (state.is(AllNuclearBlocks.RADIOACTIVE_WASTE_BARREL.get())) {
                    barrels++;
                }
            }
            List<MultiblockController> controllers = new ArrayList<>(MultiblockRegistry.all(level, MultiblockController.class));
            controllers.removeIf(controller -> !box.isInside(controller.getBlockPos()));
            helper.assertTrue(controllers.isEmpty(), "multiblocks registered in the ruin: " + controllers);
            helper.assertTrue(barrels >= 1 && barrels <= 6, "the hall holds 1 to 6 leaking barrels, found " + barrels);
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 200)
    public static void disabledRuinNeverGenerates(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        NuclearRuinStructure plant = (NuclearRuinStructure) level.registryAccess().registryOrThrow(Registries.STRUCTURE)
                .get(RuinsWorldgen.ABANDONED_POWER_PLANT);
        helper.assertTrue(plant != null && plant.kind() == RuinKind.POWER_PLANT, "the abandoned power plant is missing");
        boolean enabled = RuinsConfig.powerPlants;
        double chance = RuinsConfig.powerPlantChance;
        int slope = RuinsConfig.powerPlantMaxSlope;
        try {
            RuinsConfig.powerPlantMaxSlope = 256;
            RuinsConfig.powerPlants = false;
            for (int i = 0; i < 12; i++) {
                ChunkPos chunk = new ChunkPos(2000 + 17 * i, 3000 - 23 * i);
                helper.assertTrue(plant.findValidGenerationPoint(context(level, chunk, i)).isEmpty(),
                        "a disabled plant generated in " + chunk);
            }
            RuinsConfig.powerPlants = true;
            RuinsConfig.powerPlantChance = 0.0;
            for (int i = 0; i < 12; i++) {
                ChunkPos chunk = new ChunkPos(2000 + 17 * i, 3000 - 23 * i);
                helper.assertTrue(plant.findValidGenerationPoint(context(level, chunk, i)).isEmpty(),
                        "a plant with no chance generated in " + chunk);
            }
            RuinsConfig.powerPlantChance = 1.0;
            boolean any = false;
            for (int i = 0; i < 12 && !any; i++) {
                ChunkPos chunk = new ChunkPos(2000 + 17 * i, 3000 - 23 * i);
                any = plant.findValidGenerationPoint(context(level, chunk, i)).isPresent();
            }
            helper.assertTrue(any, "an enabled plant on flat ground never generated");
        } finally {
            RuinsConfig.powerPlants = enabled;
            RuinsConfig.powerPlantChance = chance;
            RuinsConfig.powerPlantMaxSlope = slope;
        }
        helper.succeed();
    }

    // ------------------------------------------------------------------ helpers

    private static Structure.GenerationContext context(ServerLevel level, ChunkPos chunk, long seed) {
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        return new Structure.GenerationContext(level.registryAccess(), generator, generator.getBiomeSource(),
                level.getChunkSource().randomState(), level.getStructureManager(), seed, chunk, level, biome -> true);
    }

    private static boolean ownsMultiblock(Block block) {
        if (!(block instanceof EntityBlock entityBlock)) {
            return false;
        }
        try {
            BlockEntity entity = entityBlock.newBlockEntity(BlockPos.ZERO, block.defaultBlockState());
            return entity instanceof MultiblockController;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static Set<String> names(StructureTemplatePool pool, StructureTemplateManager manager, RandomSource random) {
        Set<String> names = new HashSet<>();
        for (StructureTemplatePool p : List.of(pool, pool.getFallback().value())) {
            for (StructurePoolElement element : p.getShuffledTemplates(random)) {
                for (StructureTemplate.StructureBlockInfo jigsaw : element.getShuffledJigsawBlocks(manager, BlockPos.ZERO, Rotation.NONE, random)) {
                    if (jigsaw.nbt() != null) {
                        names.add(jigsaw.nbt().getString("name"));
                    }
                }
            }
        }
        return names;
    }

    /** A terminator: every piece of it has only the jigsaw it hangs from. */
    private static boolean isTerminator(StructureTemplatePool pool, StructureTemplateManager manager, RandomSource random) {
        return pool.getShuffledTemplates(random).stream()
                .allMatch(element -> element.getShuffledJigsawBlocks(manager, BlockPos.ZERO, Rotation.NONE, random).size() == 1);
    }

    private static CompoundTag read(MinecraftServer server, String piece) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "structure/ruins/" + piece + ".nbt");
        try (InputStream in = server.getResourceManager().open(id)) {
            return NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap());
        } catch (Exception e) {
            return null;
        }
    }

    private static List<Block> palette(CompoundTag nbt) {
        List<Block> blocks = new ArrayList<>();
        ListTag palette = nbt.getList("palette", Tag.TAG_COMPOUND);
        for (int i = 0; i < palette.size(); i++) {
            blocks.add(BuiltInRegistries.BLOCK.get(ResourceLocation.parse(palette.getCompound(i).getString("Name"))));
        }
        return blocks;
    }
}
