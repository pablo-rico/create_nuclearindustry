package org.papiricoh.create_nuclearindustry.radiation;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Supplier;

/** A pill or capsule eaten whatever the hunger, giving one treatment effect, then a short cooldown. */
public class RadiationMedicineItem extends Item {
    private static final int COOLDOWN_TICKS = 100;

    private final String tooltipKey;

    public RadiationMedicineItem(Properties properties, Supplier<Holder<MobEffect>> effect, int durationTicks, String tooltipKey) {
        super(properties.food(new FoodProperties.Builder()
                .alwaysEdible()
                .fast()
                .nutrition(0)
                .saturationModifier(0)
                .effect(() -> new MobEffectInstance(effect.get(), durationTicks), 1.0f)
                .build()));
        this.tooltipKey = tooltipKey;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof Player player) {
            player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
            if (stack.is(org.papiricoh.create_nuclearindustry.AllNuclearItems.PRUSSIAN_BLUE.get())) {
                org.papiricoh.create_nuclearindustry.advancement.NuclearAdvancements.award(player, org.papiricoh.create_nuclearindustry.advancement.NuclearEvent.RADIATION_PURGE);
            }
        }
        return super.finishUsingItem(stack, level, entity);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(tooltipKey).withStyle(ChatFormatting.GRAY));
    }
}
