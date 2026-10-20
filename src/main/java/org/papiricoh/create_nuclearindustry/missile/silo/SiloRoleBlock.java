package org.papiricoh.create_nuclearindustry.missile.silo;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

/** A concrete block of a launch site with a role of its own, explained in its tooltip when shift is held. */
public class SiloRoleBlock extends Block {
    private final String role;

    public SiloRoleBlock(Properties properties, String role) {
        super(properties);
        this.role = role;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable(Screen.hasShiftDown() ? "item.create_nuclearindustry." + role + ".tooltip"
                : "silo.create_nuclearindustry.screen.tooltip.expand").withStyle(ChatFormatting.GRAY));
    }
}
