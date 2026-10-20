package org.papiricoh.create_nuclearindustry.missile.client;

import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import org.papiricoh.create_nuclearindustry.client.NuclearFeatureClient;
import org.papiricoh.create_nuclearindustry.missile.MissileRegistry;
import org.papiricoh.create_nuclearindustry.missile.gui.LaunchPadScreen;

/** Client side of {@link MissileRegistry}: the missile in flight, the one standing on a pad and the pad's screen. */
public final class MissileClient implements NuclearFeatureClient {
    public static final MissileClient INSTANCE = new MissileClient();

    private MissileClient() {}

    @Override
    public void init(net.neoforged.bus.api.IEventBus modEventBus) {
        org.papiricoh.create_nuclearindustry.missile.rocket.client.RocketClient.init(modEventBus);
        org.papiricoh.create_nuclearindustry.missile.terrain.client.ClientTerrain.init();
        org.papiricoh.create_nuclearindustry.missile.radar.RadarClient.init();
        org.papiricoh.create_nuclearindustry.missile.holo.client.ClientHolo.init();
        org.papiricoh.create_nuclearindustry.missile.defense.client.DefenseClient.init(modEventBus);
        org.papiricoh.create_nuclearindustry.missile.orbit.OrbitRegistry.clientSync = org.papiricoh.create_nuclearindustry.missile.orbit.client.ClientSatellites::accept;
        org.papiricoh.create_nuclearindustry.missile.silo.SiloDiagramPayload.client = org.papiricoh.create_nuclearindustry.missile.silo.client.ClientLaunchDiagrams::accept;
    }

    @Override
    public void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        org.papiricoh.create_nuclearindustry.missile.rocket.client.RocketClient.registerRenderers(event);
        event.registerBlockEntityRenderer(org.papiricoh.create_nuclearindustry.missile.holo.HoloRegistry.ENTITY.get(),
                org.papiricoh.create_nuclearindustry.missile.holo.client.HoloTableRenderer::new);
        org.papiricoh.create_nuclearindustry.missile.radar.RadarClient.registerRenderers(event);
        org.papiricoh.create_nuclearindustry.missile.defense.client.DefenseClient.registerRenderers(event);
        event.registerEntityRenderer(MissileRegistry.MISSILE.get(), MissileRenderer::new);
        event.registerBlockEntityRenderer(MissileRegistry.LAUNCH_PAD_ENTITY.get(), LaunchPadRenderer::new);
    }

    @Override
    public void registerScreens(RegisterMenuScreensEvent event) {
        event.register(org.papiricoh.create_nuclearindustry.missile.silo.SiloRegistry.MENU.get(), org.papiricoh.create_nuclearindustry.missile.silo.SiloScreen::new);
        org.papiricoh.create_nuclearindustry.missile.radar.RadarClient.registerScreens(event);
        org.papiricoh.create_nuclearindustry.missile.defense.client.DefenseClient.registerScreens(event);
        event.register(org.papiricoh.create_nuclearindustry.missile.holo.HoloRegistry.MENU.get(),
                org.papiricoh.create_nuclearindustry.missile.holo.client.HoloTableScreen::new);
        event.register(MissileRegistry.LAUNCH_PAD_MENU.get(), LaunchPadScreen::new);
    }
}
