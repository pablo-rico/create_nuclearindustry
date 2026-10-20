package org.papiricoh.create_nuclearindustry.missile.orbit.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.papiricoh.create_nuclearindustry.missile.orbit.OrbitRegistry;

/** Client side of satellites: the partials and the ground station's renderer. Called from {@code MissileClient}. */
public final class OrbitClient {
    private OrbitClient() {
    }

    /** From the mod constructor: partial models must exist before models load. */
    public static void init(IEventBus modEventBus) {
        OrbitPartials.init();
    }

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(OrbitRegistry.STATION_ENTITY.get(), GroundStationRenderer::new);
    }
}
