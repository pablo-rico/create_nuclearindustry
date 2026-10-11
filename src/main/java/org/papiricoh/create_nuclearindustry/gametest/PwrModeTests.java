package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.controlroom.MonitorReading;
import org.papiricoh.create_nuclearindustry.fluids.NuclearFluidHelper;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.display.ReactorReadout;
import org.papiricoh.create_nuclearindustry.reactor.pwr.PlantReadings;
import org.papiricoh.create_nuclearindustry.reactor.pwr.PwrRegistry;

/**
 * A fission reactor with primary coolant and a pressurizer on its lid runs a pressurized loop
 * instead of boiling its water; without the pressurizer it keeps boiling water as before and
 * refuses primary coolant.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class PwrModeTests {
    private static final BlockPos ORIGIN = FissionTestBuilder.ORIGIN;

    /** Decay heat in °C per tick: enough to make heat with no fuel loaded. */
    static void heat(ReactorBlockEntity reactor, double decayHeat) {
        reactor.getPhysicsSimulator().setFissionProducts(0.0, 0.0, decayHeat);
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 200)
    public static void primaryCoolantAndAPressurizerRunPressurized(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        FissionTestBuilder.pressurizer(helper, ORIGIN, 4, 1);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        int[] filled = new int[1];
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(reactor.hasPressurizer(), "the pressurizer on the lid is part of the reactor");
            IFluidHandler coldLeg = port(helper, FissionTestBuilder.COOLANT_PORT, Direction.WEST);
            helper.assertTrue(coldLeg != null, "the coolant port has a fluid handler");
            filled[0] = coldLeg == null ? 0 : coldLeg.fill(new FluidStack(AllNuclearFluids.PRIMARY_COOLANT.get(), 14_000),
                    IFluidHandler.FluidAction.EXECUTE);
            heat(reactor, 20.0);
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(filled[0] == 14_000, "the cold leg takes primary coolant, took " + filled[0]);
            helper.assertTrue(reactor.isPrimaryLoopActive(), "the loop runs pressurized");
            PlantReadings plant = reactor.plantReadings();
            helper.assertTrue(plant.pwr(), "plant readings report a pressurized loop");
            helper.assertTrue(plant.pressure() > 100, "the loop is under pressure, got " + plant.pressure());
            helper.assertTrue(NuclearFluidHelper.isHotPrimary(reactor.getSteamFluid()) && reactor.getSteamAmount() > 0,
                    "the core turns cold coolant into hot primary, not steam");
            IFluidHandler hotLeg = port(helper, FissionTestBuilder.STEAM_PORT, Direction.EAST);
            helper.assertTrue(hotLeg != null && NuclearFluidHelper.isHotPrimary(hotLeg.drain(10, IFluidHandler.FluidAction.SIMULATE)),
                    "the output port offers hot primary coolant");
            helper.assertTrue(ReactorReadout.PRESSURE.read(reactor).getString().endsWith(" bar"), "the pressure readout shows bar");
            MonitorReading reading = MonitorReading.sample(new org.papiricoh.create_nuclearindustry.controlroom.LinkedTarget.Reactor(reactor));
            helper.assertTrue(reading.flag(MonitorReading.R_PWR) && reading.value(MonitorReading.R_PRESSURE) > 100,
                    "monitors get the pressurized loop readings");
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void withoutAPressurizerPrimaryCoolantIsRefused(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(reactor.isFormed(), "the reactor forms without a pressurizer");
            helper.assertTrue(!reactor.hasPressurizer(), "it has none");
            IFluidHandler coldLeg = port(helper, FissionTestBuilder.COOLANT_PORT, Direction.WEST);
            helper.assertTrue(coldLeg != null, "the coolant port has a fluid handler");
            int primary = coldLeg.fill(new FluidStack(AllNuclearFluids.PRIMARY_COOLANT.get(), 1_000), IFluidHandler.FluidAction.EXECUTE);
            helper.assertTrue(primary == 0, "primary coolant needs a pressurizer, took " + primary);
            int water = coldLeg.fill(new FluidStack(Fluids.WATER, 1_000), IFluidHandler.FluidAction.EXECUTE);
            helper.assertTrue(water == 1_000, "water still goes in");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void aPressurizerNeedsItsReliefValve(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        BlockPos valve = FissionTestBuilder.pressurizer(helper, ORIGIN, 3, 1);
        helper.setBlock(valve, net.minecraft.world.level.block.Blocks.AIR);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(reactor.isFormed(), "the reactor still forms");
            helper.assertTrue(!reactor.hasPressurizer(), "a stack without a relief valve is not a pressurizer");
            helper.setBlock(valve, PwrRegistry.RELIEF_VALVE.get());
        });
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(reactor.hasPressurizer(), "capping it with the valve completes it");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 400)
    public static void waterStillBoilsDirectlyBesideAPressurizer(GameTestHelper helper) {
        FissionTestBuilder.build(helper, false);
        FissionTestBuilder.pressurizer(helper, ORIGIN, 2, 0);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        helper.runAfterDelay(5, () -> {
            reactor.fillCoolant(new FluidStack(Fluids.WATER, 8_000), IFluidHandler.FluidAction.EXECUTE);
            // A running core with its rods out: only fission makes the boiling reactor's steam. Two thirds
            // loaded, so its hot water and fuel hold it at a modest power instead of a runaway.
            for (int i = 0; i < 4; i++) {
                reactor.getPhysicsSimulator().loadFuelAssembly(5f);
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(NuclearFluidHelper.isSteam(reactor.getSteamFluid()) && reactor.getSteamAmount() > 0,
                    "water boils into steam as before");
            helper.assertTrue(reactor.plantReadings() == PlantReadings.NONE, "no pressurized loop readings");
            helper.assertTrue(reactor.getPhysicsSimulator().getCoolantFeedback() == 1.0, "and no coolant feedback");
            helper.assertTrue(ReactorReadout.PRESSURE.read(reactor).getString().equals("-"), "the pressure readout shows a dash");
            // nothing drains its steam and its rods are out: take it apart before it melts down beside other tests
            ContainmentTestBuilder.clearArena(helper);
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 300)
    public static void boilingInTheCoreHoldsTheChainReactionBack(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        BlockPos valve = FissionTestBuilder.pressurizer(helper, ORIGIN, 4, 0);
        // An isolated relief valve keeps the steam in.
        helper.setBlock(valve.east(), net.minecraft.world.level.block.Blocks.REDSTONE_BLOCK);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        helper.runAfterDelay(5, () -> {
            reactor.fillCoolant(new FluidStack(AllNuclearFluids.PRIMARY_COOLANT.get(), 14_000), IFluidHandler.FluidAction.EXECUTE);
            heat(reactor, 20.0);
        });
        helper.succeedWhen(() -> {
            double voids = reactor.getPrimaryLoop().coolant().voidFraction();
            helper.assertTrue(voids > 0.05, "with no pumps the core boils, void " + voids);
            helper.assertTrue(reactor.getPhysicsSimulator().getCoolantFeedback() < 0.97,
                    "steam voids cut the chain reaction, feedback " + reactor.getPhysicsSimulator().getCoolantFeedback());
        });
    }

    static IFluidHandler port(GameTestHelper helper, BlockPos port, Direction face) {
        return helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(port), face);
    }
}
