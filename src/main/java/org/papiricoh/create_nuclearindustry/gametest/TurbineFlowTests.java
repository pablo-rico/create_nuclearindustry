package org.papiricoh.create_nuclearindustry.gametest;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import java.util.concurrent.atomic.AtomicBoolean;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.reactor.block.ReactorFluidPortMode;
import org.papiricoh.create_nuclearindustry.fusion.api.TokamakPhase;
import org.papiricoh.create_nuclearindustry.fusion.api.TripCause;
import org.papiricoh.create_nuclearindustry.fusion.blockentity.FusionReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.turbine.AbstractTurbineControllerBlockEntity;
import org.papiricoh.create_nuclearindustry.turbine.TurbineStructure;
import org.papiricoh.create_nuclearindustry.turbine.TurbineTier;

/** Working fluid in and condensate out through the ports, with real Create tanks on both sides. */
@GameTestHolder(Create_NuclearIndustry.MODID)
@PrefixGameTestTemplate(false)
public class TurbineFlowTests {
    private static final BlockPos CONTROLLER = new BlockPos(6, 6, 1);

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void steamTurbineRunsBetweenTanks(GameTestHelper helper) {
        runBetweenTanks(helper, TurbineTier.STEAM, AllNuclearFluids.STEAM.get());
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void plasmaTurbineRunsBetweenTanks(GameTestHelper helper) {
        runBetweenTanks(helper, TurbineTier.PLASMA, AllNuclearFluids.PLASMA_STEAM.get());
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void steamTurbineRunsThroughPumpedPipes(GameTestHelper helper) {
        runThroughPumpedPipes(helper, TurbineTier.STEAM, AllNuclearFluids.STEAM.get());
    }

    @GameTest(template = NuclearGameTests.SMALL_ARENA)
    public static void plasmaTurbineRunsThroughPumpedPipes(GameTestHelper helper) {
        runThroughPumpedPipes(helper, TurbineTier.PLASMA, AllNuclearFluids.PLASMA_STEAM.get());
    }

    @GameTest(template = NuclearGameTests.TOKAMAK_ARENA, batch = "turbine_flow_tokamak_0", timeoutTicks = 200)
    public static void tokamakSteamPortFillsATankBesideIt(GameTestHelper helper) {
        int radius = 4;
        TokamakTestBuilder.build(helper, radius, true, 0);
        BlockPos tankPos = TokamakTestBuilder.steamPort(radius).east();
        helper.setBlock(tankPos, createTank());
        FusionReactorBlockEntity tokamak = TokamakTestBuilder.tokamak(helper, radius);
        helper.onEachTick(() -> {
            if (tokamak != null && tokamak.isFormed()) {
                tokamak.getPlasmaSteamTank().fill(new FluidStack(AllNuclearFluids.PLASMA_STEAM.get(), 1_000), IFluidHandler.FluidAction.EXECUTE);
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(tokamak != null && tokamak.isFormed(), "tokamak should form: " + (tokamak == null ? "" : tokamak.getValidationMessage()));
            FluidTankBlockEntity tank = helper.getBlockEntity(tankPos);
            helper.assertTrue(tank != null && tank.getTankInventory().getFluidAmount() > 0, "the steam port should fill the tank beside it");
        });
    }

    @GameTest(template = NuclearGameTests.TOKAMAK_ARENA, batch = "turbine_flow_tokamak_1", timeoutTicks = 300)
    public static void dualPipesCarryPlasmaSteamToTheTurbine(GameTestHelper helper) {
        int radius = 4;
        TokamakTestBuilder.build(helper, radius, true, 0);
        BlockPos first = TokamakTestBuilder.steamPort(radius).east();
        BlockPos second = first.east();
        helper.setBlock(first, AllNuclearBlocks.DUAL_FLUID_PIPE.get());
        helper.setBlock(second, AllNuclearBlocks.DUAL_FLUID_PIPE.get());
        // The turbine's top input port right under the second pipe.
        BlockPos controller = second.below().below().north();
        TurbineStructure s = TurbineTestBuilder.build(helper, TurbineTier.PLASMA, controller, 3, 3, 5);
        helper.assertTrue(s.at(1, s.hu, 0).equals(second.below()), "turbine input port misplaced");
        FusionReactorBlockEntity tokamak = TokamakTestBuilder.tokamak(helper, radius);
        AbstractTurbineControllerBlockEntity turbine = helper.getBlockEntity(controller);
        helper.onEachTick(() -> {
            if (tokamak != null && tokamak.isFormed()) {
                tokamak.getPlasmaSteamTank().fill(new FluidStack(AllNuclearFluids.PLASMA_STEAM.get(), 1_000), IFluidHandler.FluidAction.EXECUTE);
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(tokamak != null && tokamak.isFormed(), "tokamak should form: " + (tokamak == null ? "" : tokamak.getValidationMessage()));
            helper.assertTrue(turbine.getStructure().isPresent(), "turbine should form: " + turbine.getDisplay().message());
            helper.assertTrue(turbine.getSteamTank().getFluidAmount() > 0 || turbine.getCurrentUse() > 0,
                    "plasma steam should reach the turbine through the dual pipes");
        });
    }

    /**
     * Create's pipe networks keep the fluid handler they drain from and never look it up again: a port
     * that invalidated its capability whenever its machine re-formed left the pump spinning and the
     * pipes full, with nothing flowing until the world was reloaded.
     */
    @GameTest(template = NuclearGameTests.TOKAMAK_ARENA, batch = "turbine_flow_tokamak_2", timeoutTicks = 300)
    public static void pumpedLinesKeepFlowingWhenBothMachinesReform(GameTestHelper helper) {
        PumpedLine line = pumpedLine(helper, 0);
        helper.onEachTick(() -> {
            if (line.tokamak.isFormed()) {
                line.tokamak.getPlasmaSteamTank().fill(new FluidStack(AllNuclearFluids.PLASMA_STEAM.get(), 1_000), IFluidHandler.FluidAction.EXECUTE);
            }
        });
        int[] before = {0};
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(line.turbine.getCurrentUse() > 0, "plasma steam should flow first: " + line.describe(helper));
            helper.assertTrue(line.condensed(helper) > 0, "condensate should flow first: " + line.describe(helper));
            // What a disruption, a broken shell block or a click on the controller does: both re-form.
            line.tokamak.requestStructureRevalidation(null);
            line.turbine.attemptFormation(null);
            before[0] = line.condensed(helper);
        });
        helper.runAfterDelay(160, () -> {
            helper.assertTrue(line.tokamak.isFormed() && line.turbine.getStructure().isPresent(), "both should re-form: " + line.describe(helper));
            helper.assertTrue(line.turbine.getCurrentUse() > 0, "plasma steam should keep flowing: " + line.describe(helper));
            helper.assertTrue(line.condensed(helper) > before[0] + 1_000,
                    "condensate should keep flowing, " + before[0] + " before: " + line.describe(helper));
            helper.succeed();
        });
    }

    @GameTest(template = NuclearGameTests.TOKAMAK_ARENA, batch = "turbine_flow_tokamak_3", timeoutTicks = 1200)
    public static void aBurningTokamakDrivesThePlasmaTurbineThroughPumps(GameTestHelper helper) {
        PumpedLine line = pumpedLine(helper, 256);
        FusionReactorBlockEntity tokamak = line.tokamak;
        TokamakTestBuilder.makeStartable(helper, tokamak, PumpedLine.RADIUS, 40, new AtomicBoolean(true), false);
        helper.onEachTick(() -> tokamak.getCoolantTank().fill(new FluidStack(Fluids.WATER, tokamak.getCoolantTank().getSpace()),
                IFluidHandler.FluidAction.EXECUTE));
        helper.runAfterDelay(20, () -> {
            TokamakTestBuilder.fuel(tokamak, 8000, 8000, 16000);
            helper.assertTrue(TokamakTestBuilder.primeAndStart(tokamak) == TripCause.NONE, "the start should be accepted");
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(tokamak.phase() == TokamakPhase.BURN, "should burn: " + line.describe(helper));
            helper.assertTrue(line.condensed(helper) >= 4_000, "the turbine should run on the tokamak's steam: " + line.describe(helper));
            TokamakTestBuilder.leaveSafe(tokamak);
        });
    }

    /**
     * A tokamak whose plasma steam port is pumped into the west wall of a plasma turbine, and the
     * turbine's condensate pumped from its east wall into a Create tank.
     */
    private static PumpedLine pumpedLine(GameTestHelper helper, int tokamakMotorRpm) {
        TokamakTestBuilder.build(helper, PumpedLine.RADIUS, true, tokamakMotorRpm);
        BlockPos steamPort = TokamakTestBuilder.steamPort(PumpedLine.RADIUS);
        BlockPos input = pumpEast(helper, steamPort);
        BlockPos controller = input.east().north();
        TurbineStructure s = TurbineTestBuilder.build(helper, TurbineTier.PLASMA, controller, 3, 3, 5);
        helper.assertTrue(s.at(1, 0, -s.hv).equals(input), "turbine input port misplaced");
        BlockState casing = TurbineTestBuilder.casing(TurbineTier.PLASMA).defaultBlockState();
        helper.setBlock(s.at(1, s.hu, 0), casing);
        helper.setBlock(s.at(1, -s.hu, 0), casing);
        TurbineTestBuilder.port(helper, TurbineTier.PLASMA, input, Direction.WEST, ReactorFluidPortMode.INPUT);
        BlockPos output = s.at(1, 0, s.hv);
        TurbineTestBuilder.port(helper, TurbineTier.PLASMA, output, Direction.EAST, ReactorFluidPortMode.OUTPUT);
        BlockPos tank = pumpEast(helper, output);
        helper.setBlock(tank, createTank());
        return new PumpedLine(TokamakTestBuilder.tokamak(helper, PumpedLine.RADIUS), helper.getBlockEntity(controller), tank);
    }

    /** Pipe, pump pushing east (turned by a motor through a cogwheel), pipe; returns the block after it. */
    private static BlockPos pumpEast(GameTestHelper helper, BlockPos from) {
        BlockState pipe = create("glass_fluid_pipe").defaultBlockState().setValue(BlockStateProperties.AXIS, Direction.Axis.X);
        BlockPos pump = from.east(2);
        helper.setBlock(from.east(), pipe);
        helper.setBlock(pump, create("mechanical_pump").defaultBlockState().setValue(BlockStateProperties.FACING, Direction.EAST));
        helper.setBlock(pump.east(), pipe);
        helper.setBlock(pump.south(), create("cogwheel").defaultBlockState().setValue(BlockStateProperties.AXIS, Direction.Axis.X));
        motor(helper, pump.south().east(), Direction.WEST);
        return pump.east(2);
    }

    private record PumpedLine(FusionReactorBlockEntity tokamak, AbstractTurbineControllerBlockEntity turbine, BlockPos tank) {
        static final int RADIUS = 4;

        int condensed(GameTestHelper helper) {
            return helper.getBlockEntity(tank) instanceof FluidTankBlockEntity t ? t.getTankInventory().getFluidAmount() : -1;
        }

        String describe(GameTestHelper helper) {
            return "tokamak " + (tokamak.isFormed() ? tokamak.phase() : tokamak.getValidationMessage())
                    + " holds " + tokamak.getPlasmaSteamTank().getFluidAmount() + " mB of plasma steam; turbine "
                    + (turbine.getStructure().isPresent() ? "uses " + turbine.getCurrentUse() + " mB/t, holds " + turbine.getSteamTank().getFluidAmount()
                    + " mB, condensate " + turbine.getCondensateTank().getFluidAmount() : turbine.getDisplay().message())
                    + "; condensate tank " + condensed(helper);
        }
    }

    /** A creative motor at full speed: Create pumps move fluid in proportion to their speed. */
    private static void motor(GameTestHelper helper, BlockPos pos, Direction facing) {
        helper.setBlock(pos, create("creative_motor").defaultBlockState().setValue(BlockStateProperties.FACING, facing));
        if (helper.getBlockEntity(pos) instanceof com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity motor) {
            motor.generatedSpeed.setValue(256);
        }
    }

    private static Block createTank() {
        return create("fluid_tank");
    }

    private static Block create(String id) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", id));
    }

    /** Tank, glass pipe, pump pushing down, glass pipe, input port; a creative motor turns the pump through a cogwheel. */
    private static void runThroughPumpedPipes(GameTestHelper helper, TurbineTier tier, Fluid working) {
        TurbineStructure s = TurbineTestBuilder.build(helper, tier, CONTROLLER, 3, 3, 5);
        BlockPos port = s.at(1, s.hu, 0);
        BlockPos lowerPipe = port.above();
        BlockPos pump = lowerPipe.above();
        BlockPos upperPipe = pump.above();
        BlockPos supply = upperPipe.above();
        BlockState pipe = create("glass_fluid_pipe").defaultBlockState().setValue(BlockStateProperties.AXIS, Direction.Axis.Y);
        helper.setBlock(lowerPipe, pipe);
        helper.setBlock(upperPipe, pipe);
        helper.setBlock(pump, create("mechanical_pump").defaultBlockState().setValue(BlockStateProperties.FACING, Direction.DOWN));
        helper.setBlock(pump.east(), create("cogwheel").defaultBlockState().setValue(BlockStateProperties.AXIS, Direction.Axis.Y));
        helper.setBlock(pump.east().above(), create("creative_motor").defaultBlockState().setValue(BlockStateProperties.FACING, Direction.DOWN));
        helper.setBlock(supply, createTank());
        BlockPos drain = s.at(1, -s.hu - 1, 0);
        helper.setBlock(drain, createTank());
        AbstractTurbineControllerBlockEntity turbine = helper.getBlockEntity(CONTROLLER);
        helper.runAfterDelay(2, () -> {
            FluidTankBlockEntity tank = helper.getBlockEntity(supply);
            helper.assertTrue(tank != null, "supply tank missing");
            int filled = tank.getTankInventory().fill(new FluidStack(working, 8_000), IFluidHandler.FluidAction.EXECUTE);
            helper.assertTrue(filled > 0, "the supply tank should take the working fluid");
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(turbine.getStructure().isPresent(), "turbine should form: " + turbine.getDisplay().message());
            FluidTankBlockEntity in = helper.getBlockEntity(supply);
            helper.assertTrue(turbine.getSteamTank().getFluidAmount() > 0 || turbine.getCurrentUse() > 0,
                    "the pump should feed the turbine; supply holds " + (in == null ? -1 : in.getTankInventory().getFluidAmount()));
        });
    }

    private static void runBetweenTanks(GameTestHelper helper, TurbineTier tier, Fluid working) {
        TurbineStructure s = TurbineTestBuilder.build(helper, tier, CONTROLLER, 3, 3, 5);
        BlockPos supply = s.at(1, s.hu + 1, 0);
        BlockPos drain = s.at(1, -s.hu - 1, 0);
        helper.setBlock(supply, createTank());
        helper.setBlock(drain, createTank());
        AbstractTurbineControllerBlockEntity turbine = helper.getBlockEntity(CONTROLLER);
        helper.runAfterDelay(2, () -> {
            FluidTankBlockEntity tank = helper.getBlockEntity(supply);
            helper.assertTrue(tank != null, "supply tank missing");
            int filled = tank.getTankInventory().fill(new FluidStack(working, 8_000), IFluidHandler.FluidAction.EXECUTE);
            helper.assertTrue(filled > 0, "the supply tank should take the working fluid");
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(turbine.getStructure().isPresent(), "turbine should form: " + turbine.getDisplay().message());
            FluidTankBlockEntity out = helper.getBlockEntity(drain);
            helper.assertTrue(out != null, "drain tank missing");
            FluidStack condensate = out.getTankInventory().getFluid();
            helper.assertTrue(turbine.getCurrentUse() > 0 || turbine.getCondensateTank().getFluidAmount() > 0,
                    "the turbine should take working fluid; steam tank " + turbine.getSteamTank().getFluidAmount());
            helper.assertTrue(condensate.getFluid() == Fluids.WATER && condensate.getAmount() > 0,
                    "condensate should reach the tank below, got " + condensate.getAmount() + " of " + condensate.getFluid()
                            + " (controller holds " + turbine.getCondensateTank().getFluidAmount() + ")");
        });
    }
}
