package org.papiricoh.create_nuclearindustry.missile.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.papiricoh.create_nuclearindustry.explosive.ExplosiveConfig;
import org.papiricoh.create_nuclearindustry.explosive.blast.DetonationProfile;
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
 * Sneak-using it in the air switches between a ground burst and an airburst ({@code AIRBURST}): an
 * airburst goes off above the target, digs no crater and leaves little fallout, but its blast and
 * flash reach further.
 */
public class TargetDesignatorItem extends Item {
    public TargetDesignatorItem(Properties properties) {
        super(properties);
    }

    @Nullable
    public static BlockPos getTarget(ItemStack stack) {
        return stack.get(AllNuclearDataComponents.TARGET_POS.get());
    }

    public static boolean isAirburst(ItemStack stack) {
        return Boolean.TRUE.equals(stack.get(AllNuclearDataComponents.AIRBURST.get()));
    }

    /** How high above its target a warhead of {@code horizontalRadius} set to airburst goes off: clear of its fireball. */
    public static double airburstHeight(int horizontalRadius) {
        double fireball = Math.max(DetonationProfile.MIN_FIREBALL, 0.12 * horizontalRadius);
        return ExplosiveConfig.airburstHeight * fireball;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(stack);
        }
        if (!level.isClientSide) {
            boolean airburst = !isAirburst(stack);
            stack.set(AllNuclearDataComponents.AIRBURST.get(), airburst);
            player.displayClientMessage(Component.translatable("item.create_nuclearindustry.target_designator."
                    + (airburst ? "airburst" : "ground_burst")).withStyle(ChatFormatting.GOLD), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
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
        tooltip.add(Component.translatable("item.create_nuclearindustry.target_designator."
                + (isAirburst(stack) ? "airburst" : "ground_burst")).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("item.create_nuclearindustry.target_designator.mode_hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
