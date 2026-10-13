package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.civildefense.CivilDefenseRegistry;
import org.papiricoh.create_nuclearindustry.civildefense.blastdoor.BlastDoorBlock;
import org.papiricoh.create_nuclearindustry.civildefense.blastdoor.BlastDoorControllerBlockEntity;
import org.papiricoh.create_nuclearindustry.civildefense.blastdoor.LeafState;
import org.papiricoh.create_nuclearindustry.controlroom.AlarmMetric;
import org.papiricoh.create_nuclearindustry.controlroom.ConfigureDevicePayload;
import org.papiricoh.create_nuclearindustry.controlroom.ControlRoom2Registry;
import org.papiricoh.create_nuclearindustry.controlroom.LinkedTarget;
import org.papiricoh.create_nuclearindustry.controlroom.MonitorReading;
import org.papiricoh.create_nuclearindustry.controlroom.NuclearAlarmBlockEntity;
import org.papiricoh.create_nuclearindustry.controlroom.TargetKind;
import org.papiricoh.create_nuclearindustry.controlroom.annunciator.AnnunciatorLogic;
import org.papiricoh.create_nuclearindustry.controlroom.annunciator.AnnunciatorPanelBlockEntity;
import org.papiricoh.create_nuclearindustry.controlroom.annunciator.AnnunciatorPreset;
import org.papiricoh.create_nuclearindustry.controlroom.chart.ChartRecorderBlockEntity;
import org.papiricoh.create_nuclearindustry.controlroom.desk.GaugeReading;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.pwr.PwrConfig;
import org.papiricoh.create_nuclearindustry.reactor.pwr.PwrRegistry;
import org.papiricoh.create_nuclearindustry.reactor.pwr.containment.ContainmentModel;
import org.papiricoh.create_nuclearindustry.reactor.pwr.containment.ContainmentMonitorBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.pwr.steamgen.SteamGeneratorControllerBlockEntity;

import java.util.List;

