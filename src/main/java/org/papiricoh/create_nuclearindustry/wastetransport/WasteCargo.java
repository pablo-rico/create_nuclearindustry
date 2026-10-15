package org.papiricoh.create_nuclearindustry.wastetransport;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.IItemHandler;
import org.papiricoh.create_nuclearindustry.radiation.RadiationHandler;
import org.papiricoh.create_nuclearindustry.spentfuel.CoolingGrade;
import org.papiricoh.create_nuclearindustry.spentfuel.SpentFuel;

/** What the waste containers accept and how active their contents are. */
public final class WasteCargo {

    private WasteCargo() {}

    /** Whether a cistern takes {@code stack}: only fluids in {@code radioactive_waste_fluids}. */
    public static boolean acceptsFluid(FluidStack stack) {
        return !stack.isEmpty() && stack.is(WasteTransportTags.RADIOACTIVE_WASTE_FLUIDS);
    }

    /**
     * Whether a cask takes {@code stack}: items in {@code waste_cask_cargo}, but never Hot spent
     * fuel, whose decay heat a sealed cask cannot carry away. {@code gameTime} dates the fuel; below 0
     * the stored grade is used.
     */
    public static boolean acceptsItem(ItemStack stack, long gameTime) {
        if (stack.isEmpty() || !stack.is(WasteTransportTags.WASTE_CASK_CARGO)) {
            return false;
        }
        if (SpentFuel.isSpentFuel(stack)) {
            CoolingGrade grade = gameTime >= 0 ? SpentFuel.currentGrade(stack, gameTime) : SpentFuel.gradeOf(stack);
            return grade != CoolingGrade.HOT;
        }
        return true;
    }

    /** Bare activity of everything in {@code handler}, in rad/s. */
    public static double activity(IItemHandler handler) {
        double activity = 0;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            activity += RadiationHandler.stackRate(handler.getStackInSlot(slot));
        }
        return activity;
    }

    /** Bare activity of a fluid, in rad/s. */
    public static double activity(FluidStack stack) {
        return FluidRadioactivity.of(stack);
    }
}
