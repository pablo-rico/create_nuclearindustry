package org.papiricoh.create_nuclearindustry.missile.radar;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.createmod.catnip.render.CachedBuffers;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.client.sound.MachineLoopSound;
import org.papiricoh.create_nuclearindustry.missile.fx.RocketFxRegistry;
import org.papiricoh.create_nuclearindustry.missile.radar.client.RadarScopeRenderer;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Client side of the radar: its screen, the dish turning on its mount with the antenna's kinetic angle (the same angle the screens
 * sweep by), the scope's picture, and the klaxon looping at a radar while a threat goes unacknowledged.
 */
public final class RadarClient {
    /** The dish, its mast and feed: turned about the block's vertical axis. */
    public static final PartialModel DISH = PartialModel.of(ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "block/missile_radar_dish"));
    private static final Map<RadarBlockEntity, MachineLoopSound<RadarBlockEntity>> KLAXONS = new WeakHashMap<>();

    private RadarClient() {
    }

    /** From the mod constructor: partial models must exist before models load. */
    public static void init() {
        RadarBlockEntity.clientTick = RadarClient::tick;
    }

    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(RadarRegistry.MENU.get(), RadarScreen::new);
    }

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(RadarRegistry.ENTITY.get(), Dish::new);
        event.registerBlockEntityRenderer(RadarRegistry.SCOPE_ENTITY.get(), RadarScopeRenderer::new);
    }

    /** The antenna's bearing, degrees clockwise from north, at {@code partial} of this tick: the screens use the same. */
    public static float bearing(RadarBlockEntity radar, float partial) {
        if (radar.getLevel() == null) return 0;
        double time = radar.getLevel().getGameTime() + partial;
        return (float) Mth.positiveModulo(time * radar.getSpeed() * 0.3, 360);
    }

    private static void tick(RadarBlockEntity radar) {
        if (!radar.alarmUnacked()) return;
        MachineLoopSound<RadarBlockEntity> klaxon = KLAXONS.get(radar);
        if (klaxon != null && !klaxon.isStopped()) return;
        if (!org.papiricoh.create_nuclearindustry.ClientConfig.radarSounds) return;
        klaxon = new MachineLoopSound<>(RocketFxRegistry.RADAR_KLAXON.get(), radar) {
            @Override protected boolean wanted() { return machine.alarmUnacked(); }
            @Override protected float targetVolume() { return 0.9f; }
            @Override protected float targetPitch() { return 1; }
        };
        KLAXONS.put(radar, klaxon);
        Minecraft.getInstance().getSoundManager().play(klaxon);
    }

    private static class Dish implements BlockEntityRenderer<RadarBlockEntity> {
        Dish(BlockEntityRendererProvider.Context context) {
        }

        @Override
        public void render(RadarBlockEntity radar, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            // The dish's face looks north at bearing 0 and turns clockwise seen from above.
            CachedBuffers.partial(DISH, radar.getBlockState()).translate(0.5f, 0, 0.5f).rotateYDegrees(-bearing(radar, partial))
                    .translate(-0.5f, 0, -0.5f).light(light).renderInto(pose, buffers.getBuffer(RenderType.cutoutMipped()));
        }

        @Override
        public AABB getRenderBoundingBox(RadarBlockEntity radar) {
            return new AABB(radar.getBlockPos()).inflate(1, 0, 1).expandTowards(0, 2, 0);
        }
    }
}
