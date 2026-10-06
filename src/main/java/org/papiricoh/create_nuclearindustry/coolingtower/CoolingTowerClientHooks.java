package org.papiricoh.create_nuclearindustry.coolingtower;

import org.papiricoh.create_nuclearindustry.coolingtower.blockentity.CoolingTowerControllerBlockEntity;

import java.util.function.Consumer;

/** Client-side per-tick effects (sound) for cooling towers, installed by the client setup. */
public final class CoolingTowerClientHooks {
    private static Consumer<CoolingTowerControllerBlockEntity> ticker = controller -> {};

    private CoolingTowerClientHooks() {}

    public static void install(Consumer<CoolingTowerControllerBlockEntity> clientTicker) {
        ticker = clientTicker;
    }

    public static void tick(CoolingTowerControllerBlockEntity controller) {
        ticker.accept(controller);
    }
}
