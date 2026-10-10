package org.papiricoh.create_nuclearindustry.spentfuel;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jetbrains.annotations.Nullable;

/** Where the reactor hands over spent fuel, so the spent fuel area can stamp it. */
public final class SpentFuelHooks {
    private static final int REFRESH_INTERVAL = 20;

    private SpentFuelHooks() {}

    /** Called for each spent assembly as it leaves a reactor core, before it goes to the output slots. */
    public static void onDischarge(ItemStack spent, @Nullable Level level) {
        if (level != null && SpentFuel.isSpentFuel(spent)) {
            SpentFuel.stampDischarged(spent, level.getGameTime());
        }
    }

    /** Carried assemblies cool too: once a second their grades catch up, as in a rack or cask. */
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide || player.tickCount % REFRESH_INTERVAL != 0) {
            return;
        }
        refreshInventory(player.getInventory(), player.level().getGameTime());
    }

    /** Brings every spent assembly in {@code inventory} up to date. Public so tests can drive it. */
    public static void refreshInventory(Inventory inventory, long gameTime) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (SpentFuel.refresh(stack, gameTime)) {
                inventory.setChanged();
            }
        }
    }
}
