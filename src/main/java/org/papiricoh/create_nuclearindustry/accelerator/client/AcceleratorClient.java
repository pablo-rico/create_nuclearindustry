package org.papiricoh.create_nuclearindustry.accelerator.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import org.papiricoh.create_nuclearindustry.accelerator.AcceleratorHooks;
import org.papiricoh.create_nuclearindustry.accelerator.AcceleratorRegistry;
import org.papiricoh.create_nuclearindustry.accelerator.gui.AcceleratorScreen;
import org.papiricoh.create_nuclearindustry.client.NuclearFeatureClient;

/** Client setup for the particle accelerator: the beam renderer, screens and sounds. */
public final class AcceleratorClient implements NuclearFeatureClient {
    public static final AcceleratorClient INSTANCE = new AcceleratorClient();

    private AcceleratorClient() {}

    @Override
    public void init(IEventBus modEventBus) {
        // Chained, so other parts of the area may hook the controller's client tick too.
        AcceleratorHooks.clientTick = AcceleratorHooks.clientTick.andThen(AcceleratorClientEffects::tick);
    }

    @Override
    public void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(AcceleratorRegistry.CONTROLLER_ENTITY.get(), AcceleratorBeamRenderer::new);
        event.registerBlockEntityRenderer(AcceleratorRegistry.ION_SOURCE_ENTITY.get(), AcceleratorMachineRenderer::new);
        event.registerBlockEntityRenderer(AcceleratorRegistry.KLYSTRON_ENTITY.get(), AcceleratorMachineRenderer::new);
        event.registerBlockEntityRenderer(AcceleratorRegistry.MAGNET_POWER_SUPPLY_ENTITY.get(), AcceleratorMachineRenderer::new);
        event.registerBlockEntityRenderer(AcceleratorRegistry.VACUUM_PUMP_ENTITY.get(), AcceleratorMachineRenderer::new);
    }

    @Override
    public void registerScreens(RegisterMenuScreensEvent event) {
        event.register(AcceleratorRegistry.MENU.get(), AcceleratorScreen::new);
    }
}
