package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.Config;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.radiation.AllNuclearRadiation;
import org.papiricoh.create_nuclearindustry.radiation.RadiationHandler;
import org.papiricoh.create_nuclearindustry.radiation.contamination.ChunkContamination;
import org.papiricoh.create_nuclearindustry.radiation.contamination.Contamination;
import org.papiricoh.create_nuclearindustry.radiation.contamination.ContaminationData;

/**
 * Ground contamination. These tests share chunks with whatever runs beside them, so they run
 * each in a batch of its own (so one at a time) and always leave the chunks clean.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class ContaminationTests {
    /** Prefix of each test's own batch: they share chunks, so none may run beside another. */
    static final String BATCH = "contamination";

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = ContaminationTests.BATCH + "_addReadAndCap")
    public static void addReadAndCap(GameTestHelper helper) {
        LevelChunk chunk = chunkAt(helper, BlockPos.ZERO);
        try {
            helper.assertTrue(Contamination.add(chunk, 3) == 3f, "a clean chunk should take the added contamination");
            helper.assertTrue(Math.abs(Contamination.add(chunk, 2) - 5f) < 1e-5, "additions should stack");
            Contamination.add(chunk, 100_000);
            helper.assertTrue(Contamination.read(chunk) == (float) Config.contaminationMax, "contamination should cap at the configured maximum");
            Contamination.add(chunk, -100_000);
            helper.assertTrue(Contamination.read(chunk) == 0, "removing more than there is should leave it clean");
            ChunkContamination record = chunk.getExistingDataOrNull(AllNuclearRadiation.CHUNK_CONTAMINATION.get());
            helper.assertTrue(record == null || (record.level() == 0 && record.lastEvent() > 0),
                    "a clean chunk should keep a record only to remember the fallout it has seen");
        } finally {
            clean(helper, 0);
        }
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = ContaminationTests.BATCH + "_onlySeverityChangesAreSynced")
    public static void onlySeverityChangesAreSynced(GameTestHelper helper) {
        LevelChunk chunk = chunkAt(helper, BlockPos.ZERO);
        try {
            Contamination.set(chunk, 5);
            helper.assertTrue(Contamination.syncedSeverity(chunk) == 6, "5 rad/s should be severity 6, got " + Contamination.syncedSeverity(chunk));
            Byte before = chunk.getData(AllNuclearRadiation.CONTAMINATION_SEVERITY.get());
            Contamination.set(chunk, 6);
            helper.assertTrue(chunk.getData(AllNuclearRadiation.CONTAMINATION_SEVERITY.get()) == before,
                    "a change within the same severity must not touch the synced attachment");
            Contamination.set(chunk, 0);
            helper.assertFalse(chunk.hasData(AllNuclearRadiation.CONTAMINATION_SEVERITY.get()), "a clean chunk should sync nothing");
        } finally {
            clean(helper, 0);
        }
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = ContaminationTests.BATCH + "_decaysLazily")
    public static void decaysLazily(GameTestHelper helper) {
        LevelChunk chunk = chunkAt(helper, BlockPos.ZERO);
        try {
            long now = helper.getLevel().getGameTime();
            int epoch = ContaminationData.get(helper.getLevel()).clearEpoch();
            chunk.setData(AllNuclearRadiation.CHUNK_CONTAMINATION.get(),
                    new ChunkContamination(4, now - Config.contaminationHalfLifeTicks, epoch, 0));
            float value = Contamination.read(chunk);
            helper.assertTrue(Math.abs(value - 2f) < 0.01f, "a half-life later 4 should read 2, got " + value);
            helper.assertTrue(Contamination.syncedSeverity(chunk) == 5, "the severity should follow the decay");
        } finally {
            clean(helper, 0);
        }
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = ContaminationTests.BATCH + "_groundIrradiatesAndARoofShields")
    public static void groundIrradiatesAndARoofShields(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos column = helper.absolutePos(new BlockPos(6, 1, 6));
        try {
            contaminate(helper, 1, 4);
            int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column.getX(), column.getZ());
            Vec3 body = new Vec3(column.getX() + 0.5, surface + 0.9, column.getZ() + 0.5);
            double open = Contamination.exposure(level, body, surface);
            helper.assertTrue(Math.abs(open - 4) < 0.01, "standing on uniform 4 rad/s ground should give 4, got " + open);

            double high = Contamination.exposure(level, body.add(0, 8, 0), surface + 8);
            helper.assertTrue(Math.abs(high - 2) < 0.01, "8 blocks up should halve it, got " + high);

            for (int y = 3; y <= 5; y++) {
                level.setBlockAndUpdate(new BlockPos(column.getX(), surface + y, column.getZ()), AllNuclearBlocks.REACTOR_CASING.get().defaultBlockState());
            }
            double covered = Contamination.exposure(level, body, surface);
            helper.assertTrue(covered < 0.25, "three casings overhead should stop most of it, got " + covered);
        } finally {
            clean(helper, 1);
        }
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = ContaminationTests.BATCH + "_playersOnContaminatedGroundTakeDose")
    public static void playersOnContaminatedGroundTakeDose(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos column = helper.absolutePos(new BlockPos(6, 1, 6));
        try {
            contaminate(helper, 1, 2);
            int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column.getX(), column.getZ());
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.setPos(column.getX() + 0.5, surface, column.getZ() + 0.5);
            RadiationHandler.update(player);
            float dose = player.getData(AllNuclearRadiation.DOSE);
            helper.assertTrue(Math.abs(dose - 2f) < 0.05f, "a second on 2 rad/s ground should give ~2, got " + dose);

            boolean enabled = Config.contaminationEnabled;
            Config.contaminationEnabled = false;
            try {
                helper.assertTrue(RadiationHandler.groundRate(player, player.position()) == 0, "disabled contamination should not irradiate");
            } finally {
                Config.contaminationEnabled = enabled;
            }
        } finally {
            clean(helper, 1);
        }
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = ContaminationTests.BATCH + "_falloutReachesLoadedChunksNowAndOthersOnLoad")
    public static void falloutReachesLoadedChunksNowAndOthersOnLoad(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos center = helper.absolutePos(new BlockPos(6, 1, 6));
        LevelChunk here = chunkAt(helper, new BlockPos(6, 1, 6));
        LevelChunk next = level.getChunk(here.getPos().x + 1, here.getPos().z);
        ContaminationData data = ContaminationData.get(level);
        try {
            Contamination.fallout(level, center, 64, 10);
            ContaminationData.FalloutEvent event = data.events().getLast();
            float expected = event.at(here.getPos().getMiddleBlockX(), here.getPos().getMiddleBlockZ());
            helper.assertTrue(Math.abs(Contamination.read(here) - expected) < 0.01, "a loaded chunk should get its share at once");

            // The neighbour "was unloaded": forget it ever saw the event, then let it load.
            next.removeData(AllNuclearRadiation.CHUNK_CONTAMINATION.get());
            Contamination.catchUp(level, next);
            float caught = Contamination.read(next);
            float share = event.at(next.getPos().getMiddleBlockX(), next.getPos().getMiddleBlockZ());
            helper.assertTrue(Math.abs(caught - share) < 0.01, "an unloaded chunk should catch up on load: " + caught + " vs " + share);
            Contamination.catchUp(level, next);
            helper.assertTrue(Math.abs(Contamination.read(next) - caught) < 1e-4, "fallout must be applied only once");
            data.events().remove(event);
        } finally {
            clean(helper, 4);
        }
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = ContaminationTests.BATCH + "_clearAllReachesUnloadedChunksByEpoch")
    public static void clearAllReachesUnloadedChunksByEpoch(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        LevelChunk here = chunkAt(helper, BlockPos.ZERO);
        LevelChunk next = level.getChunk(here.getPos().x + 1, here.getPos().z);
        try {
            Contamination.set(here, 5);
            Contamination.set(next, 5);
            // The neighbour "was unloaded" during the clear: it still has the old epoch.
            int oldEpoch = ContaminationData.get(level).clearEpoch();
            Contamination.clearAll(level);
            helper.assertTrue(Contamination.read(here) == 0, "a loaded chunk should be cleared at once");
            next.setData(AllNuclearRadiation.CHUNK_CONTAMINATION.get(), new ChunkContamination(5, level.getGameTime(), oldEpoch, 0));
            Contamination.catchUp(level, next);
            helper.assertTrue(Contamination.read(next) == 0, "a chunk from before the clear should load clean");
        } finally {
            clean(helper, 1);
        }
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = ContaminationTests.BATCH + "_expandSpreadsAndKeepsTheTotal")
    public static void expandSpreadsAndKeepsTheTotal(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        LevelChunk here = chunkAt(helper, BlockPos.ZERO);
        ChunkPos center = here.getPos();
        try {
            loadAround(level, center, 2);
            Contamination.set(here, 8);
            Contamination.expand(level, center, 2, 1, 0.25f);
            helper.assertTrue(Math.abs(Contamination.read(here) - 6f) < 1e-4, "the centre should give away a quarter, got " + Contamination.read(here));
            float neighbour = Contamination.get(level, new ChunkPos(center.x + 1, center.z));
            helper.assertTrue(Math.abs(neighbour - 0.5f) < 1e-4, "each neighbour should get 0.5, got " + neighbour);
            Contamination.expand(level, center, 2, 5, 0.25f);
            float total = 0;
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    total += Contamination.get(level, new ChunkPos(center.x + dx, center.z + dz));
                }
            }
            // Chunks below the clean threshold (0.01) drop out, so a little is lost at the fringe.
            helper.assertTrue(Math.abs(total - 8f) < 0.05, "spreading should keep the total, got " + total);
            helper.assertTrue(Contamination.get(level, new ChunkPos(center.x + 2, center.z)) > 0, "it should reach two chunks out");
        } finally {
            clean(helper, 2);
        }
        helper.succeed();
    }

    // ---- helpers ----

    static LevelChunk chunkAt(GameTestHelper helper, BlockPos relative) {
        return helper.getLevel().getChunkAt(helper.absolutePos(relative));
    }

    static void loadAround(ServerLevel level, ChunkPos center, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                level.getChunk(center.x + dx, center.z + dz);
            }
        }
    }

    /** Sets every chunk within {@code radius} of the arena to {@code value}. */
    static void contaminate(GameTestHelper helper, int radius, float value) {
        ChunkPos center = chunkAt(helper, new BlockPos(6, 1, 6)).getPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                Contamination.set(helper.getLevel().getChunk(center.x + dx, center.z + dz), value);
            }
        }
    }

    static void clean(GameTestHelper helper, int radius) {
        contaminate(helper, radius + 1, 0);
    }
}
