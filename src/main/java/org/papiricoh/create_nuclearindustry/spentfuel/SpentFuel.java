package org.papiricoh.create_nuclearindustry.spentfuel;

import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;

/**
 * Reads and updates the cooling history of spent assemblies. Grades are worked out from the
 * discharge day and the game time whenever an assembly is looked at (in a player's inventory, a
 * rack, a cask), so nothing ticks while fuel sits in a chest: it simply catches up when next seen.
 */
public final class SpentFuel {
    public static final long TICKS_PER_DAY = 24000L;

    private SpentFuel() {}

    /** Spent uranium, MOX or thorium assemblies: the items that carry a cooling history. */
    public static boolean isSpentFuel(ItemStack stack) {
        return stack.is(AllNuclearItems.DEPLETED_URANIUM_REACTOR_FUEL.get()) || stack.is(AllNuclearItems.DEPLETED_MOX_REACTOR_FUEL.get())
                || stack.is(org.papiricoh.create_nuclearindustry.fuelcycle.FuelCycleRegistry.DEPLETED_THORIUM_REACTOR_FUEL.get());
    }

    public static long day(long gameTime) {
        return Math.floorDiv(gameTime, TICKS_PER_DAY);
    }

    @Nullable
    public static SpentFuelData data(ItemStack stack) {
        return stack.get(SpentFuelRegistry.SPENT_FUEL.get());
    }

    /** The stored grade; spent fuel without a history (from before cooling existed) counts as {@link CoolingGrade#WARM}. */
    public static CoolingGrade gradeOf(ItemStack stack) {
        SpentFuelData data = data(stack);
        return data == null ? CoolingGrade.WARM : data.grade();
    }

    /** Fresh out of the core: {@link CoolingGrade#HOT}, dated today. */
    public static void stampDischarged(ItemStack stack, long gameTime) {
        stack.set(SpentFuelRegistry.SPENT_FUEL.get(), new SpentFuelData(day(gameTime), CoolingGrade.HOT));
    }

    /** Days on the decay curve since discharge, after the configured time scale. */
    public static double scaledDays(SpentFuelData data, long gameTime) {
        return Math.max(0.0, gameTime / (double) TICKS_PER_DAY - data.dischargeDay()) * SpentFuelConfig.timeScale;
    }

    /**
     * Brings the stored grade up to date. Fuel without a date (from before cooling existed, or shown
     * by a recipe viewer) is dated back to the start of its grade, Warm if it has none, and goes on
     * cooling from there. Grades never go back. Returns whether the stack changed.
     */
    public static boolean refresh(ItemStack stack, long gameTime) {
        if (stack.isEmpty() || !isSpentFuel(stack)) {
            return false;
        }
        SpentFuelData data = data(stack);
        if (data == null) {
            data = SpentFuelData.undated(CoolingGrade.WARM);
        }
        if (!data.dated()) {
            long age = (long) Math.ceil(SpentFuelDecay.daysUntil(data.grade()) / SpentFuelConfig.timeScale);
            data = new SpentFuelData(day(gameTime) - age, data.grade());
        }
        SpentFuelData updated = data.withGrade(gradeAt(data, gameTime));
        if (updated.equals(stack.get(SpentFuelRegistry.SPENT_FUEL.get()))) {
            return false;
        }
        stack.set(SpentFuelRegistry.SPENT_FUEL.get(), updated);
        return true;
    }

    /** The grade {@code stack} has at {@code gameTime}, without touching it. */
    public static CoolingGrade currentGrade(ItemStack stack, long gameTime) {
        SpentFuelData data = data(stack);
        if (data == null) {
            return CoolingGrade.WARM;
        }
        return data.dated() ? gradeAt(data, gameTime) : data.grade();
    }

    private static CoolingGrade gradeAt(SpentFuelData data, long gameTime) {
        return CoolingGrade.cooler(data.grade(), SpentFuelDecay.gradeAfter(scaledDays(data, gameTime)));
    }

    /** Decay heat of the whole stack in a rack, in kW; undated fuel is taken at the start of its grade. */
    public static double decayHeat(ItemStack stack, long gameTime) {
        if (stack.isEmpty() || !isSpentFuel(stack)) {
            return 0.0;
        }
        SpentFuelData data = data(stack);
        double days = data == null ? SpentFuelDecay.daysUntil(CoolingGrade.WARM)
                : data.dated() ? scaledDays(data, gameTime)
                : SpentFuelDecay.daysUntil(data.grade());
        return SpentFuelConfig.assemblyDecayHeat * SpentFuelDecay.heatFraction(days) * stack.getCount();
    }

    /** Dose multiplier of one assembly of {@code grade}, on top of its radioactivity data map entry. */
    public static double doseScale(CoolingGrade grade) {
        return switch (grade) {
            case HOT -> SpentFuelConfig.hotDoseScale;
            case WARM -> SpentFuelConfig.warmDoseScale;
            case COOLED -> SpentFuelConfig.cooledDoseScale;
            case AGED -> SpentFuelConfig.agedDoseScale;
        };
    }

    /** Decay heat that burns whoever carries one assembly of {@code grade}: only fresh fuel is that hot. */
    public static double burnHeat(CoolingGrade grade) {
        return grade == CoolingGrade.HOT ? SpentFuelConfig.hotBurnHeat : 0.0;
    }
}
