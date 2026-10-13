package org.papiricoh.create_nuclearindustry.fusion.heating.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.papiricoh.create_nuclearindustry.AllNuclearEntities;
import org.papiricoh.create_nuclearindustry.client.NuclearFeatureClient;
import org.papiricoh.create_nuclearindustry.fusion.api.TokamakClientHooks;
import org.papiricoh.create_nuclearindustry.fusion.heating.PlasmaHeatingRegistry;

/** Client setup for the tokamak's neutral beam injectors, radio-frequency antennas, viewports, plasma rendering and sounds. */
public final class PlasmaHeatingClient implements NuclearFeatureClient {
    public static final PlasmaHeatingClient INSTANCE = new PlasmaHeatingClient();

    private PlasmaHeatingClient() {}

    @Override
    public void init(IEventBus modEventBus) {
        // Chained, so other areas may hook the controller's client tick too.
        TokamakClientHooks.clientTick = TokamakClientHooks.clientTick.andThen(TokamakClientEffects::tick);
    }

    @Override
    public void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(AllNuclearEntities.FUSION_CONTROLLER.get(), PlasmaTorusRenderer::new);
        event.registerBlockEntityRenderer(PlasmaHeatingRegistry.NEUTRAL_BEAM_INJECTOR_ENTITY.get(), PlasmaHeaterRenderer::new);
        event.registerBlockEntityRenderer(PlasmaHeatingRegistry.RF_ANTENNA_ENTITY.get(), PlasmaHeaterRenderer::new);
    }
}
