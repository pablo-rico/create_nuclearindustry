package org.papiricoh.create_nuclearindustry.integration.cbc;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import org.papiricoh.create_nuclearindustry.AllNuclearDataComponents;
import org.papiricoh.create_nuclearindustry.explosive.WarheadLoad;
import org.papiricoh.create_nuclearindustry.explosive.WarheadStats;
import rbasamoyai.createbigcannons.munitions.big_cannon.ProjectileBlockItem;

import java.util.List;

/** The nuclear cannon shell's item: says how many cores its warhead holds and how far it reaches. */
public class NuclearCannonShellItem extends ProjectileBlockItem {
    public NuclearCannonShellItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        WarheadLoad load = stack.getOrDefault(AllNuclearDataComponents.WARHEAD_LOAD.get(), WarheadLoad.FULL);
        tooltip.add(Component.translatable("item.create_nuclearindustry.nuclear_cannon_shell.cores", load.cores(),
                WarheadStats.horizontalRadius(load.units(), load.enrichment())).withStyle(ChatFormatting.GOLD));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
