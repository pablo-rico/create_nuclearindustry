package org.papiricoh.create_nuclearindustry.explosive.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import org.papiricoh.create_nuclearindustry.client.NuclearFeatureClient;
import org.papiricoh.create_nuclearindustry.explosive.ExplosiveRegistry;
import org.papiricoh.create_nuclearindustry.explosive.gui.ThermonuclearBombScreen;
import org.papiricoh.create_nuclearindustry.integration.dh.DistantHorizonsCompat;

/**
 * Client side of {@link ExplosiveRegistry}: the thermonuclear bomb's screen, and the far cloud over Distant Horizons' terrain.
 * How a detonation looks, sounds and feels is {@code ExplosionFxClient}'s.
 */
public final class ExplosiveClient implements NuclearFeatureClient {
    public static final ExplosiveClient INSTANCE = new ExplosiveClient();

    private ExplosiveClient() {}

    @Override
    public void init(IEventBus modEventBus) {
        DistantHorizonsCompat.initClient(modEventBus);
    }

    @Override
    public void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ExplosiveRegistry.THERMONUCLEAR_BOMB_MENU.get(), ThermonuclearBombScreen::new);
    }
}
