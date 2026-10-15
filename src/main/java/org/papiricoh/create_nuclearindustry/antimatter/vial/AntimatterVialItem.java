package org.papiricoh.create_nuclearindustry.antimatter.vial;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.papiricoh.create_nuclearindustry.AllNuclearDataComponents;
import org.papiricoh.create_nuclearindustry.accelerator.physics.VialBattery;
import org.papiricoh.create_nuclearindustry.antimatter.AntimatterAnnihilation;
import org.papiricoh.create_nuclearindustry.antimatter.AntimatterConfig;

import java.util.List;
import java.util.Locale;

/**
 * The Antimatter Vial: a pocket Penning trap with two micro magnet heads and its own battery
 * ({@link VialBattery}). A trap's vial port fills and empties it and recharges its battery. Away from
 * a powered port the battery of a loaded vial runs down, in a player's inventory or lying on the
 * ground; when it is flat the vial annihilates where it is. Empty vials are harmless and stack to
 * 16; a loaded one does not stack. Vials in containers that do not tick keep their charge.
 */
public class AntimatterVialItem extends Item {
    /** How many empty vials stack. */
    public static final int EMPTY_STACK = 16;
    private static final String K = "item.create_nuclearindustry.antimatter_vial.";

    public AntimatterVialItem(Properties properties) {
        super(properties.stacksTo(EMPTY_STACK));
    }

    // ------------------------------------------------------------------ the vial's data

    /** Antimatter in the vial, ng. */
    public static double antimatter(ItemStack stack) {
        Double ng = stack.get(AllNuclearDataComponents.ANTIMATTER.get());
        return ng == null ? 0 : Math.max(0, ng);
    }

    /** Battery left, ticks; a vial that never left a port has a full one. */
    public static int charge(ItemStack stack) {
        Integer charge = stack.get(AllNuclearDataComponents.VIAL_CHARGE.get());
        return charge == null ? AntimatterConfig.vial().batteryTicks() : charge;
    }

    public static boolean loaded(ItemStack stack) {
        return stack.getItem() instanceof AntimatterVialItem && VialBattery.loaded(antimatter(stack));
    }

    /**
     * Writes the vial's charge and battery. An empty vial carries neither, so it stacks with fresh
     * ones; a loaded one stops stacking.
     */
    public static void set(ItemStack stack, double ng, int charge) {
        if (VialBattery.loaded(ng)) {
            stack.set(AllNuclearDataComponents.ANTIMATTER.get(), ng);
            stack.set(AllNuclearDataComponents.VIAL_CHARGE.get(), Math.max(0, charge));
            stack.set(DataComponents.MAX_STACK_SIZE, 1);
        } else {
            stack.remove(AllNuclearDataComponents.ANTIMATTER.get());
            stack.remove(AllNuclearDataComponents.VIAL_CHARGE.get());
            stack.set(DataComponents.MAX_STACK_SIZE, EMPTY_STACK);
        }
    }

    /** A vial holding {@code ng} with a full battery (creative tab, tests). */
    public ItemStack filled(double ng) {
        ItemStack stack = new ItemStack(this);
        set(stack, Math.min(ng, AntimatterConfig.vial().capacity()), AntimatterConfig.vial().batteryTicks());
        return stack;
    }

    /**
     * One tick of a loaded vial away from a powered port: its battery runs down, and at zero it
     * annihilates at {@code entity}. Returns whether it did (the caller removes the stack).
     */
    public static boolean tickAway(ItemStack stack, ServerLevel level, Entity entity) {
        double ng = antimatter(stack);
        if (!VialBattery.loaded(ng)) {
            return false;
        }
        int charge = VialBattery.tick(AntimatterConfig.vial(), charge(stack), ng, false);
        if (VialBattery.annihilates(charge, ng)) {
            AntimatterAnnihilation.schedule(level, entity.position().add(0, entity.getBbHeight() * 0.5, 0), ng * stack.getCount());
            return true;
        }
        stack.set(AllNuclearDataComponents.VIAL_CHARGE.get(), charge);
        return false;
    }

    // ------------------------------------------------------------------ where it ticks

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level instanceof ServerLevel server && tickAway(stack, server, entity)) {
            stack.setCount(0);
        }
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        if (entity.level() instanceof ServerLevel server && tickAway(stack, server, entity)) {
            entity.discard();
            return true;
        }
        return false;
    }

    /** The battery ticking down must not make the vial bob in the hand every tick. */
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || !newStack.is(oldStack.getItem());
    }

    // ------------------------------------------------------------------ looks

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return loaded(stack);
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0f * (float) VialBattery.chargeShare(AntimatterConfig.vial(), charge(stack)));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        float share = (float) VialBattery.chargeShare(AntimatterConfig.vial(), charge(stack));
        return Mth.hsvToRgb(share / 3.0f, 1.0f, 1.0f);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        VialBattery.Params p = AntimatterConfig.vial();
        double ng = antimatter(stack);
        if (!VialBattery.loaded(ng)) {
            tooltip.add(Component.translatable(K + "empty", fmt(p.capacity())).withStyle(ChatFormatting.GRAY));
            return;
        }
        tooltip.add(Component.translatable(K + "charge", fmt(ng), fmt(p.capacity())).withStyle(ChatFormatting.GREEN));
        int charge = charge(stack);
        tooltip.add(Component.translatable(K + "battery", String.format(Locale.ROOT, "%.1f", VialBattery.secondsLeft(charge)))
                .withStyle(VialBattery.lowBattery(p, charge, ng) ? ChatFormatting.RED : ChatFormatting.YELLOW));
        tooltip.add(Component.translatable(K + "warning").withStyle(ChatFormatting.DARK_RED));
    }

    private static String fmt(double ng) {
        return String.format(Locale.ROOT, "%.2f", ng);
    }
}
