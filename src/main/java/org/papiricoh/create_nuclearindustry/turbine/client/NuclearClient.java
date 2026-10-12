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
        org.papiricoh.create_nuclearindustry.controlroom.desk.client.DeskPartials.init();
        NuclearSpriteShifts.init();
        modEventBus.addListener(NuclearModelSwaps::onModifyBakingResult);
        TurbineClientHooks.install(TurbineClientEffects::tick);
        CoolingTowerClientHooks.install(CoolingTowerClientEffects::tick);
        if (java.nio.file.Files.exists(java.nio.file.Path.of("nuclear_tour.flag"))) {
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(org.papiricoh.create_nuclearindustry.dev.DevScreenshotTour::onTick);
        }
    }
}
