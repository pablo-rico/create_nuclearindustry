package org.papiricoh.create_nuclearindustry.missile.silo;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockRegistry;

import java.util.List;

/**
 * The fluid side of a silo's fuel port: a wall block pipes connect to. What is pumped in fills the rocket's tanks from the bottom
 * up, rocket fuel and liquid oxygen each into its own side; what is pulled out drains them from the top down. Nothing moves while
 * the silo is armed or counting down. It holds nothing itself: every call reads the rocket standing in the silo now.
 */
public final class SiloFuelPort implements IFluidHandler {
    private final SiloControllerEntity silo;

    private SiloFuelPort(SiloControllerEntity silo) {
        this.silo = silo;
    }

    /** The handler of the port at {@code pos}, or null while it is not part of a silo. */
    @Nullable
    public static IFluidHandler at(Level level, BlockPos pos) {
        for (SiloControllerEntity silo : MultiblockRegistry.affecting(level, pos, SiloControllerEntity.class)) {
            if (!silo.isRemoved() && silo.isWall(pos)) return new SiloFuelPort(silo);
        }
        return null;
    }

    private static boolean isFuel(Fluid fluid) {
        return fluid == AllNuclearFluids.ROCKET_FUEL.get();
    }

    private static boolean isOxidizer(Fluid fluid) {
        return fluid == AllNuclearFluids.LIQUID_OXYGEN.get();
    }

    @Nullable
    private static FluidTank side(RocketSectionEntity tank, Fluid fluid) {
        return isFuel(fluid) ? tank.fuel() : isOxidizer(fluid) ? tank.oxidizer() : null;
    }

    private Fluid fluidOf(int tank) {
        return tank == 0 ? AllNuclearFluids.ROCKET_FUEL.get() : AllNuclearFluids.LIQUID_OXYGEN.get();
    }

    @Override
    public int getTanks() {
        return 2;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        int amount = 0;
        for (RocketSectionEntity section : silo.columnTanks()) {
            FluidTank side = side(section, fluidOf(tank));
            if (side != null) amount += side.getFluidAmount();
        }
        return amount <= 0 ? FluidStack.EMPTY : new FluidStack(fluidOf(tank), amount);
    }

    @Override
    public int getTankCapacity(int tank) {
        int capacity = 0;
        for (RocketSectionEntity section : silo.columnTanks()) {
            FluidTank side = side(section, fluidOf(tank));
            if (side != null) capacity += side.getCapacity();
        }
        return capacity;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return stack.getFluid() == fluidOf(tank);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !silo.acceptsPropellant()) return 0;
        int filled = 0;
        for (RocketSectionEntity section : silo.columnTanks()) {
            FluidTank side = side(section, resource.getFluid());
            if (side == null) return 0;
            filled += side.fill(resource.copyWithAmount(resource.getAmount() - filled), action);
            if (filled >= resource.getAmount()) break;
        }
        return filled;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !silo.acceptsPropellant()) return FluidStack.EMPTY;
        List<RocketSectionEntity> tanks = silo.columnTanks();
        int drained = 0;
        for (int i = tanks.size() - 1; i >= 0 && drained < resource.getAmount(); i--) {
            FluidTank side = side(tanks.get(i), resource.getFluid());
            if (side == null) return FluidStack.EMPTY;
            drained += side.drain(resource.getAmount() - drained, action).getAmount();
        }
        return drained <= 0 ? FluidStack.EMPTY : resource.copyWithAmount(drained);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        for (int tank = 0; tank < 2; tank++) {
            FluidStack held = getFluidInTank(tank);
            if (!held.isEmpty()) return drain(held.copyWithAmount(Math.min(maxDrain, held.getAmount())), action);
        }
        return FluidStack.EMPTY;
    }
}
