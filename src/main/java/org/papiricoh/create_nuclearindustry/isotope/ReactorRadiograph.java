package org.papiricoh.create_nuclearindustry.isotope;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.papiricoh.create_nuclearindustry.reactor.blockentity.ReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.pwr.PwrConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * What a radiograph of a fission reactor shows, worst first: core damage, oxidised cladding, the
 * pressure vessel, the fuel's thermal stress, the fuel left, the control rods in place, then the
 * wear of its rod drives and relief valve, and whether it is in a refuelling outage. Kept out
 * of {@link ReactorBlockEntity}, which only delegates here.
 */
public final class ReactorRadiograph {
    private static final String KEY = "create_nuclearindustry.radiograph.reactor.";

    private ReactorRadiograph() {}

    public static List<Component> of(ReactorBlockEntity reactor) {
        List<Component> lines = new ArrayList<>();
        double damage = reactor.getCoreDamage();
        lines.add(line("core_damage", damage > 0.01 ? ChatFormatting.RED : ChatFormatting.GREEN, percent(damage)));
        int rods = reactor.getStructure().map(structure -> structure.uraniumRodCount).orElse(0);
        double cladding = reactor.getAccidents().cladding().damage(rods, PwrConfig.zirconiumPerRod);
        lines.add(line("cladding", cladding > 0.01 ? ChatFormatting.YELLOW : ChatFormatting.GREEN, percent(cladding) + "%"));
        boolean breached = reactor.getAccidents().vessel().breached();
        lines.add(line("vessel", breached ? ChatFormatting.RED : ChatFormatting.GREEN,
                Component.translatable(KEY + (breached ? "vessel_breached" : "vessel_sound"))));
        double stress = reactor.getThermalStress();
        lines.add(line("stress", stress > 0 ? ChatFormatting.YELLOW : ChatFormatting.GREEN, String.format(Locale.ROOT, "%.0f", stress)));
        double capacity = reactor.getFuelCapacity();
        lines.add(line("fuel", ChatFormatting.GRAY, capacity <= 0 ? "0" : percent(reactor.getFuelRemaining() / capacity)));
        lines.add(line("rods", ChatFormatting.GRAY, reactor.getInsertedControlRodSegments(), reactor.getExpectedControlRodSegments()));
        lines.addAll(reactor.getMaintenance().lines());
        if (reactor.inOutage()) {
            lines.add(Component.translatable(KEY + "outage").withStyle(ChatFormatting.AQUA));
        }
        return lines;
    }

    private static Component line(String key, ChatFormatting style, Object... args) {
        return Component.translatable(KEY + key, args).withStyle(style);
    }

    private static String percent(double share) {
        return String.format(Locale.ROOT, "%.0f", Math.max(0, share) * 100);
    }
}
