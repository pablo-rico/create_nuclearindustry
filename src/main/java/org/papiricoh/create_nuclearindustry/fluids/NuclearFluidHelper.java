package org.papiricoh.create_nuclearindustry.fluids;

import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;

public final class NuclearFluidHelper {
    private NuclearFluidHelper() {}

    public static boolean isWater(FluidStack stack) {
        return stack.getFluid() == Fluids.WATER;
    }

    public static boolean isHeavyWater(FluidStack stack) {
        return stack.getFluid() == AllNuclearFluids.HEAVY_WATER.get();
    }

    public static boolean isCoolant(FluidStack stack) {
        return isWater(stack) || isHeavyWater(stack);
    }

    public static boolean isSteam(FluidStack stack) {
        return stack.getFluid() == AllNuclearFluids.STEAM.get();
    }

    public static boolean isHeavySteam(FluidStack stack) {
        return stack.getFluid() == AllNuclearFluids.HEAVY_STEAM.get();
    }

    public static boolean isTurbineSteam(FluidStack stack) {
        return isSteam(stack) || isHeavySteam(stack);
    }
}
