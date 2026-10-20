package org.papiricoh.create_nuclearindustry.missile.silo;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.missile.flight.Warhead;

/**
 * The slots of a rocket section and what may go in them, the same on server and client so the console's slots refuse what
 * the section would. A warhead takes a charge (cores, radiological material or TNT), a thermonuclear secondary and tamper,
 * or one whole bomb; a tank takes a propellant cartridge and a guidance module a precision mechanism or star tracker, both
 * in slot 0.
 */
public final class WarheadBay {
    public static final int SLOTS = 10;
    /** Cores ×2 each, radiological material ×64 each, or TNT ×16 each. */
    public static final int[] CHARGE = {0, 1, 2};
    /** Lithium deuteride ×4 each: 16, a full secondary. */
    public static final int[] SECONDARY = {3, 4, 5, 6};
    /** Tampers ×2 each: 4, a full tamper. */
    public static final int[] TAMPER = {7, 8};
    /** One nuclear, dirty or thermonuclear bomb, with what it holds. */
    public static final int BOMB = 9;

    /** What a slot is for; its ordinal picks the slot's ghost icon. */
    public enum SlotType { CHARGE, SECONDARY, TAMPER, BOMB, PROPELLANT, GUIDANCE }

    private WarheadBay() {
    }

    /** How many of the slots {@code section} uses, from slot 0. */
    public static int activeSlots(RocketComposition.Section section) {
        return switch (section) {
            case ENGINE -> 0;
            case TANK, GUIDANCE -> 1;
            case WARHEAD -> SLOTS;
        };
    }

    /** What slot {@code slot} of {@code section} is for, or null if the section does not use it. */
    public static @Nullable SlotType type(RocketComposition.Section section, int slot) {
        if (slot < 0 || slot >= activeSlots(section)) return null;
        return switch (section) {
            case TANK -> SlotType.PROPELLANT;
            case GUIDANCE -> SlotType.GUIDANCE;
            case ENGINE -> null;
            case WARHEAD -> slot == BOMB ? SlotType.BOMB : slot >= TAMPER[0] ? SlotType.TAMPER : slot >= SECONDARY[0] ? SlotType.SECONDARY : SlotType.CHARGE;
        };
    }

    /** Whether {@code stack} may go in slot {@code slot} of {@code section}, whatever else it holds. */
    public static boolean accepts(RocketComposition.Section section, int slot, ItemStack stack) {
        return false;
    }

    /** How many of {@code stack} slot {@code slot} of {@code section} holds. */
    public static int limit(RocketComposition.Section section, int slot, ItemStack stack) {
        return 0;
    }

    /**
     * Why {@code stack} cannot go in slot {@code slot} of {@code section}, as a lang suffix
     * ({@code silo.create_nuclearindustry.refuse.*}), or null if it can.
     */
    public static @Nullable String refusal(RocketComposition.Section section, int slot, ItemStack stack) {
        return null;
    }

    /** Why a warhead holding {@code contents} cannot go off as loaded ({@code charge_*} error suffix), or null if it can. */
    public static @Nullable String validate(IItemHandler contents) {
        return null;
    }

    /** The warhead a warhead section holding {@code contents} carries. */
    public static Warhead warhead(IItemHandler contents) {
        return Warhead.INERT;
    }

    /** The warhead a bomb item (with what it holds) makes. */
    public static Warhead fromBomb(ItemStack bomb) {
        return Warhead.INERT;
    }

    /** The payload's mass, rocket units: 1 for the empty section, and what it holds. */
    public static double payloadMass(IItemHandler contents) {
        return 1;
    }
}
