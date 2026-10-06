package org.papiricoh.create_nuclearindustry.turbine;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.Config;
import org.papiricoh.create_nuclearindustry.fluids.NuclearFluidHelper;
import org.papiricoh.create_nuclearindustry.fusion.block.FusionTurbineFluidPortBlock;
import org.papiricoh.create_nuclearindustry.reactor.block.ReactorFluidPortMode;
import org.papiricoh.create_nuclearindustry.reactor.block.TurbineFluidPortBlock;

import java.util.function.DoubleSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;

/** The two turbine families: the same box multiblock with different blocks, fluids and rates. */
public enum TurbineTier {
    STEAM(AllNuclearBlocks.TURBINE_OUTPUT::get, AllNuclearBlocks.TURBINE_CASING::get, AllNuclearBlocks.TURBINE_ROTOR::get,
            AllNuclearBlocks.TURBINE_FLUID_PORT::get, TurbineFluidPortBlock.MODE, NuclearFluidHelper::isTurbineSteam,
            80, 320, 16_000, 1_000, () -> Config.steamCapacityPerPort, "Steam"),
    PLASMA(AllNuclearBlocks.FUSION_PLASMA_TURBINE::get, AllNuclearBlocks.FUSION_TURBINE_CASING::get, AllNuclearBlocks.FUSION_TURBINE_ROTOR::get,
            AllNuclearBlocks.FUSION_TURBINE_FLUID_PORT::get, FusionTurbineFluidPortBlock.MODE, NuclearFluidHelper::isPlasmaTurbineFluid,
            40, 160, 32_000, 2_000, () -> Config.plasmaCapacityPerPort, "Plasma steam");

    private final Supplier<? extends Block> controller;
    private final Supplier<? extends Block> casing;
    private final Supplier<? extends Block> rotor;
    private final Supplier<? extends Block> port;
    private final EnumProperty<ReactorFluidPortMode> portMode;
    private final Predicate<FluidStack> workingFluid;
    /** mB/t of working fluid each rotor block expands. */
    public final int perRotor;
    /** mB/t one input/output port pair passes; also the reference flow for the capacity config. */
    public final int perPort;
    public final int baseTank;
    public final int tankPerInteriorBlock;
    private final DoubleSupplier capacityPerPort;
    public final String fluidName;

    TurbineTier(Supplier<? extends Block> controller, Supplier<? extends Block> casing, Supplier<? extends Block> rotor,
                Supplier<? extends Block> port, EnumProperty<ReactorFluidPortMode> portMode, Predicate<FluidStack> workingFluid,
                int perRotor, int perPort, int baseTank, int tankPerInteriorBlock, DoubleSupplier capacityPerPort, String fluidName) {
        this.controller = controller;
        this.casing = casing;
        this.rotor = rotor;
        this.port = port;
        this.portMode = portMode;
        this.workingFluid = workingFluid;
        this.perRotor = perRotor;
        this.perPort = perPort;
        this.baseTank = baseTank;
        this.tankPerInteriorBlock = tankPerInteriorBlock;
        this.capacityPerPort = capacityPerPort;
        this.fluidName = fluidName;
    }

    public boolean isController(BlockState state) {
        return state.is(controller.get());
    }

    public boolean isCasing(BlockState state) {
        return state.is(casing.get());
    }

    public boolean isRotor(BlockState state) {
        return state.is(rotor.get());
    }

    public boolean isPort(BlockState state) {
        return state.is(port.get());
    }

    public ReactorFluidPortMode portMode(BlockState state) {
        return state.getValue(portMode);
    }

    public boolean isWorkingFluid(FluidStack stack) {
        return workingFluid.test(stack);
    }

    /** What the working fluid condenses into: heavy steam back to heavy water, anything else to water. */
    public Fluid condensate(FluidStack working) {
        return NuclearFluidHelper.isHeavySteam(working) ? AllNuclearFluids.HEAVY_WATER.get() : Fluids.WATER;
    }

    /** Stress capacity per RPM at {@code use} mB/t; config values are for one port pair at full flow. */
    public float capacityFor(double use) {
        return (float) (capacityPerPort.getAsDouble() * use / perPort);
    }

    public static boolean isGlass(BlockState state) {
        return state.is(AllNuclearBlocks.REINFORCED_TURBINE_GLASS.get());
    }

    public static boolean isAnyController(BlockState state) {
        for (TurbineTier tier : values()) {
            if (tier.isController(state)) {
                return true;
            }
        }
        return false;
    }
}
