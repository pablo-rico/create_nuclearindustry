package org.papiricoh.create_nuclearindustry.turbine;

import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;

/** Display Link readout attached to a steam or plasma turbine. */
public class TurbineDisplaySource extends DisplaySource {

    @Override
    public List<MutableComponent> provideText(DisplayLinkContext context, DisplayTargetStats stats) {
        if (!(context.getSourceBlockEntity() instanceof AbstractTurbineControllerBlockEntity turbine)) {
            return List.of(Component.literal("No turbine"));
        }
        TurbineDisplay display = turbine.getDisplay();
        if (!display.formed()) {
            return List.of(Component.literal("Not formed"), Component.literal(turbine.getValidationMessage()));
        }
        float speed = Math.abs(turbine.getSpeed());
        return List.of(
                Component.literal(String.format("Speed: %.0f RPM", speed)),
                Component.literal("Steam: " + display.currentUse() + "/" + display.limit() + " mB/t"),
                Component.literal(String.format("Capacity: %.0f su", turbine.calculateAddedStressCapacity() * speed)),
                Component.literal("Tank: " + turbine.getSteamTank().getFluidAmount() + " mB"),
                Component.literal(display.exhausting() ? "Exhaust: to cooling tower" : "Exhaust: condensing"));
    }

    @Override
    public int getPassiveRefreshTicks() {
        return 20;
    }
}
