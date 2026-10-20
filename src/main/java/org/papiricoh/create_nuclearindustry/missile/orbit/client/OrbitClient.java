package org.papiricoh.create_nuclearindustry.missile.orbit.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.papiricoh.create_nuclearindustry.missile.orbit.OrbitRegistry;

/**
 * Client side of satellites: the partials, the ground station's renderer and the satellites in the night sky. Their moments
 * (insertion, a kill, a re-entry) are the rocket effects'. Called from {@code MissileClient}.
 */
public final class OrbitClient {
    private OrbitClient() {
    }

    /** From the mod constructor: partial models must exist before models load. */
    public static void init(IEventBus modEventBus) {
        OrbitPartials.init();
        org.papiricoh.create_nuclearindustry.missile.fx.client.SatelliteSkyRender.init();
    }

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(OrbitRegistry.STATION_ENTITY.get(), GroundStationRenderer::new);
    }
}
