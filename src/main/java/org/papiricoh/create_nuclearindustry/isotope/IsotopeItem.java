package org.papiricoh.create_nuclearindustry.isotope;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.papiricoh.create_nuclearindustry.isotope.physics.Isotope;
import org.papiricoh.create_nuclearindustry.isotope.physics.IsotopeDecay;

import java.util.List;
import java.util.Locale;

/**
 * A sealed radioisotope source: cobalt-60, strontium-90, americium-241, technetium-99m. It decays
 * with its half-life ({@link Isotopes}); its dose on the Geiger counter and its use in machines
 * follow the activity left, which the durability bar shows.
 */
public class IsotopeItem extends Item {
    static final String KEY = "item.create_nuclearindustry.isotope.";
    private static final int DECAYED = 0xE34B3B;

    private final Isotope isotope;

    public IsotopeItem(Isotope isotope, Properties properties) {
        super(properties);
        this.isotope = isotope;
    }

    public Isotope isotope() {
        return isotope;
    }

    /** The activity a tooltip or bar shows: live when a clock is known, else as last refreshed. */
    static double shownActivity(ItemStack stack) {
        long time = Isotopes.tooltipTime();
        return time >= 0 ? Isotopes.activity(stack, time) : Isotopes.storedActivity(stack);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return Isotopes.data(stack) != null && Isotopes.storedActivity(stack) < 0.995;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0f * (float) Mth.clamp(Isotopes.storedActivity(stack), 0.0, 1.0));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        float share = (float) Mth.clamp(Isotopes.storedActivity(stack), 0.0, 1.0);
        return share < isotope.usableAbove() ? DECAYED : Mth.hsvToRgb(share / 3.0f, 0.75f, 0.9f);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        double activity = shownActivity(stack);
        boolean usable = IsotopeDecay.usable(activity, isotope.usableAbove());
        tooltip.add(Component.translatable(KEY + "activity", IsotopeDecay.percent(activity))
                .withStyle(usable ? ChatFormatting.GRAY : ChatFormatting.RED));
        double halfLife = IsotopeConfig.halfLifeDays(isotope) / IsotopeConfig.timeScale;
        tooltip.add(Component.translatable(KEY + "half_life", days(halfLife)).withStyle(ChatFormatting.DARK_GRAY));
        if (!usable) {
            tooltip.add(Component.translatable(KEY + "decayed").withStyle(ChatFormatting.RED));
        }
    }

    /** Days with sensible precision: hours under a day. */
    static Component days(double days) {
        if (days < 1.0) {
            return Component.translatable(KEY + "hours", String.format(Locale.ROOT, "%.1f", days * 24.0));
        }
        return Component.translatable(KEY + "days", days < 10 ? String.format(Locale.ROOT, "%.1f", days) : String.valueOf(Math.round(days)));
    }
}
