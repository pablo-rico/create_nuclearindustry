package org.papiricoh.create_nuclearindustry.explosive.conventional.client;

import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.papiricoh.create_nuclearindustry.client.NuclearFeatureClient;
import org.papiricoh.create_nuclearindustry.explosive.conventional.ConventionalRegistry;

/** Client side of {@link ConventionalRegistry}: a falling bomblet is drawn as its item, tumbling as it falls. */
public final class ConventionalClient implements NuclearFeatureClient {
    public static final ConventionalClient INSTANCE = new ConventionalClient();

    private ConventionalClient() {}

    @Override
    public void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ConventionalRegistry.BOMBLET.get(), context -> new ThrownItemRenderer<>(context, 1.0f, true));
    }
}
