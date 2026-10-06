package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.radiation.RadiationHandler;
import org.papiricoh.create_nuclearindustry.radiation.contamination.Contamination;
import org.papiricoh.create_nuclearindustry.radiation.contamination.ContaminationData;
import org.papiricoh.create_nuclearindustry.radiation.contamination.WaterContamination;

@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class ContaminatedWaterTests {

    /** A 3x3 pool two blocks deep with its surface at relative y 2. */
    private static void pool(GameTestHelper helper) {
        for (int x = 5; x <= 7; x++) {
            for (int z = 5; z <= 7; z++) {
                for (int y = 1; y <= 2; y++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.WATER.defaultBlockState());
                }
            }
        }
    }

    // GameTest arenas have a barrier roof, so the heightmap cannot find the pool: test the column step.
    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = ContaminationTests.BATCH + "_falloutTurnsSurfaceWaterSources")
    public static void falloutTurnsSurfaceWaterSources(GameTestHelper helper) {
        pool(helper);
        RandomSource random = RandomSource.create(1);
        for (int x = 5; x <= 7; x++) {
            for (int z = 5; z <= 7; z++) {
                WaterContamination.convertColumn(helper.getLevel(), helper.absolutePos(new BlockPos(x, 2, z)), 1f, random);
                helper.assertBlockPresent(AllNuclearFluids.CONTAMINATED_WATER_BLOCK.get(), new BlockPos(x, 2, z));
                helper.assertBlockPresent(Blocks.WATER, new BlockPos(x, 1, z));
            }
        }
        helper.setBlock(new BlockPos(3, 1, 3), Blocks.STONE.defaultBlockState());
        helper.assertFalse(WaterContamination.convertColumn(helper.getLevel(), helper.absolutePos(new BlockPos(3, 1, 3)), 1f, random),
                "only water turns");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = ContaminationTests.BATCH + "_lightFalloutLeavesWaterAlone")
    public static void lightFalloutLeavesWaterAlone(GameTestHelper helper) {
        pool(helper);
        LevelChunk chunk = helper.getLevel().getChunkAt(helper.absolutePos(new BlockPos(6, 1, 6)));
        int converted = WaterContamination.convert(helper.getLevel(), chunk, WaterContamination.MIN_SEVERITY - 1, RandomSource.create(1));
        // Below the threshold no column is even looked at.
        helper.assertTrue(converted == 0, "light contamination should not reach the water");
        helper.assertBlockPresent(Blocks.WATER, new BlockPos(6, 2, 6));
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = ContaminationTests.BATCH + "_heavyFalloutQueuesWaterWithinABudget")
    public static void heavyFalloutQueuesWaterWithinABudget(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos center = helper.absolutePos(new BlockPos(6, 1, 6));
        ChunkPos origin = new ChunkPos(center);
        ContaminationData data = ContaminationData.get(level);
        try {
            ContaminationTests.loadAround(level, origin, 2);
            WaterContamination.clear(level);
            Contamination.fallout(level, center, 40, 20);
            helper.assertTrue(WaterContamination.isQueued(level, origin), "heavy fallout should queue the water");
            int before = WaterContamination.queued(level);
            helper.assertTrue(before > 1, "several chunks should be waiting");
            WaterContamination.process(level, 256);
            helper.assertTrue(WaterContamination.queued(level) == before - 1, "one chunk's worth of columns per 256");
            data.events().removeLast();
        } finally {
            WaterContamination.clear(level);
            ContaminationTests.clean(helper, 3);
        }
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = ContaminationTests.BATCH + "_contaminatedWaterIrradiates")
    public static void contaminatedWaterIrradiates(GameTestHelper helper) {
        BlockPos water = new BlockPos(6, 1, 6);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos feet = helper.absolutePos(new BlockPos(7, 1, 6));
        player.setPos(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5);
        helper.setBlock(water, Blocks.WATER.defaultBlockState());
        helper.assertTrue(RadiationHandler.exposureRate(player) == 0, "clean water should not irradiate");
        helper.setBlock(water, AllNuclearFluids.CONTAMINATED_WATER_BLOCK.get().defaultBlockState());
        double rate = RadiationHandler.exposureRate(player);
        helper.assertTrue(rate > 0.1, "contaminated water next to you should irradiate, got " + rate);
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = ContaminationTests.BATCH + "_theCentrifugeCleansIt")
    public static void theCentrifugeCleansIt(GameTestHelper helper) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "centrifuging/contaminated_water");
        helper.assertTrue(helper.getLevel().getRecipeManager().byKey(id).isPresent(), "missing " + id);
        helper.succeed();
    }
}
