package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.reactor.pwr.PwrRegistry;
import org.papiricoh.create_nuclearindustry.reactor.pwr.containment.ContainmentModel;
import org.papiricoh.create_nuclearindustry.reactor.pwr.containment.ContainmentMonitorBlockEntity;

/**
 * Hydrogen in a sealed containment: above 8% it burns for sure, spiking the pressure; passive
 * recombiners take it out before it gets there; without them it just stays.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class HydrogenTests {

    /** Once the room is sealed, lets in hydrogen worth {@code share} of its air; returns the peak pressure seen so far. */
    private static double[] injectWhenSealed(GameTestHelper helper, double share) {
        double[] state = new double[2]; // [0] injected flag, [1] peak pressure
        helper.onEachTick(() -> {
            ContainmentMonitorBlockEntity monitor = ContainmentTestBuilder.monitor(helper, ContainmentTestBuilder.MONITOR);
            if (monitor == null || !monitor.sealed()) {
                return;
            }
            if (state[0] == 0) {
                state[0] = 1;
                monitor.addHydrogen(ContainmentTestBuilder.airMoles(monitor) * share, ContainmentModel.WALL_TEMPERATURE);
            }
            state[1] = Math.max(state[1], monitor.getPressure());
        });
        return state;
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 200)
    public static void hydrogenAboveEightPercentIgnites(GameTestHelper helper) {
        ContainmentTestBuilder.concreteRoom(helper);
        double[] state = injectWhenSealed(helper, 0.09);
        helper.succeedWhen(() -> {
            ContainmentMonitorBlockEntity monitor = ContainmentTestBuilder.monitor(helper, ContainmentTestBuilder.MONITOR);
            helper.assertTrue(state[0] == 1 && monitor != null, "hydrogen went in");
            helper.assertTrue(monitor.getHydrogen() < 0.005, "it burnt, " + monitor.getHydrogen() + " left");
            helper.assertTrue(state[1] > 3.0, "the burn spiked the pressure, peak " + state[1]);
            helper.assertTrue(monitor.sealed(), "concrete holds a burn of that size");
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 400)
    public static void recombinersRemoveHydrogen(GameTestHelper helper) {
        ContainmentTestBuilder.concreteRoom(helper);
        helper.setBlock(new BlockPos(3, 2, 3), PwrRegistry.RECOMBINER.get());
        helper.setBlock(new BlockPos(7, 2, 7), PwrRegistry.RECOMBINER.get());
        double[] state = injectWhenSealed(helper, 0.035);
        helper.succeedWhen(() -> {
            ContainmentMonitorBlockEntity monitor = ContainmentTestBuilder.monitor(helper, ContainmentTestBuilder.MONITOR);
            helper.assertTrue(state[0] == 1 && monitor != null, "hydrogen went in");
            helper.assertTrue(monitor.getShape().recombiners().size() == 2, "both recombiners count");
            helper.assertTrue(monitor.getHydrogen() < 0.01, "they take it below 1%, " + monitor.getHydrogen());
            helper.assertTrue(state[1] < 1.3, "quietly, without a burn: peak " + state[1]);
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 200)
    public static void withoutRecombinersItStays(GameTestHelper helper) {
        ContainmentTestBuilder.concreteRoom(helper);
        double[] state = injectWhenSealed(helper, 0.035);
        helper.runAfterDelay(120, () -> {
            ContainmentMonitorBlockEntity monitor = ContainmentTestBuilder.monitor(helper, ContainmentTestBuilder.MONITOR);
            helper.assertTrue(state[0] == 1 && monitor != null, "hydrogen went in");
            helper.assertTrue(monitor.getHydrogen() > 0.03, "below the flammability limit and with nothing to remove it, it stays: "
                    + monitor.getHydrogen());
            helper.succeed();
        });
    }
}
