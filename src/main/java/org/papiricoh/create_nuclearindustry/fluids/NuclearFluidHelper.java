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

    /** Borated water of a pressurized reactor's loop, coming back cold from the steam generators. */
    public static boolean isPrimaryCoolant(FluidStack stack) {
        return stack.getFluid() == AllNuclearFluids.PRIMARY_COOLANT.get();
    }

    /** Primary coolant leaving the core, on its way to the steam generators. */
    public static boolean isHotPrimary(FluidStack stack) {
        return stack.getFluid() == AllNuclearFluids.HOT_PRIMARY_COOLANT.get();
    }

    /** Either side of a pressurized reactor's loop. */
    public static boolean isPrimaryLoopFluid(FluidStack stack) {
        return isPrimaryCoolant(stack) || isHotPrimary(stack);
    }

    /** What a fission reactor's coolant tank holds: water to boil, or the pressurized loop's coolant. */
    public static boolean isReactorCoolant(FluidStack stack) {
        return isCoolant(stack) || isPrimaryCoolant(stack);
    }

    /** What a fission reactor's output tank holds: steam it boiled, or hot primary coolant. */
    public static boolean isReactorOutput(FluidStack stack) {
        return isTurbineSteam(stack) || isHotPrimary(stack);
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

    /** Low-pressure steam from a turbine's exhaust port. */
    public static boolean isExhaustSteam(FluidStack stack) {
        return stack.getFluid() == AllNuclearFluids.EXHAUST_STEAM.get() || stack.getFluid() == AllNuclearFluids.HEAVY_EXHAUST_STEAM.get();
    }

    /** What a cooling tower condenses: reactor steam or turbine exhaust, light or heavy. */
    public static boolean isCondensable(FluidStack stack) {
        return isTurbineSteam(stack) || isExhaustSteam(stack);
    }

    /** Heavy steam or heavy exhaust: condenses to heavy water. */
    public static boolean isHeavyVapour(FluidStack stack) {
        return isHeavySteam(stack) || stack.getFluid() == AllNuclearFluids.HEAVY_EXHAUST_STEAM.get();
    }

    public static boolean isDeuterium(FluidStack stack) {
        return stack.getFluid() == AllNuclearFluids.DEUTERIUM.get();
    }

    public static boolean isTritium(FluidStack stack) {
        return stack.getFluid() == AllNuclearFluids.TRITIUM.get();
    }

    public static boolean isPlasmaSteam(FluidStack stack) {
        return stack.getFluid() == AllNuclearFluids.PLASMA_STEAM.get();
    }

    public static boolean isHelium(FluidStack stack) {
        return stack.getFluid() == AllNuclearFluids.HELIUM.get();
    }

    public static boolean isLiquidHelium(FluidStack stack) {
        return stack.getFluid() == AllNuclearFluids.LIQUID_HELIUM.get();
    }

    public static boolean isLiquidAir(FluidStack stack) {
        return stack.getFluid() == AllNuclearFluids.LIQUID_AIR.get();
    }

    public static boolean isLiquidNitrogen(FluidStack stack) {
        return stack.getFluid() == AllNuclearFluids.LIQUID_NITROGEN.get();
    }

    /** Working fluid accepted by the fusion plasma turbine. */
    public static boolean isPlasmaTurbineFluid(FluidStack stack) {
        return isPlasmaSteam(stack);
    }
}
