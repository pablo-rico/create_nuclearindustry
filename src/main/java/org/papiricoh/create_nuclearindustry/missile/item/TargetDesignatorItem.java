package org.papiricoh.create_nuclearindustry.missile.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.AllNuclearDataComponents;

import java.util.List;

/**
 * Designador de objetivo: al hacer click derecho sobre un bloque guarda esas coordenadas en el
 * propio stack (via DataComponent TARGET_POS). Ese stack se inserta luego en la plataforma de
 * lanzamiento, que lee el objetivo desde el. Evita necesitar packets cliente->servidor.
 */
public class TargetDesignatorItem extends Item {
    public TargetDesignatorItem(Properties properties) {
        super(properties);
    }

    @Nullable
    public static BlockPos getTarget(ItemStack stack) {
        return stack.get(AllNuclearDataComponents.TARGET_POS.get());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!context.getLevel().isClientSide && context.getPlayer() != null) {
            BlockPos pos = context.getClickedPos();
            context.getItemInHand().set(AllNuclearDataComponents.TARGET_POS.get(), pos);
            context.getPlayer().displayClientMessage(Component.translatable(
                    "item.create_nuclearindustry.target_designator.set",
                    pos.getX(), pos.getY(), pos.getZ()).withStyle(ChatFormatting.GREEN), true);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        BlockPos target = getTarget(stack);
        if (target != null) {
            tooltip.add(Component.translatable("item.create_nuclearindustry.target_designator.target",
                    target.getX(), target.getY(), target.getZ()).withStyle(ChatFormatting.AQUA));
        } else {
            tooltip.add(Component.translatable("item.create_nuclearindustry.target_designator.empty")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
