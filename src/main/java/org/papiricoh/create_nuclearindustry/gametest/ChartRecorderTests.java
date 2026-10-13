package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.controlroom.ConfigureDevicePayload;
import org.papiricoh.create_nuclearindustry.controlroom.ControlRoom2Config;
import org.papiricoh.create_nuclearindustry.controlroom.ControlRoom2Registry;
import org.papiricoh.create_nuclearindustry.controlroom.LinkedTarget;
import org.papiricoh.create_nuclearindustry.controlroom.MonitorReading;
import org.papiricoh.create_nuclearindustry.controlroom.TargetKind;
import org.papiricoh.create_nuclearindustry.controlroom.chart.ChartBuffer;
import org.papiricoh.create_nuclearindustry.controlroom.chart.ChartData;
import org.papiricoh.create_nuclearindustry.controlroom.chart.ChartRecorderBlockEntity;
import org.papiricoh.create_nuclearindustry.controlroom.desk.GaugeReading;

/** Chart recorders: pens follow the linked machine, paper runs out, charts tear off, hoppers feed paper. */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class ChartRecorderTests {
    private static final BlockPos RECORDER = new BlockPos(8, 1, 9);

    private static ChartRecorderBlockEntity place(GameTestHelper helper) {
        helper.setBlock(RECORDER, ControlRoom2Registry.CHART_RECORDER.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
        return helper.getBlockEntity(RECORDER);
    }

    private static GlobalPos global(GameTestHelper helper, BlockPos relative) {
        return GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(relative));
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void pensDrawTheReactorAndPaperRunsOut(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        ChartRecorderBlockEntity recorder = place(helper);
        helper.runAfterDelay(5, () -> {
            recorder.setTarget(global(helper, FissionTestBuilder.CONTROLLER));
            helper.assertTrue(recorder.kind() == TargetKind.REACTOR, "linked to a reactor, got " + recorder.kind());
            helper.assertTrue(recorder.pen(0) == GaugeReading.REACTOR_TEMPERATURE && recorder.pen(1) == GaugeReading.REACTOR_POWER,
                    "a reactor gets temperature and power pens by default");
            helper.assertFalse(recorder.sample(), "no paper, no chart");
            int taken = recorder.insertPaper(new ItemStack(Items.PAPER, 2), false);
            helper.assertTrue(taken == 2 && recorder.paper() == 2, "two sheets go in");
            int before = ControlRoom2Config.chartSamplesPerPaper;
            try {
                ControlRoom2Config.chartSamplesPerPaper = 3;
                int drawn = 0;
                for (int i = 0; i < 10; i++) {
                    if (recorder.sample()) {
                        drawn++;
                    }
                }
                helper.assertTrue(drawn == 6, "two sheets of three samples draw six, got " + drawn);
            } finally {
                ControlRoom2Config.chartSamplesPerPaper = before;
            }
            helper.assertTrue(recorder.paper() == 0 && !recorder.running(), "and the paper is used up");
            ChartBuffer chart = recorder.chart();
            helper.assertTrue(chart.size() == 6, "six samples on the chart, got " + chart.size());
            MonitorReading sample = MonitorReading.sample(LinkedTarget.resolve(helper.getLevel(), recorder.getBlockPos(), recorder.getTarget()));
            float expected = GaugeReading.REACTOR_TEMPERATURE.fraction(sample);
            helper.assertTrue(Math.abs(chart.latest(0) - expected) <= 1.0f / ChartBuffer.LEVELS,
                    "pen 1 draws the core temperature, " + chart.latest(0) + " vs " + expected);
            helper.assertTrue(chart.latest(2) == 0, "the third pen is off");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void tearingOffMakesAChartRoll(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        ChartRecorderBlockEntity recorder = place(helper);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(recorder.tearOff().isEmpty(), "a blank chart does not tear off");
            recorder.setTarget(global(helper, FissionTestBuilder.CONTROLLER));
            recorder.insertPaper(new ItemStack(Items.PAPER, 1), false);
            for (int i = 0; i < 4; i++) {
                recorder.sample();
            }
            ItemStack roll = recorder.tearOff();
            ChartData data = roll.get(ControlRoom2Registry.CHART_DATA.get());
            helper.assertTrue(roll.is(ControlRoom2Registry.CHART_ROLL.get()) && data != null, "a Chart Roll with its chart");
            helper.assertTrue(data.samples() == 4, "carrying the four samples, got " + data.samples());
            helper.assertTrue(data.pen(0) == GaugeReading.REACTOR_TEMPERATURE && data.pen(2) == null, "and the pens");
            helper.assertTrue(TargetKind.byName(data.kind()) == TargetKind.REACTOR, "and what it watched");
            helper.assertTrue(data.spanTicks() == 4L * recorder.ticksPerSample(), "covering four samples of time");
            helper.assertTrue(recorder.chart().isEmpty(), "the recorder starts a fresh chart");
            var registries = helper.getLevel().registryAccess();
            ItemStack copy = ItemStack.parseOptional(registries, (CompoundTag) roll.save(registries));
            helper.assertTrue(data.equals(copy.get(ControlRoom2Registry.CHART_DATA.get())), "the chart survives saving");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void hoppersFeedPaperOnly(GameTestHelper helper) {
        place(helper);
        helper.runAfterDelay(2, () -> {
            IItemHandler handler = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(RECORDER), Direction.UP);
            helper.assertTrue(handler != null, "the recorder takes items");
            ItemStack left = handler.insertItem(0, new ItemStack(Items.PAPER, 64), false);
            helper.assertTrue(left.isEmpty(), "a stack of paper goes in");
            left = handler.insertItem(0, new ItemStack(Items.PAPER, 5), false);
            helper.assertTrue(left.getCount() == 5, "but the tray holds " + ChartRecorderBlockEntity.MAX_PAPER);
            helper.assertTrue(handler.insertItem(0, new ItemStack(Items.STONE), true).getCount() == 1, "only paper");
            helper.assertTrue(handler.extractItem(0, 10, false).isEmpty(), "nothing comes back out");
            ChartRecorderBlockEntity recorder = helper.getBlockEntity(RECORDER);
            helper.assertTrue(recorder.comparatorSignal() == 15, "a full tray reads 15 on a comparator");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void theSetupScreenIsCheckedOnTheServer(GameTestHelper helper) {
        ChartRecorderBlockEntity recorder = place(helper);
        helper.runAfterDelay(2, () -> {
            CompoundTag config = new CompoundTag();
            config.putString("Pen0", "NOT_A_READING");
            config.putString("Pen1", GaugeReading.TURBINE_SPEED.name());
            config.putInt("Speed", 99);
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            BlockPos at = helper.absolutePos(RECORDER);
            player.setPos(at.getX() + 40, at.getY(), at.getZ());
            helper.assertFalse(ConfigureDevicePayload.apply(player, new ConfigureDevicePayload(at, 0, config)),
                    "a player out of reach cannot set it up");
            helper.assertTrue(recorder.ticksPerSample() == 5, "so nothing changed");
            player.setPos(at.getX() + 2, at.getY(), at.getZ());
            helper.assertTrue(ConfigureDevicePayload.apply(player, new ConfigureDevicePayload(at, 0, config)), "within reach it applies");
            helper.assertTrue(recorder.pen(0) == null, "an unknown reading turns the pen off");
            helper.assertTrue(recorder.pen(1) == GaugeReading.TURBINE_SPEED, "a known one is taken");
            helper.assertTrue(recorder.ticksPerSample() == 20, "and the speed is clamped to the slowest");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 100)
    public static void thePaperRunsOnItsOwn(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        ChartRecorderBlockEntity recorder = place(helper);
        helper.runAfterDelay(2, () -> {
            recorder.setTarget(global(helper, FissionTestBuilder.CONTROLLER));
            CompoundTag config = new CompoundTag();
            config.putInt("Speed", 0);
            recorder.configure(0, config);
            recorder.insertPaper(new ItemStack(Items.PAPER, 1), false);
        });
        helper.succeedWhen(() -> helper.assertTrue(recorder.chart().size() >= 10,
                "at one sample a tick the chart fills, got " + recorder.chart().size()));
    }
}
