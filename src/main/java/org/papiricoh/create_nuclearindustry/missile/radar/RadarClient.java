package org.papiricoh.create_nuclearindustry.missile.radar;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.client.event.*;

public final class RadarClient {
    public static void registerScreens(RegisterMenuScreensEvent event) { event.register(RadarRegistry.MENU.get(), RadarScreen::new); }
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) { event.registerBlockEntityRenderer(RadarRegistry.ENTITY.get(), Antenna::new); }
    private static class Antenna implements BlockEntityRenderer<RadarBlockEntity> {
        private final BlockEntityRendererProvider.Context context;
        Antenna(BlockEntityRendererProvider.Context context) { this.context = context; }
        @Override public void render(RadarBlockEntity radar, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            pose.pushPose(); pose.translate(.5, .8, .5);
            if (radar.operational()) pose.mulPose(Axis.YP.rotationDegrees((radar.getLevel().getGameTime() + partial) * 3));
            pose.mulPose(Axis.XP.rotationDegrees(-20)); pose.scale(.85f, .5f, .125f); pose.translate(-.5, 0, -.5);
            context.getBlockRenderDispatcher().renderSingleBlock(Blocks.IRON_BLOCK.defaultBlockState(), pose, buffers, light, overlay);
            pose.popPose();
        }
        @Override public boolean shouldRenderOffScreen(RadarBlockEntity entity) { return true; }
    }
    private RadarClient() {}
}
