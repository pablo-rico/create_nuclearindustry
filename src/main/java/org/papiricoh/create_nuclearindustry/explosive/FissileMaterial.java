package org.papiricoh.create_nuclearindustry.explosive;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.enrichment.item.UraniumItem;

/**
 * What a warhead can be loaded with: uranium at weapons grade (90% or more) or plutonium.
 * Plutonium has a smaller critical mass, so each piece counts as two units of 90% uranium:
 * a slot of 8 plutonium is worth a slot of 16 uranium.
 */
public final class FissileMaterial {
    public static final int PLUTONIUM_UNIT_WEIGHT = 2;
    public static final float PLUTONIUM_EFFECTIVE_ENRICHMENT = WarheadStats.REQUIRED_ENRICHMENT;

    private FissileMaterial() {}

    public static boolean isWeaponsFissile(ItemStack stack) {
        return isPlutonium(stack)
                || (stack.is(AllNuclearItems.URANIUM.get()) && UraniumItem.getEnrichment(stack) >= WarheadStats.REQUIRED_ENRICHMENT);
    }

    public static boolean isPlutonium(ItemStack stack) {
        return stack.is(AllNuclearItems.PLUTONIUM.get());
    }

    /** Warhead units in a stack, or 0 if it is not weapons material. */
    public static int units(ItemStack stack) {
        if (!isWeaponsFissile(stack)) {
            return 0;
        }
        return stack.getCount() * (isPlutonium(stack) ? PLUTONIUM_UNIT_WEIGHT : 1);
    }

    public static float effectiveEnrichment(ItemStack stack) {
        return isPlutonium(stack) ? PLUTONIUM_EFFECTIVE_ENRICHMENT : UraniumItem.getEnrichment(stack);
    }

    /** Warhead units in slots {@code [from, to)}. */
    public static int units(IItemHandler handler, int from, int to) {
        int units = 0;
        for (int slot = from; slot < to; slot++) {
            units += units(handler.getStackInSlot(slot));
        }
        return units;
    }

    /** Unit-weighted enrichment of the weapons material in slots {@code [from, to)}. */
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
