package org.papiricoh.create_nuclearindustry.missile.orbit;

import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.papiricoh.create_nuclearindustry.controlroom.link.LinkCode;
import java.util.List;

/** Station status and the next satellite pass for a Display Link. */
public class GroundStationDisplaySource extends DisplaySource {
    @Override public List<MutableComponent> provideText(DisplayLinkContext context, DisplayTargetStats stats) {
        return context.getSourceBlockEntity() instanceof GroundStationBlockEntity station ? read(station)
                : List.of(Component.translatable("ground_station.create_nuclearindustry.offline"));
    }
    public List<MutableComponent> read(GroundStationBlockEntity station) {
        return List.of(station.statusLine().copy(),
                Component.translatable("ground_station.create_nuclearindustry.code", LinkCode.complete(station.code()) ? station.code() : "----"),
                Component.translatable("ground_station.create_nuclearindustry.satellites", station.satelliteCount()),
                station.nextPass() < 0 ? Component.translatable("ground_station.create_nuclearindustry.no_pass")
                        : Component.translatable("ground_station.create_nuclearindustry.next_pass", (station.nextPass() + 19) / 20));
    }
    @Override public int getPassiveRefreshTicks() { return 20; }
}
