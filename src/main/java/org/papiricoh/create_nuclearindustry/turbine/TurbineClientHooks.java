package org.papiricoh.create_nuclearindustry.turbine;

import java.util.function.Consumer;

/**
 * Client-side per-tick effects (sound) for turbine controllers, installed by the client
 * setup so this common code never touches client classes.
 */
public final class TurbineClientHooks {
    private static Consumer<AbstractTurbineControllerBlockEntity> ticker = controller -> {};

    private TurbineClientHooks() {}

    public static void install(Consumer<AbstractTurbineControllerBlockEntity> clientTicker) {
        ticker = clientTicker;
    }

    static void tick(AbstractTurbineControllerBlockEntity controller) {
        ticker.accept(controller);
    }
}
