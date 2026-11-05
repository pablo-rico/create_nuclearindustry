package org.papiricoh.create_nuclearindustry.dyson.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import org.papiricoh.create_nuclearindustry.client.NuclearFeatureClient;
import org.papiricoh.create_nuclearindustry.dyson.DysonFxHooks;
import org.papiricoh.create_nuclearindustry.dyson.DysonRegistry;
import org.papiricoh.create_nuclearindustry.dyson.client.fx.DysonLaunchFx;
import org.papiricoh.create_nuclearindustry.dyson.client.fx.DysonSkyRender;

/** Client side of the Dyson swarm: the partials, the machines' renderers and screens, the swarm in the sky and its effects. */
public final class DysonClient implements NuclearFeatureClient {
    public static final DysonClient INSTANCE = new DysonClient();

    private DysonClient() {
    }

    @Override
    public void init(IEventBus modEventBus) {
        DysonPartials.init();
        ClientSwarm.init();
        DysonRegistry.clientSync = ClientSwarm::accept;
        DysonSkyRender.init();
        DysonLaunchFx.init();
        DysonFxHooks.ejectorFired = DysonLaunchFx::fired;
        DysonFxHooks.receiverLit = ReceiverRenderer::firstLight;
    }

    @Override
    public void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(DysonRegistry.EJECTOR_ENTITY.get(), EjectorRenderer::new);
        event.registerBlockEntityRenderer(DysonRegistry.RECEIVER_ENTITY.get(), ReceiverRenderer::new);
    }

    @Override
    public void registerScreens(RegisterMenuScreensEvent event) {
        event.register(DysonRegistry.EJECTOR_MENU.get(), EjectorScreen::new);
        event.register(DysonRegistry.TERMINAL_MENU.get(), SwarmTerminalScreen::new);
    }
}
