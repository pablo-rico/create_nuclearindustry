package org.papiricoh.create_nuclearindustry.turbine.client;

import net.neoforged.bus.api.IEventBus;
import org.papiricoh.create_nuclearindustry.coolingtower.CoolingTowerClientHooks;
import org.papiricoh.create_nuclearindustry.coolingtower.client.CoolingTowerClientEffects;
import org.papiricoh.create_nuclearindustry.turbine.TurbineClientHooks;

/** Client-only setup that must happen in the mod constructor, before resources load. */
public final class NuclearClient {

    private NuclearClient() {}

    public static void init(IEventBus modEventBus) {
        NuclearPartialModels.init();
        NuclearSpriteShifts.init();
        modEventBus.addListener(NuclearModelSwaps::onModifyBakingResult);
        TurbineClientHooks.install(TurbineClientEffects::tick);
        CoolingTowerClientHooks.install(CoolingTowerClientEffects::tick);
    }
}
