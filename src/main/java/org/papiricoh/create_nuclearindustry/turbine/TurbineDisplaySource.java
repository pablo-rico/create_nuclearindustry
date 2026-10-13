package org.papiricoh.create_nuclearindustry.turbine;

import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.papiricoh.create_nuclearindustry.multiblock.MultiblockText;

import java.util.List;

/** Display Link readout attached to a steam or plasma turbine. */
public class TurbineDisplaySource extends DisplaySource {
    private static final String KEY = AbstractTurbineControllerBlockEntity.KEY + "display.";

    @Override
    public List<MutableComponent> provideText(DisplayLinkContext context, DisplayTargetStats stats) {
        if (!(context.getSourceBlockEntity() instanceof AbstractTurbineControllerBlockEntity turbine)) {
            return List.of(Component.translatable(KEY + "no_turbine"));
        }
        TurbineDisplay display = turbine.getDisplay();
        if (!display.formed()) {
            return List.of(Component.translatable(KEY + "not_formed"), MultiblockText.text(turbine.getValidationMessage()).copy());
        }
        float speed = Math.abs(turbine.getSpeed());
        return List.of(
                Component.translatable(KEY + "speed", Math.round(speed)),
                Component.translatable(KEY + "steam", display.currentUse(), display.limit()),
                Component.translatable(KEY + "capacity", Math.round(turbine.calculateAddedStressCapacity() * speed)),
                Component.translatable(KEY + "tank", turbine.getSteamTank().getFluidAmount()),
                Component.translatable(KEY + (display.exhausting() ? "exhaust_tower" : "exhaust_condensing")));
    }

    @Override
    public int getPassiveRefreshTicks() {
        return 20;
    }
}
