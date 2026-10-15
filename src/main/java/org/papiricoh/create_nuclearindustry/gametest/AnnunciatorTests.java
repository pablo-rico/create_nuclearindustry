package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import com.simibubi.create.api.behaviour.display.DisplaySource;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearDisplaySources;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.controlroom.AlarmMetric;
import org.papiricoh.create_nuclearindustry.controlroom.ControlRoom2Registry;
import org.papiricoh.create_nuclearindustry.controlroom.MonitorReading;
import org.papiricoh.create_nuclearindustry.controlroom.TargetKind;
import org.papiricoh.create_nuclearindustry.controlroom.MonitorTheme;
import org.papiricoh.create_nuclearindustry.controlroom.annunciator.AnnunciatorBoardLayout.Button;
import org.papiricoh.create_nuclearindustry.controlroom.annunciator.AnnunciatorDisplaySource;
import org.papiricoh.create_nuclearindustry.controlroom.annunciator.AnnunciatorLogic.State;
import org.papiricoh.create_nuclearindustry.controlroom.annunciator.AnnunciatorPanelBlock;
import org.papiricoh.create_nuclearindustry.controlroom.annunciator.AnnunciatorPanelBlockEntity;
import org.papiricoh.create_nuclearindustry.controlroom.annunciator.AnnunciatorSeverity;

