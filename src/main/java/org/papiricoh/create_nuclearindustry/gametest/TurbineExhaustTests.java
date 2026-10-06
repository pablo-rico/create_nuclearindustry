package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.coolingtower.blockentity.CoolingTowerControllerBlockEntity;
import org.papiricoh.create_nuclearindustry.turbine.AbstractTurbineControllerBlockEntity;
import org.papiricoh.create_nuclearindustry.turbine.TurbineExhaustPortBlock;
import org.papiricoh.create_nuclearindustry.turbine.TurbinePortBlockEntity;
import org.papiricoh.create_nuclearindustry.turbine.TurbineStructure;
import org.papiricoh.create_nuclearindustry.turbine.TurbineTier;

/** A turbine exhausting to a cooling tower does twice the work with the same steam. */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class TurbineExhaustTests {
    private static final BlockPos CONTROLLER = new BlockPos(6, 6, 1);

    /** A 3x3x6 steam turbine whose output port is swapped for an exhaust port when {@code exhaust}. */
    private static TurbineStructure turbine(GameTestHelper helper, boolean exhaust) {
        TurbineStructure s = TurbineTestBuilder.build(helper, TurbineTier.STEAM, CONTROLLER, 3, 3, 6);
        if (exhaust) {
            helper.setBlock(s.at(1, -s.hu, 0), AllNuclearBlocks.TURBINE_EXHAUST_PORT.get().defaultBlockState()
                    .setValue(TurbineExhaustPortBlock.FACING, Direction.DOWN));
        }
        return s;
    }

    /** Keeps the turbine supplied, and its outlet emptied, while the smoothed capacity settles. */
    private static void keepRunning(GameTestHelper helper, TurbineStructure s, AbstractTurbineControllerBlockEntity turbine) {
        for (int t = 5; t < 195; t += 5) {
            helper.runAfterDelay(t, () -> {
                TurbinePortBlockEntity input = helper.getBlockEntity(s.at(1, s.hu, 0));
                input.getFluidHandler(Direction.UP).fill(new FluidStack(AllNuclearFluids.STEAM.get(), 4_000), IFluidHandler.FluidAction.EXECUTE);
                turbine.getCondensateTank().drain(4_000, IFluidHandler.FluidAction.EXECUTE);
            });
        }
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 200)
    public static void exhaustPortDoublesThePower(GameTestHelper helper) {
        TurbineStructure s = turbine(helper, true);
        AbstractTurbineControllerBlockEntity turbine = helper.getBlockEntity(CONTROLLER);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(turbine.getStructure().map(TurbineStructure::exhausting).orElse(false),
                    "a turbine with only an exhaust port forms and exhausts: " + turbine.getDisplay().message());
        });
        keepRunning(helper, s, turbine);
        helper.succeedWhen(() -> {
            helper.assertTrue(turbine.getCurrentUse() == 320, "it still expands 320 mB/t, got " + turbine.getCurrentUse());
            float expected = TurbineTier.STEAM.capacityFor(320) * AbstractTurbineControllerBlockEntity.EXHAUST_POWER_MULTIPLIER;
            helper.assertTrue(Math.abs(turbine.calculateAddedStressCapacity() - expected) < 1f,
                    "twice the capacity of a condensing turbine, " + turbine.calculateAddedStressCapacity() + " vs " + expected);
            helper.assertTrue(turbine.getCondensateTank().isEmpty() || turbine.getCondensateTank().getFluid().is(AllNuclearFluids.EXHAUST_STEAM.get()),
                    "its outlet holds exhaust steam, not water");
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 200)
    public static void condensingTurbineKeepsNormalPower(GameTestHelper helper) {
        TurbineStructure s = turbine(helper, false);
        AbstractTurbineControllerBlockEntity turbine = helper.getBlockEntity(CONTROLLER);
        keepRunning(helper, s, turbine);
        helper.succeedWhen(() -> {
            helper.assertTrue(turbine.getCurrentUse() == 320, "expands 320 mB/t, got " + turbine.getCurrentUse()
                    + " formed=" + turbine.getStructure().isPresent() + " " + turbine.getDisplay().message()
                    + " steam=" + turbine.getSteamTank().getFluidAmount() + " out=" + turbine.getCondensateTank().getFluidAmount());
            helper.assertTrue(Math.abs(turbine.calculateAddedStressCapacity() - TurbineTier.STEAM.capacityFor(320)) < 1f,
                    "a condensing turbine keeps the base capacity");
            helper.assertTrue(turbine.getCondensateTank().isEmpty() || turbine.getCondensateTank().getFluid().is(Fluids.WATER), "and gives water");
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void turbinesRefuseExhaustSteam(GameTestHelper helper) {
        TurbineStructure s = turbine(helper, false);
        AbstractTurbineControllerBlockEntity turbine = helper.getBlockEntity(CONTROLLER);
        helper.runAfterDelay(5, () -> {
            int filled = turbine.fillSteam(new FluidStack(AllNuclearFluids.EXHAUST_STEAM.get(), 1000), IFluidHandler.FluidAction.EXECUTE);
            helper.assertTrue(filled == 0, "exhaust steam cannot drive another turbine");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 100)
    public static void coolingTowerCondensesExhaust(GameTestHelper helper) {
        CoolingTowerTestBuilder tower = CoolingTowerTestBuilder.build(helper, new BlockPos(6, 1, 6), 4, 12);
        CoolingTowerControllerBlockEntity controller = tower.controller(helper);
        helper.runAfterDelay(5, () -> tower.input(helper).getFluidHandler(Direction.DOWN)
                .fill(new FluidStack(AllNuclearFluids.HEAVY_EXHAUST_STEAM.get(), 1000), IFluidHandler.FluidAction.EXECUTE));
        helper.succeedWhen(() -> helper.assertTrue(controller.getWaterTank().getFluid().is(AllNuclearFluids.HEAVY_WATER.get()),
                "heavy exhaust condenses to heavy water"));
    }
}
