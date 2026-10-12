package org.papiricoh.create_nuclearindustry.controlroom.client;

import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import org.papiricoh.create_nuclearindustry.client.NuclearFeatureClient;
import org.papiricoh.create_nuclearindustry.controlroom.ControlRoom2Registry;

/**
 * Client setup for the chart recorder, the annunciator panel and automatic rod control. The
 * controller module's partial models live in {@link org.papiricoh.create_nuclearindustry.controlroom.desk.client.DeskPartials}
 * with the other modules.
 */
public final class ControlRoom2Client implements NuclearFeatureClient {
    public static final ControlRoom2Client INSTANCE = new ControlRoom2Client();

    private ControlRoom2Client() {}

    @Override
    public void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ControlRoom2Registry.CHART_RECORDER_ENTITY.get(), ChartRecorderRenderer::new);
        event.registerBlockEntityRenderer(ControlRoom2Registry.ANNUNCIATOR_ENTITY.get(), AnnunciatorPanelRenderer::new);
    }

    @Override
    public void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ControlRoom2Registry.CHART_RECORDER_MENU.get(), ChartRecorderScreen::new);
        event.register(ControlRoom2Registry.ANNUNCIATOR_MENU.get(), AnnunciatorScreen::new);
        event.register(ControlRoom2Registry.MONITOR_MENU.get(), MonitorConfigScreen::new);
    }
}
