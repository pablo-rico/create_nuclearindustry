package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.fluids.NuclearFluidHelper;
import org.papiricoh.create_nuclearindustry.fusion.cryo.CryoConfig;
import org.papiricoh.create_nuclearindustry.fusion.cryo.CryoRegistry;
import org.papiricoh.create_nuclearindustry.fusion.cryo.well.HeliumWellBlock;
import org.papiricoh.create_nuclearindustry.fusion.cryo.well.HeliumWellBlockEntity;

/** The helium well: it trickles helium gas into a tank beside it, does nothing when too slow and stops when full. */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class HeliumWellTests {
    /** The well, gauge north, so its shaft comes in from the south. */
    private static final BlockPos WELL = new BlockPos(4, 1, 4);
    private static final BlockPos MOTOR = WELL.south();
    private static final BlockPos TANK = WELL.east();

    private static HeliumWellBlockEntity well(GameTestHelper helper, int rpm) {
        helper.setBlock(WELL, CryoRegistry.HELIUM_WELL.get().defaultBlockState().setValue(HeliumWellBlock.FACING, Direction.NORTH));
        CryoplantTestBuilder.motor(helper, MOTOR, Direction.NORTH, rpm);
        return helper.getBlockEntity(WELL);
    }

    private static void tank(GameTestHelper helper) {
        helper.setBlock(TANK, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", "fluid_tank")));
    }

    @Nullable
    private static IFluidHandler handler(GameTestHelper helper, BlockPos pos) {
        return helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(pos), Direction.UP);
    }

    private static int tankAmount(GameTestHelper helper) {
        IFluidHandler handler = handler(helper, TANK);
        return handler == null ? 0 : handler.getFluidInTank(0).getAmount();
    }

    /** All the helium the well has drawn so far: in the tank beside it, in its own tank and still owed. */
    private static double drawn(GameTestHelper helper, HeliumWellBlockEntity well) {
        return tankAmount(helper) + well.tank().getFluidAmount() + well.balance();
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 400)
    public static void aHeliumWellTricklesHeliumIntoANeighbouringTank(GameTestHelper helper) {
        HeliumWellBlockEntity well = well(helper, 128);
        tank(helper);
        double[] start = new double[1];
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(well != null, "the helium well has its block entity");
            start[0] = drawn(helper, well);
        });
        helper.runAfterDelay(320, () -> {
            helper.assertTrue(well != null, "the helium well has its block entity");
            helper.assertTrue(Math.abs(Math.abs(well.getSpeed()) - 128) < 0.01, "the motor turns the well at 128 RPM, got " + well.getSpeed());
            helper.assertTrue(!well.isOverStressed(), "a creative motor carries the well");
            IFluidHandler outside = handler(helper, TANK);
            helper.assertTrue(outside != null, "the Create tank has a fluid handler");
            FluidStack inTank = outside.getFluidInTank(0);
            helper.assertTrue(NuclearFluidHelper.isHelium(inTank) && inTank.getAmount() > 0,
                    "the well pushes helium gas into the tank beside it, got " + inTank.getAmount() + " of " + inTank.getFluid());
            double made = drawn(helper, well) - start[0];
            double expected = CryoConfig.heliumWellRate * 300;
            helper.assertTrue(Math.abs(made - expected) <= 1.0, "300 ticks at rated speed draw " + expected + " mB, drew " + made);
            helper.assertTrue(well.tank().getFluidAmount() <= 1, "the well keeps almost nothing while a tank takes it, has "
                    + well.tank().getFluidAmount());
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 300)
    public static void aSlowHeliumWellMakesNothing(GameTestHelper helper) {
        HeliumWellBlockEntity well = well(helper, 16);
        tank(helper);
        helper.runAfterDelay(200, () -> {
            helper.assertTrue(well != null, "the helium well has its block entity");
            helper.assertTrue(Math.abs(Math.abs(well.getSpeed()) - 16) < 0.01, "the motor turns the well at 16 RPM, got " + well.getSpeed());
            helper.assertTrue(well.speedFactor() == 0, "below 32 RPM the well is too slow");
            helper.assertTrue(well.balance() == 0 && well.tank().isEmpty(), "and it draws nothing: balance " + well.balance()
                    + ", tank " + well.tank().getFluidAmount());
            helper.assertTrue(tankAmount(helper) == 0, "the tank beside it stays empty, has " + tankAmount(helper));
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 400)
    public static void theWellStopsWhenItsTankIsFullAndNothingTakesIt(GameTestHelper helper) {
        HeliumWellBlockEntity well = well(helper, 128);
        double[] balance = new double[1];
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(well != null, "the helium well has its block entity");
            well.tank().fill(new FluidStack(AllNuclearFluids.HELIUM.get(), HeliumWellBlockEntity.CAPACITY - 1),
                    IFluidHandler.FluidAction.EXECUTE);
        });
        helper.runAfterDelay(200, () -> {
            helper.assertTrue(well != null, "the helium well has its block entity");
            helper.assertTrue(well.speedFactor() > 0.99, "the well runs at rated speed, got " + well.speedFactor());
            helper.assertTrue(well.tank().getFluidAmount() == HeliumWellBlockEntity.CAPACITY,
                    "the last mB tops the tank up, has " + well.tank().getFluidAmount());
            balance[0] = well.balance();
            helper.assertTrue(balance[0] >= 0 && balance[0] <= 1.0, "the balance stays within one mB, got " + balance[0]);
        });
        helper.runAfterDelay(350, () -> {
            helper.assertTrue(well != null, "the helium well has its block entity");
            helper.assertTrue(well.tank().getFluidAmount() == HeliumWellBlockEntity.CAPACITY, "the tank stays full");
            helper.assertTrue(well.balance() == balance[0], "a full well draws nothing more: balance went from "
                    + balance[0] + " to " + well.balance());
            IFluidHandler face = handler(helper, WELL);
            helper.assertTrue(face != null, "the well has a fluid handler on its other sides");
            helper.assertTrue(face.fill(new FluidStack(AllNuclearFluids.HELIUM.get(), 100), IFluidHandler.FluidAction.SIMULATE) == 0,
                    "nothing can be piped into a well");
            helper.assertTrue(face.drain(250, IFluidHandler.FluidAction.SIMULATE).getAmount() == 250, "but its helium can be drained");
            helper.succeed();
        });
    }
}
