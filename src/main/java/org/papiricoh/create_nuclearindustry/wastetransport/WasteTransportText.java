package org.papiricoh.create_nuclearindustry.wastetransport;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;

/** Lang keys and goggle lines of the waste containers. */
public final class WasteTransportText {
    public static final String PREFIX = "create_nuclearindustry.waste_transport.";

    private WasteTransportText() {}

    /** Sealed or breached (with how badly), and the activity inside against what gets out. */
    public static void addStatus(List<Component> tooltip, float breach, double activity, double emission) {
        if (breach > 0) {
            tooltip.add(line(Component.translatable(PREFIX + "goggles.breached", Math.round(breach * 100)), ChatFormatting.RED));
            tooltip.add(line(Component.translatable(PREFIX + "goggles.repair_hint"), ChatFormatting.GRAY));
        } else {
            tooltip.add(line(Component.translatable(PREFIX + "goggles.sealed"), ChatFormatting.GREEN));
        }
        tooltip.add(line(Component.translatable(PREFIX + "goggles.activity", rate(activity)), ChatFormatting.GRAY));
        tooltip.add(line(Component.translatable(PREFIX + "goggles.emission", rate(emission)),
                breach > 0 ? ChatFormatting.GOLD : ChatFormatting.DARK_GREEN));
    }

    private static Component line(Component text, ChatFormatting style) {
        return Component.literal("     ").append(text).withStyle(style);
    }

    /** A dose rate with as many decimals as it needs to read as more than zero. */
    public static String rate(double value) {
        if (value <= 0) {
            return "0";
        }
        if (value >= 100) {
            return String.format(Locale.ROOT, "%.0f", value);
        }
        if (value >= 1) {
            return String.format(Locale.ROOT, "%.1f", value);
        }
        return String.format(Locale.ROOT, "%.3f", value);
    }
}
