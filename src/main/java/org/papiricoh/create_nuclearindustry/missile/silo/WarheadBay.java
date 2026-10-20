package org.papiricoh.create_nuclearindustry.missile.silo;

import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.papiricoh.create_nuclearindustry.AllNuclearBlocks;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.AllNuclearTags;
import org.papiricoh.create_nuclearindustry.explosive.*;
import org.papiricoh.create_nuclearindustry.explosive.blockentity.*;
import org.papiricoh.create_nuclearindustry.missile.MissileConfig;
import org.papiricoh.create_nuclearindustry.missile.physics.ConventionalYield;
import org.papiricoh.create_nuclearindustry.missile.physics.GuidanceTier;
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
    public enum SlotType { CHARGE, SECONDARY, TAMPER, BOMB, PROPELLANT, GUIDANCE, SATELLITE }

    private WarheadBay() {
    }

    /** How many of the slots {@code section} uses, from slot 0. */
    public static int activeSlots(RocketComposition.Section section) {
        return switch (section) {
            case ENGINE, KILL_VEHICLE -> 0;
            case TANK, GUIDANCE, SATELLITE -> 1;
            case WARHEAD -> SLOTS;
        };
    }

    /** What slot {@code slot} of {@code section} is for, or null if the section does not use it. */
    public static @Nullable SlotType type(RocketComposition.Section section, int slot) {
        if (slot < 0 || slot >= activeSlots(section)) return null;
        return switch (section) {
            case TANK -> SlotType.PROPELLANT;
            case GUIDANCE -> SlotType.GUIDANCE;
            case SATELLITE -> SlotType.SATELLITE;
            case ENGINE, KILL_VEHICLE -> null;
            case WARHEAD -> slot == BOMB ? SlotType.BOMB : slot >= TAMPER[0] ? SlotType.TAMPER : slot >= SECONDARY[0] ? SlotType.SECONDARY : SlotType.CHARGE;
        };
    }

    /** Whether {@code stack} may go in slot {@code slot} of {@code section}, whatever else it holds. */
    public static boolean accepts(RocketComposition.Section section, int slot, ItemStack stack) {
        if (stack.isEmpty()) return false;
        SlotType type = type(section, slot);
        if (type == null) return false;
        return switch (type) {
            case CHARGE -> FissileMaterial.isWarheadCore(stack) || DirtyWarheadStats.isRadiological(stack)
                    || stack.is(SiloRegistry.CONVENTIONAL_EXPLOSIVES);
            case SECONDARY -> stack.is(ExplosiveRegistry.LITHIUM_DEUTERIDE.get());
            case TAMPER -> stack.is(AllNuclearTags.Items.THERMONUCLEAR_TAMPERS);
            case BOMB -> isBomb(stack);
            case PROPELLANT -> stack.is(SiloRegistry.PROPELLANT.get());
            case GUIDANCE -> RocketSectionEntity.tierOf(stack) != GuidanceTier.BASIC;
            case SATELLITE -> stack.getItem() instanceof org.papiricoh.create_nuclearindustry.missile.orbit.SatelliteItem;
        };
    }

    private static boolean isBomb(ItemStack stack) {
        return stack.is(AllNuclearBlocks.NUCLEAR_BOMB.get().asItem()) || stack.is(AllNuclearBlocks.DIRTY_BOMB.get().asItem())
                || stack.is(ExplosiveRegistry.THERMONUCLEAR_BOMB_ITEM.get());
    }

    /** How many of {@code stack} slot {@code slot} of {@code section} holds. */
    public static int limit(RocketComposition.Section section, int slot, ItemStack stack) {
        if (!accepts(section, slot, stack)) return 0;
        return switch (type(section, slot)) {
            case CHARGE -> FissileMaterial.isWarheadCore(stack) ? NuclearBombBlockEntity.CORES_PER_SLOT
                    : stack.is(SiloRegistry.CONVENTIONAL_EXPLOSIVES) ? 16 : 64;
            case SECONDARY -> ThermonuclearStats.SECONDARY_FULL / SECONDARY.length;
            case TAMPER -> ThermonuclearStats.TAMPER_FULL / TAMPER.length;
            default -> 1;
        };
    }

    /**
     * As {@link #limit(RocketComposition.Section, int, ItemStack)}, given what the bay already holds: once it has a secondary
     * or a tamper, its cores are the thermonuclear primary, one core across all the charge slots, as in the bomb.
     */
    public static int limit(RocketComposition.Section section, int slot, ItemStack stack, IItemHandler contents) {
        int limit = limit(section, slot, stack);
        if (limit == 0 || !FissileMaterial.isWarheadCore(stack) || !thermonuclear(contents)) return limit;
        int elsewhere = 0;
        for (int i : CHARGE) if (i != slot && i < contents.getSlots() && FissileMaterial.isWarheadCore(contents.getStackInSlot(i)))
            elsewhere += contents.getStackInSlot(i).getCount();
        return Math.max(0, Math.min(limit, 1 - elsewhere));
    }

    /** Whether {@code contents} holds any lithium deuteride or tamper, making its charge a thermonuclear primary. */
    public static boolean thermonuclear(IItemHandler contents) {
        for (int i = SECONDARY[0]; i <= TAMPER[TAMPER.length - 1] && i < contents.getSlots(); i++)
            if (!contents.getStackInSlot(i).isEmpty()) return true;
        return false;
    }

    /** How many cores the charge slots of {@code contents} hold. */
    public static int cores(IItemHandler contents) {
        int cores = 0;
        for (int i : CHARGE) if (i < contents.getSlots() && FissileMaterial.isWarheadCore(contents.getStackInSlot(i)))
            cores += contents.getStackInSlot(i).getCount();
        return cores;
    }

    /**
     * Why {@code stack} cannot go in slot {@code slot} of {@code section}, as a lang suffix
     * ({@code silo.create_nuclearindustry.refuse.*}), or null if it can.
     */
    public static @Nullable String refusal(RocketComposition.Section section, int slot, ItemStack stack) {
        if (type(section, slot) == null) return "wrong_section";
        return accepts(section, slot, stack) ? null : "not_for_slot";
    }

    /** Why a warhead holding {@code contents} cannot go off as loaded ({@code charge_*} error suffix), or null if it can. */
    public static @Nullable String validate(IItemHandler contents) {
        int cores = 0, tnt = 0, secondary = 0, tamper = 0;
        boolean other = false, radiological = false;
        for (int i = 0; i < Math.min(contents.getSlots(), BOMB); i++) {
            ItemStack stack = contents.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            other = true;
            if (!accepts(RocketComposition.Section.WARHEAD, i, stack)
                    || stack.getCount() > limit(RocketComposition.Section.WARHEAD, i, stack)) return "charge_invalid";
            if (i < SECONDARY[0]) {
                if (FissileMaterial.isWarheadCore(stack)) cores += stack.getCount();
                else if (stack.is(SiloRegistry.CONVENTIONAL_EXPLOSIVES)) tnt += stack.getCount();
                else radiological = true;
            } else if (i < TAMPER[0]) secondary += stack.getCount();
            else tamper += stack.getCount();
        }
        ItemStack bomb = contents.getSlots() > BOMB ? contents.getStackInSlot(BOMB) : ItemStack.EMPTY;
        if (!bomb.isEmpty()) {
            if (other) return "charge_bomb_and_loose";
            if (!isBomb(bomb) || bomb.getCount() != 1) return "charge_invalid";
        }
        if (tnt > 0 && (cores > 0 || radiological || secondary > 0 || tamper > 0)) return "charge_mixed_conventional";
        if ((secondary > 0 || tamper > 0) && (cores != 1 || radiological || tnt > 0)) return "charge_thermo_primary";
        if (tamper > 0 && secondary == 0) return "charge_tamper_alone";
        return null;
    }

    /** The warhead a warhead section holding {@code contents} carries. */
    public static Warhead warhead(IItemHandler contents) {
        if (validate(contents) != null) return Warhead.INERT;
        if (contents.getSlots() > BOMB && !contents.getStackInSlot(BOMB).isEmpty()) return fromBomb(contents.getStackInSlot(BOMB));
        ItemStackHandler primary = new ItemStackHandler(NuclearBombBlockEntity.SLOT_COUNT);
        int tnt = 0, secondary = 0, tamper = 0;
        boolean plutonium = false;
        for (int i : CHARGE) {
            ItemStack stack = contents.getStackInSlot(i);
            primary.setStackInSlot(i, stack);
            if (stack.is(SiloRegistry.CONVENTIONAL_EXPLOSIVES)) tnt += stack.getCount();
            plutonium |= stack.is(AllNuclearItems.WEAPONS_GRADE_PLUTONIUM_CORE.get());
        }
        for (int i : SECONDARY) secondary += contents.getStackInSlot(i).getCount();
        for (int i : TAMPER) tamper += contents.getStackInSlot(i).getCount();
        if (tnt > 0) return Warhead.conventional((float) ConventionalYield.power(tnt, MissileConfig.conventionalMaxPower));
        if (secondary > 0) return Warhead.thermonuclear(new ThermonuclearLoad(plutonium ? ThermonuclearStats.Primary.PLUTONIUM
                : ThermonuclearStats.Primary.URANIUM, secondary, tamper));
        return NuclearBombBlockEntity.charge(primary, false);
    }

    /** The warhead a bomb item (with what it holds) makes. */
    public static Warhead fromBomb(ItemStack bomb) {
        if (!isBomb(bomb)) return Warhead.INERT;
        boolean thermo = bomb.is(ExplosiveRegistry.THERMONUCLEAR_BOMB_ITEM.get());
        ItemStackHandler inventory = new ItemStackHandler(thermo ? ThermonuclearBombBlockEntity.SLOTS : NuclearBombBlockEntity.SLOT_COUNT);
        ItemContainerContents stored = bomb.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        for (int i = 0; i < inventory.getSlots(); i++) inventory.setStackInSlot(i, i < stored.getSlots() ? stored.getStackInSlot(i) : ItemStack.EMPTY);
        return thermo ? Warhead.thermonuclear(ThermonuclearBombBlockEntity.load(inventory))
                : NuclearBombBlockEntity.charge(inventory, bomb.is(AllNuclearBlocks.DIRTY_BOMB.get().asItem()));
    }

    /** The payload's mass, rocket units: 1 for the empty section, and what it holds. */
    public static double payloadMass(IItemHandler contents) {
        double mass = 1;
        for (int i = 0; i < contents.getSlots(); i++) {
            ItemStack stack = contents.getStackInSlot(i);
            mass += stack.getCount() * 0.1;
            if (i == BOMB) for (ItemStack item : stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).nonEmptyItems())
                mass += item.getCount() * 0.1;
        }
        return mass;
    }
}