/**
 * Annunciator panels: the alarm sequence on a real reactor, first-out, buttons, redstone and setup;
 * panels hung on a wall, joined into boards that share their buttons, horn and first-out.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class AnnunciatorTests {
    /**
     * Hangs facing north on a wall to its south: its ACK side (the viewer's left) is east, RESET west,
     * SILENCE above and TEST below; the output goes into the wall.
     */
    private static final BlockPos PANEL = new BlockPos(8, 1, 9);

    private static AnnunciatorPanelBlockEntity place(GameTestHelper helper) {
        return hang(helper, PANEL);
    }

    /** Hangs a panel facing north at {@code pos}, on a stone wall built to its south. */
    private static AnnunciatorPanelBlockEntity hang(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos.south(), Blocks.STONE);
        BlockState state = ControlRoom2Registry.ANNUNCIATOR_PANEL.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH).setValue(AnnunciatorPanelBlock.MOUNTED, true);
        helper.setBlock(pos, AnnunciatorPanelBlock.connect(state, helper.getLevel(), helper.absolutePos(pos)));
        return helper.getBlockEntity(pos);
    }

    private static GlobalPos reactor(GameTestHelper helper) {
        return GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(FissionTestBuilder.CONTROLLER));
    }

    /** Sets window {@code window} to watch the core temperature past {@code setting} (40 °C a step). */
    private static void watchTemperature(AnnunciatorPanelBlockEntity panel, int window, int setting) {
        CompoundTag config = new CompoundTag();
        config.putString("Metric", AlarmMetric.TEMPERATURE.name());
        config.putInt("Setting", setting);
        panel.configure(window, config);
    }

    /** The output into the wall the panel hangs on. */
    private static int outputSignal(GameTestHelper helper) {
        return wallSignal(helper, PANEL);
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void alertAcknowledgeRingbackReset(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        AnnunciatorPanelBlockEntity panel = place(helper);
        helper.runAfterDelay(5, () -> {
            panel.linkWindow(0, reactor(helper));
            helper.assertTrue(panel.window(0).kind() == TargetKind.REACTOR, "window 1 watches a reactor");
            helper.assertTrue(panel.window(1).target() == null, "and only window 1 is linked");
            watchTemperature(panel, 0, 0);
            helper.assertTrue(panel.logic().state(0) == State.ALERT, "a 20 °C core is past a 0 °C threshold: ALERT");
            helper.assertTrue(panel.logic().firstOut() == 0, "it is the first out");
            helper.assertTrue(outputSignal(helper) == 15, "an unacknowledged alarm powers the wall");
            helper.assertTrue(panel.comparatorSignal() == 3, "one window lit and unacknowledged reads 3, got " + panel.comparatorSignal());

            helper.assertTrue(panel.acknowledge(), "acknowledge");
            helper.assertTrue(panel.logic().state(0) == State.ACKNOWLEDGED, "still hot: steady");
            helper.assertTrue(outputSignal(helper) == 0, "and the output goes off");
            helper.assertFalse(panel.reset(), "reset does nothing while the condition holds");

            watchTemperature(panel, 0, 10);
            helper.assertTrue(panel.logic().state(0) == State.RINGBACK, "below a 400 °C threshold it clears: ringback");
            helper.assertTrue(panel.reset(), "reset");
            helper.assertTrue(panel.logic().state(0) == State.NORMAL && panel.logic().firstOut() == -1, "back to normal");
            helper.assertTrue(panel.comparatorSignal() == 0, "a dark panel reads 0");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void theFirstWindowToTripIsMarked(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        AnnunciatorPanelBlockEntity panel = place(helper);
        helper.runAfterDelay(5, () -> {
            panel.setTarget(reactor(helper));
            for (int i = 0; i < AnnunciatorPanelBlockEntity.WINDOWS; i++) {
                helper.assertTrue(panel.window(i).target() != null, "linking the panel links every window");
                watchTemperature(panel, i, 50);
            }
            watchTemperature(panel, 4, 0);
            watchTemperature(panel, 1, 0);
            helper.assertTrue(panel.logic().firstOut() == 4, "window 5 tripped first, got " + panel.logic().firstOut());
            helper.assertTrue(panel.logic().state(1) == State.ALERT && panel.logic().state(0) == State.NORMAL,
                    "window 2 alerts too, the rest stay dark");
            CompoundTag coolant = new CompoundTag();
            coolant.putString("Metric", AlarmMetric.COOLANT_LOW.name());
            coolant.putInt("Setting", 50);
            panel.configure(3, coolant);
            helper.assertTrue(panel.logic().state(3) == State.ALERT, "an empty coolant tank is below 50%");
            helper.assertTrue(panel.comparatorSignal() == 7, "three windows lit, unacknowledged: 7, got " + panel.comparatorSignal());
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void redstoneAcknowledgesAndResets(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        AnnunciatorPanelBlockEntity panel = place(helper);
        helper.runAfterDelay(5, () -> {
            panel.linkWindow(2, reactor(helper));
            watchTemperature(panel, 2, 0);
            helper.assertTrue(panel.logic().state(2) == State.ALERT, "alerting");
            helper.setBlock(PANEL.west(), Blocks.REDSTONE_BLOCK);
            helper.assertTrue(panel.logic().state(2) == State.ALERT, "the RESET side does not acknowledge");
            helper.setBlock(PANEL.east(), Blocks.REDSTONE_BLOCK);
            helper.assertTrue(panel.logic().state(2) == State.ACKNOWLEDGED, "a pulse on the ACK side acknowledges");
            watchTemperature(panel, 2, 10);
            helper.assertTrue(panel.logic().state(2) == State.RINGBACK, "cleared: ringback");
            helper.assertTrue(panel.logic().state(2) == State.RINGBACK, "a RESET side held on does nothing without a new edge");
            helper.setBlock(PANEL.west(), Blocks.AIR);
            helper.setBlock(PANEL.west(), Blocks.REDSTONE_BLOCK);
            helper.assertTrue(panel.logic().state(2) == State.NORMAL, "a fresh pulse on the RESET side resets");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void aLinkerClickLinksTheWindowUnderIt(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        AnnunciatorPanelBlockEntity panel = place(helper);
        helper.runAfterDelay(5, () -> {
            BlockPos at = helper.absolutePos(PANEL);
            // The north face: the viewer's right is west, so window 4 (second row, right column) sits at x + 0.25.
            // The plate's face is 13 pixels behind the block's north side.
            double face = at.getZ() + AnnunciatorPanelBlock.FACE_DEPTH / 16.0;
            panel.setTarget(reactor(helper), new Vec3(at.getX() + 0.25, at.getY() + 9.5 / 16, face));
            for (int i = 0; i < AnnunciatorPanelBlockEntity.WINDOWS; i++) {
                helper.assertTrue((panel.window(i).target() != null) == (i == 3), "only window 4 is linked, window " + (i + 1)
                        + " is " + panel.window(i).target());
            }
            panel.setTarget(null, new Vec3(at.getX() + 0.25, at.getY() + 9.5 / 16, face));
            helper.assertTrue(panel.window(3).target() == null, "and unlinked the same way");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void theSetupIsSanitised(GameTestHelper helper) {
        AnnunciatorPanelBlockEntity panel = place(helper);
        helper.runAfterDelay(2, () -> {
            CompoundTag config = new CompoundTag();
            config.putString("Metric", "NOT_A_METRIC");
            config.putInt("Setting", 500);
            config.putString("Label", "§cPRIMARY PRESSURE HIGH AND LONG");
            helper.assertTrue(panel.configure(5, config), "window 6 takes it");
            helper.assertTrue(panel.window(5).metric() == AlarmMetric.TEMPERATURE, "an unknown metric is ignored");
            helper.assertTrue(panel.window(5).setting() == 100, "the threshold is clamped");
            String label = panel.window(5).label();
            helper.assertTrue(!label.contains("§") && label.length() <= 16, "the label is cleaned and cut, got " + label);
            helper.assertFalse(panel.configure(6, config), "there is no window 7");
            CompoundTag more = new CompoundTag();
            more.putString("Severity", "PANIC");
            more.putString("Nameplate", "§aREACTOR COOLANT SYSTEM LOOP 1");
            more.putString("Theme", "NEON");
            helper.assertTrue(panel.configure(0, more), "window 1 takes it");
            helper.assertTrue(panel.window(0).severity() == AnnunciatorSeverity.WARNING, "an unknown severity is ignored");
            helper.assertTrue(!panel.nameplate().contains("§") && panel.nameplate().length() <= 16,
                    "the nameplate is cleaned and cut, got " + panel.nameplate());
            helper.assertTrue(panel.theme() == MonitorTheme.STANDARD, "an unknown style is the standard one");
            CompoundTag status = new CompoundTag();
            status.putString("Severity", "STATUS");
            status.putString("Theme", "MIMIC_BOARD");
            panel.configure(2, status);
            helper.assertTrue(panel.window(2).severity() == AnnunciatorSeverity.STATUS && panel.theme() == MonitorTheme.MIMIC_BOARD,
                    "a known severity and style are taken");
            CompoundTag preset = new CompoundTag();
            preset.putString("Preset", "CONTAINMENT");
            panel.configure(0, preset);
            helper.assertTrue(panel.window(5).severity() == AnnunciatorSeverity.STATUS && panel.window(1).severity() == AnnunciatorSeverity.ALARM,
                    "a preset sets the severities too");
            helper.assertFalse(panel.configure(-1, config), "nor window 0");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void pressurizedMetricsIgnoreABoilingReactor(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        helper.runAfterDelay(5, () -> {
            MonitorReading sample = MonitorReading.sample(org.papiricoh.create_nuclearindustry.controlroom.LinkedTarget
                    .resolve(helper.getLevel(), helper.absolutePos(PANEL), reactor(helper)));
            helper.assertTrue(sample.kind() == TargetKind.REACTOR && !sample.flag(MonitorReading.R_PWR), "a reactor that boils directly");
            for (AlarmMetric metric : new AlarmMetric[]{AlarmMetric.PRESSURE_LOW, AlarmMetric.SUBCOOLING_LOW, AlarmMetric.PZR_LEVEL,
                    AlarmMetric.LOSS_OF_FLOW, AlarmMetric.PRESSURE_HIGH}) {
                helper.assertFalse(metric.tripped(sample, 100), metric + " stays quiet on it even at the widest threshold");
            }
            helper.assertTrue(AlarmMetric.CONTAINMENT_PRESSURE.watches(TargetKind.REACTOR), "containment metrics read a reactor too");
            helper.assertFalse(AlarmMetric.CONTAINMENT_PRESSURE.tripped(sample, 10), "a reactor with no containment reads 0 bar");
            helper.assertFalse(AlarmMetric.SG_LEVEL_LOW.tripped(sample, 100), "a steam generator metric ignores a reactor");
            helper.succeed();
        });
    }

    /** The output into the wall behind {@code panel}. */
    private static int wallSignal(GameTestHelper helper, BlockPos panel) {
        return helper.getLevel().getBestNeighborSignal(helper.absolutePos(panel.south()));
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void panelsHungTogetherShareOneBoard(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        // Two wide and two high; seen from the north the viewer's left is east, so the origin is the
        // bottom panel at x 9.
        BlockPos[] at = {new BlockPos(8, 1, 9), new BlockPos(9, 1, 9), new BlockPos(8, 2, 9), new BlockPos(9, 2, 9)};
        AnnunciatorPanelBlockEntity[] panels = new AnnunciatorPanelBlockEntity[at.length];
        for (int i = 0; i < at.length; i++) {
            panels[i] = hang(helper, at[i]);
        }
        helper.runAfterDelay(5, () -> {
            for (AnnunciatorPanelBlockEntity panel : panels) {
                helper.assertTrue(panel.boardWidth() == 2 && panel.boardHeight() == 2,
                        "a 2x2 board, got " + panel.boardWidth() + "x" + panel.boardHeight());
            }
            helper.assertTrue(panels[1].isOrigin() && !panels[0].isOrigin(), "the bottom-left panel as seen from the front holds the board");
            BlockState joined = helper.getBlockState(at[0]);
            helper.assertTrue(joined.getValue(AnnunciatorPanelBlock.LEFT) && joined.getValue(AnnunciatorPanelBlock.UP)
                    && !joined.getValue(AnnunciatorPanelBlock.RIGHT) && !joined.getValue(AnnunciatorPanelBlock.DOWN),
                    "the frame drops between joined panels");

            panels[2].linkWindow(0, reactor(helper));
            watchTemperature(panels[2], 0, 0);
            panels[1].linkWindow(3, reactor(helper));
            watchTemperature(panels[1], 3, 0);
            helper.assertTrue(panels[2].isFirstOut(0) && !panels[1].isFirstOut(3), "the board's first out is the first window to trip on it");
            for (AnnunciatorPanelBlockEntity panel : panels) {
                helper.assertTrue(panel.comparatorSignal() == 5, "two windows lit on the board, one unacknowledged: 5 at every panel, got "
                        + panel.comparatorSignal());
                helper.assertTrue(panel.outputOn(), "every panel powers its wall while the board waits");
            }
            helper.assertTrue(wallSignal(helper, at[0]) == 15, "even one with no window lit");

            helper.assertTrue(panels[0].press(Button.ACK), "ACK at any panel");
            helper.assertTrue(panels[2].logic().state(0) == State.ACKNOWLEDGED && panels[1].logic().state(3) == State.ACKNOWLEDGED,
                    "acknowledges the whole board");
            helper.assertTrue(wallSignal(helper, at[0]) == 0 && wallSignal(helper, at[3]) == 0, "and the walls go quiet");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void anUnevenGroupSplitsIntoLonePanels(GameTestHelper helper) {
        AnnunciatorPanelBlockEntity[] panels = {hang(helper, new BlockPos(8, 1, 9)), hang(helper, new BlockPos(9, 1, 9)),
                hang(helper, new BlockPos(8, 2, 9))};
        helper.runAfterDelay(5, () -> {
            for (AnnunciatorPanelBlockEntity panel : panels) {
                helper.assertTrue(panel.isOrigin() && panel.boardWidth() == 1 && panel.boardHeight() == 1,
                        "an L is no rectangle: every panel is a board of its own");
            }
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 200)
    public static void theLampTestLightsWithoutTouchingTheAlarms(GameTestHelper helper) {
        AnnunciatorPanelBlockEntity panel = place(helper);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(panel.press(Button.TEST), "TEST");
            helper.assertTrue(panel.lampTest(), "lights the board");
            helper.assertTrue(panel.logic().active() == 0 && panel.comparatorSignal() == 0, "without changing a window");
        });
        helper.runAfterDelay(10 + AnnunciatorPanelBlockEntity.LAMP_TEST_TICKS, () -> {
            helper.assertFalse(panel.lampTest(), "for a few seconds");
            helper.setBlock(PANEL.below(), Blocks.REDSTONE_BLOCK);
        });
        helper.runAfterDelay(20 + 2 * AnnunciatorPanelBlockEntity.LAMP_TEST_TICKS, () -> {
            helper.assertTrue(panel.lampTest(), "power from below holds the test as long as it stays");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void silenceQuietsTheHornUntilANewAlert(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        AnnunciatorPanelBlockEntity panel = place(helper);
        helper.runAfterDelay(5, () -> {
            panel.setTarget(reactor(helper));
            for (int i = 0; i < AnnunciatorPanelBlockEntity.WINDOWS; i++) {
                watchTemperature(panel, i, 50);
            }
            helper.assertFalse(panel.press(Button.SILENCE), "nothing to silence");
            watchTemperature(panel, 0, 0);
            helper.setBlock(PANEL.above(), Blocks.REDSTONE_BLOCK);
            helper.assertTrue(panel.silenced(), "power from above silences the horn");
            helper.assertTrue(panel.logic().state(0) == State.ALERT && panel.outputOn(), "but the window still waits for ACK");
            watchTemperature(panel, 1, 0);
            helper.assertFalse(panel.silenced(), "a new alert sounds again");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void aStatusWindowOnlyShowsItsCondition(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        AnnunciatorPanelBlockEntity panel = place(helper);
        helper.runAfterDelay(5, () -> {
            panel.linkWindow(1, reactor(helper));
            CompoundTag status = new CompoundTag();
            status.putString("Severity", AnnunciatorSeverity.STATUS.name());
            panel.configure(1, status);
            watchTemperature(panel, 1, 0);
            helper.assertTrue(panel.logic().state(1) == State.ACKNOWLEDGED, "lit steady while its condition holds");
            helper.assertFalse(panel.logic().unacknowledged() || panel.outputOn(), "nothing to acknowledge, no output");
            helper.assertFalse(panel.isFirstOut(1), "never the first out");
            helper.assertTrue(panel.comparatorSignal() == 2, "but it counts as lit");
            watchTemperature(panel, 1, 10);
            helper.assertTrue(panel.logic().state(1) == State.NORMAL, "dark again once it clears, with no reset");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void aHungPanelFallsWithItsWallButAnOldOneStands(GameTestHelper helper) {
        place(helper);
        // Placed before panels hung on walls: standing in the open.
        BlockPos old = new BlockPos(10, 1, 7);
        helper.setBlock(old, ControlRoom2Registry.ANNUNCIATOR_PANEL.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
        helper.runAfterDelay(2, () -> {
            helper.setBlock(PANEL.south(), Blocks.AIR);
            helper.setBlock(old.south(), Blocks.STONE);
            helper.setBlock(old.south(), Blocks.AIR);
        });
        helper.runAfterDelay(4, () -> {
            helper.assertBlockNotPresent(ControlRoom2Registry.ANNUNCIATOR_PANEL.get(), PANEL);
            helper.assertBlockPresent(ControlRoom2Registry.ANNUNCIATOR_PANEL.get(), old);
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void aDisplayLinkListsTheBoard(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        AnnunciatorPanelBlockEntity panel = place(helper);
        helper.runAfterDelay(5, () -> {
            java.util.List<DisplaySource> sources = DisplaySource.BY_BLOCK_ENTITY.get(ControlRoom2Registry.ANNUNCIATOR_ENTITY.get());
            helper.assertTrue(sources != null && sources.contains(AllNuclearDisplaySources.ANNUNCIATOR.get()),
                    "a panel offers its board to a Display Link");
            helper.assertTrue(key(AnnunciatorDisplaySource.lines(panel).get(0)).endsWith(".normal"), "a dark board is all normal");
            panel.linkWindow(0, reactor(helper));
            watchTemperature(panel, 0, 0);
            java.util.List<MutableComponent> lines = AnnunciatorDisplaySource.lines(panel);
            helper.assertTrue(lines.size() == 3, "a summary, the first out and the window, got " + lines.size());
            helper.assertTrue(key(lines.get(0)).endsWith(".summary") && key(lines.get(1)).endsWith(".first_out")
                    && key(lines.get(2)).endsWith(".window"), "every line is translated");
            helper.succeed();
        });
    }

    private static String key(MutableComponent line) {
        return line.getContents() instanceof TranslatableContents contents ? contents.getKey() : "";
    }
}
