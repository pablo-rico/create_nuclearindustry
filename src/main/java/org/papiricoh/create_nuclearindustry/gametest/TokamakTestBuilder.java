package org.papiricoh.create_nuclearindustry.gametest;

import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.fusion.api.TokamakSupply;
import org.papiricoh.create_nuclearindustry.fusion.api.TripCause;
import org.papiricoh.create_nuclearindustry.fusion.physics.VacuumModel;
import org.papiricoh.create_nuclearindustry.fusion.block.FusionFluidPortBlock;
import org.papiricoh.create_nuclearindustry.fusion.block.FusionFuelInjectorBlock;
import org.papiricoh.create_nuclearindustry.fusion.block.FusionMagnetInputBlock;
import org.papiricoh.create_nuclearindustry.fusion.blockentity.FusionReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.fusion.multiblock.TokamakGeometry;
import org.papiricoh.create_nuclearindustry.reactor.block.ReactorFluidPortMode;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Builds a complete tokamak in the 41x9x41 arena: coils on the sides of the cross-section,
 * breeding blankets on top and bottom, controller on the east equator, ports above and
 * below it, an injector on the west and a magnet drive on the south driven by a creative motor.
 */
public final class TokamakTestBuilder {
    static final BlockPos CENTER = new BlockPos(20, 4, 20);

    private TokamakTestBuilder() {}

    static BlockPos controller(int radius) {
        return controller(CENTER, radius);
    }

    /** The controller on the east equator of a tokamak of {@code radius} around {@code center}. */
    public static BlockPos controller(BlockPos center, int radius) {
        return center.offset(TokamakGeometry.controllerToAxis(radius), 0, 0);
    }

    static BlockPos injector(int radius) {
        return CENTER.offset(-TokamakGeometry.controllerToAxis(radius), 0, 0);
    }

    static BlockPos magnetInput(int radius) {
        return CENTER.offset(0, 0, TokamakGeometry.controllerToAxis(radius));
    }

    static BlockPos coolantPort(int radius) {
        return controller(radius).above();
    }

    static BlockPos steamPort(int radius) {
        return controller(radius).below();
    }

    static void build(GameTestHelper helper, int radius, boolean blankets, int motorRpm) {
        build(BlockPlacer.of(helper), CENTER, radius, blankets, motorRpm);
    }

