package org.papiricoh.create_nuclearindustry.controlroom.desk;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;

import java.util.List;

/** A control module waiting to be fitted into a control desk slot. */
public class ControlModuleItem extends Item {
    private final ModuleType type;

    public ControlModuleItem(Properties properties, ModuleType type) {
        super(properties);
        this.type = type;
    }

    public ModuleType type() {
        return type;
    }

    public static ItemStack stackOf(ModuleType type) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, type.itemId())));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("create_nuclearindustry.control_module.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
