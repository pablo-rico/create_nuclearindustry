package org.papiricoh.create_nuclearindustry.missile.defense.battery;

import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.papiricoh.create_nuclearindustry.controlroom.link.LinkCode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** An interceptor battery's state, rounds and mode for a Display Link. */
public class BatteryDisplaySource extends DisplaySource {
    private static final String KEY = "battery.create_nuclearindustry.display.";

    @Override
    public List<MutableComponent> provideText(DisplayLinkContext context, DisplayTargetStats stats) {
        if (!(context.getSourceBlockEntity() instanceof InterceptorBatteryEntity battery))
            return List.of(Component.translatable(KEY + "offline"));
        return read(battery);
    }

    /** What it shows: what the battery is doing, its rounds, and how it is told of missiles. */
    public List<MutableComponent> read(InterceptorBatteryEntity battery) {
        List<MutableComponent> lines = new ArrayList<>();
        lines.add(Component.translatable("battery.create_nuclearindustry." + battery.status().name().toLowerCase(Locale.ROOT)));
        int ready = 0, loading = 0;
        for (int i = 0; i < InterceptorBatteryEntity.TUBES; i++) {
            if (battery.tubeInventory().getStackInSlot(i).isEmpty()) continue;
            if (battery.loadingTicks(i) > 0) loading++;
            else ready++;
        }
        lines.add(Component.translatable(KEY + "rounds", ready, InterceptorBatteryEntity.TUBES, loading));
        lines.add(Component.translatable(KEY + (battery.settings().auto() ? "auto" : "manual"),
                LinkCode.complete(battery.code()) ? battery.code() : "----"));
        return lines;
    }

    @Override
    public int getPassiveRefreshTicks() {
        return 20;
    }
}
