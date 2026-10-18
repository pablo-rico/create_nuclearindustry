package org.papiricoh.create_nuclearindustry.missile.client;

import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import org.papiricoh.create_nuclearindustry.client.NuclearFeatureClient;
import org.papiricoh.create_nuclearindustry.missile.MissileRegistry;
import org.papiricoh.create_nuclearindustry.missile.gui.LaunchPadScreen;

/** Client side of {@link MissileRegistry}: the missile in flight, the one standing on a pad and the pad's screen. */
public final class MissileClient implements NuclearFeatureClient {
    public static final MissileClient INSTANCE = new MissileClient();

    private MissileClient() {}

    @Override
    public void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(MissileRegistry.MISSILE.get(), MissileRenderer::new);
        event.registerBlockEntityRenderer(MissileRegistry.LAUNCH_PAD_ENTITY.get(), LaunchPadRenderer::new);
    }

    @Override
    public void registerScreens(RegisterMenuScreensEvent event) {
        event.register(MissileRegistry.LAUNCH_PAD_MENU.get(), LaunchPadScreen::new);
    }
}
