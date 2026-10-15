package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.controlroom.AlarmMetric;
import org.papiricoh.create_nuclearindustry.controlroom.LinkedTarget;
import org.papiricoh.create_nuclearindustry.controlroom.MonitorReading;
import org.papiricoh.create_nuclearindustry.controlroom.TargetKind;
import org.papiricoh.create_nuclearindustry.reactor.physics.kinetics.ShutdownState;
import org.papiricoh.create_nuclearindustry.civildefense.CivilDefenseConfig;
import org.papiricoh.create_nuclearindustry.civildefense.CivilDefenseRegistry;
import org.papiricoh.create_nuclearindustry.civildefense.ThreatRegistry;
import org.papiricoh.create_nuclearindustry.civildefense.siren.AirRaidSirenBlock;
import org.papiricoh.create_nuclearindustry.civildefense.siren.AirRaidSirenBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.pwr.containment.ContainmentModel;
import org.papiricoh.create_nuclearindustry.reactor.pwr.containment.ContainmentMonitorBlockEntity;

/**
 * Plant accidents sound the air raid sirens around them with no redstone: a meltdown, and a
 * hydrogen burn in a containment. Threats are shared by the whole level, so each test runs in a
 * batch of its own and clears them when done; the meltdown also cleans up its fallout. The control
 * room's safe shutdown alarms (margin, cold shutdown, core level, decay heat) are checked here too.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class PlantAlarmTests {
    private static final BlockPos SIREN = new BlockPos(11, 1, 11);

    private static boolean sounding(GameTestHelper helper) {
        return helper.getBlockState(SIREN).getValue(AirRaidSirenBlock.SOUNDING);
    }

    private static void assertAttackWarning(GameTestHelper helper) {
        helper.assertTrue(sounding(helper), "the siren sounds with no redstone");
        AirRaidSirenBlockEntity siren = helper.getBlockEntity(SIREN);
        helper.assertTrue(siren != null && siren.getCause() == AirRaidSirenBlockEntity.Cause.THREAT,
                "sounding the attack warning for the accident");
        ServerLevel level = helper.getLevel();
        helper.assertTrue(ThreatRegistry.threatening(level, helper.absolutePos(SIREN), CivilDefenseConfig.sirenThreatRadius) != null,
                "a hazard is raised in its reach");
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = "contamination_plant_alarm", timeoutTicks = 700)
    public static void aMeltdownSoundsTheSirens(GameTestHelper helper) {
        ThreatRegistry.clear(helper.getLevel());
        int events = ContainmentTestBuilder.falloutEvents(helper);
        FissionTestBuilder.build(helper, false);
        helper.setBlock(SIREN, CivilDefenseRegistry.AIR_RAID_SIREN.get());
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        boolean[] early = new boolean[1];
        helper.runAfterDelay(5, () -> {
            reactor.getPhysicsSimulator().loadFuelAssembly(20f);
            reactor.getPhysicsSimulator().loadFuelAssembly(20f);
            reactor.getPhysicsSimulator().applyExternalHeat(4_200);
            PwrModeTests.heat(reactor, 60.0);
        });
        helper.onEachTick(() -> {
            if (reactor.isFormed() && sounding(helper)) {
                early[0] = true;
            }
        });
        helper.succeedWhen(() -> {
            helper.assertFalse(early[0], "the siren stayed silent while the reactor stood");
            helper.assertFalse(reactor.isFormed(), "the core melted");
            assertAttackWarning(helper);
            ThreatRegistry.clear(helper.getLevel());
            ContainmentTestBuilder.clearArena(helper);
            ContainmentTestBuilder.cleanFallout(helper, events, 3);
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = "civildefense_plant_alarm", timeoutTicks = 300)
    public static void aHydrogenBurnSoundsTheSirens(GameTestHelper helper) {
        ThreatRegistry.clear(helper.getLevel());
        ContainmentTestBuilder.concreteRoom(helper);
        helper.setBlock(SIREN, CivilDefenseRegistry.AIR_RAID_SIREN.get());
        boolean[] injected = new boolean[2];
        helper.onEachTick(() -> {
            ContainmentMonitorBlockEntity monitor = ContainmentTestBuilder.monitor(helper, ContainmentTestBuilder.MONITOR);
            if (!injected[0] && monitor != null && monitor.sealed()) {
                injected[0] = true;
                injected[1] = sounding(helper);
                // Past 8% hydrogen always ignites.
                monitor.addHydrogen(ContainmentTestBuilder.airMoles(monitor) * 0.09, ContainmentModel.WALL_TEMPERATURE);
            }
        });
        helper.succeedWhen(() -> {
            ContainmentMonitorBlockEntity monitor = ContainmentTestBuilder.monitor(helper, ContainmentTestBuilder.MONITOR);
            helper.assertTrue(injected[0] && !injected[1], "the siren was silent until the hydrogen went in");
            helper.assertTrue(monitor != null && monitor.getModel().hydrogenFraction() < 0.01, "the hydrogen burned");
            assertAttackWarning(helper);
            ThreatRegistry.clear(helper.getLevel());
        });
    }

    // ------------------------------------------------------------------ safe shutdown alarms

    /** A formed reactor's sample with the given shutdown readings: 1 MW of decay heat, some fuel. */
    private static MonitorReading shutdownSample(ShutdownState state, float coldMargin, float coverage, float heatRemoved) {
        float[] v = new float[46];
        v[MonitorReading.R_FORMED] = 1;
        v[MonitorReading.R_FUEL] = 0.5f;
        v[MonitorReading.R_DECAY_HEAT] = 1.0f;
        v[MonitorReading.R_SHUTDOWN_STATE] = state.ordinal();
        v[MonitorReading.R_COLD_MARGIN] = coldMargin;
        v[MonitorReading.R_CORE_COVERAGE] = coverage;
        v[MonitorReading.R_HEAT_REMOVED] = heatRemoved;
        return new MonitorReading(TargetKind.REACTOR, "Idle", v, 0, 0);
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void shutdownAlarmsReadTheirSample(GameTestHelper helper) {
        MonitorReading hot = shutdownSample(ShutdownState.HOT_SHUTDOWN, 600, 1.0f, 0.2f);
        helper.assertTrue(AlarmMetric.SHUTDOWN_MARGIN_LOW.tripped(hot, 20), "600 pcm is under the 1000 pcm setting");
        helper.assertTrue(Math.abs(AlarmMetric.SHUTDOWN_MARGIN_LOW.scaled(20) - 1000) < 1e-3, "50 pcm a step");
        helper.assertFalse(AlarmMetric.SHUTDOWN_MARGIN_LOW.tripped(shutdownSample(ShutdownState.HOT_SHUTDOWN, 2500, 1, 1), 20),
                "2500 pcm is plenty");
        helper.assertTrue(AlarmMetric.DECAY_HEAT_UNREMOVED.tripped(hot, 90), "a fifth of the decay heat carried away");
        helper.assertFalse(AlarmMetric.DECAY_HEAT_UNREMOVED.tripped(shutdownSample(ShutdownState.HOT_SHUTDOWN, 2500, 1, 1.0f), 90),
                "all of it carried away");
        helper.assertFalse(AlarmMetric.DECAY_HEAT_UNREMOVED.tripped(shutdownSample(ShutdownState.OPERATING, 2500, 1, 0), 90),
                "a running core is not watched");
        helper.assertFalse(AlarmMetric.CORE_LEVEL_LOW.tripped(hot, 99), "a covered core");
        helper.assertTrue(AlarmMetric.CORE_LEVEL_LOW.tripped(shutdownSample(ShutdownState.HOT_SHUTDOWN, 2500, 0.6f, 1), 99),
                "a core 60% under water");
        helper.assertFalse(AlarmMetric.COLD_SHUTDOWN.tripped(hot, 0), "hot is not cold");
        helper.assertTrue(AlarmMetric.COLD_SHUTDOWN.tripped(shutdownSample(ShutdownState.COLD_SHUTDOWN, 2500, 1, 1), 0),
                "the cold shutdown lamp lights");
        helper.assertTrue(AlarmMetric.COLD_SHUTDOWN.isFlag() && !AlarmMetric.SHUTDOWN_MARGIN_LOW.isFlag(), "only the state is a flag");
        float[] building = new float[46];
        building[MonitorReading.R_BUILDING_H2] = 0.05f;
        helper.assertTrue(AlarmMetric.HYDROGEN.tripped(new MonitorReading(TargetKind.REACTOR, "Idle", building, 0, 0), 40),
                "the hydrogen alarm also reads the building around a reactor with no containment");
        helper.succeed();
    }

    /** A core held down by boric acid and every rod reads, through a real sample, as a cold shutdown. */
    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 200)
    public static void aColdShutdownLightsItsAlarm(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(reactor.isFormed(), "reactor should form");
            reactor.fillCoolant(new FluidStack(Fluids.WATER, 14_700), IFluidHandler.FluidAction.EXECUTE);
            reactor.fillCoolant(new FluidStack(AllNuclearFluids.BORIC_ACID.get(), 1_300), IFluidHandler.FluidAction.EXECUTE);
            for (int i = 0; i < 6; i++) {
                reactor.getPhysicsSimulator().loadFuelAssembly(20f);
            }
        });
        helper.runAfterDelay(60, () -> {
            MonitorReading sample = MonitorReading.sample(new LinkedTarget.Reactor(reactor));
            helper.assertTrue(sample.value(MonitorReading.R_SHUTDOWN_STATE) == ShutdownState.COLD_SHUTDOWN.ordinal(),
                    "sampled in a cold shutdown, " + reactor.getShutdownState());
            helper.assertTrue(AlarmMetric.COLD_SHUTDOWN.tripped(sample, 0), "the cold shutdown window lights");
            helper.assertFalse(AlarmMetric.SHUTDOWN_MARGIN_LOW.tripped(sample, 20), "with margin: " + sample.value(MonitorReading.R_COLD_MARGIN));
            helper.assertFalse(AlarmMetric.CORE_LEVEL_LOW.tripped(sample, 99), "and the core covered");
            helper.assertTrue(Math.abs(sample.value(MonitorReading.R_BORON_PPM) - reactor.getBoronPpm()) < 1, "boron sampled");
            helper.assertTrue(sample.value(MonitorReading.R_REACTIVITY_BORON) < -2000, "with its share of the reactivity");
            helper.assertTrue(sample.value(MonitorReading.R_FUEL_RODS) == reactor.getFuelRodCount()
                    && sample.value(MonitorReading.R_RODS_EFFECTIVE) >= reactor.getFuelRodCount() / 2f, "rods and fuel counted");
            helper.assertTrue(sample.value(MonitorReading.R_BUILDING_H2) == 0, "no hydrogen anywhere");
            helper.succeed();
        });
    }
}
