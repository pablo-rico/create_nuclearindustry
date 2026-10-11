package org.papiricoh.create_nuclearindustry.reactor.pwr.client;

import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.papiricoh.create_nuclearindustry.client.NuclearFeatureClient;
import org.papiricoh.create_nuclearindustry.reactor.pwr.PwrRegistry;

/** Client setup for the pressurized water loop, steam generator, containment and the accident chain. */
public final class PwrClient implements NuclearFeatureClient {
    public static final PwrClient INSTANCE = new PwrClient();

    private PwrClient() {}

    @Override
    public void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(PwrRegistry.CONTAINMENT_DOOR_ENTITY.get(), ContainmentDoorRenderer::new);
    }
}
