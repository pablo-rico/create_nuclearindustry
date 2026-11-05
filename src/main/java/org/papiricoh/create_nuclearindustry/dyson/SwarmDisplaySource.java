package org.papiricoh.create_nuclearindustry.dyson;

import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import org.papiricoh.create_nuclearindustry.dyson.receiver.RayReceiverBlockEntity;
import org.papiricoh.create_nuclearindustry.dyson.swarm.DysonSwarm;

import java.util.ArrayList;
import java.util.List;

/**
 * The Dyson swarm for a Display Link: read from a swarm terminal, the whole swarm (sails, power beamed down, receivers sharing
 * it); read from a ray receiver, that receiver.
 */
public class SwarmDisplaySource extends DisplaySource {
    private final boolean receiver;

    private SwarmDisplaySource(boolean receiver) {
        this.receiver = receiver;
    }

    public static SwarmDisplaySource swarm() {
        return new SwarmDisplaySource(false);
    }

    public static SwarmDisplaySource receiver() {
        return new SwarmDisplaySource(true);
    }

    @Override
    public List<MutableComponent> provideText(DisplayLinkContext context, DisplayTargetStats stats) {
        if (receiver) {
            if (!(context.getSourceBlockEntity() instanceof RayReceiverBlockEntity entity)) return List.of();
            List<MutableComponent> lines = new ArrayList<>();
            entity.statusLines().forEach(line -> lines.add(line.copy()));
            return lines;
        }
        if (!(context.level() instanceof ServerLevel level)) return List.of();
        return lines(DysonSwarm.get(level));
    }

    /** The whole swarm in a few lines. */
    public static List<MutableComponent> lines(DysonSwarm swarm) {
        return List.of(Component.translatable("create_nuclearindustry.swarm.sails", swarm.totalSails()),
                Component.translatable("create_nuclearindustry.swarm.pool", Math.round(swarm.pool())),
                Component.translatable("create_nuclearindustry.swarm.sun", Math.round(swarm.sunFactor() * 100)),
                Component.translatable("create_nuclearindustry.swarm.receivers", swarm.receivers(), swarm.delivered()));
    }

    @Override
    public int getPassiveRefreshTicks() {
        return 20;
    }
}
