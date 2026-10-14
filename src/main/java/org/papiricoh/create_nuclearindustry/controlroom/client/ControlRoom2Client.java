package org.papiricoh.create_nuclearindustry.controlroom.client;

import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import org.papiricoh.create_nuclearindustry.client.NuclearFeatureClient;
import org.papiricoh.create_nuclearindustry.controlroom.ControlRoom2Registry;
import org.papiricoh.create_nuclearindustry.controlroom.desk.client.ControlDeskRenderer;
import org.papiricoh.create_nuclearindustry.controlroom.link.client.AdvancedLinkRenderer;
import org.papiricoh.create_nuclearindustry.controlroom.servo.client.ServoGearboxRenderer;

/**
 * Client setup for tables and wall panels (drawn like desks), the chart recorder, the annunciator panel, the Servo Gearbox and automatic rod control. The
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
        event.registerBlockEntityRenderer(ControlRoom2Registry.MODULE_SURFACE_ENTITY.get(), ControlDeskRenderer::new);
        event.registerBlockEntityRenderer(ControlRoom2Registry.ADVANCED_LINK_ENTITY.get(), AdvancedLinkRenderer::new);
        event.registerBlockEntityRenderer(ControlRoom2Registry.SERVO_GEARBOX_ENTITY.get(), ServoGearboxRenderer::new);
    }

    @Override
    public void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ControlRoom2Registry.CHART_RECORDER_MENU.get(), ChartRecorderScreen::new);
        event.register(ControlRoom2Registry.ANNUNCIATOR_MENU.get(), AnnunciatorScreen::new);
        event.register(ControlRoom2Registry.MONITOR_MENU.get(), MonitorConfigScreen::new);
    }
}
