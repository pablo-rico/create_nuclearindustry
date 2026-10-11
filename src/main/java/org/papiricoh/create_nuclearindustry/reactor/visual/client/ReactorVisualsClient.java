package org.papiricoh.create_nuclearindustry.reactor.visual.client;

import net.neoforged.bus.api.IEventBus;
import org.papiricoh.create_nuclearindustry.client.NuclearFeatureClient;
import org.papiricoh.create_nuclearindustry.reactor.visual.ReactorVisualHooks;

/** Client setup for the visible and audible fission reactor: its sounds and effects. */
public final class ReactorVisualsClient implements NuclearFeatureClient {
    public static final ReactorVisualsClient INSTANCE = new ReactorVisualsClient();

    private ReactorVisualsClient() {}

    @Override
    public void init(IEventBus modEventBus) {
        // Chained, so other areas may hook these client ticks too.
        ReactorVisualHooks.reactorClientTick = ReactorVisualHooks.reactorClientTick.andThen(ReactorClientEffects::tick);
        ReactorVisualHooks.machineClientTick = ReactorVisualHooks.machineClientTick.andThen(MachineSounds::tick);
    }
}
