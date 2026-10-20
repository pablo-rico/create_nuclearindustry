package org.papiricoh.create_nuclearindustry.missile.defense.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import org.papiricoh.create_nuclearindustry.missile.defense.DefenseRegistry;

/** Client side of missile defence: the battery's renderer and screen. Called from {@code MissileClient}. */
public final class DefenseClient {
    private DefenseClient() {
    }

    public static void init(IEventBus modEventBus) {
        DefensePartials.init();
    }

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(DefenseRegistry.BATTERY_ENTITY.get(), BatteryRenderer::new);
    }

    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(DefenseRegistry.BATTERY_MENU.get(), BatteryScreen::new);
    }
}
