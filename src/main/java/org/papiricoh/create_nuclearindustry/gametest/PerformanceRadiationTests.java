package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.radiation.AllNuclearRadiation;
import org.papiricoh.create_nuclearindustry.radiation.FalloutData;
import org.papiricoh.create_nuclearindustry.radiation.RadiationEmitter;
import org.papiricoh.create_nuclearindustry.radiation.RadiationEmitters;
import org.papiricoh.create_nuclearindustry.radiation.RadiationHandler;
import org.papiricoh.create_nuclearindustry.radiation.RadiationStats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

/**
 * What the radiation system saves at the pace it really runs: a standing player's rays and a
 * monitored spot's survive from one update to the next (20 ticks later), fallout is looked up by
 * chunk, fading fallout is not re-saved every second, and the emitter index answers without asking
 * every emitter where it is. Tests that span ticks register sources the whole time, so each has a
 * batch of its own and meets nobody else's players or block changes.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class PerformanceRadiationTests {
    private static final BlockPos PLAYER = new BlockPos(6, 2, 6);
    /** Ticks between two updates of a player, as {@link RadiationHandler#onPlayerTick} runs them. */
    private static final int UPDATE_INTERVAL = 20;

    private static Player playerAt(GameTestHelper helper, BlockPos relative) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(helper.absoluteVec(Vec3.atBottomCenterOf(relative)));
        return player;
    }

    /**
     * The default cache used to expire 20 ticks after a ray was cast, just when the next update
     * read it: every standing player cast every ray every second. Now a ray read every update
     * lives on. Fallout fades every tick, so sources other batches left in the level are set aside
     * while it runs: the two readings then differ only if a ray did.
     */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = "perf_radiation_cadence", timeoutTicks = 100)
    public static void aStandingPlayersRaysLastFromOneUpdateToTheNext(GameTestHelper helper) {
        Player player = playerAt(helper, PLAYER);
        RadiationEmitter emitter = PerformanceBenchmarks.emitter(helper.getLevel(), helper.absoluteVec(new Vec3(2.5, 2.5, 2.5)));
        RadiationStats[] first = new RadiationStats[1];
        float[] rates = new float[2];
        FalloutData fallout = FalloutData.get(helper.getLevel());
        List<FalloutData.Source> setAside = new ArrayList<>();
        helper.runAfterDelay(1, () -> {
            setAside.addAll(fallout.sources());
            fallout.sources().clear();
            RadiationEmitters.register(emitter);
            RadiationHandler.update(player);
            first[0] = RadiationStats.last();
            rates[0] = player.getData(AllNuclearRadiation.RATE);
        });
        helper.runAfterDelay(1 + UPDATE_INTERVAL, () -> {
            RadiationStats second;
            try {
                RadiationHandler.update(player);
                second = RadiationStats.last();
                rates[1] = player.getData(AllNuclearRadiation.RATE);
            } finally {
                RadiationEmitters.unregister(emitter);
                fallout.sources().addAll(setAside);
            }
            helper.assertTrue(first[0] != null && first[0].raysCast >= 1, "the first update casts the emitter's ray: " + first[0]);
            helper.assertTrue(second.raysCast == 0 && second.raysReused >= 1,
                    "the next update, " + UPDATE_INTERVAL + " ticks later, casts no ray: " + second);
            helper.assertTrue(rates[0] > 0 && Math.abs(rates[0] - rates[1]) <= 1e-4f * rates[0],
                    "and the dose rate is the same, " + rates[0] + " -> " + rates[1]);
            helper.succeed();
        });
    }

    /**
     * A control room area target is read again and again for the same spot: its rays are reused
     * like a player's, and a wall built in between still counts at once.
     */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = "perf_radiation_spot", timeoutTicks = 100)
    public static void aMonitoredSpotReusesItsRays(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Vec3 body = Vec3.atCenterOf(helper.absolutePos(PLAYER)).add(0, 0.5, 0);
        RadiationEmitter emitter = PerformanceBenchmarks.emitter(level, helper.absoluteVec(new Vec3(1.5, 3.0, 6.5)));
        double[] rates = new double[4];
        RadiationStats[] stats = new RadiationStats[4];
        helper.runAfterDelay(1, () -> {
            RadiationEmitters.register(emitter);
            rates[0] = RadiationHandler.cachedAmbientRate(level, body);
            stats[0] = RadiationStats.last();
        });
        helper.runAfterDelay(1 + UPDATE_INTERVAL, () -> {
            try {
                rates[1] = RadiationHandler.cachedAmbientRate(level, body);
                stats[1] = RadiationStats.last();
                rates[2] = RadiationHandler.ambientRate(level, body);
                stats[2] = RadiationStats.last();
                for (int y = 1; y <= 5; y++) {
                    helper.setBlock(new BlockPos(4, y, 6), Blocks.STONE);
                }
                rates[3] = RadiationHandler.cachedAmbientRate(level, body);
                stats[3] = RadiationStats.last();
            } finally {
                RadiationEmitters.unregister(emitter);
            }
            helper.assertTrue(stats[0] != null && stats[0].raysCast >= 1 && rates[0] > 0,
                    "the first reading casts the emitter's ray: " + stats[0]);
            helper.assertTrue(stats[1].raysCast == 0 && stats[1].raysReused >= 1 && rates[1] == rates[0],
                    UPDATE_INTERVAL + " ticks later the same reading casts nothing: " + stats[1]);
            helper.assertTrue(stats[2].raysCast >= 1 && Math.abs(rates[2] - rates[1]) < 1e-9,
                    "the uncached reading casts its rays and agrees, " + rates[2] + " / " + rates[1]);
            helper.assertTrue(stats[3].raysCast >= 1 && rates[3] < rates[1],
                    "a wall built in between is counted at once, " + rates[1] + " -> " + rates[3] + ": " + stats[3]);
            helper.succeed();
        });
    }

    /**
     * Fallout used to be looked up by walking every source in the level for every player. Now only
     * the sources in the chunks around the player are looked at, and a change made straight to the
     * list (as tests and clean-ups do) is still seen.
     */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = "perf_radiation_fallout")
    public static void falloutIsLookedUpByChunk(GameTestHelper helper) {
        helper.runAfterDelay(1, () -> {
            ServerLevel level = helper.getLevel();
            Player player = playerAt(helper, PLAYER);
            FalloutData fallout = FalloutData.get(level);
            List<FalloutData.Source> before = new ArrayList<>(fallout.sources());
            double clean;
            double hot;
            double cleanedUp;
            RadiationStats near;
            try {
                clean = RadiationHandler.exposureRate(player);
                int alreadyNear = RadiationStats.last().falloutSources;
                BlockPos far = helper.absolutePos(PLAYER).offset(5000, 0, 5000);
                for (int i = 0; i < 300; i++) {
                    FalloutData.contaminate(level, far.offset((i % 20) * 16, 0, (i / 20) * 16), 40, 1200);
                }
                FalloutData.contaminate(level, helper.absolutePos(PLAYER.east(3)), 40, 1200);
                hot = RadiationHandler.exposureRate(player);
                near = RadiationStats.last();
                helper.assertTrue(near.falloutSources == alreadyNear + 1,
                        "of 301 sources only the one nearby is looked at, looked at " + near.falloutSources + " (" + alreadyNear + " before)");
                fallout.sources().removeIf(source -> source.pos().equals(helper.absolutePos(PLAYER.east(3))));
                cleanedUp = RadiationHandler.exposureRate(player);
            } finally {
                fallout.sources().retainAll(before);
            }
            helper.assertTrue(hot > clean + 1, "the source nearby irradiates, " + clean + " -> " + hot);
            helper.assertTrue(Math.abs(cleanedUp - clean) < 1e-9, "and taking it out of the list stops it, " + clean + " -> " + cleanedUp);
            helper.succeed();
        });
    }

    /**
     * Fading fallout used to rewrite every source and mark the data for saving every second; now a
     * source keeps its end time, so the data is only saved when a source comes or goes.
     */
    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void fadingFalloutIsSavedOnlyWhenASourceComesOrGoes(GameTestHelper helper) {
        helper.runAfterDelay(1, () -> {
            FalloutData data = new FalloutData();
            BlockPos pos = helper.absolutePos(PLAYER);
            data.add(pos, 10.0, 200, 1000);
            helper.assertTrue(data.isDirty(), "a new source is saved");
            data.setDirty(false);
            data.tick(1020);
            data.tick(1100);
            helper.assertTrue(!data.isDirty(), "fading changes nothing that is saved");
            FalloutData.Source source = data.sources().get(0);
            helper.assertTrue(Math.abs(source.currentStrength(1100) - 5.0) < 1e-9,
                    "halfway it gives off half, " + source.currentStrength(1100));
            helper.assertTrue(source.currentStrength(1200) == 0, "and nothing once it has run out");
            data.tick(1200);
            helper.assertTrue(data.sources().isEmpty() && data.isDirty(), "a source that ran out is dropped and that is saved");
            helper.succeed();
        });
    }

    /** Worlds saved before end times stored the ticks left: they load with the same time left. */
    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void falloutSavedTheOldWayLoads(GameTestHelper helper) {
        helper.runAfterDelay(1, () -> {
            var registries = helper.getLevel().registryAccess();
            CompoundTag old = new CompoundTag();
            ListTag list = new ListTag();
            CompoundTag entry = new CompoundTag();
            entry.putLong("pos", helper.absolutePos(PLAYER).asLong());
            entry.putDouble("strength", 40.0);
            entry.putInt("duration", 1200);
            entry.putInt("remaining", 300);
            list.add(entry);
            old.put("sources", list);

            FalloutData loaded = FalloutData.loadForTest(old, registries, 5000);
            helper.assertTrue(loaded.sources().size() == 1, "the source loads");
            FalloutData.Source source = loaded.sources().get(0);
            helper.assertTrue(source.expires() == 5300 && source.remaining(5000) == 300,
                    "with the 300 ticks it had left, ending at " + source.expires());
            helper.assertTrue(Math.abs(source.currentStrength(5000) - 10.0) < 1e-9, "at a quarter of its strength");
            helper.assertTrue(loaded.isDirty(), "and is saved again in the new format");

            CompoundTag saved = loaded.save(new CompoundTag(), registries);
            CompoundTag savedEntry = saved.getList("sources", Tag.TAG_COMPOUND).getCompound(0);
            helper.assertTrue(savedEntry.getInt("remaining") == 300,
                    "the ticks left are saved too, for older builds: " + savedEntry);
            FalloutData again = FalloutData.loadForTest(saved, registries, 9999);
            helper.assertTrue(again.sources().size() == 1 && again.sources().get(0).expires() == 5300,
                    "the new format keeps the end time");
            helper.succeed();
        });
    }

    /**
     * The chunk index is rebuilt from a change counter of its own: a list iterator's {@code set}
     * and {@code Collections.reverse} (which once broke on the list's modification count) work, and
     * a source replaced in place is found where it is now.
     */
    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void falloutListEditsInPlaceAreSeen(GameTestHelper helper) {
        helper.runAfterDelay(1, () -> {
            FalloutData data = new FalloutData();
            BlockPos here = helper.absolutePos(PLAYER);
            BlockPos far = here.offset(4000, 0, 4000);
            for (int i = 0; i < 20; i++) {
                data.add(here.offset(i % 4, 0, i / 4), 1.0, 1000, 0);
            }
            helper.assertTrue(countNear(data, here) == 20, "the sources are found near where they are");
            boolean reversed;
            try {
                Collections.reverse(data.sources());
                reversed = true;
            } catch (RuntimeException e) {
                reversed = false;
            }
            helper.assertTrue(reversed, "the list can be reversed in place");
            ListIterator<FalloutData.Source> iterator = data.sources().listIterator();
            FalloutData.Source moved = iterator.next();
            iterator.set(new FalloutData.Source(far, moved.strength(), moved.duration(), moved.expires()));
            helper.assertTrue(countNear(data, here) == 19 && countNear(data, far) == 1,
                    "a source replaced through an iterator is found where it is now: "
                            + countNear(data, here) + " here, " + countNear(data, far) + " far");
            data.sources().subList(0, 10).clear();
            helper.assertTrue(countNear(data, here) + countNear(data, far) == 10, "and cleared through a sub-list it is gone");
            helper.succeed();
        });
    }

    private static int countNear(FalloutData data, BlockPos at) {
        int[] count = {0};
        data.forEachNear(Vec3.atCenterOf(at), 8, source -> count[0]++);
        return count[0];
    }

    /** An emitter that counts how often it is asked where it is, as a reactor builds a vector each time. */
    private static final class CountingEmitter implements RadiationEmitter {
        private final Level level;
        private Vec3 origin;
        int asked;

        CountingEmitter(Level level, Vec3 origin) {
            this.level = level;
            this.origin = origin;
        }

        @Override
        public Level getLevel() {
            return level;
        }

        @Override
        public boolean isRemoved() {
            return false;
        }

        @Override
        public Vec3 radiationOrigin() {
            asked++;
            return origin;
        }

        @Override
        public double radiationStrength() {
            return 1.0;
        }
    }

    /**
     * The index used to ask every emitter in range for its origin on every query, and again for
     * every comparison of its sort, and re-filed every emitter of the level every second. Now
     * queries go by the origins of the last reindex, which re-files only emitters that moved chunk.
     */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = "perf_radiation_emitters")
    public static void emitterQueriesDoNotAskEmittersWhereTheyAre(GameTestHelper helper) {
        helper.runAfterDelay(1, () -> {
            Level level = helper.getLevel();
            Vec3 at = helper.absoluteVec(new Vec3(6.5, 2.5, 6.5));
            List<CountingEmitter> emitters = new ArrayList<>();
            for (int i = 0; i < 12; i++) {
                emitters.add(new CountingEmitter(level, at.add(1 + i * 1.5, 0, 0)));
            }
            List<RadiationEmitter> found;
            int askedByQueries;
            int refiledStill;
            int refiledMoved;
            try {
                emitters.forEach(RadiationEmitters::register);
                refiledStill = RadiationEmitters.reindex(level);
                emitters.forEach(emitter -> emitter.asked = 0);
                found = new ArrayList<>(RadiationEmitters.near(level, at, RadiationHandler.EMITTER_RANGE, 5));
                RadiationEmitters.near(level, at, RadiationHandler.EMITTER_RANGE, RadiationHandler.EMITTER_LIMIT);
                askedByQueries = emitters.stream().mapToInt(emitter -> emitter.asked).sum();
                emitters.get(11).origin = at.add(-1.2, 0, 0).add(-64, 0, 0);
                emitters.get(10).origin = emitters.get(10).origin.add(0.1, 0, 0);
                refiledMoved = RadiationEmitters.reindex(level);
            } finally {
                emitters.forEach(RadiationEmitters::unregister);
            }
            helper.assertTrue(askedByQueries == 0, "two queries asked emitters for their origin " + askedByQueries + " times");
            helper.assertTrue(found.size() == 5 && found.get(0) == emitters.get(0) && found.get(4) == emitters.get(4),
                    "the five nearest are found, nearest first");
            helper.assertTrue(refiledStill == 0, "emitters that stayed put are not re-filed, re-filed " + refiledStill);
            helper.assertTrue(refiledMoved == 1, "only the one that left its chunk is, re-filed " + refiledMoved);
            helper.succeed();
        });
    }

    /**
     * Uranium ore is not in the radioactivity data map, so ore veins give nothing off: the palette
     * check skips their sections without reading a block.
     */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = "perf_radiation_ore")
    public static void oreSectionsAreSkippedByTheirPalette(GameTestHelper helper) {
        helper.runAfterDelay(1, () -> {
            Player player = playerAt(helper, PLAYER);
            RadiationStats stats;
            List<BlockPos> floor = new ArrayList<>();
            List<BlockState> was = new ArrayList<>();
            for (int x = 4; x <= 8; x++) {
                for (int z = 4; z <= 8; z++) {
                    floor.add(new BlockPos(x, 1, z));
                    was.add(helper.getBlockState(new BlockPos(x, 1, z)));
                }
            }
            try {
                floor.forEach(pos -> helper.setBlock(pos, AllNuclearBlocks.URANIUM_ORE.get()));
                RadiationHandler.exposureRate(player);
                stats = RadiationStats.last();
            } finally {
                for (int i = 0; i < floor.size(); i++) {
                    helper.setBlock(floor.get(i), was.get(i));
                }
            }
            helper.assertTrue(stats.blockLookups == 0 && stats.sectionsScanned == 0 && stats.sectionsSkipped > 0,
                    "a floor of uranium ore reads no block: " + stats);
            helper.succeed();
        });
    }
}
