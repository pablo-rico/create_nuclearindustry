package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.AllNuclearEntities;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.controlroom.ControlMonitorBlockEntity;
import org.papiricoh.create_nuclearindustry.controlroom.ControlRoom2Registry;
import org.papiricoh.create_nuclearindustry.controlroom.NuclearAlarmBlockEntity;
import org.papiricoh.create_nuclearindustry.controlroom.SharedReadings;
import org.papiricoh.create_nuclearindustry.controlroom.chart.ChartBuffer;
import org.papiricoh.create_nuclearindustry.controlroom.chart.ChartRecorderBlockEntity;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ControlDeskBlock;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ControlDeskBlockEntity;

/**
 * What control room devices cost the server and the network: a screen at rest sends its clients next
 * to nothing and later updates carry only the new graph samples; a chart recorder sends one update a
 * second at most whatever its paper speed; a screen or console works out its shape once, not every two
 * seconds from every block; alarms on one machine share its sample.
 *
 * <p>The devices watch a steam generator with nothing in it, whose readings never move.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class PerformanceControlRoomTests {
    private static final BlockPos GENERATOR = new BlockPos(9, 1, 9);

    private static GlobalPos generator(GameTestHelper helper) {
        return GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(SteamGeneratorTestBuilder.controller(GENERATOR)));
    }

    private static BlockState monitor() {
        return AllNuclearBlocks.CONTROL_MONITOR.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH);
    }

    /** Used to send an update every 20 ticks, ten in 200; at rest now only the forced one every 100. */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 400)
    public static void aMonitorAtRestOnlySendsTheForcedUpdates(GameTestHelper helper) {
        SteamGeneratorTestBuilder.build(helper, GENERATOR, 4);
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, monitor());
        int[] before = new int[2];
        helper.runAfterDelay(5, () -> {
            ControlMonitorBlockEntity monitor = helper.getBlockEntity(pos);
            monitor.setTarget(generator(helper));
        });
        helper.runAfterDelay(60, () -> {
            ControlMonitorBlockEntity monitor = helper.getBlockEntity(pos);
            before[0] = monitor.clientSyncCount();
            before[1] = monitor.historySize();
        });
        helper.runAfterDelay(260, () -> {
            ControlMonitorBlockEntity monitor = helper.getBlockEntity(pos);
            int sent = monitor.clientSyncCount() - before[0];
            helper.assertTrue(monitor.historySize() - before[1] >= 9, "it kept sampling once a second, history "
                    + before[1] + " -> " + monitor.historySize());
            helper.assertTrue(sent <= 3, "200 ticks at rest send the forced updates only, not ten: " + sent);
            helper.succeed();
        });
    }

    /**
     * The chunk carries the whole history; a later update only the samples since the broadcast before
     * the last one (here the one at tick 25, after the link), which a client appends, dropping those it
     * has; a client that got the chunk in between does not take them twice.
     */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 300)
    public static void monitorUpdatesCarryOnlyTheNewSamples(GameTestHelper helper) {
        SteamGeneratorTestBuilder.build(helper, GENERATOR, 4);
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, monitor());
        ControlMonitorBlockEntity[] clients = new ControlMonitorBlockEntity[2];
        helper.runAfterDelay(5, () -> {
            ControlMonitorBlockEntity monitor = helper.getBlockEntity(pos);
            monitor.setTarget(generator(helper));
        });
        helper.runAfterDelay(70, () -> {
            ControlMonitorBlockEntity monitor = helper.getBlockEntity(pos);
            ClientboundBlockEntityDataPacket packet = monitor.getUpdatePacket();
            helper.assertTrue(packet != null && packet.getTag().contains("HistoryA"),
                    "the first packet after a new link carries the history");
            clients[0] = new ControlMonitorBlockEntity(AllNuclearEntities.CONTROL_MONITOR.get(), monitor.getBlockPos(), monitor.getBlockState());
            clients[0].handleUpdateTag(packet.getTag(), helper.getLevel().registryAccess());
            helper.assertTrue(clients[0].historySize() == monitor.historySize(), "the client has the history");
        });
        helper.runAfterDelay(130, () -> {
            var registries = helper.getLevel().registryAccess();
            ControlMonitorBlockEntity monitor = helper.getBlockEntity(pos);
            // A player arriving now gets the chunk, with everything so far.
            clients[1] = new ControlMonitorBlockEntity(AllNuclearEntities.CONTROL_MONITOR.get(), monitor.getBlockPos(), monitor.getBlockState());
            CompoundTag chunk = monitor.getUpdateTag(registries);
            clients[1].handleUpdateTag(chunk, registries);
            ClientboundBlockEntityDataPacket packet = monitor.getUpdatePacket();
            helper.assertTrue(packet != null, "an update packet is made");
            CompoundTag update = packet.getTag();
            helper.assertTrue(!update.contains("HistoryA") && update.contains("AppendA"), "a later packet carries new samples only");
            int fresh = update.getIntArray("AppendA").length;
            helper.assertTrue(fresh >= 3 && fresh < monitor.historySize(),
                    "the samples since the broadcast before the last, not the whole history: " + fresh + " of " + monitor.historySize());
            helper.assertTrue(update.sizeInBytes() < chunk.sizeInBytes(), "and is smaller, " + update.sizeInBytes() + " B against "
                    + chunk.sizeInBytes() + " B");
            for (ControlMonitorBlockEntity client : clients) {
                client.handleUpdateTag(update, registries);
                helper.assertTrue(client.historySize() == monitor.historySize(),
                        "every client ends with the server's history, " + client.historySize() + " of " + monitor.historySize());
                int last = monitor.historySize() - 1;
                helper.assertTrue(client.historyA(last) == monitor.historyA(last) && client.historyB(last) == monitor.historyB(last),
                        "down to the newest sample");
            }
            helper.succeed();
        });
    }

    /**
     * NeoForge also makes a screen's update packet for a single player (a block break a claim mod
     * cancelled, say). Such a packet must change nothing: two in a row are the same, and the next
     * broadcast still brings every other client the samples it does not have.
     */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 200)
    public static void aPacketForOnePlayerDoesNotMakeTheOthersSkipSamples(GameTestHelper helper) {
        SteamGeneratorTestBuilder.build(helper, GENERATOR, 4);
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, monitor());
        ControlMonitorBlockEntity[] clients = new ControlMonitorBlockEntity[2];
        helper.runAfterDelay(5, () -> {
            ControlMonitorBlockEntity monitor = helper.getBlockEntity(pos);
            helper.assertTrue(monitor != null, "the monitor is there");
            monitor.setTarget(generator(helper));
        });
        helper.runAfterDelay(30, () -> {
            var registries = helper.getLevel().registryAccess();
            ControlMonitorBlockEntity monitor = helper.getBlockEntity(pos);
            CompoundTag chunk = monitor.getUpdateTag(registries);
            for (int i = 0; i < clients.length; i++) {
                clients[i] = new ControlMonitorBlockEntity(AllNuclearEntities.CONTROL_MONITOR.get(), monitor.getBlockPos(), monitor.getBlockState());
                clients[i].handleUpdateTag(chunk, registries);
            }
            // A broadcast, and its packet (the whole history, the link being new) going out to both.
            monitor.sendData();
            CompoundTag broadcast = monitor.getUpdatePacket().getTag();
            for (ControlMonitorBlockEntity client : clients) {
                client.handleUpdateTag(broadcast, registries);
            }
        });
        helper.runAfterDelay(31, () -> {
            ControlMonitorBlockEntity monitor = helper.getBlockEntity(pos);
            monitor.sendData();
        });
        helper.runAfterDelay(32, () -> {
            // The next broadcast's packet: new samples only, none yet.
            var registries = helper.getLevel().registryAccess();
            ControlMonitorBlockEntity monitor = helper.getBlockEntity(pos);
            CompoundTag broadcast = monitor.getUpdatePacket().getTag();
            helper.assertTrue(broadcast.contains("AppendA"), "a later broadcast carries the new samples only");
            for (ControlMonitorBlockEntity client : clients) {
                client.handleUpdateTag(broadcast, registries);
            }
        });
        helper.runAfterDelay(50, () -> {
            var registries = helper.getLevel().registryAccess();
            ControlMonitorBlockEntity monitor = helper.getBlockEntity(pos);
            helper.assertTrue(monitor.historySize() > clients[0].historySize(), "a sample was taken since, at about tick 46");
            CompoundTag stray = monitor.getUpdatePacket().getTag();
            CompoundTag again = monitor.getUpdatePacket().getTag();
            helper.assertTrue(stray.equals(again), "making a packet changes nothing: the second is the same, " + stray + " / " + again);
            helper.assertTrue(stray.contains("AppendA"), "a packet carries the samples since the last broadcast");
            // Only the second player gets it.
            clients[1].handleUpdateTag(stray, registries);
        });
        helper.runAfterDelay(51, () -> {
            ControlMonitorBlockEntity monitor = helper.getBlockEntity(pos);
            monitor.sendData();
        });
        helper.runAfterDelay(52, () -> {
            var registries = helper.getLevel().registryAccess();
            ControlMonitorBlockEntity monitor = helper.getBlockEntity(pos);
            CompoundTag broadcast = monitor.getUpdatePacket().getTag();
            for (ControlMonitorBlockEntity client : clients) {
                client.handleUpdateTag(broadcast, registries);
                helper.assertTrue(client.historySize() == monitor.historySize(),
                        "every client ends with the server's history, " + client.historySize() + " of " + monitor.historySize());
                for (int i = 0; i < monitor.historySize(); i++) {
                    helper.assertTrue(client.historyA(i) == monitor.historyA(i) && client.historyB(i) == monitor.historyB(i),
                            "sample " + i + " matches");
                }
            }
            helper.succeed();
        });
    }

    /** The same for a chart recorder: a packet for one player repeats the samples, the next broadcast still carries them. */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 200)
    public static void aChartPacketForOnePlayerDoesNotMakeTheOthersSkipSamples(GameTestHelper helper) {
        SteamGeneratorTestBuilder.build(helper, GENERATOR, 4);
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, ControlRoom2Registry.CHART_RECORDER.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
        long[] drawnAt = new long[1];
        helper.runAfterDelay(5, () -> {
            ChartRecorderBlockEntity recorder = helper.getBlockEntity(pos);
            helper.assertTrue(recorder != null, "the recorder is there");
            recorder.setTarget(generator(helper));
            recorder.setSpeed(0);
            recorder.insertPaper(new ItemStack(Items.PAPER, 8), false);
        });
        helper.runAfterDelay(30, () -> {
            ChartRecorderBlockEntity recorder = helper.getBlockEntity(pos);
            // A broadcast whose packet goes out: the clients have everything drawn so far.
            recorder.sendData();
            recorder.getUpdatePacket();
            drawnAt[0] = recorder.drawn();
        });
        helper.runAfterDelay(40, () -> {
            ChartRecorderBlockEntity recorder = helper.getBlockEntity(pos);
            CompoundTag stray = recorder.getUpdatePacket().getTag();
            CompoundTag again = recorder.getUpdatePacket().getTag();
            helper.assertTrue(stray.equals(again), "making a packet changes nothing: the second is the same");
        });
        helper.runAfterDelay(41, () -> {
            ChartRecorderBlockEntity recorder = helper.getBlockEntity(pos);
            recorder.sendData();
        });
        helper.runAfterDelay(42, () -> {
            ChartRecorderBlockEntity recorder = helper.getBlockEntity(pos);
            CompoundTag broadcast = recorder.getUpdatePacket().getTag();
            int carried = broadcast.getByteArray("Append0").length;
            long since = recorder.drawn() - drawnAt[0];
            helper.assertTrue(broadcast.contains("Append0") && carried >= since,
                    "the broadcast carries every sample since the one at tick 30, " + carried + " for " + since + " drawn");
            helper.succeed();
        });
    }

    /** At one sample a tick a recorder sent twenty updates a second; now one, carrying the samples drawn since. */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 300)
    public static void aFastChartSendsOneUpdateASecond(GameTestHelper helper) {
        SteamGeneratorTestBuilder.build(helper, GENERATOR, 4);
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, ControlRoom2Registry.CHART_RECORDER.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
        int[] before = new int[2];
        ChartBuffer client = new ChartBuffer();
        helper.runAfterDelay(5, () -> {
            ChartRecorderBlockEntity recorder = helper.getBlockEntity(pos);
            recorder.setTarget(generator(helper));
            recorder.setSpeed(0);
            recorder.insertPaper(new ItemStack(Items.PAPER, 8), false);
        });
        helper.runAfterDelay(20, () -> {
            ChartRecorderBlockEntity recorder = helper.getBlockEntity(pos);
            before[0] = recorder.clientSyncCount();
            before[1] = (int) recorder.drawn();
            ClientboundBlockEntityDataPacket packet = recorder.getUpdatePacket();
            helper.assertTrue(packet != null && packet.getTag().contains("Trace0"), "the first packet carries the trace");
        });
        helper.runAfterDelay(120, () -> {
            ChartRecorderBlockEntity recorder = helper.getBlockEntity(pos);
            int sent = recorder.clientSyncCount() - before[0];
            int drawn = (int) recorder.drawn() - before[1];
            helper.assertTrue(drawn >= 95, "a sample every tick, drew " + drawn);
            helper.assertTrue(sent >= 4 && sent <= 6, "100 ticks send about five updates, not a hundred: " + sent);
            ClientboundBlockEntityDataPacket packet = recorder.getUpdatePacket();
            helper.assertTrue(packet != null, "an update packet is made");
            CompoundTag update = packet.getTag();
            helper.assertTrue(!update.contains("Trace0") && update.contains("Append0"), "a later packet carries the new samples only");
            int fresh = update.getByteArray("Append0").length;
            helper.assertTrue(fresh >= 1 && fresh <= ChartRecorderBlockEntity.VISIBLE, "and at most the visible part, got " + fresh);
            helper.succeed();
        });
    }

    /** A 4x4 screen used to search for its shape from all sixteen blocks every 40 ticks: eighty searches in 200. */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 400)
    public static void aScreenWorksOutItsShapeOnlyWhenABlockChanges(GameTestHelper helper) {
        for (int x = 4; x <= 7; x++) {
            for (int y = 1; y <= 4; y++) {
                helper.setBlock(new BlockPos(x, y, 9), monitor());
            }
        }
        // Facing north the viewer's right is west, so the bottom-left block is the easternmost.
        BlockPos origin = new BlockPos(7, 1, 9);
        BlockPos corner = new BlockPos(4, 4, 9);
        int[] scans = new int[1];
        helper.runAfterDelay(10, () -> {
            assertScreen(helper, origin, 4, 4);
            scans[0] = totalScans(helper);
        });
        helper.runAfterDelay(210, () -> {
            int searched = totalScans(helper) - scans[0];
            helper.assertTrue(searched == 0, "200 ticks with nothing changed need no search, made " + searched);
            scans[0] = totalScans(helper);
            helper.setBlock(corner, Blocks.AIR);
        });
        helper.runAfterDelay(215, () -> {
            for (int x = 4; x <= 7; x++) {
                for (int y = 1; y <= 4; y++) {
                    BlockPos pos = new BlockPos(x, y, 9);
                    if (pos.equals(corner)) {
                        continue;
                    }
                    ControlMonitorBlockEntity monitor = helper.getBlockEntity(pos);
                    helper.assertTrue(monitor.isOrigin() && monitor.screenWidth() == 1, "without a corner the screen splits up");
                }
            }
            helper.setBlock(corner, monitor());
        });
        helper.runAfterDelay(220, () -> {
            assertScreen(helper, origin, 4, 4);
            int searched = totalScans(helper) - scans[0];
            helper.assertTrue(searched >= 1 && searched <= 4, "taking a corner off and back took a few searches, made " + searched);
            helper.succeed();
        });
    }

    private static void assertScreen(GameTestHelper helper, BlockPos origin, int width, int height) {
        BlockPos absolute = helper.absolutePos(origin);
        for (int x = 4; x <= 7; x++) {
            for (int y = 1; y <= 4; y++) {
                ControlMonitorBlockEntity monitor = helper.getBlockEntity(new BlockPos(x, y, 9));
                ControlMonitorBlockEntity screen = monitor.origin().orElse(null);
                helper.assertTrue(screen != null && screen.getBlockPos().equals(absolute), "every monitor shares the origin");
            }
        }
        ControlMonitorBlockEntity screen = helper.getBlockEntity(origin);
        helper.assertTrue(screen.screenWidth() == width && screen.screenHeight() == height,
                "a " + width + "x" + height + " screen, got " + screen.screenWidth() + "x" + screen.screenHeight());
    }

    private static int totalScans(GameTestHelper helper) {
        int total = 0;
        for (int x = 4; x <= 7; x++) {
            for (int y = 1; y <= 4; y++) {
                if (helper.getLevel().getBlockEntity(helper.absolutePos(new BlockPos(x, y, 9))) instanceof ControlMonitorBlockEntity monitor) {
                    total += monitor.clusterScanCount();
                }
            }
        }
        return total;
    }

    /** Six desks used to search for their console every 40 ticks each: thirty searches in 200 ticks. */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 300)
    public static void aConsoleWorksOutItsDesksOnlyWhenABlockChanges(GameTestHelper helper) {
        for (int x = 3; x <= 8; x++) {
            helper.setBlock(new BlockPos(x, 1, 9), AllNuclearBlocks.REACTOR_CONTROL_PANEL.get().defaultBlockState()
                    .setValue(ControlDeskBlock.FACING, Direction.NORTH));
        }
        int[] scans = new int[1];
        helper.runAfterDelay(10, () -> {
            for (int x = 3; x <= 8; x++) {
                ControlDeskBlockEntity desk = helper.getBlockEntity(new BlockPos(x, 1, 9));
                helper.assertTrue(desk.consoleSize() == 6, "six desks make one console, got " + desk.consoleSize());
                scans[0] += desk.clusterScanCount();
            }
        });
        helper.runAfterDelay(210, () -> {
            int total = 0;
            for (int x = 3; x <= 8; x++) {
                ControlDeskBlockEntity desk = helper.getBlockEntity(new BlockPos(x, 1, 9));
                total += desk.clusterScanCount();
            }
            helper.assertTrue(total == scans[0], "200 ticks with nothing changed need no search, made " + (total - scans[0]));
            helper.setBlock(new BlockPos(8, 1, 9), Blocks.AIR);
        });
        helper.runAfterDelay(215, () -> {
            for (int x = 3; x <= 7; x++) {
                ControlDeskBlockEntity desk = helper.getBlockEntity(new BlockPos(x, 1, 9));
                helper.assertTrue(desk.consoleSize() == 5, "a desk taken away leaves five, got " + desk.consoleSize());
            }
            helper.succeed();
        });
    }

    /** Two alarms on one machine checking in the same tick take one sample between them. */
    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void alarmsOnOneMachineShareItsSample(GameTestHelper helper) {
        SteamGeneratorTestBuilder.build(helper, GENERATOR, 4);
        BlockPos first = new BlockPos(2, 1, 2);
        BlockPos second = new BlockPos(3, 1, 2);
        helper.setBlock(first, AllNuclearBlocks.NUCLEAR_ALARM.get());
        helper.setBlock(second, AllNuclearBlocks.NUCLEAR_ALARM.get());
        helper.runAfterDelay(5, () -> {
            NuclearAlarmBlockEntity a = helper.getBlockEntity(first);
            NuclearAlarmBlockEntity b = helper.getBlockEntity(second);
            GlobalPos target = generator(helper);
            a.setTarget(target);
            b.setTarget(target);
        });
        helper.runAfterDelay(15, () -> {
            NuclearAlarmBlockEntity a = helper.getBlockEntity(first);
            NuclearAlarmBlockEntity b = helper.getBlockEntity(second);
            // A fresh tick for this pair: the first check samples, the second reuses it.
            long misses = SharedReadings.misses();
            long hits = SharedReadings.hits();
            a.check(true);
            b.check(true);
            a.check(true);
            long sampled = SharedReadings.misses() - misses;
            long shared = SharedReadings.hits() - hits;
            helper.assertTrue(sampled <= 1 && shared >= 2, "one sample for three checks, sampled " + sampled + ", shared " + shared);
            helper.succeed();
        });
    }
}
