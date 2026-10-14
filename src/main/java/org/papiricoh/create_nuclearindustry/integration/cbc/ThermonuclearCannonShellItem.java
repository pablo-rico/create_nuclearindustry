package org.papiricoh.create_nuclearindustry.integration.cbc;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import org.papiricoh.create_nuclearindustry.explosive.ExplosiveConfig;
import org.papiricoh.create_nuclearindustry.explosive.ExplosiveRegistry;
import org.papiricoh.create_nuclearindustry.explosive.ThermonuclearLoad;
import rbasamoyai.createbigcannons.munitions.big_cannon.ProjectileBlockItem;

import java.util.List;

/** The thermonuclear cannon shell's item: says what its secondary and tamper hold and how far it reaches. */
public class ThermonuclearCannonShellItem extends ProjectileBlockItem {
    public ThermonuclearCannonShellItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        ThermonuclearLoad load = stack.getOrDefault(ExplosiveRegistry.THERMONUCLEAR_LOAD.get(), ThermonuclearLoad.SHELL_FULL);
        tooltip.add(Component.translatable("item.create_nuclearindustry.thermonuclear_cannon_shell.load", load.secondary(), load.tamper(),
                load.stats(ExplosiveConfig.thermonuclearCaps()).radius()).withStyle(ChatFormatting.GOLD));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
