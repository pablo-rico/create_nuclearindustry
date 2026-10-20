package org.papiricoh.create_nuclearindustry.missile.defense.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** Client side of missile defence: the battery's renderer and screen. Called from {@code MissileClient}. */
public final class DefenseClient {
    private DefenseClient() {
    }

    public static void init(IEventBus modEventBus) {
        DefensePartials.init();
    }

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
    }

    public static void registerScreens(RegisterMenuScreensEvent event) {
    }
}
