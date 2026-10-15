package org.papiricoh.create_nuclearindustry.maintenance.incident;

import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;

import java.util.List;

/**
 * The newest entries of the server's incident log, one a row, as many as the display holds. Offered
 * by reactor controllers and seismic detectors.
 */
public class IncidentLogDisplaySource extends DisplaySource {
    @Override
    public List<MutableComponent> provideText(DisplayLinkContext context, DisplayTargetStats stats) {
        if (!(context.level() instanceof ServerLevel level)) {
            return EMPTY;
        }
        List<Incident> incidents = IncidentLog.get(level.getServer()).incidents();
        if (incidents.isEmpty()) {
            return List.of(Component.translatable(Incident.KEY + "none"));
        }
        return incidents.stream().limit(Math.max(1, stats.maxRows())).map(Incident::line).toList();
    }

    @Override
    public int getPassiveRefreshTicks() {
        return 40;
    }
}
