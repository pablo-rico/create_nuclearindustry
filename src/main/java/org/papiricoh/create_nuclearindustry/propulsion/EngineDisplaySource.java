package org.papiricoh.create_nuclearindustry.propulsion;

import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.source.SingleLineDisplaySource;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.papiricoh.create_nuclearindustry.propulsion.blockentity.RocketEngineBlockEntity;

import java.util.Locale;

/** A nuclear engine on a Display Link: what it is doing, its thrust and its throttle. */
public class EngineDisplaySource extends SingleLineDisplaySource {
    @Override
    protected MutableComponent provideLine(DisplayLinkContext context, DisplayTargetStats stats) {
        if (!(context.getSourceBlockEntity() instanceof RocketEngineBlockEntity engine)) {
            return DisplaySource.EMPTY_LINE;
        }
        return Component.translatable("create_nuclearindustry.display_source.rocket_engine.line",
                Component.translatable(engine.statusKey()), String.format(Locale.ROOT, "%.0f", engine.thrust()),
                String.format(Locale.ROOT, "%.0f", engine.throttle() * 100));
    }

    @Override
    protected boolean allowsLabeling(DisplayLinkContext context) {
        return true;
    }

    @Override
    public int getPassiveRefreshTicks() {
        return 10;
    }
}
