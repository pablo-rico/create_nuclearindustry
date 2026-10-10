package org.papiricoh.create_nuclearindustry.fusion.cryo.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.papiricoh.create_nuclearindustry.client.NuclearFeatureClient;
import org.papiricoh.create_nuclearindustry.fusion.cryo.CryoRegistry;
import org.papiricoh.create_nuclearindustry.fusion.cryo.plant.CryoplantClientHooks;

/** Client setup for the cryogenic plant, the tokamak's vacuum pumps and cryogenic ports. */
public final class CryoClient implements NuclearFeatureClient {
    public static final CryoClient INSTANCE = new CryoClient();

    private CryoClient() {}

    @Override
    public void init(IEventBus modEventBus) {
        CryoPartials.init();
        CryoplantClientHooks.clientTick = CryoplantClientEffects::tick;
    }

    @Override
    public void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(CryoRegistry.CRYOPLANT_COMPRESSOR_ENTITY.get(),
                context -> new CryoRotorRenderer<>(context, CryoPartials.COMPRESSOR_IMPELLER));
        event.registerBlockEntityRenderer(CryoRegistry.FUSION_VACUUM_PUMP_ENTITY.get(),
                context -> new CryoRotorRenderer<>(context, CryoPartials.VACUUM_PUMP_ROTOR));
    }
}
