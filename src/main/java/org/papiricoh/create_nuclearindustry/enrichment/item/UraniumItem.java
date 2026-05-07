package org.papiricoh.create_nuclearindustry.enrichment.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.papiricoh.create_nuclearindustry.AllNuclearDataComponents;

import java.util.List;

public class UraniumItem extends Item {
    public static final float NATURAL_ENRICHMENT = 0.7f;

    public UraniumItem(Properties properties) {
        super(properties);
    }

    public static float getEnrichment(ItemStack stack) {
        return Math.max(0.0f, stack.getOrDefault(AllNuclearDataComponents.ENRICHMENT.get(), NATURAL_ENRICHMENT));
    }

    public static void setEnrichment(ItemStack stack, float enrichment) {
        stack.set(AllNuclearDataComponents.ENRICHMENT.get(), Math.max(0.0f, Math.min(100.0f, enrichment)));
    }

    public static ItemStack withEnrichment(ItemStack stack, float enrichment) {
        ItemStack copy = stack.copy();
        setEnrichment(copy, enrichment);
        return copy;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag tooltipFlag) {
        tooltip.add(Component.translatable(
                "item.create_nuclearindustry.uranium.enrichment",
                String.format("%.2f", getEnrichment(stack))
        ).withStyle(ChatFormatting.GRAY));
    }
}
