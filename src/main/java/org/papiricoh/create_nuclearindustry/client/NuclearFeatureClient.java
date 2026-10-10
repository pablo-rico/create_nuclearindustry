package org.papiricoh.create_nuclearindustry.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** Client side of a {@link org.papiricoh.create_nuclearindustry.NuclearFeature}: partials, renderers and screens. */
public interface NuclearFeatureClient {

    /** Called from the mod constructor on the client, before resources load (partial models go here). */
    default void init(IEventBus modEventBus) {}

    default void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {}

    default void registerScreens(RegisterMenuScreensEvent event) {}

    default void registerGeometryLoaders(ModelEvent.RegisterGeometryLoaders event) {}
}
