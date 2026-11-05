package org.papiricoh.create_nuclearindustry.explosive.fx.client;

import net.neoforged.bus.api.IEventBus;
import org.papiricoh.create_nuclearindustry.client.NuclearFeatureClient;
import org.papiricoh.create_nuclearindustry.explosive.DetonationPayload;
import org.papiricoh.create_nuclearindustry.explosive.client.NuclearExplosionEffects;
import org.papiricoh.create_nuclearindustry.explosive.conventional.ConventionalBlastPayload;
import org.papiricoh.create_nuclearindustry.explosive.fx.ExplosionFxRegistry;

/**
 * Client side of {@link ExplosionFxRegistry}: where the server's detonation packets land. A nuclear detonation hands over to
 * the mushroom cloud's effects, a conventional one to the fireball, smoke and debris of a non-nuclear blast; both schedule their
 * sounds and shaking by the shock front's arrival.
 */
public final class ExplosionFxClient implements NuclearFeatureClient {
    public static final ExplosionFxClient INSTANCE = new ExplosionFxClient();

    private ExplosionFxClient() {}

    @Override
    public void init(IEventBus modEventBus) {
    }

    /** A nuclear detonation (or an antimatter flash) went off, or one still standing came into range. */
    public static void onNuclear(DetonationPayload payload) {
        NuclearExplosionEffects.add(payload);
    }

    /** A conventional warhead, bomblet or dirty bomb went off, or one still smoking came into range. */
    public static void onConventional(ConventionalBlastPayload payload) {
    }
}
