package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.controlroom.AlarmMetric;
import org.papiricoh.create_nuclearindustry.controlroom.ControlMonitorBlockEntity;
import org.papiricoh.create_nuclearindustry.controlroom.LinkedTarget;
import org.papiricoh.create_nuclearindustry.controlroom.NuclearAlarmBlock;
import org.papiricoh.create_nuclearindustry.controlroom.NuclearAlarmBlockEntity;
import org.papiricoh.create_nuclearindustry.controlroom.PanelButton;
import org.papiricoh.create_nuclearindustry.controlroom.ReactorControlPanelBlockEntity;
import org.papiricoh.create_nuclearindustry.controlroom.TargetKind;
import org.papiricoh.create_nuclearindustry.reactor.block.RedstonePortMode;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorRedstonePortBlockEntity;

/** Control room: linking, multiblock monitors, the reactor panel, rod ports and alarms. */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class ControlRoomTests {
    private static final BlockPos PANEL = new BlockPos(7, 1, 7);
    private static final BlockPos ALARM = new BlockPos(9, 1, 7);

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void linkerResolvesAnyReactorBlockToItsController(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        helper.succeedWhen(() -> {
            helper.assertTrue(reactor.isFormed(), "reactor should form");
            BlockPos linked = LinkedTarget.linkPosFor(helper.getLevel(), helper.absolutePos(FissionTestBuilder.WALL_CASING));
            helper.assertTrue(linked.equals(reactor.getBlockPos()), "a click on the shell links the controller, got " + linked);
            helper.assertTrue(LinkedTarget.kindAt(helper.getLevel(), linked) == TargetKind.REACTOR, "and it is a reactor");
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void linkerOnAPlainBlockWatchesRadiation(GameTestHelper helper) {
        BlockPos stone = new BlockPos(10, 1, 10);
        helper.setBlock(stone, Blocks.STONE);
        BlockPos absolute = helper.absolutePos(stone);
        helper.assertTrue(LinkedTarget.linkPosFor(helper.getLevel(), absolute).equals(absolute), "a plain block links itself");
        helper.assertTrue(LinkedTarget.kindAt(helper.getLevel(), absolute) == TargetKind.AREA, "and watches the radiation");
        LinkedTarget.Resolved resolved = LinkedTarget.resolve(helper.getLevel(), absolute, global(helper, stone));
        helper.assertTrue(resolved instanceof LinkedTarget.Area, "it resolves to an area, got " + resolved);
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void distantOrForeignTargetsHaveNoSignal(GameTestHelper helper) {
        Level level = helper.getLevel();
        BlockPos from = helper.absolutePos(BlockPos.ZERO);
        LinkedTarget.Resolved far = LinkedTarget.resolve(level, from, GlobalPos.of(level.dimension(), from.offset(1000, 0, 0)));
        helper.assertTrue(far instanceof LinkedTarget.NoSignal signal && signal.reason().equals(LinkedTarget.OUT_OF_RANGE),
                "1000 blocks is out of range, got " + far);
        LinkedTarget.Resolved nether = LinkedTarget.resolve(level, from, GlobalPos.of(Level.NETHER, from));
        helper.assertTrue(nether instanceof LinkedTarget.NoSignal signal && signal.reason().equals(LinkedTarget.OTHER_DIMENSION),
                "another dimension has no signal, got " + nether);
        helper.assertTrue(LinkedTarget.resolve(level, from, null).kind() == TargetKind.NONE, "no link is unlinked");
        helper.succeed();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void monitorsSideBySideJoinIntoOneScreen(GameTestHelper helper) {
        for (int x = 7; x <= 9; x++) {
            for (int y = 1; y <= 2; y++) {
                helper.setBlock(new BlockPos(x, y, 9), monitor(Direction.NORTH));
            }
        }
        // Facing north the viewer's right is west, so the bottom-left block is the easternmost.
        BlockPos origin = helper.absolutePos(new BlockPos(9, 1, 9));
        helper.succeedWhen(() -> {
            for (int x = 7; x <= 9; x++) {
                for (int y = 1; y <= 2; y++) {
                    ControlMonitorBlockEntity monitor = helper.getBlockEntity(new BlockPos(x, y, 9));
                    ControlMonitorBlockEntity screen = monitor.origin().orElse(null);
                    helper.assertTrue(screen != null && screen.getBlockPos().equals(origin),
                            "every monitor should share the bottom-left origin");
                }
            }
            ControlMonitorBlockEntity screen = helper.getBlockEntity(new BlockPos(9, 1, 9));
            helper.assertTrue(screen.screenWidth() == 3 && screen.screenHeight() == 2,
                    "a 3x2 screen, got " + screen.screenWidth() + "x" + screen.screenHeight());
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void unevenMonitorsStaySeparate(GameTestHelper helper) {
        BlockPos[] shape = {new BlockPos(7, 1, 9), new BlockPos(8, 1, 9), new BlockPos(7, 2, 9)};
        for (BlockPos pos : shape) {
            helper.setBlock(pos, monitor(Direction.NORTH));
        }
        helper.runAfterDelay(45, () -> {
            for (BlockPos pos : shape) {
                ControlMonitorBlockEntity monitor = helper.getBlockEntity(pos);
                helper.assertTrue(monitor.isOrigin() && monitor.screenWidth() == 1 && monitor.screenHeight() == 1,
                        "an L of monitors is three single screens");
            }
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void monitorSamplesItsReactor(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        BlockPos pos = new BlockPos(8, 1, 9);
        helper.setBlock(pos, monitor(Direction.NORTH));
        helper.runAfterDelay(2, () -> helper.<ControlMonitorBlockEntity>getBlockEntity(pos)
                .setTarget(global(helper, FissionTestBuilder.CONTROLLER)));
        helper.succeedWhen(() -> {
            ControlMonitorBlockEntity monitor = helper.getBlockEntity(pos);
            helper.assertTrue(monitor.getReading().kind() == TargetKind.REACTOR, "the monitor reads a reactor, got "
                    + monitor.getReading().kind());
            helper.assertTrue(monitor.getReading().value(org.papiricoh.create_nuclearindustry.controlroom.MonitorReading.R_TEMPERATURE) >= 20,
                    "with its temperature");
            helper.assertTrue(monitor.historySize() >= 2, "and keeps a history");
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void panelScramsAndResetsTheReactor(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        helper.setBlock(PANEL, AllNuclearBlocks.REACTOR_CONTROL_PANEL.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
        helper.runAfterDelay(5, () -> {
            ReactorControlPanelBlockEntity panel = helper.getBlockEntity(PANEL);
            panel.setTarget(global(helper, FissionTestBuilder.CONTROLLER));
            helper.assertTrue(reactor.isFormed(), "reactor should form");
            panel.press(PanelButton.SCRAM, null);
            helper.assertTrue(reactor.isScramRequested(), "SCRAM from the panel");
            panel.press(PanelButton.SCRAM, null);
            helper.assertFalse(reactor.isScramRequested(), "and reset, the core being cold");
            panel.press(PanelButton.RODS_OUT, null);
            helper.assertTrue(Math.abs(reactor.getRodTarget() - 0.9f) < 1e-4, "rods out from fully in is 90%, got " + reactor.getRodTarget());
            boolean auto = reactor.getAutoScram().isEnabled();
            panel.press(PanelButton.AUTO_SCRAM, null);
            helper.assertTrue(reactor.getAutoScram().isEnabled() != auto, "the auto SCRAM toggles");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void rodPortsDriveTowardsTheTarget(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(reactor.getControlRodInsertionRatio() == 1.0f, "the static rods sit fully in");
            helper.assertTrue(signal(RedstonePortMode.ROD_WITHDRAW, reactor) == 0 && signal(RedstonePortMode.ROD_INSERT, reactor) == 0,
                    "no target, no signal");
            reactor.setRodTarget(0.5f);
            helper.assertTrue(signal(RedstonePortMode.ROD_WITHDRAW, reactor) == 15, "rods above the target: withdraw");
            helper.assertTrue(signal(RedstonePortMode.ROD_INSERT, reactor) == 0, "not insert");
            reactor.setRodTarget(1.0f);
            helper.assertTrue(signal(RedstonePortMode.ROD_WITHDRAW, reactor) == 0, "at the target: hold");
            reactor.setRodTarget(0.0f);
            reactor.setManualScram(true);
            helper.assertTrue(signal(RedstonePortMode.ROD_WITHDRAW, reactor) == 0, "a SCRAM overrides the target");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void openChannelAsksForInsertion(GameTestHelper helper) {
        FissionTestBuilder.build(helper, false);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(reactor.isFormed(), "reactor should form");
            reactor.setRodTarget(1.0f);
            helper.assertTrue(signal(RedstonePortMode.ROD_INSERT, reactor) == 15, "empty channel, full target: insert");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void alarmSoundsPastItsThreshold(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        helper.setBlock(ALARM, AllNuclearBlocks.NUCLEAR_ALARM.get());
        helper.runAfterDelay(5, () -> {
            NuclearAlarmBlockEntity alarm = helper.getBlockEntity(ALARM);
            alarm.setTarget(global(helper, FissionTestBuilder.CONTROLLER));
            alarm.setMetric(AlarmMetric.TEMPERATURE);
            alarm.setSetting(0);
            alarm.check();
            helper.assertTrue(helper.getBlockState(ALARM).getValue(NuclearAlarmBlock.LIT), "20 °C is past a 0 °C threshold");
            helper.assertTrue(helper.getLevel().getBestNeighborSignal(helper.absolutePos(ALARM).east()) == 15,
                    "a sounding alarm powers its sides");
            alarm.setSetting(10);
            alarm.check();
            helper.assertFalse(helper.getBlockState(ALARM).getValue(NuclearAlarmBlock.LIT), "but not a 400 °C one");
            alarm.setMetric(AlarmMetric.COOLANT_LOW);
            alarm.setSetting(50);
            alarm.check();
            helper.assertTrue(helper.getBlockState(ALARM).getValue(NuclearAlarmBlock.LIT), "an empty coolant tank is below 50%");
            alarm.setMetric(AlarmMetric.TURBINE_LOAD);
            alarm.setSetting(0);
            alarm.check();
            helper.assertFalse(helper.getBlockState(ALARM).getValue(NuclearAlarmBlock.LIT), "turbine metrics ignore a reactor");
            helper.succeed();
        });
    }

    private static int signal(RedstonePortMode mode, ReactorBlockEntity reactor) {
        return ReactorRedstonePortBlockEntity.signalFor(mode, reactor);
    }

    private static BlockState monitor(Direction facing) {
        return AllNuclearBlocks.CONTROL_MONITOR.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, facing);
    }

    private static GlobalPos global(GameTestHelper helper, BlockPos relative) {
        return GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(relative));
    }
}
