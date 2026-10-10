package org.papiricoh.create_nuclearindustry.spentfuel.client;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import org.papiricoh.create_nuclearindustry.client.NuclearFeatureClient;
import org.papiricoh.create_nuclearindustry.spentfuel.SpentFuel;
import org.papiricoh.create_nuclearindustry.spentfuel.SpentFuelData;
import org.papiricoh.create_nuclearindustry.spentfuel.SpentFuelText;

/** Client setup for spent fuel cooling: pool racks and dry casks. */
public final class SpentFuelClient implements NuclearFeatureClient {
    public static final SpentFuelClient INSTANCE = new SpentFuelClient();

    private SpentFuelClient() {}

    @Override
    public void init(IEventBus modEventBus) {
        NeoForge.EVENT_BUS.addListener(SpentFuelClient::onItemTooltip);
    }

    /** Spent assemblies show how far they have cooled and how long ago they left the core. */
    private static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!SpentFuel.isSpentFuel(stack)) {
            return;
        }
        event.getToolTip().add(Component.translatable(SpentFuelText.PREFIX + "tooltip.grade",
                SpentFuelText.grade(SpentFuel.gradeOf(stack))).withStyle(ChatFormatting.GRAY));
        SpentFuelData data = SpentFuel.data(stack);
        Level level = event.getContext().level();
        if (data != null && data.dated() && level != null) {
            long days = Math.max(0, SpentFuel.day(level.getGameTime()) - data.dischargeDay());
            event.getToolTip().add(Component.translatable(SpentFuelText.PREFIX + "tooltip.days", days).withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
