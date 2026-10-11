package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.explosive.NuclearBlastManager;
import org.papiricoh.create_nuclearindustry.radiation.AllNuclearRadiation;
import org.papiricoh.create_nuclearindustry.radiation.FalloutData;
import org.papiricoh.create_nuclearindustry.radiation.RadiationEmitter;
import org.papiricoh.create_nuclearindustry.radiation.RadiationEmitters;
import org.papiricoh.create_nuclearindustry.radiation.RadiationHandler;
import org.papiricoh.create_nuclearindustry.radiation.RadiationStats;
import org.papiricoh.create_nuclearindustry.reactor.pwr.containment.ContainmentMonitorBlockEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * A nuclear crater removes blocks without block events, so it tells the caches itself: a cached
 * ray through a wall the crater dug away is cast again at the next update, and a sealed
 * containment it breaches is measured again at once rather than at its next routine survey. The
 * craters are small and centred high, so they dig only inside the arena.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class CraterAnnouncementTests {
    /** Most steps a test crater can take; a small one finishes in a few. */
    private static final int MAX_STEPS = 200;

    private static boolean dig(GameTestHelper helper, BlockPos relativeCenter) {
        NuclearBlastManager.BlastJob job = NuclearBlastManager.create(helper.getLevel(), helper.absolutePos(relativeCenter), 2, 4, 0f);
        if (job == null) {
            return false;
        }
        for (int i = 0; i < MAX_STEPS; i++) {
            if (job.step()) {
                return true;
            }
        }
        return false;
    }

    /**
     * A player behind a stone wall from an emitter: the crater digs the wall out between two
     * updates, and the second update casts the ray again and sees more radiation.
     */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = "crater_announce_radiation", timeoutTicks = 100)
    public static void aCraterThroughAWallRecastsTheRaysAcrossIt(GameTestHelper helper) {
        for (int y = 1; y <= 5; y++) {
            for (int z = 3; z <= 9; z++) {
                helper.setBlock(new BlockPos(6, y, z), Blocks.STONE);
            }
        }
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(helper.absoluteVec(Vec3.atBottomCenterOf(new BlockPos(10, 2, 6))));
        RadiationEmitter emitter = PerformanceBenchmarks.emitter(helper.getLevel(), helper.absoluteVec(new Vec3(2.5, 2.5, 6.5)));
        FalloutData fallout = FalloutData.get(helper.getLevel());
        List<FalloutData.Source> setAside = new ArrayList<>();
        float[] rates = new float[2];
        RadiationStats[] stats = new RadiationStats[2];
        boolean[] dug = new boolean[1];
        helper.runAfterDelay(1, () -> {
            // Fallout left by other batches fades every tick; set it aside so only the wall changes.
            setAside.addAll(fallout.sources());
            fallout.sources().clear();
            RadiationEmitters.register(emitter);
            RadiationHandler.update(player);
            stats[0] = RadiationStats.last();
            rates[0] = player.getData(AllNuclearRadiation.RATE);
        });
        helper.runAfterDelay(2, () -> dug[0] = dig(helper, new BlockPos(6, 10, 6)));
        helper.runAfterDelay(3, () -> {
            try {
                RadiationHandler.update(player);
                stats[1] = RadiationStats.last();
                rates[1] = player.getData(AllNuclearRadiation.RATE);
            } finally {
                RadiationEmitters.unregister(emitter);
                fallout.sources().addAll(setAside);
            }
            helper.assertTrue(dug[0], "the crater is dug");
            helper.assertBlockPresent(Blocks.AIR, new BlockPos(6, 3, 6));
            helper.assertTrue(stats[0] != null && stats[0].raysCast >= 1 && rates[0] > 0,
                    "the first update casts the ray through the wall: " + stats[0]);
            helper.assertTrue(stats[1] != null && stats[1].raysCast >= 1,
                    "the update after the crater casts its ray again: " + stats[1]);
            helper.assertTrue(rates[1] > rates[0], "and the wall is gone from it, " + rates[0] + " -> " + rates[1]);
            helper.succeed();
        });
    }

    /**
     * A crater through the wall of a sealed containment: the monitor measures the building again
     * within a few ticks, not at its next routine survey ({@link ContainmentMonitorBlockEntity#RESURVEY_INTERVAL}
     * ticks, or eight of them while it thinks nothing changed), and finds it open.
     */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = "crater_announce_containment", timeoutTicks = 300)
    public static void aCraterThroughAContainmentMakesItMeasureAgain(GameTestHelper helper) {
        ContainmentTestBuilder.concreteRoom(helper);
        int[] surveys = new int[1];
        long[] dugAt = new long[1];
        helper.startSequence()
                .thenWaitUntil(() -> {
                    ContainmentMonitorBlockEntity monitor = ContainmentTestBuilder.monitor(helper, ContainmentTestBuilder.MONITOR);
                    helper.assertTrue(monitor != null && monitor.sealed() && !monitor.isSurveying(), "the room seals");
                })
                .thenExecute(() -> {
                    ContainmentMonitorBlockEntity monitor = ContainmentTestBuilder.monitor(helper, ContainmentTestBuilder.MONITOR);
                    helper.assertTrue(monitor != null, "the monitor is there");
                    surveys[0] = monitor.getSurveysStarted();
                    dugAt[0] = helper.getTick();
                    // Through the east wall's middle, clear of the monitor at (5, 2, 5).
                    helper.assertTrue(dig(helper, new BlockPos(8, 10, 6)), "the crater is dug");
                    helper.assertBlockPresent(Blocks.AIR, new BlockPos(8, 3, 6));
                    helper.assertTrue(monitor.sealed(), "nothing told the monitor yet but the crater itself");
                })
                .thenWaitUntil(() -> {
                    ContainmentMonitorBlockEntity monitor = ContainmentTestBuilder.monitor(helper, ContainmentTestBuilder.MONITOR);
                    helper.assertTrue(monitor != null && monitor.getSurveysStarted() > surveys[0],
                            "the breached building is measured again");
                    helper.assertTrue(helper.getTick() - dugAt[0] < ContainmentMonitorBlockEntity.RESURVEY_INTERVAL,
                            "before its routine survey");
                })
                .thenWaitUntil(() -> {
                    ContainmentMonitorBlockEntity monitor = ContainmentTestBuilder.monitor(helper, ContainmentTestBuilder.MONITOR);
                    helper.assertTrue(monitor != null && !monitor.isSurveying() && !monitor.sealed(), "and found open");
                })
                .thenExecute(() -> ContainmentTestBuilder.clearArena(helper))
                .thenSucceed();
    }
}