    /**
     * A tokamak of {@code radius} around {@code center} of {@code placer}, with a creative motor on its
     * magnet drive when {@code motorRpm} is above 0.
     */
    public static void build(BlockPlacer helper, BlockPos CENTER, int radius, boolean blankets, int motorRpm) {
        TokamakGeometry geometry = TokamakGeometry.of(radius);
        BlockPos controllerPos = controller(CENTER, radius);
        BlockPos injectorPos = CENTER.offset(-TokamakGeometry.controllerToAxis(radius), 0, 0);
        BlockPos magnetPos = CENTER.offset(0, 0, TokamakGeometry.controllerToAxis(radius));
        BlockState coil = AllNuclearBlocks.FUSION_ELECTROMAGNET.get().defaultBlockState();
        BlockState blanket = (blankets ? AllNuclearBlocks.FUSION_BREEDING_BLANKET : AllNuclearBlocks.FUSION_ACCELERATOR_SEGMENT)
                .get().defaultBlockState();
        for (int i = 0; i < geometry.shellCount(); i++) {
            int dy = geometry.shellY(i);
            BlockPos pos = CENTER.offset(geometry.shellX(i), dy, geometry.shellZ(i));
            helper.set(pos, Math.abs(dy) == TokamakGeometry.HALF_HEIGHT ? blanket : coil);
        }
        for (int dy = -TokamakGeometry.HALF_HEIGHT; dy <= TokamakGeometry.HALF_HEIGHT; dy++) {
            helper.set(CENTER.offset(0, dy, 0), AllNuclearBlocks.FUSION_CENTRAL_SOLENOID.get());
        }

        helper.set(controllerPos.above(), AllNuclearBlocks.FUSION_FLUID_PORT.get().defaultBlockState()
                .setValue(FusionFluidPortBlock.FACING, Direction.EAST).setValue(FusionFluidPortBlock.MODE, ReactorFluidPortMode.INPUT));
        helper.set(controllerPos.below(), AllNuclearBlocks.FUSION_FLUID_PORT.get().defaultBlockState()
                .setValue(FusionFluidPortBlock.FACING, Direction.EAST).setValue(FusionFluidPortBlock.MODE, ReactorFluidPortMode.OUTPUT));
        helper.set(injectorPos, AllNuclearBlocks.FUSION_FUEL_INJECTOR.get().defaultBlockState()
                .setValue(FusionFuelInjectorBlock.FACING, Direction.WEST).setValue(FusionFuelInjectorBlock.MODE, ReactorFluidPortMode.INPUT));
        helper.set(magnetPos, AllNuclearBlocks.FUSION_MAGNET_INPUT.get().defaultBlockState()
                .setValue(FusionMagnetInputBlock.FACING, Direction.SOUTH));
        if (motorRpm > 0) {
            BlockPos motorPos = magnetPos.south();
            Block motor = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", "creative_motor"));
            helper.set(motorPos, motor.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.NORTH));
            CreativeMotorBlockEntity motorEntity = helper.blockEntity(motorPos, CreativeMotorBlockEntity.class);
            if (motorEntity != null) {
                motorEntity.generatedSpeed.setValue(motorRpm);
            }
        }
        helper.set(controllerPos, AllNuclearBlocks.FUSION_CONTROLLER.get());
    }

    static FusionReactorBlockEntity tokamak(GameTestHelper helper, int radius) {
        return helper.getBlockEntity(controller(radius));
    }

    /**
     * Lets a test tokamak start without the vacuum pumps, cryogenic plant and heaters of the other
     * tokamak areas: the shell's supply is replaced by the pumps a tokamak this size needs (at rated
     * speed) plus {@code beamPower} MW of neutral beams, and the liquid helium tank is kept full (and the
     * warm helium tank empty) every tick while {@code cryo} is true. With {@code coolant} the water tank
     * is kept full and the plasma steam taken away too: a burning R=4 boils a full tank in seconds.
     */
    static void makeStartable(GameTestHelper helper, FusionReactorBlockEntity tokamak, int radius, double beamPower,
                              AtomicBoolean cryo, boolean coolant) {
        TokamakSupply.Builder supply = TokamakSupply.builder();
        for (int i = 0; i < VacuumModel.pumpsNeeded(radius); i++) {
            supply.pump(1.0);
        }
        supply.neutralBeam(beamPower, beamPower * 0.02);
        tokamak.setSupplyOverride(supply.build());
        helper.onEachTick(() -> {
            if (cryo.get()) {
                FusionReactorBlockEntity.CryoTanks tanks = tokamak.cryoTanks();
                tanks.liquidHelium().fill(new FluidStack(AllNuclearFluids.LIQUID_HELIUM.get(), tanks.liquidHelium().getSpace()),
                        IFluidHandler.FluidAction.EXECUTE);
                tanks.warmHelium().setFluid(FluidStack.EMPTY);
            }
            if (coolant) {
                tokamak.getCoolantTank().fill(new FluidStack(Fluids.WATER, tokamak.getCoolantTank().getSpace()), IFluidHandler.FluidAction.EXECUTE);
                tokamak.getPlasmaSteamTank().setFluid(FluidStack.EMPTY);
            }
        });
    }

    static void makeStartable(GameTestHelper helper, FusionReactorBlockEntity tokamak, int radius, double beamPower,
                              AtomicBoolean cryo) {
        makeStartable(helper, tokamak, radius, beamPower, cryo, true);
    }

    static void makeStartable(GameTestHelper helper, FusionReactorBlockEntity tokamak, int radius, double beamPower) {
        makeStartable(helper, tokamak, radius, beamPower, new AtomicBoolean(true), true);
    }

    /** Fills the fuel and coolant tanks. */
    static void fuel(FusionReactorBlockEntity tokamak, int deuterium, int tritium, int water) {
        if (deuterium > 0) {
            tokamak.fillFuel(new FluidStack(AllNuclearFluids.DEUTERIUM.get(), deuterium), IFluidHandler.FluidAction.EXECUTE);
        }
        if (tritium > 0) {
            tokamak.fillFuel(new FluidStack(AllNuclearFluids.TRITIUM.get(), tritium), IFluidHandler.FluidAction.EXECUTE);
        }
        if (water > 0) {
            tokamak.getCoolantTank().fill(new FluidStack(Fluids.WATER, water), IFluidHandler.FluidAction.EXECUTE);
        }
    }

    /**
     * Shuts a test tokamak down as its test passes. The world outlives the test, and a tokamak left
     * burning would run out of coolant and disrupt (with explosions, once another test has restored
     * the config) next to the tests still running.
     */
    static void leaveSafe(FusionReactorBlockEntity tokamak) {
        tokamak.requestShutdown();
        tokamak.setSupplyOverride(TokamakSupply.builder().pump(1.0).build());
    }

    /** Skips the pump-down and cool-down (see {@link FusionReactorBlockEntity#primeForTest()}) and starts. */
    static TripCause primeAndStart(FusionReactorBlockEntity tokamak) {
        tokamak.primeForTest();
        return tokamak.requestStart();
    }
}
