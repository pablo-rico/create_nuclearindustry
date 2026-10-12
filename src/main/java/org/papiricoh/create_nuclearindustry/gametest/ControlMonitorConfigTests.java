package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.controlroom.ConfigureDevicePayload;
import org.papiricoh.create_nuclearindustry.controlroom.ControlMonitorBlockEntity;
import org.papiricoh.create_nuclearindustry.controlroom.MonitorConfig;
import org.papiricoh.create_nuclearindustry.controlroom.MonitorLayout;
import org.papiricoh.create_nuclearindustry.controlroom.MonitorRow;
import org.papiricoh.create_nuclearindustry.controlroom.TargetKind;
import org.papiricoh.create_nuclearindustry.controlroom.desk.GaugeReading;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** The wrench setup of a control room screen: checked on the server, kept by the origin, plotted. */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class ControlMonitorConfigTests {
    private static final BlockPos MONITOR = new BlockPos(8, 1, 9);

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void aWrenchedScreenKeepsItsSetup(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        helper.setBlock(MONITOR, monitor(Direction.NORTH));
        helper.runAfterDelay(2, () -> {
            ControlMonitorBlockEntity monitor = helper.getBlockEntity(MONITOR);
            helper.assertTrue(monitor != null, "the monitor is placed");
            monitor.setTarget(global(helper, FissionTestBuilder.CONTROLLER));
        });
        helper.runAfterDelay(4, () -> {
            ControlMonitorBlockEntity monitor = helper.getBlockEntity(MONITOR);
            helper.assertTrue(monitor != null && monitor.targetKind() == TargetKind.REACTOR, "the screen watches a reactor");
            MonitorConfig sent = new MonitorConfig("§cMain reactor\nloop number one and two", MonitorLayout.BOTH,
                    List.of(MonitorRow.TURBINE_SPEED, MonitorRow.REACTOR_FUEL), Set.of(MonitorRow.TOKAMAK_ASH, MonitorRow.REACTOR_XENON),
                    GaugeReading.TURBINE_SPEED, GaugeReading.REACTOR_XENON);
            BlockPos at = helper.absolutePos(MONITOR);
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.setPos(at.getX() + 40, at.getY(), at.getZ());
            helper.assertFalse(ConfigureDevicePayload.apply(player, new ConfigureDevicePayload(at, 0, sent.write())),
                    "a player out of reach cannot set the screen up");
            helper.assertTrue(monitor.getConfig().isDefault(), "so nothing changed");
            player.setPos(at.getX() + 0.5, at.getY(), at.getZ() - 1.5);
            helper.assertTrue(ConfigureDevicePayload.apply(player, new ConfigureDevicePayload(at, 0, sent.write())),
                    "within reach it applies");
            MonitorConfig kept = monitor.getConfig();
            helper.assertTrue(kept.title().length() <= MonitorConfig.TITLE_MAX && !kept.title().contains("§")
                    && !kept.title().contains("\n") && kept.title().startsWith("Main reactor"), "the title is cleaned, got " + kept.title());
            helper.assertTrue(kept.layout() == MonitorLayout.BOTH, "the layout is taken");
            helper.assertTrue(kept.rows().stream().allMatch(row -> row.kind() == TargetKind.REACTOR) && !kept.rows().isEmpty()
                    && kept.rows().get(0) == MonitorRow.REACTOR_FUEL, "only reactor rows, the chosen one first: " + kept.rows());
            helper.assertTrue(kept.hidden().equals(Set.of(MonitorRow.REACTOR_XENON)), "only reactor rows hidden: " + kept.hidden());
            helper.assertTrue(kept.seriesA() == null, "a turbine reading is not plotted for a reactor");
            helper.assertTrue(kept.seriesB() == GaugeReading.REACTOR_XENON, "a reactor one is");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void theSetupMovesToTheOriginWhenScreensJoin(GameTestHelper helper) {
        BlockPos second = new BlockPos(9, 1, 9);
        MonitorConfig setup = MonitorConfig.DEFAULT.withTitle("Turbine hall").withLayout(MonitorLayout.LIST);
        helper.setBlock(MONITOR, monitor(Direction.NORTH));
        helper.runAfterDelay(3, () -> {
            ControlMonitorBlockEntity monitor = helper.getBlockEntity(MONITOR);
            helper.assertTrue(monitor != null && monitor.isOrigin(), "a single monitor is its own screen");
            monitor.setConfig(setup);
            // Facing north the viewer's right is west: the new block to the east becomes the bottom-left origin.
            helper.setBlock(second, monitor(Direction.NORTH));
        });
        helper.succeedWhen(() -> {
            ControlMonitorBlockEntity origin = helper.getBlockEntity(second);
            ControlMonitorBlockEntity old = helper.getBlockEntity(MONITOR);
            helper.assertTrue(origin != null && old != null, "both monitors are placed");
            helper.assertTrue(origin.isOrigin() && origin.screenWidth() == 2, "the two join into one screen");
            helper.assertTrue(!old.isOrigin(), "the first monitor is no longer the origin");
            helper.assertTrue(origin.getConfig().equals(setup), "the new origin took the setup, got " + origin.getConfig());
            helper.assertTrue(old.getConfig().equals(setup), "and the old monitor reads it from there");
            helper.assertTrue(origin.pageCount() == 1, "a list-only screen has one page");
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void chosenSeriesFillTheHistory(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        helper.setBlock(MONITOR, monitor(Direction.NORTH));
        helper.runAfterDelay(2, () -> {
            ControlMonitorBlockEntity monitor = helper.getBlockEntity(MONITOR);
            helper.assertTrue(monitor != null, "the monitor is placed");
            monitor.setTarget(global(helper, FissionTestBuilder.CONTROLLER));
            helper.assertTrue(monitor.historySize() == 1, "linking takes a first sample");
            monitor.configure(0, MonitorConfig.DEFAULT.withSeries(GaugeReading.REACTOR_RODS, GaugeReading.REACTOR_FUEL).write());
            helper.assertTrue(monitor.historySize() == 0, "new series start the graph again");
        });
        helper.succeedWhen(() -> {
            ControlMonitorBlockEntity monitor = helper.getBlockEntity(MONITOR);
            helper.assertTrue(monitor != null && monitor.historySize() >= 2, "the graph fills");
            int last = monitor.historySize() - 1;
            float rods = GaugeReading.REACTOR_RODS.fraction(monitor.getReading());
            float fuel = GaugeReading.REACTOR_FUEL.fraction(monitor.getReading());
            helper.assertTrue(monitor.historyA(last) == rods, "line A plots the rods, " + rods + ", got " + monitor.historyA(last));
            helper.assertTrue(monitor.historyB(last) == fuel, "line B plots the fuel, " + fuel + ", got " + monitor.historyB(last));
            helper.assertTrue(monitor.historyA(last) != monitor.getReading().seriesA(),
                    "not the usual temperature line (" + monitor.getReading().seriesA() + ")");
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void aSingleBlockPagesThroughItsRows(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        helper.setBlock(MONITOR, monitor(Direction.NORTH));
        helper.runAfterDelay(2, () -> {
            ControlMonitorBlockEntity monitor = helper.getBlockEntity(MONITOR);
            helper.assertTrue(monitor != null, "the monitor is placed");
            monitor.setTarget(global(helper, FissionTestBuilder.CONTROLLER));
        });
        helper.runAfterDelay(4, () -> {
            ControlMonitorBlockEntity monitor = helper.getBlockEntity(MONITOR);
            helper.assertTrue(monitor != null && monitor.getReading().kind() == TargetKind.REACTOR, "the screen reads a reactor");
            boolean pressurized = monitor.getReading().flag(org.papiricoh.create_nuclearindustry.controlroom.MonitorReading.R_PWR);
            helper.assertTrue(monitor.pageCount() == (pressurized ? 10 : 6), "an unset block keeps its usual pages, got " + monitor.pageCount());
            EnumSet<MonitorRow> hidden = EnumSet.copyOf(MonitorRow.forKind(TargetKind.REACTOR));
            hidden.remove(MonitorRow.REACTOR_POWER);
            hidden.remove(MonitorRow.REACTOR_FUEL);
            monitor.setConfig(MonitorConfig.DEFAULT.withRows(List.of(MonitorRow.REACTOR_FUEL), hidden));
            helper.assertTrue(monitor.pageCount() == 2, "two readings shown, two pages, got " + monitor.pageCount());
            helper.assertTrue(MonitorRow.visible(monitor.getReading(), monitor.getConfig()).get(0).row() == MonitorRow.REACTOR_FUEL,
                    "in the chosen order");
            monitor.setConfig(monitor.getConfig().withLayout(MonitorLayout.BOTH));
            helper.assertTrue(monitor.pageCount() == 3, "and the graph after them, got " + monitor.pageCount());
            monitor.setConfig(monitor.getConfig().withLayout(MonitorLayout.CHART));
            helper.assertTrue(monitor.pageCount() == 1, "a graph-only block has one page");
            helper.succeed();
        });
    }

    private static BlockState monitor(Direction facing) {
        return AllNuclearBlocks.CONTROL_MONITOR.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, facing);
    }

    private static GlobalPos global(GameTestHelper helper, BlockPos relative) {
        return GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(relative));
    }
}
