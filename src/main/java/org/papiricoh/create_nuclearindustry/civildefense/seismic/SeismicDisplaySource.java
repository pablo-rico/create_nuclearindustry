package org.papiricoh.create_nuclearindustry.civildefense.seismic;

import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.source.SingleLineDisplaySource;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** The latest detonation a seismic detector felt: power, distance and compass point. */
public class SeismicDisplaySource extends SingleLineDisplaySource {
    @Override
    protected MutableComponent provideLine(DisplayLinkContext context, DisplayTargetStats stats) {
        if (!(context.getSourceBlockEntity() instanceof SeismicDetectorBlockEntity detector)) {
            return EMPTY_LINE;
        }
        return detector.latest().map(SeismicEvent::describe)
                .orElseGet(() -> Component.translatable("create_nuclearindustry.seismic.quiet"));
    }

    @Override
    protected boolean allowsLabeling(DisplayLinkContext context) {
        return true;
    }

    @Override
    public int getPassiveRefreshTicks() {
        return 20;
    }
}
