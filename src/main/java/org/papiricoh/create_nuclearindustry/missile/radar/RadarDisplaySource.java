package org.papiricoh.create_nuclearindustry.missile.radar;

import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * What a Display Link reads off a missile radar: a summary ({@link #summary}), every track it shows ({@link #tracks}) or only the
 * threats to its defended zone ({@link #threats}), one line each with designation and seconds to impact.
 */
public class RadarDisplaySource extends DisplaySource {
    private final Function<RadarBlockEntity, List<MutableComponent>> lines;

    private RadarDisplaySource(Function<RadarBlockEntity, List<MutableComponent>> lines) {
        this.lines = lines;
    }

    public static RadarDisplaySource summary() {
        return new RadarDisplaySource(radar -> List.of(
                Component.translatable("radar.create_nuclearindustry.contacts", radar.trackCount()),
                Component.translatable("radar.create_nuclearindustry.threats", radar.threatCount(), radar.soonestSeconds())));
    }

    public static RadarDisplaySource tracks() {
        return new RadarDisplaySource(radar -> listing(radar.tracks(), radar));
    }

    public static RadarDisplaySource threats() {
        return new RadarDisplaySource(radar -> listing(radar.threats(), radar));
    }

    private static List<MutableComponent> listing(List<RadarTrack> tracks, RadarBlockEntity radar) {
        if (tracks.isEmpty()) return List.of(Component.translatable("radar.create_nuclearindustry.clear"));
        List<MutableComponent> lines = new ArrayList<>();
        for (RadarTrack track : tracks) {
            boolean threat = radar.threats().contains(track);
            lines.add(Component.translatable("radar.create_nuclearindustry.track_line", track.designation(), (track.eta() + 19) / 20,
                    Component.translatable("radar.create_nuclearindustry.phase." + track.phase().name().toLowerCase(java.util.Locale.ROOT)),
                    threat ? "!" : ""));
        }
        return lines;
    }

    @Override
    public List<MutableComponent> provideText(DisplayLinkContext context, DisplayTargetStats stats) {
        if (!(context.getSourceBlockEntity() instanceof RadarBlockEntity radar) || !radar.operational())
            return List.of(Component.translatable("radar.create_nuclearindustry.offline"));
        return lines.apply(radar);
    }

    @Override
    public int getPassiveRefreshTicks() {
        return 20;
    }
}
