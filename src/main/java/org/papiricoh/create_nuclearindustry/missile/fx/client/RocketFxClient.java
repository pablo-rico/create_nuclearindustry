package org.papiricoh.create_nuclearindustry.missile.fx.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.papiricoh.create_nuclearindustry.client.NuclearFeatureClient;
import org.papiricoh.create_nuclearindustry.missile.flight.client.ClientFlights;

/**
 * Client side of {@link org.papiricoh.create_nuclearindustry.missile.fx.RocketFxRegistry}: the rocket effects. Hooks the
 * flights' moments, the client tick, the frame and the camera to {@link RocketFxManager} and {@link RocketFxRender}, and
 * registers the effects' shaders and particles.
 */
public final class RocketFxClient implements NuclearFeatureClient {
    public static final RocketFxClient INSTANCE = new RocketFxClient();

    private RocketFxClient() {
    }

    @Override
    public void init(IEventBus modEventBus) {
        // Part of the shared flight contract: keep this call whatever else changes here. It hooks the client tick first, so
        // the flights have moved on by the time the effects tick.
        ClientFlights.init();
        ClientFlights.addListener(RocketFxManager.INSTANCE);
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, RocketFxManager.INSTANCE::tick);
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, RocketFxManager.INSTANCE::onLoggingOut);
        NeoForge.EVENT_BUS.addListener(ViewportEvent.ComputeCameraAngles.class, RocketFxManager.INSTANCE::onCameraAngles);
        NeoForge.EVENT_BUS.addListener(RenderLevelStageEvent.class, RocketFxRender::onRenderLevel);
        modEventBus.addListener(RocketRenderTypes::registerShaders);
        modEventBus.addListener(RegisterParticleProvidersEvent.class, RocketParticles::registerProviders);
    }
}
