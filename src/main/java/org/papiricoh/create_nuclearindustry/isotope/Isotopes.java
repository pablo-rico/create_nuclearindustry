package org.papiricoh.create_nuclearindustry.isotope;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.AllNuclearItems;
import org.papiricoh.create_nuclearindustry.isotope.physics.AmericiumIngrowth;
import org.papiricoh.create_nuclearindustry.isotope.physics.Isotope;
import org.papiricoh.create_nuclearindustry.isotope.physics.IsotopeDecay;

import java.util.function.LongSupplier;

/**
 * Reads and updates the decay clocks of isotope items. The activity is worked out from the
 * {@link IsotopeData} date whenever an item is looked at (a player's inventory every second, a
 * machine using it), so nothing ticks while an isotope sits in a chest: it simply catches up when
 * next seen. Plutonium carries the same clock for the americium-241 its plutonium-241 grows.
 */
public final class Isotopes {
    private static final int REFRESH_INTERVAL = 20;
    /** The client level's game time, or -1 without one; set by the client so tooltips stay live. */
    public static LongSupplier clientClock = () -> -1;

    private Isotopes() {}

    /** The isotope {@code stack} is made of, or {@code null}. */
    @Nullable
    public static Isotope isotopeOf(ItemStack stack) {
        return stack.getItem() instanceof IsotopeItem item ? item.isotope() : null;
    }

    /** Items that carry an {@link IsotopeData} clock: isotopes, and plutonium for its americium. */
    public static boolean dated(ItemStack stack) {
        return isotopeOf(stack) != null || isPlutonium(stack);
    }

    public static boolean isPlutonium(ItemStack stack) {
        return stack.is(AllNuclearItems.PLUTONIUM.get());
    }

    @Nullable
    public static IsotopeData data(ItemStack stack) {
        return stack.get(IsotopeRegistry.ISOTOPE.get());
    }

    /** The relative activity of one item of {@code stack} at {@code gameTime}; undated items are fresh. */
    public static double activity(ItemStack stack, long gameTime) {
        Isotope isotope = isotopeOf(stack);
        if (isotope == null) {
            return 0.0;
        }
        IsotopeData data = data(stack);
        if (data == null) {
            return 1.0;
        }
        if (!data.dated()) {
            return data.initialActivity();
        }
        return IsotopeDecay.activityAt(data.initialActivity(), IsotopeConfig.halfLifeDays(isotope), data.createdTick(), gameTime,
                IsotopeConfig.timeScale);
    }

    /** The activity as last refreshed: what tooltips, the client and dose without a clock see. */
    public static double storedActivity(ItemStack stack) {
        IsotopeData data = data(stack);
        return data == null ? 1.0 : data.percent() / 100.0;
    }

    /** Whether one item of {@code stack} is still strong enough to use. */
    public static boolean usable(ItemStack stack, long gameTime) {
        Isotope isotope = isotopeOf(stack);
        return isotope != null && IsotopeDecay.usable(activity(stack, gameTime), isotope.usableAbove());
    }

    /** Dates {@code stack} now at {@code activity}, rounding the date so items made close together stack. */
    public static ItemStack stamp(ItemStack stack, long gameTime, double activity) {
        Isotope isotope = isotopeOf(stack);
        double halfLife = isotope == null ? IsotopeConfig.americium().ingrowthHalfLifeDays() : IsotopeConfig.halfLifeDays(isotope);
        long tick = IsotopeDecay.stampTick(gameTime, IsotopeDecay.quantumTicks(halfLife, IsotopeConfig.timeScale));
        float initial = (float) Math.max(0.0, activity);
        stack.set(IsotopeRegistry.ISOTOPE.get(), new IsotopeData(tick, initial, isotope == null ? 0 : IsotopeDecay.percent(initial)));
        return stack;
    }

    /** A fresh stack of {@code item} dated now. */
    public static ItemStack fresh(Item item, int count, long gameTime, double activity) {
        return stamp(new ItemStack(item, count), gameTime, activity);
    }

    /**
     * Brings the stored percent of {@code stack} up to date, dating undated items at their stored
     * activity first. Returns whether the stack changed.
     */
    public static boolean refresh(ItemStack stack, long gameTime) {
        if (stack.isEmpty() || !dated(stack)) {
            return false;
        }
        IsotopeData data = data(stack);
        if (data == null || !data.dated()) {
            stamp(stack, gameTime, data == null ? (isPlutonium(stack) ? 0.0 : 1.0) : data.initialActivity());
            return true;
        }
        int percent = isPlutonium(stack) ? (int) Math.floor(americiumShare(stack, gameTime) * 100)
                : IsotopeDecay.percent(activity(stack, gameTime));
        if (percent == data.percent()) {
            return false;
        }
        stack.set(IsotopeRegistry.ISOTOPE.get(), data.withPercent(percent));
        return true;
    }

    /** Share of americium-241 a plutonium stack has grown since it was dated; undated plutonium has none. */
    public static double americiumShare(ItemStack stack, long gameTime) {
        IsotopeData data = data(stack);
        if (data == null || !data.dated()) {
            return 0.0;
        }
        double days = IsotopeDecay.scaledDays(data.createdTick(), gameTime, IsotopeConfig.timeScale);
        return AmericiumIngrowth.ingrown(IsotopeConfig.americium(), days);
    }

    /** The overworld's game time on the server, or -1 off it (recipe viewers, the client). */
    public static long serverTime() {
        var server = ServerLifecycleHooks.getCurrentServer();
        return server == null ? -1 : server.overworld().getGameTime();
    }

    /** Dose multiplier of one item: its activity as last refreshed, so a decayed source reads weak on the Geiger. */
    public static double doseScale(ItemStack stack) {
        return isotopeOf(stack) == null ? 1.0 : storedActivity(stack);
    }

    /** Carried isotopes decay too: once a second their activities catch up. */
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide || player.tickCount % REFRESH_INTERVAL != 0) {
            return;
        }
        refreshInventory(player.getInventory(), player.level().getGameTime());
    }

    /** Brings every dated item in {@code inventory} up to date. Public so tests can drive it. */
    public static void refreshInventory(Inventory inventory, long gameTime) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (refresh(inventory.getItem(slot), gameTime)) {
                inventory.setChanged();
            }
        }
    }

    /** Game time for a tooltip: the client's clock, else the server's; -1 when neither is known. */
    public static long tooltipTime() {
        long client = clientClock.getAsLong();
        return client >= 0 ? client : serverTime();
    }

    /** The game time of {@code level}, or of the server if it is gone. */
    public static long time(@Nullable Level level) {
        return level != null ? level.getGameTime() : Math.max(0, serverTime());
    }
}
