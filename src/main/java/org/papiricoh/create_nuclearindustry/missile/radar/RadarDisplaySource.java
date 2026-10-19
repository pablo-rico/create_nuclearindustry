package org.papiricoh.create_nuclearindustry.missile.radar;
import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import net.minecraft.network.chat.*;
import java.util.List;
public class RadarDisplaySource extends DisplaySource {
    @Override public List<MutableComponent> provideText(DisplayLinkContext context, DisplayTargetStats stats) {
        if (!(context.getSourceBlockEntity() instanceof RadarBlockEntity radar) || !radar.operational())
            return List.of(Component.translatable("radar.create_nuclearindustry.offline"));
        var contacts = radar.contacts();
        int nearest = contacts.stream().mapToInt(RadarContacts.Contact::ticks).min().orElse(0);
        return List.of(Component.translatable("radar.create_nuclearindustry.contacts", contacts.size()),
                Component.translatable("radar.create_nuclearindustry.impact", (nearest + 19) / 20));
    }
    @Override public int getPassiveRefreshTicks() { return 20; }
}
