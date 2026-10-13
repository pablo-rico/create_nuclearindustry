package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.controlroom.AlarmMetric;
import org.papiricoh.create_nuclearindustry.controlroom.ControlRoom2Registry;
import org.papiricoh.create_nuclearindustry.controlroom.MonitorReading;
import org.papiricoh.create_nuclearindustry.controlroom.TargetKind;
import org.papiricoh.create_nuclearindustry.controlroom.annunciator.AnnunciatorLogic.State;
import org.papiricoh.create_nuclearindustry.controlroom.annunciator.AnnunciatorPanelBlockEntity;

/** Annunciator panels: the alarm sequence on a real reactor, first-out, buttons, redstone and setup. */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class AnnunciatorTests {
    /** Faces north: its ACK side (the viewer's left) is east, RESET west, the output south and below. */
    private static final BlockPos PANEL = new BlockPos(8, 1, 9);

    private static AnnunciatorPanelBlockEntity place(GameTestHelper helper) {
        helper.setBlock(PANEL, ControlRoom2Registry.ANNUNCIATOR_PANEL.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
        return helper.getBlockEntity(PANEL);
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

    private static int outputSignal(GameTestHelper helper) {
        return helper.getLevel().getBestNeighborSignal(helper.absolutePos(PANEL.south()));
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
            helper.assertTrue(outputSignal(helper) == 15, "an unacknowledged alarm powers the back");
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
            panel.setTarget(reactor(helper), new Vec3(at.getX() + 0.25, at.getY() + 9.5 / 16, at.getZ()));
            for (int i = 0; i < AnnunciatorPanelBlockEntity.WINDOWS; i++) {
                helper.assertTrue((panel.window(i).target() != null) == (i == 3), "only window 4 is linked, window " + (i + 1)
                        + " is " + panel.window(i).target());
            }
            panel.setTarget(null, new Vec3(at.getX() + 0.25, at.getY() + 9.5 / 16, at.getZ()));
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
}
