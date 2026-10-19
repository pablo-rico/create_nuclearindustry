package org.papiricoh.create_nuclearindustry.missile.rocket.client;

import com.simibubi.create.content.contraptions.render.ContraptionEntityRenderer;
import com.simibubi.create.content.contraptions.render.ContraptionVisual;
import dev.engine_room.flywheel.lib.visualization.SimpleEntityVisualizer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Quaternionf;
import org.papiricoh.create_nuclearindustry.missile.flight.client.ClientFlight;
import org.papiricoh.create_nuclearindustry.missile.flight.client.ClientFlights;
import org.papiricoh.create_nuclearindustry.missile.physics.RocketAttitude;
import org.papiricoh.create_nuclearindustry.missile.physics.V3;
import org.papiricoh.create_nuclearindustry.missile.rocket.RocketContraptionEntity;
import org.papiricoh.create_nuclearindustry.missile.rocket.RocketPose;
import org.papiricoh.create_nuclearindustry.missile.rocket.RocketRegistry;

/**
 * Client side of the rocket contraption: Create's contraption renderer and Flywheel visual for the rocket and its falling stages,
 * and the tick that places each rocket from its flight's client state. That tick runs after the flights themselves have been
 * flown on ({@link ClientFlights}), so the entity's last and current positions are exactly the flight's, and the puppet sits on
 * the plume the effects draw.
 */
public final class RocketClient {
    private RocketClient() {
    }

    public static void init(IEventBus modEventBus) {
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, ClientTickEvent.Post.class, event -> placeRockets());
        modEventBus.addListener(FMLClientSetupEvent.class, event -> event.enqueueWork(RocketClient::registerVisuals));
    }

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(RocketRegistry.ROCKET.get(), ContraptionEntityRenderer::new);
        event.registerEntityRenderer(RocketRegistry.ROCKET_DEBRIS.get(), ContraptionEntityRenderer::new);
    }

    /** As Create does for its own contraptions: Flywheel draws the blocks, the renderer what lives in block entities. */
    private static void registerVisuals() {
        SimpleEntityVisualizer.builder(RocketRegistry.ROCKET.get()).factory(ContraptionVisual::new).skipVanillaRender(entity -> false).apply();
        SimpleEntityVisualizer.builder(RocketRegistry.ROCKET_DEBRIS.get()).factory(ContraptionVisual::new).skipVanillaRender(entity -> false).apply();
    }

    private static void placeRockets() {
        if (Minecraft.getInstance().level == null) return;
        for (RocketContraptionEntity rocket : RocketContraptionEntity.clientRockets()) {
            if (rocket.isRemoved() || rocket.level() != Minecraft.getInstance().level) continue;
            ClientFlight flight = rocket.flightId() == null ? null : ClientFlights.get(rocket.flightId());
            if (flight == null) {
                rocket.placeFromServer();
                continue;
            }
            Quaternionf q = flight.attitude(1);
            RocketAttitude.Quat attitude = new RocketAttitude.Quat(q.x, q.y, q.z, q.w);
            rocket.placeOnClient(RocketPose.anchor(v3(flight.position(0)), attitude), RocketPose.anchor(v3(flight.position(1)), attitude), q);
        }
    }

    private static V3 v3(Vec3 v) {
        return new V3(v.x, v.y, v.z);
    }
}
