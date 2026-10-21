package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.turbine.AbstractTurbineControllerBlockEntity;
import org.papiricoh.create_nuclearindustry.turbine.TurbineStructure;
import org.papiricoh.create_nuclearindustry.turbine.TurbineTier;

/**
 * Multiblock ports must keep their fluid capability stable: Create's pipe networks reset on
 * every invalidation, and a port that invalidated once a second never let water reach a tank.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class PortCapabilityTests {
    private static final BlockPos CONTROLLER = new BlockPos(6, 6, 1);

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 200)
    public static void linkedTurbinePortKeepsItsCapability(GameTestHelper helper) {
        TurbineStructure s = TurbineTestBuilder.build(helper, TurbineTier.STEAM, CONTROLLER, 3, 3, 8);
        BlockPos output = helper.absolutePos(s.at(1, -s.hu, 0));
        int[] invalidations = new int[1];
        BlockCapabilityCache<?, ?>[] cache = new BlockCapabilityCache<?, ?>[1];
        helper.runAfterDelay(10, () -> cache[0] = BlockCapabilityCache.create(Capabilities.FluidHandler.BLOCK, helper.getLevel(),
                output, Direction.DOWN, () -> true, () -> invalidations[0]++));
        // A cache re-arms its listener when queried, as Create's pipes do every tick.
        for (int t = 11; t < 80; t++) {
            helper.runAfterDelay(t, () -> cache[0].getCapability());
        }
        helper.runAfterDelay(80, () -> {
            helper.assertTrue(cache[0] != null && cache[0].getCapability() != null, "the output port exposes a fluid handler");
            helper.assertTrue(invalidations[0] == 0, "a linked port must not invalidate its capability, did " + invalidations[0] + " times");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 200)
    public static void drainingAFullTurbineRestartsIt(GameTestHelper helper) {
        TurbineStructure s = TurbineTestBuilder.build(helper, TurbineTier.STEAM, CONTROLLER, 3, 3, 8);
        AbstractTurbineControllerBlockEntity turbine = helper.getBlockEntity(CONTROLLER);
        BlockPos output = s.at(1, -s.hu, 0);
        helper.runAfterDelay(5, () -> {
            turbine.getSteamTank().fill(new FluidStack(AllNuclearFluids.STEAM.get(), 1_000_000), IFluidHandler.FluidAction.EXECUTE);
            turbine.getCondensateTank().fill(new FluidStack(Fluids.WATER, 1_000_000), IFluidHandler.FluidAction.EXECUTE);
        });
        helper.runAfterDelay(8, () -> helper.assertTrue(turbine.getCurrentUse() == 0, "a full condensate tank stops the turbine"));
        for (int t = 10; t < 30; t++) {
            helper.runAfterDelay(t, () -> {
                IFluidHandler port = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK,
                        helper.absolutePos(output), Direction.DOWN);
                port.drain(500, IFluidHandler.FluidAction.EXECUTE);
            });
        }
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(turbine.getCurrentUse() > 0, "pumping the water out lets the turbine run again");
            helper.succeed();
        });
    }
}
