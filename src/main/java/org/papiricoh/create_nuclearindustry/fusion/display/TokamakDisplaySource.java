package org.papiricoh.create_nuclearindustry.fusion.display;

import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.papiricoh.create_nuclearindustry.controlroom.TokamakScales;
import org.papiricoh.create_nuclearindustry.fusion.api.TokamakPhase;
import org.papiricoh.create_nuclearindustry.fusion.api.TokamakReadings;
import org.papiricoh.create_nuclearindustry.fusion.blockentity.FusionReactorBlockEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Display Link readout attached to a tokamak controller: its phase (or why it tripped), the plasma
 * and what it gives, then what holds and feeds it. Every line is translated.
 */
public class TokamakDisplaySource extends DisplaySource {
    public static final String KEY = "create_nuclearindustry.display_source.tokamak.";

    @Override
    public List<MutableComponent> provideText(DisplayLinkContext context, DisplayTargetStats stats) {
        if (!(context.getSourceBlockEntity() instanceof FusionReactorBlockEntity tokamak)) {
            return List.of(Component.translatable(KEY + "none"));
        }
        return lines(tokamak.readings(), tokamak.getValidationMessage());
    }

    /** The lines shown for a tokamak reading {@code readings}; {@code validation} says why it is not formed. */
    public static List<MutableComponent> lines(TokamakReadings readings, String validation) {
        if (!readings.formed()) {
            return List.of(Component.translatable(KEY + "unformed"), Component.literal(validation));
        }
        List<MutableComponent> lines = new ArrayList<>();
        if (TokamakScales.tripped(readings.phase(), readings.tripCause())) {
            lines.add(Component.translatable(KEY + "tripped", Component.translatable(readings.tripCause().langKey())));
        } else {
            lines.add(Component.translatable(KEY + "phase", Component.translatable(readings.phase().langKey())));
        }
        String mode = readings.phase() != TokamakPhase.BURN ? "cold" : readings.dtMode() ? "dt" : "dd";
        lines.add(Component.translatable(KEY + "plasma", fmt("%.0f", readings.temperature()), Component.translatable(KEY + "mode." + mode)));
        float gain = TokamakScales.storedGain(readings.gain());
        lines.add(Component.translatable(KEY + "power", fmt("%.0f", readings.fusionPower()),
                TokamakScales.ignited(gain) ? Component.translatable(KEY + "ignited") : Component.literal(TokamakScales.formatGain(gain))));
        lines.add(Component.translatable(KEY + "density", fmt("%.0f", readings.density() * 100), fmt("%.1f", readings.ashFraction() * 100)));
        lines.add(Component.translatable(KEY + "magnets", fmt("%.0f", readings.field() * 100),
                fmt(readings.magnetTemperature() < 100 ? "%.1f" : "%.0f", readings.magnetTemperature())));
        lines.add(Component.translatable(KEY + "vacuum", TokamakScales.formatPressure(readings.vacuum())));
        lines.add(readings.heatingEnabled()
                ? Component.translatable(KEY + "heating", fmt("%.1f", readings.heatingPower()), fmt("%.0f", readings.heatingLevel() * 100))
                : Component.translatable(KEY + "heating_off"));
        lines.add(Component.translatable(KEY + "fuel", fmt("%.2f", readings.fuelRate()), readings.deuterium(), readings.tritium()));
        lines.add(Component.translatable(KEY + "helium", readings.liquidHelium(), readings.warmHelium()));
        lines.add(Component.translatable(KEY + "stability", fmt("%.0f", readings.instability()), fmt("%.0f", readings.confinement() * 100)));
        lines.add(Component.translatable(KEY + "tbr", fmt("%.2f", readings.tritiumBreeding())));
        return lines;
    }

    private static String fmt(String format, double value) {
        return String.format(Locale.ROOT, format, value);
    }

    @Override
    public int getPassiveRefreshTicks() {
        return 20;
    }
}
