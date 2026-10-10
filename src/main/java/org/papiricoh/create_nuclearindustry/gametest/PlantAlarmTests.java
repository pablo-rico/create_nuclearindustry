package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
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
 * batch of its own and clears them when done; the meltdown also cleans up its fallout.
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
}
