package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.pwr.PwrConfig;
import org.papiricoh.create_nuclearindustry.reactor.pwr.PwrRegistry;
import org.papiricoh.create_nuclearindustry.reactor.pwr.block.PressurizerHeaterPortBlock;
import org.papiricoh.create_nuclearindustry.reactor.pwr.block.ReliefValveBlock;

/** The pressurizer on a reactor's lid: its heaters raise the pressure and its relief valve caps it. */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class PressurizerTests {
    private static final BlockPos ORIGIN = FissionTestBuilder.ORIGIN;

    private static ReactorBlockEntity filledReactor(GameTestHelper helper, double decayHeat) {
        ReactorBlockEntity reactor = FissionTestBuilder.controller(helper);
        helper.runAfterDelay(5, () -> {
            int filled = reactor.fillCoolant(new FluidStack(AllNuclearFluids.PRIMARY_COOLANT.get(), 14_000), IFluidHandler.FluidAction.EXECUTE);
            helper.assertTrue(filled == 14_000, "the reactor takes the primary coolant, took " + filled);
            PwrModeTests.heat(reactor, decayHeat);
        });
        return reactor;
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 400)
    public static void reliefValveOpensAtHighPressure(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        BlockPos valve = FissionTestBuilder.pressurizer(helper, ORIGIN, 4, 0);
        // No pumps: the core's heat stays in the vessel, boils it and drives the pressure up.
        ReactorBlockEntity reactor = filledReactor(helper, 20.0);
        helper.succeedWhen(() -> {
            helper.assertBlockProperty(valve, ReliefValveBlock.OPEN, true);
            helper.assertTrue(reactor.plantReadings().pressure() >= PwrConfig.reliefClosePressure,
                    "it opened near its set pressure, reading " + reactor.plantReadings().pressure());
            helper.assertTrue(reactor.getCoolantAmount() + reactor.getSteamAmount() < 14_000, "steam left the loop through it");
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 400)
    public static void anIsolatedReliefValveStaysShut(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        BlockPos valve = FissionTestBuilder.pressurizer(helper, ORIGIN, 4, 0);
        helper.setBlock(valve.east(), Blocks.REDSTONE_BLOCK);
        ReactorBlockEntity reactor = filledReactor(helper, 20.0);
        helper.succeedWhen(() -> {
            helper.assertTrue(reactor.plantReadings().pressure() > PwrConfig.reliefOpenPressure + 5,
                    "the pressure climbs past the relief pressure, reading " + reactor.plantReadings().pressure());
            helper.assertBlockProperty(valve, ReliefValveBlock.OPEN, false);
            helper.assertTrue(reactor.getCoolantAmount() + reactor.getSteamAmount() == 14_000, "nothing left the loop");
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 300)
    public static void manualHeatersRaiseThePressure(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        FissionTestBuilder.pressurizer(helper, ORIGIN, 4, 2);
        BlockPos heater = ORIGIN.offset(3, FissionTestBuilder.SIZE, 1);
        for (int i = 0; i < 2; i++) {
            helper.setBlock(heater.above(i), PwrRegistry.PRESSURIZER_HEATER_PORT.get().defaultBlockState()
                    .setValue(PressurizerHeaterPortBlock.AUTOMATIC, false));
            helper.setBlock(heater.above(i).east(), Blocks.REDSTONE_BLOCK);
        }
        ReactorBlockEntity reactor = filledReactor(helper, 0.0);
        helper.succeedWhen(() -> {
            helper.assertTrue(reactor.plantReadings().pwr(), "the loop runs with a cold core");
            helper.assertTrue(reactor.getPrimaryLoop().heatersRunning(), "the powered heaters run");
            helper.assertTrue(reactor.plantReadings().pressure() > PwrConfig.pressureSetpoint + 8,
                    "they raise the pressure past the setpoint, reading " + reactor.plantReadings().pressure());
        });
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA, timeoutTicks = 300)
    public static void automaticHeatersHoldTheSetpoint(GameTestHelper helper) {
        FissionTestBuilder.build(helper);
        FissionTestBuilder.pressurizer(helper, ORIGIN, 4, 2);
        ReactorBlockEntity reactor = filledReactor(helper, 0.0);
        helper.runAfterDelay(250, () -> {
            float pressure = reactor.plantReadings().pressure();
            helper.assertTrue(Math.abs(pressure - PwrConfig.pressureSetpoint) < 4,
                    "automatic heaters keep it near " + PwrConfig.pressureSetpoint + " bar, reading " + pressure);
            helper.succeed();
        });
    }
}