/**
 * The control room reading the plant: a pressurized reactor's loop reaches monitors, alarms and
 * gauges; steam generators and containments fill the values and graph series the monitors expect;
 * chart recorders pick pens that suit what they are linked to; annunciator presets set up all six
 * windows; and a shut blast door counts as containment wall until it opens.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class PlantIntegrationTests {
    private static final BlockPos ORIGIN = FissionTestBuilder.ORIGIN;
    private static final BlockPos DEVICE = new BlockPos(9, 1, 9);
    private static final BlockPos SECOND_DEVICE = new BlockPos(11, 1, 9);
    /** A closed vestibule outside the blast door in the room's south wall. */
    private static final BlockPos BLAST_VESTIBULE_MIN = new BlockPos(2, 1, 8);
    private static final BlockPos BLAST_VESTIBULE_MAX = new BlockPos(8, 5, 11);

    private static GlobalPos global(GameTestHelper helper, BlockPos relative) {
        return GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(relative));
    }

    private static ReactorBlockEntity pressurizedReactor(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        FissionTestBuilder.pressurizer(helper, ORIGIN, 4, 2);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        helper.runAfterDelay(5, () -> {
            int filled = reactor.fillCoolant(new FluidStack(AllNuclearFluids.PRIMARY_COOLANT.get(), 14_000), IFluidHandler.FluidAction.EXECUTE);
            helper.assertTrue(filled == 14_000, "the reactor takes the primary coolant, took " + filled);
            PwrModeTests.heat(reactor, 0.0);
        });
        return reactor;
    }

    private static <T extends net.minecraft.world.level.block.entity.BlockEntity> T place(GameTestHelper helper, BlockPos pos, net.minecraft.world.level.block.Block block) {
        helper.setBlock(pos, block.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
        return helper.getBlockEntity(pos);
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 200)
    public static void alarmsAndGaugesReadThePressurizedLoop(GameTestHelper helper) {
        ReactorBlockEntity reactor = pressurizedReactor(helper);
        helper.runAfterDelay(40, () -> {
            helper.setBlock(DEVICE, AllNuclearBlocks.NUCLEAR_ALARM.get());
            NuclearAlarmBlockEntity alarm = helper.getBlockEntity(DEVICE);
            helper.assertTrue(alarm != null, "the alarm has its block entity");
            alarm.setMetric(AlarmMetric.PRESSURE_HIGH);
            alarm.setSetting(40);
            alarm.setTarget(global(helper, FissionTestBuilder.CONTROLLER));
            helper.assertTrue(alarm.isTripped(), "155 bar is past a 100 bar high-pressure alarm");
            alarm.setSetting(66);
            alarm.check();
            helper.assertFalse(alarm.isTripped(), "but not past " + AlarmMetric.PRESSURE_HIGH.format(66));

            MonitorReading sample = MonitorReading.sample(LinkedTarget.resolve(helper.getLevel(), alarm.getBlockPos(), alarm.getTarget()));
            float pressure = sample.value(MonitorReading.R_PRESSURE);
            helper.assertTrue(sample.flag(MonitorReading.R_PWR) && Math.abs(pressure - PwrConfig.pressureSetpoint) < 5,
                    "the sample carries the primary pressure, " + pressure);
            helper.assertTrue(sample.value(MonitorReading.R_T_COLD) > 200 && sample.value(MonitorReading.R_PZR_LEVEL) > 0,
                    "the cold leg and the pressurizer level");
            helper.assertFalse(sample.flag(MonitorReading.R_RELIEF), "and the relief valve shut");
            float needle = GaugeReading.REACTOR_PRESSURE.fraction(sample);
            helper.assertTrue(needle > 0.55f && needle < 0.7f, "a pressure gauge stands at about 62%, " + needle);
            helper.assertTrue(sample.value(MonitorReading.R_SUBCOOLING) > 20 && !AlarmMetric.SUBCOOLING_LOW.tripped(sample, 20),
                    "a cold loop is far from boiling, margin " + sample.value(MonitorReading.R_SUBCOOLING));
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 200)
    public static void steamGeneratorsAndContainmentsFillTheirGraphs(GameTestHelper helper) {
        BlockPos generatorAt = new BlockPos(10, 1, 10);
        SteamGeneratorTestBuilder.build(helper, generatorAt, 4);
        ContainmentTestBuilder.concreteRoom(helper);
        helper.runAfterDelay(5, () -> {
            SteamGeneratorControllerBlockEntity generator = helper.getBlockEntity(SteamGeneratorTestBuilder.controller(generatorAt));
            generator.getFeedTank().fill(new FluidStack(Fluids.WATER, 8_000), IFluidHandler.FluidAction.EXECUTE);
        });
        boolean[] injected = new boolean[1];
        helper.onEachTick(() -> {
            ContainmentMonitorBlockEntity monitor = ContainmentTestBuilder.monitor(helper, ContainmentTestBuilder.MONITOR);
            if (!injected[0] && monitor != null && monitor.sealed()) {
                injected[0] = true;
                monitor.addHydrogen(ContainmentTestBuilder.airMoles(monitor) * 0.02, ContainmentModel.WALL_TEMPERATURE);
            }
        });
        helper.succeedWhen(() -> {
            SteamGeneratorControllerBlockEntity generator = helper.getBlockEntity(SteamGeneratorTestBuilder.controller(generatorAt));
            helper.assertTrue(generator != null && generator.isFormed(), "the steam generator forms");
            MonitorReading sg = MonitorReading.sample(new LinkedTarget.Plant(generator));
            helper.assertTrue(sg.kind() == TargetKind.STEAM_GENERATOR && sg.flag(MonitorReading.S_FORMED), "read as a formed steam generator");
            helper.assertTrue(Math.abs(sg.value(MonitorReading.S_LEVEL) - 0.5f) < 1e-3, "half full of feedwater, " + sg.value(MonitorReading.S_LEVEL));
            helper.assertTrue(sg.seriesA() == sg.value(MonitorReading.S_LEVEL), "the graph's first series is the level the legend names");
            helper.assertTrue(Math.abs(sg.seriesB() - Math.min(1.0f, sg.value(MonitorReading.S_STEAM) / MonitorReading.SG_STEAM_MAX)) < 1e-4f,
                    "and the second the steam made, on the gauges' scale");
            helper.assertTrue(AlarmMetric.SG_STEAM_LOW.tripped(sg, 2) && !AlarmMetric.SG_LEVEL_HIGH.tripped(sg, 90),
                    "a cold generator makes no steam and is not overfilled");

            ContainmentMonitorBlockEntity monitor = ContainmentTestBuilder.monitor(helper, ContainmentTestBuilder.MONITOR);
            helper.assertTrue(injected[0] && monitor != null && monitor.sealed(), "the room is sealed and holds hydrogen");
            MonitorReading room = MonitorReading.sample(new LinkedTarget.Plant(monitor));
            float hydrogen = room.value(MonitorReading.N_H2);
            helper.assertTrue(hydrogen > 0.015f && hydrogen < 0.025f, "about 2% hydrogen, " + hydrogen);
            helper.assertTrue(room.seriesA() > 0 && room.seriesA() < 0.2f, "the first series is the pressure, a little up, " + room.seriesA());
            helper.assertTrue(Math.abs(room.seriesB() - hydrogen / (float) ContainmentModel.CERTAIN_IGNITION) < 1e-4,
                    "the second the hydrogen, full at certain ignition");
            helper.assertTrue(AlarmMetric.HYDROGEN.tripped(room, 15) && !AlarmMetric.HYDROGEN.tripped(room, 40),
                    "past a 1.5% hydrogen alarm, short of 4%");
            helper.assertFalse(AlarmMetric.CONTAINMENT_OPEN.tripped(room, 0), "and the containment-open alarm stays dark");

            ChartRecorderBlockEntity recorder = place(helper, new BlockPos(11, 1, 1), ControlRoom2Registry.CHART_RECORDER.get());
            recorder.setTarget(global(helper, ContainmentTestBuilder.MONITOR));
            helper.assertTrue(recorder.kind() == TargetKind.CONTAINMENT && pens(recorder).equals(List.of(GaugeReading.CONTAINMENT_PRESSURE,
                    GaugeReading.CONTAINMENT_HYDROGEN, GaugeReading.CONTAINMENT_ACTIVITY)),
                    "a containment gets pressure, hydrogen and activity pens, got " + pens(recorder));
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 200)
    public static void chartRecordersPickPensThatSuitTheirTarget(GameTestHelper helper) {
        pressurizedReactor(helper);
        BlockPos generatorAt = new BlockPos(10, 1, 10);
        SteamGeneratorTestBuilder.build(helper, generatorAt, 4);
        helper.runAfterDelay(20, () -> {
            ChartRecorderBlockEntity recorder = place(helper, new BlockPos(8, 1, 1), ControlRoom2Registry.CHART_RECORDER.get());
            recorder.setTarget(global(helper, FissionTestBuilder.CONTROLLER));
            helper.assertTrue(pens(recorder).equals(List.of(GaugeReading.REACTOR_PRESSURE, GaugeReading.REACTOR_HOT_LEG,
                    GaugeReading.PRESSURIZER_LEVEL)), "a pressurized reactor: pressure, hot leg, pressurizer, got " + pens(recorder));
            recorder.setTarget(global(helper, SteamGeneratorTestBuilder.controller(generatorAt)));
            helper.assertTrue(recorder.kind() == TargetKind.STEAM_GENERATOR && pens(recorder).equals(List.of(GaugeReading.SG_LEVEL,
                    GaugeReading.SG_STEAM, GaugeReading.SG_PRIMARY_DT)), "a steam generator: level, steam, primary ΔT, got " + pens(recorder));
            recorder.setPen(2, null);
            recorder.setTarget(global(helper, SteamGeneratorTestBuilder.controller(generatorAt)));
            helper.assertTrue(recorder.pen(2) == null, "pens the player changed stay as they are");
            helper.succeed();
        });
    }

    private static List<GaugeReading> pens(ChartRecorderBlockEntity recorder) {
        List<GaugeReading> pens = new java.util.ArrayList<>();
        for (int pen = 0; pen < 3; pen++) {
            if (recorder.pen(pen) != null) {
                pens.add(recorder.pen(pen));
            }
        }
        return pens;
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 200)
    public static void anAnnunciatorPresetSetsUpEveryWindow(GameTestHelper helper) {
        pressurizedReactor(helper);
        helper.runAfterDelay(30, () -> {
            AnnunciatorPanelBlockEntity panel = place(helper, DEVICE, ControlRoom2Registry.ANNUNCIATOR_PANEL.get());
            panel.setTarget(global(helper, FissionTestBuilder.CONTROLLER));
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            BlockPos at = helper.absolutePos(DEVICE);
            player.setPos(at.getX() + 2, at.getY(), at.getZ());

            CompoundTag bogus = new CompoundTag();
            bogus.putString("Preset", "NOT_A_PRESET");
            helper.assertFalse(ConfigureDevicePayload.apply(player, new ConfigureDevicePayload(at, 0, bogus)), "an unknown preset is refused");

            CompoundTag config = new CompoundTag();
            config.putString("Preset", AnnunciatorPreset.PWR_PRIMARY.name());
            ListTag labels = new ListTag();
            labels.add(StringTag.valueOf(" §PRESIÓN ALTA DEL PRIMARIO"));
            labels.add(StringTag.valueOf("  "));
            config.put("Labels", labels);
            helper.assertTrue(ConfigureDevicePayload.apply(player, new ConfigureDevicePayload(at, 3, config)), "a preset applies");

            List<AnnunciatorPreset.Window> expected = AnnunciatorPreset.PWR_PRIMARY.windows(AnnunciatorPanelBlockEntity.plantConfig());
            for (int i = 0; i < AnnunciatorPanelBlockEntity.WINDOWS; i++) {
                AnnunciatorPanelBlockEntity.Window window = panel.window(i);
                helper.assertTrue(window.metric().name().equals(expected.get(i).metric()) && window.setting() == expected.get(i).setting(),
                        "window " + (i + 1) + " watches " + expected.get(i) + ", got " + window.metric() + " " + window.setting());
                helper.assertTrue(window.target() != null, "and keeps its link");
            }
            helper.assertTrue(panel.window(0).label().equals("PRESIÓN ALTA DEL"), "labels are cleaned and cut to fit, got "
                    + panel.window(0).label());
            helper.assertTrue(panel.window(1).label().equals("PRESS LO"), "a blank label takes the preset's, got " + panel.window(1).label());
            helper.assertTrue(panel.window(5).label().equals("LOSS OF FLOW"), "and so does a missing one");
            float high = AlarmMetric.PRESSURE_HIGH.scaled(panel.window(0).setting());
            helper.assertTrue(high > PwrConfig.pressureSetpoint && high < PwrConfig.reliefOpenPressure,
                    "the high-pressure window sits between the setpoint and the relief valve, " + high);

            panel.check();
            helper.assertTrue(panel.logic().state(0) == AnnunciatorLogic.State.NORMAL && panel.logic().state(1) == AnnunciatorLogic.State.NORMAL,
                    "a loop at its setpoint lights neither pressure window");
            helper.assertTrue(panel.logic().state(5) == AnnunciatorLogic.State.ALERT, "a loop with no pumps lights loss of flow");
            helper.succeed();
        });
    }

    /**
     * The door opens into a closed vestibule rather than the open arena: a survey that runs out into
     * the unloaded world around a game test keeps its last answer, which made this test flaky. Open,
     * the room and the vestibule are one sealed space; a survey capped below that space then shows
     * the doorway as an opening, every time.
     */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 600)
    public static void aShutBlastDoorKeepsTheContainmentSealed(GameTestHelper helper) {
        // The vestibule shares the room's south wall: 5x3x2 of air, less the switch standing in it.
        ContainmentTestBuilder.room(helper, BLAST_VESTIBULE_MIN, BLAST_VESTIBULE_MAX, PwrRegistry.CONTAINMENT_CONCRETE.get());
        ContainmentTestBuilder.concreteRoom(helper);
        // A 2x2 door in the south wall, its frame and controller part of the wall.
        BlockPos controller = BlastDoorTests.buildDoor(helper, CivilDefenseRegistry.STEEL_BLAST_DOOR.get(), 4, 2, 8, 2, 2);
        // Against the controller, inside the vestibule.
        BlockPos switchPos = controller.south();
        int joined = ContainmentTestBuilder.ROOM_AIR + 2 * 2 + (5 * 3 * 2 - 1);
        helper.startSequence()
                .thenWaitUntil(() -> {
                    BlastDoorControllerBlockEntity door = helper.getBlockEntity(controller);
                    helper.assertTrue(door != null && door.getLayout() != null, "the door forms in its frame");
                    ContainmentMonitorBlockEntity monitor = ContainmentTestBuilder.monitor(helper, ContainmentTestBuilder.MONITOR);
                    helper.assertTrue(monitor != null && monitor.sealed() && !monitor.isSurveying(), "the room with a shut door is sealed");
                    helper.assertTrue(monitor.getShape().volume() == ContainmentTestBuilder.ROOM_AIR, "with the same air inside");
                    helper.assertTrue(monitor.getShape().weakestRating() == 6.0,
                            "the door (10 bar) is no weaker than the concrete, " + monitor.getShape().weakestRating());
                })
                .thenExecute(() -> helper.setBlock(switchPos, Blocks.REDSTONE_BLOCK))
                .thenWaitUntil(() -> {
                    helper.assertTrue(helper.getBlockState(new BlockPos(5, 2, 8)).getValue(BlastDoorBlock.LEAF) == LeafState.OPEN,
                            "the door opens");
                    ContainmentMonitorBlockEntity monitor = ContainmentTestBuilder.monitor(helper, ContainmentTestBuilder.MONITOR);
                    helper.assertTrue(monitor != null && monitor.sealed() && !monitor.isSurveying()
                                    && monitor.getShape() != null && monitor.getShape().volume() == joined,
                            "the open doorway joins the room to the vestibule, " + (monitor == null || monitor.getShape() == null
                                    ? "-" : monitor.getShape().volume()) + " blocks of air, expected " + joined);
                    helper.assertFalse(AlarmMetric.CONTAINMENT_OPEN.tripped(MonitorReading.sample(new LinkedTarget.Plant(monitor)), 0),
                            "a doorway into a closed space leaves the containment-open alarm dark");
                })
                .thenExecute(() -> {
                    ContainmentMonitorBlockEntity monitor = ContainmentTestBuilder.monitor(helper, ContainmentTestBuilder.MONITOR);
                    helper.assertTrue(monitor != null, "the monitor stands");
                    int cap = PwrConfig.containmentMaxVolume;
                    try {
                        // No more air than the room alone: the open doorway now leads past the cap.
                        PwrConfig.containmentMaxVolume = joined - 1;
                        monitor.inspect(null);
                        for (int i = 0; i < 4 && (i == 0 || monitor.isSurveying()); i++) {
                            monitor.tick();
                        }
                    } finally {
                        PwrConfig.containmentMaxVolume = cap;
                    }
                    helper.assertTrue(!monitor.sealed() && !monitor.isSurveying(), "the monitor measures the open doorway as an opening");
                    MonitorReading room = MonitorReading.sample(new LinkedTarget.Plant(monitor));
                    helper.assertTrue(AlarmMetric.CONTAINMENT_OPEN.tripped(room, 0), "and the containment-open alarm lights");
                })
                .thenExecute(() -> helper.setBlock(switchPos, Blocks.AIR))
                .thenWaitUntil(() -> {
                    helper.assertTrue(helper.getBlockState(new BlockPos(5, 2, 8)).getValue(BlastDoorBlock.LEAF) == LeafState.CLOSED,
                            "the door shuts");
                    ContainmentMonitorBlockEntity monitor = ContainmentTestBuilder.monitor(helper, ContainmentTestBuilder.MONITOR);
                    helper.assertTrue(monitor != null && monitor.sealed() && !monitor.isSurveying(), "and the room seals again");
                })
                .thenSucceed();
    }
}
