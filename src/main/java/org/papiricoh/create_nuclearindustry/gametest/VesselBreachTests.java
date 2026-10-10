package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.pwr.PwrConfig;
import org.papiricoh.create_nuclearindustry.reactor.pwr.PwrRegistry;
import org.papiricoh.create_nuclearindustry.reactor.pwr.containment.ContainmentMonitorBlockEntity;

/**
 * The vessel of a pressurized reactor held far above its failure pressure gives way and dumps its
 * water into the containment as steam; water pumped onto a molten core flashes into a steam
 * explosion.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class VesselBreachTests {
    private static final BlockPos ORIGIN = new BlockPos(10, 1, 10);
    private static final BlockPos MONITOR = new BlockPos(2, 1, 2);
    private static final BlockPos PLANT_MAX = new BlockPos(24, 16, 24);

    @GameTest(template = NuclearGameTests.PLANT_ARENA, timeoutTicks = 1500)
    public static void anOverpressuredVesselBreaksIntoTheContainment(GameTestHelper helper) {
        // a big steel-lined building, roomy enough for the whole primary flashing to steam
        ContainmentTestBuilder.room(helper, BlockPos.ZERO, PLANT_MAX, PwrRegistry.STEEL_LINER.get());
        FissionTestBuilder.build(helper, ORIGIN, true);
        BlockPos valve = FissionTestBuilder.pressurizer(helper, ORIGIN, 4, 0);
        // the relief valve is isolated: nothing caps the pressure
        helper.setBlock(valve.east(), Blocks.REDSTONE_BLOCK);
        helper.setBlock(MONITOR, PwrRegistry.CONTAINMENT_MONITOR.get());
        ReactorBlockEntity reactor = helper.getBlockEntity(FissionTestBuilder.controllerAt(ORIGIN));
        float[] before = new float[1];
        helper.runAfterDelay(5, () -> {
            int filled = reactor.fillCoolant(new FluidStack(AllNuclearFluids.PRIMARY_COOLANT.get(), 14_000), IFluidHandler.FluidAction.EXECUTE);
            helper.assertTrue(filled == 14_000, "the reactor takes the primary coolant, took " + filled);
            PwrModeTests.heat(reactor, 60.0);
        });
        helper.onEachTick(() -> {
            ContainmentMonitorBlockEntity monitor = ContainmentTestBuilder.monitor(helper, MONITOR);
            if (monitor != null && !reactor.getAccidents().vessel().breached()) {
                before[0] = monitor.getPressure();
            }
        });
        helper.succeedWhen(() -> {
            ContainmentMonitorBlockEntity monitor = ContainmentTestBuilder.monitor(helper, MONITOR);
            helper.assertTrue(monitor != null, "the monitor is there");
            helper.assertTrue(monitor.sealed() && monitor.getRuptures() == 0, "the containment stands: sealed " + monitor.sealed()
                    + ", ruptures " + monitor.getRuptures() + ", measuring " + monitor.isSurveying() + ", pressure " + monitor.getPressure());
            helper.assertTrue(reactor.getAccidents().vessel().breached(), "held above " + PwrConfig.vesselFailurePressure
                    + " bar the vessel fails; pressure now " + reactor.plantReadings().pressure());
            helper.assertTrue(reactor.getCoolantAmount() == 0 && reactor.getSteamAmount() == 0, "the primary emptied");
            helper.assertTrue(monitor.getPressure() > before[0] + 0.5f,
                    "its water flashed into the containment: " + before[0] + " -> " + monitor.getPressure() + " bar");
            ContainmentTestBuilder.clear(helper, BlockPos.ZERO, PLANT_MAX);
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, batch = "contamination_reflood", timeoutTicks = 200)
    public static void waterOnAMoltenCoreExplodes(GameTestHelper helper) {
        int events = ContainmentTestBuilder.falloutEvents(helper);
        FissionTestBuilder.build(helper);
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        double[] hot = new double[1];
        helper.runAfterDelay(5, () -> {
            reactor.getPhysicsSimulator().applyExternalHeat(PwrConfig.steamExplosionTemperature + 400);
            PwrModeTests.heat(reactor, 30.0);
        });
        helper.runAfterDelay(10, () -> {
            hot[0] = reactor.getCoreTemperature();
            helper.assertTrue(hot[0] > PwrConfig.steamExplosionTemperature, "the dry core is hotter than " + PwrConfig.steamExplosionTemperature);
            reactor.fillCoolant(new FluidStack(Fluids.WATER, 4_000), IFluidHandler.FluidAction.EXECUTE);
        });
        helper.runAfterDelay(13, () -> {
            helper.assertTrue(reactor.getCoolantAmount() == 0, "the water flashed off at once, " + reactor.getCoolantAmount() + " mB left");
            helper.assertTrue(reactor.getCoreTemperature() < hot[0] - 300, "quenching the core: " + hot[0] + " -> " + reactor.getCoreTemperature());
            ContainmentTestBuilder.clearArena(helper);
            ContainmentTestBuilder.cleanFallout(helper, events, 2);
            helper.succeed();
        });
    }
}
