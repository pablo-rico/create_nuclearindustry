package org.papiricoh.create_nuclearindustry.reactor.pwr.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import org.papiricoh.create_nuclearindustry.AllNuclearParticles;
import org.papiricoh.create_nuclearindustry.client.NuclearFeatureClient;
import org.papiricoh.create_nuclearindustry.reactor.pwr.PwrRegistry;
import org.papiricoh.create_nuclearindustry.reactor.pwr.containment.ContainmentClientHooks;
import org.papiricoh.create_nuclearindustry.reactor.visual.ReactorVisualHooks;

/** Client setup for the pressurized water loop, steam generator, containment and the accident chain. */
public final class PwrClient implements NuclearFeatureClient {
    public static final PwrClient INSTANCE = new PwrClient();

    private PwrClient() {}

    @Override
    public void init(IEventBus modEventBus) {
        modEventBus.addListener(PwrClient::registerParticles);
        // the hydrogen haze in reactor buildings and containments
        ReactorVisualHooks.reactorClientTick = ReactorVisualHooks.reactorClientTick.andThen(HydrogenHaze::reactorTick);
        ContainmentClientHooks.monitorClientTick = ContainmentClientHooks.monitorClientTick.andThen(HydrogenHaze::containmentTick);
    }

    @Override
    public void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(PwrRegistry.CONTAINMENT_DOOR_ENTITY.get(), ContainmentDoorRenderer::new);
    }

    private static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(AllNuclearParticles.HYDROGEN_GAS.get(), HydrogenGasParticle.Provider::new);
    }
}
