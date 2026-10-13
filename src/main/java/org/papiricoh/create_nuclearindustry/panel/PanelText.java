package org.papiricoh.create_nuclearindustry.panel;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Locale;

/**
 * Panel text shared by the cards: labels under {@code create_nuclearindustry.panel.}, and values with
 * their units, whose numbers go in as arguments so every word comes from the lang files.
 */
public final class PanelText {
    public static final String KEY = "create_nuclearindustry.panel.";

    private PanelText() {}

    public static MutableComponent label(String id, Object... args) {
        return Component.translatable(KEY + id, args);
    }

    /** A value with its unit: {@code create_nuclearindustry.panel.unit.<unit>}, such as "%s MW". */
    public static MutableComponent unit(String unit, Object value) {
        return Component.translatable(KEY + "unit." + unit, value);
    }

    /** A number with {@code decimals} places, as a plain argument. */
    public static String number(double value, int decimals) {
        return String.format(Locale.ROOT, "%." + decimals + "f", value);
    }

    /** A signed number, for reactivity. */
    public static String signed(double value) {
        return String.format(Locale.ROOT, "%+d", Math.round(value));
    }

    public static MutableComponent percent(double fraction) {
        return unit("percent", Math.round(fraction * 100));
    }

    /** "a / b" with a unit, for tanks and counts. */
    public static MutableComponent of(Object amount, Object capacity) {
        return Component.translatable(KEY + "unit.of", amount, capacity);
    }
}
