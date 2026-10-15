package org.papiricoh.create_nuclearindustry.isotope;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Unit;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;

/**
 * Food sterilised by gamma rays: the bacteria and parasites that make raw chicken, rotten flesh or
 * a spider eye harmful are dead, so it keeps its nourishment and loses its harmful effects. The
 * food itself does not become radioactive. Marked with the {@code irradiated} component.
 */
public final class SterileFood {
    private static final String KEY = "item.create_nuclearindustry.sterile_food";

    private SterileFood() {}

    /** Food with a harmful effect that has not been sterilised yet. */
    public static boolean sterilisable(ItemStack stack) {
        FoodProperties food = stack.get(DataComponents.FOOD);
        return food != null && !sterile(stack) && food.effects().stream()
                .anyMatch(effect -> effect.effect().getEffect().value().getCategory() == MobEffectCategory.HARMFUL);
    }

    public static boolean sterile(ItemStack stack) {
        return stack.has(IsotopeRegistry.IRRADIATED.get());
    }

    /** A sterilised copy of {@code stack}: the same food without its harmful effects. */
    public static ItemStack sterilise(ItemStack stack) {
        ItemStack copy = stack.copy();
        FoodProperties food = copy.get(DataComponents.FOOD);
        if (food == null) {
            return copy;
        }
        List<FoodProperties.PossibleEffect> kept = food.effects().stream()
                .filter(effect -> effect.effect().getEffect().value().getCategory() != MobEffectCategory.HARMFUL).toList();
        copy.set(DataComponents.FOOD, new FoodProperties(food.nutrition(), food.saturation(), food.canAlwaysEat(), food.eatSeconds(),
                food.usingConvertsTo(), kept));
        copy.set(IsotopeRegistry.IRRADIATED.get(), Unit.INSTANCE);
        return copy;
    }

    /** Says so on the tooltip. */
    public static void onTooltip(ItemTooltipEvent event) {
        if (sterile(event.getItemStack())) {
            event.getToolTip().add(Component.translatable(KEY).withStyle(ChatFormatting.AQUA));
        }
    }
}
