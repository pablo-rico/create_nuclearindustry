package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.radiation.contamination.ContaminationData;
import org.papiricoh.create_nuclearindustry.reactor.pwr.PwrRegistry;
import org.papiricoh.create_nuclearindustry.reactor.pwr.containment.ContainmentMonitorBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.pwr.containment.FilteredVentBlock;

import java.util.List;

/**
 * The filtered containment vent: opened with redstone it bleeds the building's overpressure out
 * and lays only a trace of the airborne activity as fallout, close by; shut, it holds.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class VentingTests {
    private static final BlockPos VENT = new BlockPos(8, 3, 5);
    private static final double STEAM_K = 400.0;
    private static final double ACTIVITY = 50.0;

    /** Once sealed, fills the room with steam and activity; [0] done flag, [1] pressure right after. */
    private static double[] pressurizeWhenSealed(GameTestHelper helper) {
        double[] state = new double[2];
        helper.onEachTick(() -> {
            ContainmentMonitorBlockEntity monitor = ContainmentTestBuilder.monitor(helper, ContainmentTestBuilder.MONITOR);
            if (state[0] == 0 && monitor != null && monitor.sealed()) {
                state[0] = 1;
                monitor.addSteam(ContainmentTestBuilder.airMoles(monitor) * 1.2, STEAM_K);
                monitor.addActivity(ACTIVITY);
                state[1] = monitor.getPressure();
            }
        });
        return state;
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = "contamination_venting", timeoutTicks = 400)
    public static void anOpenVentBleedsPressureThroughItsFilters(GameTestHelper helper) {
        int events = ContainmentTestBuilder.falloutEvents(helper);
        ContainmentTestBuilder.concreteRoom(helper);
        helper.setBlock(VENT, PwrRegistry.FILTERED_VENT.get());
        helper.setBlock(VENT.east(), Blocks.REDSTONE_BLOCK);
        double[] state = pressurizeWhenSealed(helper);
        helper.succeedWhen(() -> {
            ContainmentMonitorBlockEntity monitor = ContainmentTestBuilder.monitor(helper, ContainmentTestBuilder.MONITOR);
            helper.assertTrue(state[0] == 1 && monitor != null, "the room was pressurized");
            helper.assertBlockProperty(VENT, FilteredVentBlock.OPEN, true);
            helper.assertTrue(monitor.getShape().vents().size() == 1 && monitor.getOpenVents() == 1, "the vent is open");
            helper.assertTrue(monitor.getPressure() < state[1] - 0.5, "it bleeds the pressure: " + state[1] + " -> " + monitor.getPressure());
            List<ContaminationData.FalloutEvent> added = ContaminationData.get(helper.getLevel()).events();
            helper.assertTrue(added.size() > events, "what passed the filters fell out");
            ContaminationData.FalloutEvent fallout = added.get(added.size() - 1);
            BlockPos vent = helper.absolutePos(VENT);
            helper.assertTrue(Math.abs(fallout.x() - vent.getX()) <= 1 && Math.abs(fallout.z() - vent.getZ()) <= 1, "at the vent");
            helper.assertTrue(fallout.radius() <= 16 && fallout.intensity() < 0.5f,
                    "only a trace, close by: radius " + fallout.radius() + ", " + fallout.intensity() + " rad/s");
            ContainmentTestBuilder.cleanFallout(helper, events, 1);
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 300)
    public static void aShutVentHolds(GameTestHelper helper) {
        ContainmentTestBuilder.concreteRoom(helper);
        helper.setBlock(VENT, PwrRegistry.FILTERED_VENT.get());
        double[] state = pressurizeWhenSealed(helper);
        helper.runAfterDelay(120, () -> {
            ContainmentMonitorBlockEntity monitor = ContainmentTestBuilder.monitor(helper, ContainmentTestBuilder.MONITOR);
            helper.assertTrue(state[0] == 1 && monitor != null, "the room was pressurized");
            helper.assertTrue(monitor.sealed() && monitor.getShape().vents().size() == 1, "a shut vent is part of the wall");
            helper.assertTrue(monitor.getOpenVents() == 0, "and stays shut without redstone");
            helper.assertTrue(monitor.getModel().activity() > ACTIVITY * 0.9, "the activity stays inside, " + monitor.getModel().activity());
            helper.succeed();
        });
    }
}
