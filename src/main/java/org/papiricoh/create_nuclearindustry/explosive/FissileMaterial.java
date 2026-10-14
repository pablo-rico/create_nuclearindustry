package org.papiricoh.create_nuclearindustry.explosive;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import org.papiricoh.create_nuclearindustry.AllNuclearDataComponents;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.AllNuclearTags;
import org.papiricoh.create_nuclearindustry.enrichment.item.UraniumItem;

/**
 * What a warhead is loaded with: weapons-grade cores, each worth {@link WarheadLoad#UNITS_PER_CORE}
 * units, exactly the metal that went into it (eight uranium at 90% or more, or four plutonium, whose
 * smaller critical mass makes each piece count twice). Loose weapons-grade metal has to be cast into a
 * core first; it still counts as weapons material, so a dirty bomb does not take it either.
 */
public final class FissileMaterial {
    public static final int PLUTONIUM_UNIT_WEIGHT = 2;
    public static final float PLUTONIUM_EFFECTIVE_ENRICHMENT = WarheadStats.REQUIRED_ENRICHMENT;

    private FissileMaterial() {}

    /** What a nuclear warhead takes: a weapons-grade core. */
    public static boolean isWeaponsFissile(ItemStack stack) {
        return isWarheadCore(stack);
    }

    public static boolean isWarheadCore(ItemStack stack) {
        return !stack.isEmpty() && stack.is(AllNuclearTags.Items.NUCLEAR_WARHEAD_CORES);
    }

    /** Uranium at weapons grade or plutonium, before it is cast into a core. */
    public static boolean isWeaponsMetal(ItemStack stack) {
        return isPlutonium(stack)
                || (stack.is(AllNuclearItems.URANIUM.get()) && UraniumItem.getEnrichment(stack) >= WarheadStats.REQUIRED_ENRICHMENT);
    }

    /** A core or the metal for one: never a dirty bomb's load. */
    public static boolean isWeaponsMaterial(ItemStack stack) {
        return isWarheadCore(stack) || isWeaponsMetal(stack);
    }

    public static boolean isPlutonium(ItemStack stack) {
        return stack.is(AllNuclearItems.PLUTONIUM.get());
    }

    /** Warhead units in a stack of cores, or 0 if it is not one. */
    public static int units(ItemStack stack) {
        return isWarheadCore(stack) ? stack.getCount() * WarheadLoad.UNITS_PER_CORE : 0;
    }

    /**
     * The enrichment a core counts at: a uranium core keeps the leanest enrichment of the metal cast into
     * it, a plutonium one counts as weapons grade.
     */
    public static float effectiveEnrichment(ItemStack stack) {
        if (stack.is(AllNuclearItems.WEAPONS_GRADE_URANIUM_CORE.get())) {
            return stack.getOrDefault(AllNuclearDataComponents.ENRICHMENT.get(), WarheadStats.REQUIRED_ENRICHMENT);
        }
        if (isPlutonium(stack)) {
            return PLUTONIUM_EFFECTIVE_ENRICHMENT;
        }
        if (stack.is(AllNuclearItems.URANIUM.get())) {
            return UraniumItem.getEnrichment(stack);
        }
        return WarheadStats.REQUIRED_ENRICHMENT;
    }

    /** Warhead units in slots {@code [from, to)}. */
    public static int units(IItemHandler handler, int from, int to) {
        int units = 0;
        for (int slot = from; slot < to; slot++) {
            units += units(handler.getStackInSlot(slot));
        }
        return units;
    }

    /** Unit-weighted enrichment of the cores in slots {@code [from, to)}. */
    public static float averageEnrichment(IItemHandler handler, int from, int to) {
        int units = 0;
        float total = 0;
        for (int slot = from; slot < to; slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            int stackUnits = units(stack);
            units += stackUnits;
            total += effectiveEnrichment(stack) * stackUnits;
        }
        return units == 0 ? 0 : total / units;
    }
}
