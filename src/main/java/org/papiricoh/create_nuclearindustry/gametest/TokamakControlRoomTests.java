package org.papiricoh.create_nuclearindustry.gametest;

import com.simibubi.create.api.behaviour.display.DisplaySource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.AllNuclearDisplaySources;
import org.papiricoh.create_nuclearindustry.AllNuclearEntities;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.controlroom.AlarmMetric;
import org.papiricoh.create_nuclearindustry.controlroom.ControlRoom2Registry;
import org.papiricoh.create_nuclearindustry.controlroom.LinkedTarget;
import org.papiricoh.create_nuclearindustry.controlroom.MonitorReading;
import org.papiricoh.create_nuclearindustry.controlroom.TargetKind;
import org.papiricoh.create_nuclearindustry.controlroom.TokamakScales;
import org.papiricoh.create_nuclearindustry.controlroom.annunciator.AnnunciatorPanelBlockEntity;
import org.papiricoh.create_nuclearindustry.controlroom.annunciator.AnnunciatorPreset;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ControlDeskBlock;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ControlDeskBlockEntity;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ControlModule;
import org.papiricoh.create_nuclearindustry.controlroom.desk.GaugeReading;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ModuleFunction;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ModuleType;
import org.papiricoh.create_nuclearindustry.fusion.api.TokamakPhase;
import org.papiricoh.create_nuclearindustry.fusion.api.TokamakReadings;
import org.papiricoh.create_nuclearindustry.fusion.api.TripCause;
import org.papiricoh.create_nuclearindustry.fusion.blockentity.FusionReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.fusion.display.TokamakDisplaySource;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The tokamak in the control room: monitor readings, Display Link lines, start, shutdown and heating
 * from a desk, the automatic fusion power controller, the new alarms and the annunciator preset.
 * Everything goes through the controller's {@link org.papiricoh.create_nuclearindustry.fusion.api.TokamakControl},
 * so the assertions hold whatever startup sequence sits behind it.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class TokamakControlRoomTests {
    private static final int RADIUS = 4;
    private static final BlockPos DESK = new BlockPos(1, 1, 1);
    private static final BlockPos PANEL = new BlockPos(1, 1, 4);

    @GameTest(template = NuclearGameTests.TOKAMAK_ARENA)
    public static void aMonitorSampleCarriesTheNewReadings(GameTestHelper helper) {
        TokamakTestBuilder.build(helper, RADIUS, true, 0);
        FusionReactorBlockEntity tokamak = TokamakTestBuilder.tokamak(helper, RADIUS);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(tokamak.isFormed(), "the tokamak should form");
            MonitorReading sample = MonitorReading.sample(new LinkedTarget.Tokamak(tokamak));
            TokamakReadings readings = tokamak.readings();
            helper.assertTrue(sample.kind() == TargetKind.TOKAMAK && sample.flag(MonitorReading.K_FORMED), "a formed tokamak sample");
            helper.assertTrue(sample.values().length > MonitorReading.K_TRIPPED, "the sample holds the new readings");
            helper.assertTrue(TokamakScales.phase(sample.value(MonitorReading.K_PHASE)) == readings.phase(),
                    "the phase, got " + sample.value(MonitorReading.K_PHASE));
            helper.assertTrue(TokamakScales.trip(sample.value(MonitorReading.K_TRIP)) == readings.tripCause(), "the trip cause");
            helper.assertTrue(sample.value(MonitorReading.K_VACUUM) == (float) readings.vacuum(), "the chamber pressure");
            helper.assertTrue(sample.value(MonitorReading.K_MAGNET_TEMPERATURE) == (float) readings.magnetTemperature(),
                    "the magnet temperature");
            helper.assertTrue(sample.value(MonitorReading.K_FIELD) == (float) readings.field(), "the field");
            helper.assertTrue(sample.value(MonitorReading.K_DENSITY) == (float) readings.density(), "the density");
            helper.assertTrue(sample.value(MonitorReading.K_GAIN) == TokamakScales.storedGain(readings.gain()), "the gain, finite");
            helper.assertTrue(sample.value(MonitorReading.K_FUEL_RATE_MAX) == (float) tokamak.maxFuelRate(), "the fuelling ceiling");
            helper.assertTrue(sample.flag(MonitorReading.K_HEATING_ON) == readings.heatingEnabled(), "the heating switch");
            for (GaugeReading gauge : GaugeReading.forKind(TargetKind.TOKAMAK)) {
                float needle = gauge.fraction(sample);
                helper.assertTrue(needle >= 0 && needle <= 1, gauge + " reads on its scale, got " + needle);
            }
            helper.assertTrue(GaugeReading.CHAMBER_PRESSURE.fraction(sample) > 0.5f || readings.vacuum() < 0,
                    "a chamber at atmosphere reads high on the vacuum gauge");
            helper.assertTrue(GaugeReading.defaultPens(TargetKind.TOKAMAK, false)
                            .equals(List.of(GaugeReading.FUSION_POWER, GaugeReading.PLASMA_TEMPERATURE, GaugeReading.PLASMA_DENSITY)),
                    "a chart recorder on a tokamak starts with power, temperature and density");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.TOKAMAK_ARENA)
    public static void theDisplayLinkShowsTranslatedLines(GameTestHelper helper) {
        TokamakTestBuilder.build(helper, RADIUS, true, 0);
        FusionReactorBlockEntity tokamak = TokamakTestBuilder.tokamak(helper, RADIUS);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(tokamak.isFormed(), "the tokamak should form");
            List<DisplaySource> sources = DisplaySource.BY_BLOCK_ENTITY.get(AllNuclearEntities.FUSION_CONTROLLER.get());
            helper.assertTrue(sources != null && sources.contains(AllNuclearDisplaySources.TOKAMAK.get()),
                    "the controller offers its status to a Display Link");
            Set<String> keys = keys(TokamakDisplaySource.lines(tokamak.readings(), tokamak.getValidationMessage()));
            for (String line : new String[]{"plasma", "power", "density", "magnets", "vacuum", "fuel", "helium", "stability"}) {
                helper.assertTrue(keys.contains(TokamakDisplaySource.KEY + line), "a translated " + line + " line, got " + keys);
            }
            helper.assertTrue(keys.contains(TokamakDisplaySource.KEY + "phase") || keys.contains(TokamakDisplaySource.KEY + "tripped"),
                    "the first line says what the tokamak is doing");
            helper.assertTrue(keys.contains(TokamakDisplaySource.KEY + "heating") || keys.contains(TokamakDisplaySource.KEY + "heating_off"),
                    "and its heating");
            Set<String> unformed = keys(TokamakDisplaySource.lines(TokamakReadings.NONE, "Not formed"));
            helper.assertTrue(unformed.contains(TokamakDisplaySource.KEY + "unformed"), "an unformed tokamak says so");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.TOKAMAK_ARENA)
    public static void deskButtonsStartAndShutDownTheTokamak(GameTestHelper helper) {
        TokamakTestBuilder.build(helper, RADIUS, true, 0);
        FusionReactorBlockEntity tokamak = TokamakTestBuilder.tokamak(helper, RADIUS);
        placeDesk(helper);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(tokamak.isFormed(), "the tokamak should form");
            ControlDeskBlockEntity desk = desk(helper, tokamak);
            helper.assertTrue(desk != null, "a control desk");
            setUp(helper, desk, 0, ModuleType.PUSH_BUTTON, ModuleFunction.TOKAMAK_SHUTDOWN);
            setUp(helper, desk, 1, ModuleType.PUSH_BUTTON, ModuleFunction.TOKAMAK_START);
            helper.assertTrue(desk.press(0, false), "the shutdown button acts on a tokamak");
            helper.assertTrue(desk.takeFeedback(0) != null, "and tells the player");
            helper.assertTrue(tokamak.phase() == TokamakPhase.OFFLINE || tokamak.phase() == TokamakPhase.RAMP_DOWN,
                    "the tokamak shuts down, got " + tokamak.phase());
            helper.assertTrue(tokamak.tripCause() == TripCause.NONE || tokamak.tripCause() == TripCause.OPERATOR,
                    "an operator's shutdown, got " + tokamak.tripCause());
            MonitorReading afterShutdown = MonitorReading.sample(new LinkedTarget.Tokamak(tokamak));
            helper.assertFalse(AlarmMetric.TOKAMAK_TRIP.tripped(afterShutdown, 0), "the operator's own shutdown is no trip");

            helper.assertTrue(desk.press(1, false), "the start button acts on the tokamak");
            Component said = desk.takeFeedback(1);
            helper.assertTrue(said != null, "and says whether it started");
            if (tokamak.tripCause() == TripCause.NONE) {
                helper.assertTrue(tokamak.phase() != TokamakPhase.OFFLINE || tokamak.isFuelEnabled(),
                        "an accepted start sets the tokamak going");
                helper.assertTrue(key(said).endsWith("tokamak.start"), "the player hears it started, got " + key(said));
            } else {
                helper.assertTrue(key(said).endsWith("tokamak.refused"), "a refused start says why, got " + key(said));
            }
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.TOKAMAK_ARENA)
    public static void aRefusedStartSaysWhyAndTheLeverSpringsBack(GameTestHelper helper) {
        // A lone controller: no structure, so any start is refused.
        helper.setBlock(TokamakTestBuilder.controller(RADIUS), AllNuclearBlocks.FUSION_CONTROLLER.get());
        FusionReactorBlockEntity tokamak = TokamakTestBuilder.tokamak(helper, RADIUS);
        placeDesk(helper);
        helper.runAfterDelay(5, () -> {
            helper.assertFalse(tokamak.isFormed(), "a lone controller forms nothing");
            ControlDeskBlockEntity desk = desk(helper, tokamak);
            helper.assertTrue(desk != null, "a control desk");
            setUp(helper, desk, 0, ModuleType.LEVER, ModuleFunction.TOKAMAK_START);
            helper.assertTrue(desk.press(0, false), "the lever acts on a tokamak");
            Component said = desk.takeFeedback(0);
            helper.assertTrue(said != null && key(said).endsWith("tokamak.refused"), "the start is refused");
            helper.assertTrue(said.getContents() instanceof TranslatableContents refused && refused.getArgs().length == 1
                            && refused.getArgs()[0] instanceof Component reason
                            && key(reason).equals(TripCause.STRUCTURE.langKey()),
                    "because there is no structure");
            ControlModule lever = desk.module(0);
            helper.assertTrue(lever != null && lever.shown() == 0, "the lever springs back down");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.TOKAMAK_ARENA)
    public static void aLeverSwitchesTheHeating(GameTestHelper helper) {
        TokamakTestBuilder.build(helper, RADIUS, true, 0);
        FusionReactorBlockEntity tokamak = TokamakTestBuilder.tokamak(helper, RADIUS);
        placeDesk(helper);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(tokamak.isFormed(), "the tokamak should form");
            ControlDeskBlockEntity desk = desk(helper, tokamak);
            helper.assertTrue(desk != null, "a control desk");
            setUp(helper, desk, 0, ModuleType.LEVER, ModuleFunction.TOKAMAK_HEATING);
            tokamak.setHeating(true, 0.7);
            boolean before = tokamak.readings().heatingEnabled();
            helper.assertTrue(desk.press(0, false), "the lever acts on the tokamak");
            TokamakReadings after = tokamak.readings();
            helper.assertTrue(after.heatingEnabled() != before, "and switches its heating");
            helper.assertTrue(Math.abs(after.heatingLevel() - 0.7) < 1.0e-6, "keeping its level, got " + after.heatingLevel());
            ControlModule lever = desk.module(0);
            helper.assertTrue(lever != null && (lever.shown() > 0.5f) == after.heatingEnabled(), "the lever shows it");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.TOKAMAK_ARENA)
    public static void automaticFusionPowerWaitsForABurn(GameTestHelper helper) {
        TokamakTestBuilder.build(helper, RADIUS, true, 0);
        FusionReactorBlockEntity tokamak = TokamakTestBuilder.tokamak(helper, RADIUS);
        placeDesk(helper);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(tokamak.isFormed(), "the tokamak should form");
            ControlDeskBlockEntity desk = desk(helper, tokamak);
            helper.assertTrue(desk != null, "a control desk");
            setUp(helper, desk, 0, ModuleType.CONTROLLER, ModuleFunction.AUTO_FUSION_POWER);
            ControlModule controller = desk.module(0);
            helper.assertTrue(controller != null && controller.isController(), "a fusion power controller");
            helper.assertTrue(controller.formatSetpoint().equals("250 MW"), "starting near what R=4 makes, got " + controller.formatSetpoint());
            tokamak.setFuelRate(0.5);
        });
        helper.runAfterDelay(70, () -> {
            ControlDeskBlockEntity desk = desk(helper, tokamak);
            ControlModule controller = desk == null ? null : desk.module(0);
            helper.assertTrue(controller != null && controller.active(), "the controller works on a tokamak");
            if (tokamak.phase() != TokamakPhase.BURN) {
                helper.assertTrue(controller.demand() < 0, "with no burn it stands by, got " + controller.demand());
                helper.assertTrue(Math.abs(tokamak.readings().fuelRate() - 0.5) < 1.0e-6,
                        "and leaves the fuelling alone, got " + tokamak.readings().fuelRate());
            }
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.TOKAMAK_ARENA)
    public static void tokamakAlarmsReadTheirReadings(GameTestHelper helper) {
        helper.runAfterDelay(1, () -> {
            float[] values = new float[MonitorReading.K_TRIPPED + 1];
            values[MonitorReading.K_FORMED] = 1;
            values[MonitorReading.K_PHASE] = TokamakPhase.BURN.ordinal();
            values[MonitorReading.K_GAIN] = 2.0f;
            values[MonitorReading.K_VACUUM] = -2.0f;
            values[MonitorReading.K_MAGNET_TEMPERATURE] = 30.0f;
            values[MonitorReading.K_FIELD] = 1.0f;
            values[MonitorReading.K_DENSITY] = 0.95f;
            values[MonitorReading.K_TRITIUM] = 0.05f;
            MonitorReading poor = sample(values);
            int gainSetting = TokamakScales.gainSetting(TokamakScales.NET_GAIN);
            helper.assertTrue(AlarmMetric.FUSION_GAIN_LOW.tripped(poor, gainSetting), "Q of 2 is below 3");
            helper.assertTrue(AlarmMetric.VACUUM_POOR.tripped(poor, TokamakScales.vacuumSetting(-3.5)), "1e-2 Pa is poor vacuum");
            helper.assertTrue(AlarmMetric.MAGNET_TEMPERATURE.tripped(poor, TokamakScales.magnetSetting(10)), "30 K magnets are warm");
            helper.assertTrue(AlarmMetric.PLASMA_DENSITY.tripped(poor, 90), "95% of the density limit");
            helper.assertTrue(AlarmMetric.TRITIUM_LOW.tripped(poor, 10), "5% tritium");

            values[MonitorReading.K_GAIN] = TokamakScales.GAIN_IGNITED;
            values[MonitorReading.K_VACUUM] = -4.0f;
            values[MonitorReading.K_MAGNET_TEMPERATURE] = 4.5f;
            MonitorReading good = sample(values);
            helper.assertFalse(AlarmMetric.FUSION_GAIN_LOW.tripped(good, gainSetting), "an ignited plasma has gain to spare");
            helper.assertFalse(AlarmMetric.VACUUM_POOR.tripped(good, TokamakScales.vacuumSetting(-3.5)), "1e-4 Pa is fine");
            helper.assertFalse(AlarmMetric.MAGNET_TEMPERATURE.tripped(good, TokamakScales.magnetSetting(10)), "4.5 K is cold");

            values[MonitorReading.K_PHASE] = TokamakPhase.COOL_DOWN.ordinal();
            values[MonitorReading.K_GAIN] = 0;
            values[MonitorReading.K_VACUUM] = 5.0f;
            values[MonitorReading.K_MAGNET_TEMPERATURE] = 293.0f;
            values[MonitorReading.K_FIELD] = 0;
            MonitorReading cooling = sample(values);
            helper.assertFalse(AlarmMetric.FUSION_GAIN_LOW.tripped(cooling, gainSetting), "no burn, no gain to judge");
            helper.assertFalse(AlarmMetric.VACUUM_POOR.tripped(cooling, 0), "no plasma, no vacuum needed");
            helper.assertFalse(AlarmMetric.MAGNET_TEMPERATURE.tripped(cooling, 0), "magnets cooling down with no field");

            values[MonitorReading.K_PHASE] = TokamakPhase.OFFLINE.ordinal();
            values[MonitorReading.K_TRIPPED] = 1;
            helper.assertTrue(AlarmMetric.TOKAMAK_TRIP.tripped(sample(values), 0), "a trip lights the trip alarm");
            helper.assertFalse(AlarmMetric.TOKAMAK_TRIP.watches(TargetKind.REACTOR), "tokamak alarms watch tokamaks only");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.TOKAMAK_ARENA)
    public static void theTokamakPresetSetsUpTheAnnunciator(GameTestHelper helper) {
        TokamakTestBuilder.build(helper, RADIUS, true, 0);
        FusionReactorBlockEntity tokamak = TokamakTestBuilder.tokamak(helper, RADIUS);
        helper.setBlock(PANEL, ControlRoom2Registry.ANNUNCIATOR_PANEL.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(tokamak.isFormed(), "the tokamak should form");
            AnnunciatorPanelBlockEntity panel = helper.getBlockEntity(PANEL);
            helper.assertTrue(panel != null, "an annunciator panel");
            GlobalPos target = GlobalPos.of(helper.getLevel().dimension(), tokamak.getBlockPos());
            for (int i = 0; i < AnnunciatorPanelBlockEntity.WINDOWS; i++) {
                panel.linkWindow(i, target);
            }
            panel.applyPreset(AnnunciatorPreset.TOKAMAK, new ListTag());
            List<AnnunciatorPreset.Window> windows = AnnunciatorPreset.TOKAMAK.windows(AnnunciatorPanelBlockEntity.plantConfig());
            MonitorReading sample = MonitorReading.sample(new LinkedTarget.Tokamak(tokamak));
            for (int i = 0; i < AnnunciatorPanelBlockEntity.WINDOWS; i++) {
                AnnunciatorPanelBlockEntity.Window window = panel.window(i);
                helper.assertTrue(window.metric().name().equals(windows.get(i).metric()), "window " + (i + 1) + " watches "
                        + windows.get(i).metric() + ", got " + window.metric());
                helper.assertTrue(window.metric().watches(TargetKind.TOKAMAK), window.metric() + " is a tokamak alarm");
                helper.assertTrue(window.label().equals(windows.get(i).label()), "with its label, got " + window.label());
                helper.assertTrue(window.target() != null && window.kind() == TargetKind.TOKAMAK, "and keeps its link");
                if (tokamak.phase() == TokamakPhase.OFFLINE && tokamak.tripCause() == TripCause.NONE) {
                    helper.assertFalse(window.metric().tripped(sample, window.setting()), window.metric() + " is quiet on a cold tokamak");
                }
            }
            helper.succeed();
        });
    }

    // ------------------------------------------------------------------ helpers

    private static MonitorReading sample(float[] values) {
        return new MonitorReading(TargetKind.TOKAMAK, "test", values.clone(), 0, 0);
    }

    private static void placeDesk(GameTestHelper helper) {
        BlockState state = AllNuclearBlocks.REACTOR_CONTROL_PANEL.get().defaultBlockState().setValue(ControlDeskBlock.FACING, Direction.NORTH);
        helper.setBlock(DESK, state);
        helper.getLevel().setBlock(helper.absolutePos(DESK),
                ControlDeskBlock.withConnections(helper.getBlockState(DESK), helper.getLevel(), helper.absolutePos(DESK)), 3);
    }

    /** The desk, linked to the tokamak's controller; null if it is not there. */
    private static ControlDeskBlockEntity desk(GameTestHelper helper, FusionReactorBlockEntity tokamak) {
        if (!(helper.getLevel().getBlockEntity(helper.absolutePos(DESK)) instanceof ControlDeskBlockEntity desk)) {
            return null;
        }
        GlobalPos target = GlobalPos.of(helper.getLevel().dimension(), tokamak.getBlockPos());
        if (!target.equals(desk.getTarget())) {
            desk.setTarget(target);
        }
        return desk;
    }

    private static void setUp(GameTestHelper helper, ControlDeskBlockEntity desk, int slot, ModuleType type, ModuleFunction function) {
        if (desk.module(slot) == null) {
            desk.insert(slot, type);
        }
        ControlModule module = desk.module(slot);
        helper.assertTrue(module != null, "a module fits slot " + slot);
        desk.configure(slot, module.withFunction(function).writeConfig(helper.getLevel().registryAccess()));
        helper.assertTrue(desk.module(slot).function() == function, type + " takes " + function);
    }

    private static String key(Component component) {
        return component.getContents() instanceof TranslatableContents translatable ? translatable.getKey() : component.getString();
    }

    private static Set<String> keys(List<MutableComponent> lines) {
        return lines.stream().map(TokamakControlRoomTests::key).collect(Collectors.toSet());
    }
}
