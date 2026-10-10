package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.fusion.api.TokamakPhase;
import org.papiricoh.create_nuclearindustry.fusion.api.TokamakSupply;
import org.papiricoh.create_nuclearindustry.fusion.api.TripCause;
import org.papiricoh.create_nuclearindustry.fusion.block.FusionFuelInjectorBlock;
import org.papiricoh.create_nuclearindustry.fusion.blockentity.FusionFluidPortBlockEntity;
import org.papiricoh.create_nuclearindustry.fusion.blockentity.FusionReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.fusion.gui.TokamakControlPayload;
import org.papiricoh.create_nuclearindustry.fusion.multiblock.TokamakGeometry;
import org.papiricoh.create_nuclearindustry.reactor.block.ReactorFluidPortMode;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The tokamak's startup sequence: a start walks pump-down, cool-down, energise, breakdown and heating
 * to a burn; anything missing is reported instead; losing helium trips the plasma safely. Also the
 * coolant ports' water-only rule and where bred tritium goes. The vacuum pumps, cryogenic plant and
 * heaters of the other tokamak areas are stood in for by {@link TokamakTestBuilder#makeStartable}.
 */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class TokamakStartupTests {

    /** The real sequence, with no shortcuts: about 15 s of pumping, 20 s of cooling and 10 s of field ramp. */
    @GameTest(template = NuclearGameTests.TOKAMAK_ARENA, timeoutTicks = 2400)
    public static void fullSequenceReachesBurn(GameTestHelper helper) {
        TokamakTestBuilder.build(helper, 4, true, 256);
        FusionReactorBlockEntity tokamak = TokamakTestBuilder.tokamak(helper, 4);
        TokamakTestBuilder.makeStartable(helper, tokamak, 4, 40);
        List<TokamakPhase> seen = new ArrayList<>();
        helper.runAfterDelay(20, () -> {
            TokamakTestBuilder.fuel(tokamak, 8000, 8000, 16000);
            helper.assertTrue(tokamak.requestStart() == TripCause.NONE, "the start should be accepted, got " + tokamak.tripCause());
        });
        helper.onEachTick(() -> {
            TokamakPhase phase = tokamak.phase();
            if (seen.isEmpty() || seen.get(seen.size() - 1) != phase) {
                seen.add(phase);
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(tokamak.phase() == TokamakPhase.BURN, "should reach BURN, at " + tokamak.phase()
                    + " vacuum " + tokamak.readings().vacuum() + " coils " + tokamak.readings().magnetTemperature() + " K");
            helper.assertTrue(seen.equals(List.of(TokamakPhase.OFFLINE, TokamakPhase.PUMP_DOWN, TokamakPhase.COOL_DOWN,
                    TokamakPhase.ENERGIZE, TokamakPhase.BREAKDOWN, TokamakPhase.HEATING, TokamakPhase.BURN)),
                    "phases should come in order, saw " + seen);
            helper.assertTrue(tokamak.readings().fusionPower() > 0 && tokamak.readings().gain() > 1,
                    "burning with Q above 1, got " + tokamak.readings().gain());
            TokamakTestBuilder.leaveSafe(tokamak);
        });
    }

    @GameTest(template = NuclearGameTests.TOKAMAK_ARENA)
    public static void aStartReportsWhatIsMissing(GameTestHelper helper) {
        TokamakTestBuilder.build(helper, 4, true, 256);
        FusionReactorBlockEntity tokamak = TokamakTestBuilder.tokamak(helper, 4);
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(tokamak.isFormed(), "the tokamak should form");
            TokamakTestBuilder.fuel(tokamak, 8000, 0, 16000);
            // No vacuum pumps in the shell.
            helper.assertTrue(tokamak.requestStart() == TripCause.VACUUM_LOST, "no pumps: vacuum, got " + tokamak.tripCause());
            helper.assertTrue(tokamak.phase() == TokamakPhase.OFFLINE && tokamak.tripCause() == TripCause.VACUUM_LOST,
                    "the tokamak stays offline and shows why");
            // Too few pumps for the plasma vacuum.
            tokamak.setSupplyOverride(TokamakSupply.builder().pump(0.5).build());
            helper.assertTrue(tokamak.requestStart() == TripCause.VACUUM_LOST, "half the pumping is not enough");
            // Pumps but no liquid helium.
            tokamak.setSupplyOverride(TokamakSupply.builder().pump(1.0).build());
            helper.assertTrue(tokamak.requestStart() == TripCause.MAGNETS_WARM, "no helium: magnets, got " + tokamak.tripCause());
            tokamak.cryoTanks().liquidHelium().fill(new FluidStack(AllNuclearFluids.LIQUID_HELIUM.get(), 1000), IFluidHandler.FluidAction.EXECUTE);
            // No deuterium for the breakdown.
            tokamak.getDeuteriumTank().setFluid(FluidStack.EMPTY);
            helper.assertTrue(tokamak.requestStart() == TripCause.NO_FUEL, "no deuterium: fuel, got " + tokamak.tripCause());
            TokamakTestBuilder.fuel(tokamak, 8000, 0, 0);
            helper.assertTrue(tokamak.requestStart() == TripCause.NONE, "with everything the start is accepted");
            helper.assertTrue(tokamak.phase() == TokamakPhase.PUMP_DOWN, "and pumping down begins");
            TokamakTestBuilder.leaveSafe(tokamak);
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.TOKAMAK_ARENA)
    public static void stoppedMagnetDrivesMeanNoField(GameTestHelper helper) {
        TokamakTestBuilder.build(helper, 4, true, 0);
        FusionReactorBlockEntity tokamak = TokamakTestBuilder.tokamak(helper, 4);
        TokamakTestBuilder.makeStartable(helper, tokamak, 4, 40);
        helper.runAfterDelay(20, () -> {
            TokamakTestBuilder.fuel(tokamak, 8000, 0, 0);
            helper.assertTrue(tokamak.requestStart() == TripCause.NO_FIELD, "unpowered drives: no field, got " + tokamak.tripCause());
            helper.succeed();
        });
    }

    /** Running out of liquid helium warms the coils: a safe trip, with no disruption and no damage. */
    @GameTest(template = NuclearGameTests.TOKAMAK_ARENA, timeoutTicks = 1200)
    public static void losingLiquidHeliumTripsSafely(GameTestHelper helper) {
        TokamakTestBuilder.build(helper, 4, true, 256);
        FusionReactorBlockEntity tokamak = TokamakTestBuilder.tokamak(helper, 4);
        AtomicBoolean cryo = new AtomicBoolean(true);
        TokamakTestBuilder.makeStartable(helper, tokamak, 4, 40, cryo);
        helper.runAfterDelay(20, () -> {
            TokamakTestBuilder.fuel(tokamak, 8000, 8000, 16000);
            TokamakTestBuilder.primeAndStart(tokamak);
        });
        AtomicBoolean cut = new AtomicBoolean(false);
        helper.onEachTick(() -> {
            if (!cut.get() && tokamak.phase() == TokamakPhase.BURN) {
                cut.set(true);
                cryo.set(false);
                tokamak.cryoTanks().liquidHelium().setFluid(FluidStack.EMPTY);
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(cut.get(), "the plasma should burn first, at " + tokamak.phase());
            helper.assertTrue(tokamak.tripCause() == TripCause.MAGNETS_WARM, "warm coils should trip the plasma, got " + tokamak.tripCause());
            helper.assertTrue(tokamak.phase() == TokamakPhase.OFFLINE, "and ramp down to offline, at " + tokamak.phase());
            helper.assertFalse(tokamak.getTritiumTank().isEmpty(), "a safe trip keeps the tritium (no disruption)");
            helper.assertTrue(tokamak.isFormed() && intactShell(helper, 4), "and the tokamak is undamaged");
        });
    }

    @GameTest(template = NuclearGameTests.TOKAMAK_ARENA)
    public static void coolantPortsTakeOnlyWater(GameTestHelper helper) {
        TokamakTestBuilder.build(helper, 4, true, 0);
        FusionReactorBlockEntity tokamak = TokamakTestBuilder.tokamak(helper, 4);
        helper.succeedWhen(() -> {
            helper.assertTrue(tokamak.isFormed(), "the tokamak should form");
            FusionFluidPortBlockEntity port = helper.getBlockEntity(TokamakTestBuilder.coolantPort(4));
            IFluidHandler handler = port.getFluidHandler(null);
            helper.assertTrue(handler.fill(new FluidStack(AllNuclearFluids.HEAVY_WATER.get(), 1000), IFluidHandler.FluidAction.EXECUTE) == 0,
                    "heavy water is refused");
            helper.assertTrue(handler.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE) == 1000,
                    "water is taken");
        });
    }

    /** Tritium above the reserve leaves through an injector set to OUTPUT, into the tank next to it. */
    @GameTest(template = NuclearGameTests.TOKAMAK_ARENA)
    public static void surplusTritiumLeavesThroughAnOutputInjector(GameTestHelper helper) {
        TokamakTestBuilder.build(helper, 4, true, 0);
        BlockPos output = TokamakTestBuilder.CENTER.offset(0, 0, -TokamakGeometry.controllerToAxis(4));
        helper.setBlock(output, AllNuclearBlocks.FUSION_FUEL_INJECTOR.get().defaultBlockState()
                .setValue(FusionFuelInjectorBlock.FACING, Direction.NORTH).setValue(FusionFuelInjectorBlock.MODE, ReactorFluidPortMode.OUTPUT));
        Block fluidTank = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", "fluid_tank"));
        helper.setBlock(output.north(), fluidTank);
        FusionReactorBlockEntity tokamak = TokamakTestBuilder.tokamak(helper, 4);
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(tokamak.isFormed(), "the tokamak should form with the output injector");
            TokamakTestBuilder.fuel(tokamak, 0, 8000, 0);
        });
        helper.succeedWhen(() -> {
            IFluidHandler tank = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(output.north()), Direction.SOUTH);
            helper.assertTrue(tank != null, "the Create tank should take fluids");
            helper.assertTrue(tank.getFluidInTank(0).getFluid() == AllNuclearFluids.TRITIUM.get() && tank.getFluidInTank(0).getAmount() > 0,
                    "surplus tritium should reach the tank");
            helper.assertTrue(tokamak.getTritiumTank().getFluidAmount() >= FusionReactorBlockEntity.TRITIUM_RESERVE,
                    "the reserve stays in the tokamak");
        });
    }

    /** With nowhere to put bred tritium the blankets stop breeding and the tokamak warns; nothing is vented. */
    @GameTest(template = NuclearGameTests.TOKAMAK_ARENA, timeoutTicks = 900)
    public static void fullTritiumTankStopsBreedingWithAWarning(GameTestHelper helper) {
        TokamakTestBuilder.build(helper, 4, true, 256);
        FusionReactorBlockEntity tokamak = TokamakTestBuilder.tokamak(helper, 4);
        TokamakTestBuilder.makeStartable(helper, tokamak, 4, 40);
        helper.runAfterDelay(20, () -> {
            TokamakTestBuilder.fuel(tokamak, 8000, FusionReactorBlockEntity.FUEL_TANK_CAPACITY, 16000);
            // Deuterium-only fuelling: the full tritium tank is not drawn on.
            tokamak.setDtRatio(1.0);
            TokamakTestBuilder.primeAndStart(tokamak);
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(tokamak.readings().tritiumOverflow(), "bred tritium with no room should raise the warning, phase "
                    + tokamak.phase() + " mode " + tokamak.getPhysics().getMode());
            helper.assertTrue(tokamak.getTritiumTank().getFluidAmount() == FusionReactorBlockEntity.FUEL_TANK_CAPACITY,
                    "the tank stays full");
            TokamakTestBuilder.leaveSafe(tokamak);
        });
    }

    @GameTest(template = NuclearGameTests.TOKAMAK_ARENA)
    public static void aRedstonePulseStartsIt(GameTestHelper helper) {
        TokamakTestBuilder.build(helper, 4, true, 256);
        FusionReactorBlockEntity tokamak = TokamakTestBuilder.tokamak(helper, 4);
        TokamakTestBuilder.makeStartable(helper, tokamak, 4, 40);
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(tokamak.isFormed() && tokamak.phase() == TokamakPhase.OFFLINE, "formed and offline first");
            TokamakTestBuilder.fuel(tokamak, 8000, 0, 0);
            helper.setBlock(TokamakTestBuilder.controller(4).east(), Blocks.REDSTONE_BLOCK);
        });
        helper.runAfterDelay(23, () -> {
            helper.assertTrue(tokamak.phase() != TokamakPhase.OFFLINE, "a redstone pulse should start it, got " + tokamak.tripCause());
            TokamakTestBuilder.leaveSafe(tokamak);
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.TOKAMAK_ARENA)
    public static void theControlScreenIsChecked(GameTestHelper helper) {
        TokamakTestBuilder.build(helper, 4, true, 256);
        FusionReactorBlockEntity tokamak = TokamakTestBuilder.tokamak(helper, 4);
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(tokamak.isFormed(), "the tokamak should form");
            BlockPos controller = helper.absolutePos(TokamakTestBuilder.controller(4));
            Player far = helper.makeMockPlayer(GameType.SURVIVAL);
            far.setPos(controller.getCenter().add(40, 0, 0));
            helper.assertFalse(TokamakControlPayload.apply(far, new TokamakControlPayload(controller, TokamakControlPayload.FUEL_RATE, 0.5)),
                    "a player out of reach cannot change it");
            Player near = helper.makeMockPlayer(GameType.SURVIVAL);
            near.setPos(controller.getCenter().add(2, 0, 0));
            helper.assertTrue(TokamakControlPayload.apply(near, new TokamakControlPayload(controller, TokamakControlPayload.FUEL_RATE, 1.0e9)),
                    "a player in reach can");
            helper.assertTrue(tokamak.readings().fuelRate() == tokamak.maxFuelRate(), "and the fuelling is clamped");
            helper.assertTrue(TokamakControlPayload.apply(near, new TokamakControlPayload(controller, TokamakControlPayload.DT_RATIO, -3)),
                    "the D:T mix can be set");
            helper.assertTrue(tokamak.readings().dtRatio() == 0, "and is clamped to 0..1");
            helper.assertTrue(TokamakControlPayload.apply(near, new TokamakControlPayload(controller, TokamakControlPayload.HEATING_TOGGLE, 0)),
                    "heating can be switched");
            helper.assertFalse(tokamak.readings().heatingEnabled(), "off");
            helper.assertFalse(TokamakControlPayload.apply(near, new TokamakControlPayload(controller, TokamakControlPayload.HEATING_LEVEL, Double.NaN)),
                    "a broken value is refused");
            helper.assertFalse(TokamakControlPayload.apply(near, new TokamakControlPayload(controller.above(5), TokamakControlPayload.START, 0)),
                    "only a tokamak controller takes it");
            helper.succeed();
        });
    }

    private static boolean intactShell(GameTestHelper helper, int radius) {
        TokamakGeometry geometry = TokamakGeometry.of(radius);
        for (int i = 0; i < geometry.shellCount(); i++) {
            BlockPos pos = TokamakTestBuilder.CENTER.offset(geometry.shellX(i), geometry.shellY(i), geometry.shellZ(i));
            if (helper.getBlockState(pos).isAir()) {
                return false;
            }
        }
        return true;
    }
}
