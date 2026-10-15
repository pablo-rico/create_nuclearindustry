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
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.coolingtower.blockentity.CoolingTowerControllerBlockEntity;
import org.papiricoh.create_nuclearindustry.fluids.NuclearFluidHelper;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.physics.FissionConfig;
import org.papiricoh.create_nuclearindustry.reactor.physics.ReactorPhysicsSimulator;
import org.papiricoh.create_nuclearindustry.reactor.physics.kinetics.ShutdownState;
import org.papiricoh.create_nuclearindustry.reactor.pwr.PlantReadings;

/**
 * Bringing a fission reactor to a safe, cold shutdown: the rods and boric acid hold it subcritical,
 * and cold water fed through the input port while the output port bleeds the hot water takes away
 * the decay heat that boiling alone never takes below saturation. The bled water goes to a cooling
 * tower, never straight back in; a damaged core bleeds contaminated water.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class SafeShutdownTests {
    /** Decay heat of a core scrammed after running a while, in °C per tick of fuel: 2.5 MW a rod. */
    private static final double DECAY_HEAT = 0.25;
    /** What each pump moves a tick, mB. */
    private static final int FEED = 50;

    private static IFluidHandler input(GameTestHelper helper) {
        return PwrModeTests.port(helper, FissionTestBuilder.COOLANT_PORT, Direction.WEST);
    }

    private static IFluidHandler output(GameTestHelper helper) {
        return PwrModeTests.port(helper, FissionTestBuilder.STEAM_PORT, Direction.EAST);
    }

    /** Six fresh fuel assemblies, a full tank, every rod in, and the heat of a core just scrammed. */
    private static void hotScrammedCore(ReactorBlockEntity reactor) {
        ReactorPhysicsSimulator physics = reactor.getPhysicsSimulator();
        for (int i = 0; i < 6; i++) {
            physics.loadFuelAssembly(5f);
        }
        reactor.fillCoolant(new FluidStack(Fluids.WATER, reactor.getTankCapacity()), IFluidHandler.FluidAction.EXECUTE);
        physics.core().thermal().restore(400, 285);
        physics.setFissionProducts(0.0, 0.0, DECAY_HEAT);
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 2400)
    public static void feedingAndBleedingThroughThePortsReachesAColdShutdown(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        boolean[] started = {false};
        int[] bled = {0};
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(reactor.isFormed(), "reactor should form");
            hotScrammedCore(reactor);
            started[0] = true;
        });
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(reactor.getShutdownState() == ShutdownState.HOT_SHUTDOWN, "held off with margin, but hot: "
                    + reactor.getShutdownState());
            helper.assertTrue(reactor.getColdShutdownMargin(true) >= FissionConfig.requiredShutdownMargin,
                    "three rod blocks hold six of 5% fuel: " + reactor.getColdShutdownMargin(true));
        });
        helper.onEachTick(() -> {
            if (!started[0]) {
                return;
            }
            IFluidHandler out = output(helper);
            IFluidHandler in = input(helper);
            if (out == null || in == null) {
                return;
            }
            // The steam to its turbine, the bleed to a tower, and as much cold water back in.
            out.drain(new FluidStack(AllNuclearFluids.STEAM.get(), 1000), IFluidHandler.FluidAction.EXECUTE);
            bled[0] += out.drain(new FluidStack(AllNuclearFluids.HOT_WATER.get(), FEED), IFluidHandler.FluidAction.EXECUTE).getAmount();
            in.fill(new FluidStack(Fluids.WATER, FEED), IFluidHandler.FluidAction.EXECUTE);
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(output(helper) != null && input(helper) != null, "the ports have fluid handlers");
            helper.assertTrue(bled[0] > 0, "the output port bleeds hot water");
            helper.assertTrue(reactor.getShutdownState() == ShutdownState.COLD_SHUTDOWN, "fed and bled to a cold shutdown: "
                    + reactor.getShutdownState() + " at " + reactor.getCoreTemperature() + " / " + reactor.getCoolantTemperature() + " °C");
            helper.assertTrue(reactor.getCoreCoverage() >= 1.0, "with the core still under water");
            helper.assertTrue(reactor.getDisplaySnapshot().shutdownState() == ShutdownState.COLD_SHUTDOWN,
                    "the display snapshot shows it");
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 700)
    public static void pumpsOnlyReplacingTheSteamLeaveItHot(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        boolean[] started = {false};
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(reactor.isFormed(), "reactor should form");
            hotScrammedCore(reactor);
            started[0] = true;
        });
        helper.onEachTick(() -> {
            IFluidHandler out = output(helper);
            IFluidHandler in = input(helper);
            if (!started[0] || out == null || in == null) {
                return;
            }
            out.drain(new FluidStack(AllNuclearFluids.STEAM.get(), 1000), IFluidHandler.FluidAction.EXECUTE);
            in.fill(new FluidStack(Fluids.WATER, FEED), IFluidHandler.FluidAction.EXECUTE);
        });
        helper.runAfterDelay(600, () -> {
            helper.assertTrue(reactor.getCoolantTemperature() > 100, "the decay heat boils its water at saturation: "
                    + reactor.getCoolantTemperature());
            helper.assertTrue(reactor.getCoreTemperature() > 100, "and the fuel stays hot: " + reactor.getCoreTemperature());
            helper.assertTrue(reactor.getShutdownState() == ShutdownState.HOT_SHUTDOWN, "a hot shutdown, " + reactor.getShutdownState());
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 200)
    public static void boricAcidHoldsARichCoreWithFewRods(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        int[] filled = new int[2];
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(reactor.isFormed(), "reactor should form");
            IFluidHandler in = input(helper);
            helper.assertTrue(in != null, "the coolant port has a fluid handler");
            filled[0] = in.fill(new FluidStack(Fluids.WATER, 14_700), IFluidHandler.FluidAction.EXECUTE);
            filled[1] = in.fill(new FluidStack(AllNuclearFluids.BORIC_ACID.get(), 1_300), IFluidHandler.FluidAction.EXECUTE);
            // 20% fuel, one rod block for every two of fuel: the rods alone hold 1500 pcm too little.
            for (int i = 0; i < 6; i++) {
                reactor.getPhysicsSimulator().loadFuelAssembly(20f);
            }
        });
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(filled[0] == 14_700 && filled[1] == 1_300, "water and boric acid go in: " + filled[0] + ", " + filled[1]);
            helper.assertTrue(NuclearFluidHelper.isWater(reactor.getCoolantFluid()), "boric acid is water with boron in it");
            double ppm = reactor.getBoronPpm();
            helper.assertTrue(Math.abs(ppm - 1_300 * FissionConfig.boronSolutionPpm / 16_000) < 1, "about 570 ppm, got " + ppm);
            double margin = reactor.getColdShutdownMargin(true);
            double boron = FissionConfig.boronWorthPerPpm * ppm;
            helper.assertTrue(margin - boron < FissionConfig.requiredShutdownMargin, "without the boron the rods fall short: " + (margin - boron));
            helper.assertTrue(margin >= FissionConfig.requiredShutdownMargin, "with it the margin is enough: " + margin);
            helper.assertTrue(!reactor.getPhysicsSimulator().isRunning() && reactor.getCoreTemperature() < 100, "it never starts");
            helper.assertTrue(reactor.getShutdownState() == ShutdownState.COLD_SHUTDOWN, "a cold shutdown, " + reactor.getShutdownState());
            helper.assertTrue(reactor.getReactivityParts().boron() < -2000, "the boron's share of the reactivity: "
                    + reactor.getReactivityParts().boron());
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 100)
    public static void bleedingTakesTheBoronWithIt(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(reactor.isFormed(), "reactor should form");
            IFluidHandler in = input(helper);
            IFluidHandler out = output(helper);
            helper.assertTrue(in != null && out != null, "the ports have fluid handlers");
            in.fill(new FluidStack(Fluids.WATER, 14_000), IFluidHandler.FluidAction.EXECUTE);
            in.fill(new FluidStack(AllNuclearFluids.BORIC_ACID.get(), 2_000), IFluidHandler.FluidAction.EXECUTE);
            double mass = reactor.getBoronMass();
            double ppm = reactor.getBoronPpm();
            helper.assertTrue(ppm > 800, "boron dissolved: " + ppm);
            FluidStack shown = out.getFluidInTank(0);
            helper.assertTrue(NuclearFluidHelper.isHotWater(shown), "with no steam the output port offers hot water: " + shown.getFluid());
            FluidStack bled = out.drain(4_000, IFluidHandler.FluidAction.EXECUTE);
            helper.assertTrue(bled.getAmount() == 4_000 && NuclearFluidHelper.isHotWater(bled), "it bleeds 4000 mB of hot water: "
                    + bled.getAmount());
            helper.assertTrue(Math.abs(reactor.getBoronMass() - mass * 0.75) < 1, "a quarter of the water took a quarter of the boron");
            helper.assertTrue(Math.abs(reactor.getBoronPpm() - ppm) < 1e-6, "leaving the concentration as it was");
            in.fill(new FluidStack(Fluids.WATER, 4_000), IFluidHandler.FluidAction.EXECUTE);
            helper.assertTrue(Math.abs(reactor.getBoronPpm() - ppm * 0.75) < 1e-6, "fresh water dilutes it: " + reactor.getBoronPpm());
            FluidStack rest = out.drain(16_000, IFluidHandler.FluidAction.EXECUTE);
            helper.assertTrue(reactor.getCoolantAmount() == 4_000, "the bleed never uncovers the core: " + reactor.getCoolantAmount()
                    + " left after " + rest.getAmount());
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 100)
    public static void aHeavyWaterCoreRefusesBoricAcid(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(reactor.isFormed(), "reactor should form");
            IFluidHandler in = input(helper);
            helper.assertTrue(in != null, "the coolant port has a fluid handler");
            int heavy = in.fill(new FluidStack(AllNuclearFluids.HEAVY_WATER.get(), 8_000), IFluidHandler.FluidAction.EXECUTE);
            helper.assertTrue(heavy == 8_000, "heavy water goes in");
            FluidStack acid = new FluidStack(AllNuclearFluids.BORIC_ACID.get(), 1_000);
            helper.assertTrue(!in.isFluidValid(0, acid), "boric acid would spoil the heavy water");
            helper.assertTrue(in.fill(acid, IFluidHandler.FluidAction.EXECUTE) == 0, "so the port refuses it");
            helper.assertTrue(reactor.getBoronMass() == 0, "and no boron gets in");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 100)
    public static void theInputRefusesHotWater(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(reactor.isFormed(), "reactor should form");
            IFluidHandler in = input(helper);
            helper.assertTrue(in != null, "the coolant port has a fluid handler");
            for (FluidStack hot : new FluidStack[] {new FluidStack(AllNuclearFluids.HOT_WATER.get(), 1_000),
                    new FluidStack(AllNuclearFluids.HOT_HEAVY_WATER.get(), 1_000)}) {
                helper.assertTrue(!in.isFluidValid(0, hot), "bled water is not coolant");
                helper.assertTrue(in.fill(hot, IFluidHandler.FluidAction.EXECUTE) == 0, "it has to cool in a tower first");
            }
            helper.assertTrue(reactor.getCoolantAmount() == 0, "nothing went in");
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 100)
    public static void aCoolingTowerCondensesHotWater(GameTestHelper helper) {
        CoolingTowerTestBuilder tower = CoolingTowerTestBuilder.build(helper, new BlockPos(6, 1, 6), 4, 12);
        CoolingTowerControllerBlockEntity controller = tower.controller(helper);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(controller.getStructure().isPresent(), "tower should form");
            IFluidHandler port = tower.input(helper).getFluidHandler(Direction.DOWN);
            helper.assertTrue(port != null, "the tower's input has a fluid handler");
            int filled = port.fill(new FluidStack(AllNuclearFluids.HOT_WATER.get(), 1_000), IFluidHandler.FluidAction.EXECUTE);
            helper.assertTrue(filled == 1_000, "the tower takes hot water, took " + filled);
        });
        helper.succeedWhen(() -> helper.assertTrue(controller.getWaterTank().getFluid().is(Fluids.WATER)
                && controller.getWaterTank().getFluidAmount() > 0, "hot water cools back into water"));
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 100)
    public static void aCoolingTowerTurnsHotHeavyWaterBackIntoHeavyWater(GameTestHelper helper) {
        CoolingTowerTestBuilder tower = CoolingTowerTestBuilder.build(helper, new BlockPos(6, 1, 6), 4, 12);
        CoolingTowerControllerBlockEntity controller = tower.controller(helper);
        helper.runAfterDelay(5, () -> {
            IFluidHandler port = tower.input(helper).getFluidHandler(Direction.DOWN);
            helper.assertTrue(port != null, "the tower's input has a fluid handler");
            port.fill(new FluidStack(AllNuclearFluids.HOT_HEAVY_WATER.get(), 1_000), IFluidHandler.FluidAction.EXECUTE);
        });
        helper.succeedWhen(() -> helper.assertTrue(controller.getWaterTank().getFluid().is(AllNuclearFluids.HEAVY_WATER.get()),
                "hot heavy water cools back into heavy water"));
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = "contamination_safe_shutdown", timeoutTicks = 100)
    public static void aDamagedCoreBleedsContaminatedWater(GameTestHelper helper) {
        int events = ContainmentTestBuilder.falloutEvents(helper);
        FissionTestBuilder.build(helper);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(reactor.isFormed(), "reactor should form");
            reactor.fillCoolant(new FluidStack(Fluids.WATER, 10_000), IFluidHandler.FluidAction.EXECUTE);
            reactor.getPhysicsSimulator().core().damageModel().restore(FissionConfig.contaminatedDamage + 0.02);
        });
        helper.runAfterDelay(8, () -> {
            IFluidHandler out = output(helper);
            helper.assertTrue(out != null, "the output port has a fluid handler");
            FluidStack bled = out.drain(500, IFluidHandler.FluidAction.EXECUTE);
            helper.assertTrue(NuclearFluidHelper.isContaminatedWater(bled) && bled.getAmount() == 500,
                    "the failed cladding contaminates the bled water: " + bled.getFluid() + " x" + bled.getAmount());
            helper.assertTrue(reactor.isBleeding(), "the reactor knows it is bleeding");
            ContainmentTestBuilder.clearArena(helper);
            ContainmentTestBuilder.cleanFallout(helper, events, 2);
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 600)
    public static void makeupWaterTakesAPressurizedLoopBelowBoiling(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        FissionTestBuilder.pressurizer(helper, FissionTestBuilder.ORIGIN, 4, 0);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        boolean[] started = {false};
        int[] letDown = {0};
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(reactor.hasPressurizer(), "the pressurizer is part of the reactor");
            reactor.fillCoolant(new FluidStack(AllNuclearFluids.PRIMARY_COOLANT.get(), 14_000), IFluidHandler.FluidAction.EXECUTE);
        });
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(reactor.isPrimaryLoopActive(), "the loop runs pressurized");
            helper.assertTrue(reactor.plantReadings().coldLeg() > 250, "at its cold leg temperature: " + reactor.plantReadings().coldLeg());
            IFluidHandler in = input(helper);
            helper.assertTrue(in != null, "the coolant port has a fluid handler");
            int acid = in.fill(new FluidStack(AllNuclearFluids.BORIC_ACID.get(), 500), IFluidHandler.FluidAction.EXECUTE);
            helper.assertTrue(acid == 500 && NuclearFluidHelper.isPrimaryCoolant(reactor.getCoolantFluid()),
                    "boric acid joins the loop as primary coolant");
            helper.assertTrue(reactor.getBoronPpm() > 100, "bringing its boron: " + reactor.getBoronPpm());
            started[0] = true;
        });
        helper.onEachTick(() -> {
            IFluidHandler out = output(helper);
            IFluidHandler in = input(helper);
            if (!started[0] || out == null || in == null) {
                return;
            }
            // Letdown through the output port (as hot water, which a steam generator would not pass back), cold
            // makeup water through the input.
            helper.assertTrue(out.drain(new FluidStack(AllNuclearFluids.PRIMARY_COOLANT.get(), 200), IFluidHandler.FluidAction.SIMULATE)
                    .isEmpty(), "the letdown is not primary coolant");
            letDown[0] += out.drain(new FluidStack(AllNuclearFluids.HOT_WATER.get(), 200), IFluidHandler.FluidAction.EXECUTE)
                    .getAmount();
            in.fill(new FluidStack(Fluids.WATER, 200), IFluidHandler.FluidAction.EXECUTE);
        });
        helper.succeedWhen(() -> {
            PlantReadings plant = reactor.plantReadings();
            helper.assertTrue(letDown[0] > 0, "the output port lets primary coolant down");
            helper.assertTrue(plant.pwr() && plant.coldLeg() < 100, "makeup water takes the cold leg below 100 °C: " + plant.coldLeg());
            helper.assertTrue(reactor.getPrimaryLoop().baseline() < 100, "the vessel's water is cold: " + reactor.getPrimaryLoop().baseline());
            helper.assertTrue(NuclearFluidHelper.isPrimaryCoolant(reactor.getCoolantFluid()), "the water joined the loop as primary coolant");
            helper.assertTrue(reactor.getCoolantAmount() + reactor.getSteamAmount() >= 12_000, "the letdown never uncovers the core");
        });
    }
}
