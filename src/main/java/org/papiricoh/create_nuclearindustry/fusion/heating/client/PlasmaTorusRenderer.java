package org.papiricoh.create_nuclearindustry.fusion.heating.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.createmod.catnip.render.SuperRenderTypeBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.fusion.api.TokamakReadings;
import org.papiricoh.create_nuclearindustry.fusion.blockentity.FusionReactorBlockEntity;
import org.papiricoh.create_nuclearindustry.fusion.multiblock.TokamakGeometry;

/**
 * Draws the plasma inside a tokamak: a glowing ring of the major radius around the solenoid, with an
 * elongated cross-section of about 2.4 blocks that fills most of the 3x3 chamber. A bright core
 * and a faint halo, coloured by temperature, dense or thin with the density, flickering with the
 * instability and flashing magenta at breakdown (see {@link PlasmaLook}). The shell hides it; it is
 * seen through viewports, or from inside the chamber. A Ponder scene does not draw the plasma's own
 * render type, so there the same shells go into the translucent block layer instead, a plain white
 * sprite tinted the plasma's colour, at full brightness and with both windings.
 */
public class PlasmaTorusRenderer implements BlockEntityRenderer<FusionReactorBlockEntity> {
    /** Cross-section half-width and half-height of the halo and the core, in blocks. */
    static final double HALO_HALF_WIDTH = 1.15;
    static final double HALO_HALF_HEIGHT = 1.3;
    static final double CORE_HALF_WIDTH = 0.7;
    static final double CORE_HALF_HEIGHT = 0.85;
    private static final int MINOR_SEGMENTS = 16;
    /** Seen from this far beyond the ring, in blocks. */
    private static final int VIEW_DISTANCE = 96;

    private TorusMesh halo;
    private TorusMesh core;
    private final float[] corners = new float[12];
    private static final ResourceLocation WHITE = ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID,
            "block/penning_trap_led_lit");

    public PlasmaTorusRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(FusionReactorBlockEntity controller, float partialTicks, PoseStack ms, MultiBufferSource buffers,
                       int light, int overlay) {
        if (controller.getLevel() == null) {
            return;
        }
        TokamakReadings readings = controller.readings();
        int radius = readings.majorRadius();
        if (!readings.formed() || radius < TokamakGeometry.MIN_RADIUS || readings.phase() == null || !readings.phase().hasPlasma()) {
            return;
        }
        TokamakClientEffects.State state = TokamakClientEffects.state(controller);
        double temperature = state != null ? state.temperature(partialTicks) : readings.temperature();
        double density = state != null ? state.density(partialTicks) : readings.density();
        double instability = state != null ? state.instability(partialTicks) : readings.instability();
        double time = controller.getLevel().getGameTime() + partialTicks;
        PlasmaLook look = PlasmaLook.of(readings.phase(), temperature, density, instability, time);
        if (look == null || (look.coreAlpha() <= 0 && look.haloAlpha() <= 0)) {
            return;
        }

        int segments = TorusMesh.majorSegmentsFor(radius);
        if (halo == null || !halo.matches(radius, HALO_HALF_WIDTH, HALO_HALF_HEIGHT, segments, MINOR_SEGMENTS)) {
            halo = TorusMesh.build(radius, HALO_HALF_WIDTH, HALO_HALF_HEIGHT, segments, MINOR_SEGMENTS);
        }
        if (core == null || !core.matches(radius, CORE_HALF_WIDTH, CORE_HALF_HEIGHT, segments, MINOR_SEGMENTS)) {
            core = TorusMesh.build(radius, CORE_HALF_WIDTH, CORE_HALF_HEIGHT, segments, MINOR_SEGMENTS);
        }

        BlockPos center = controller.getCenter();
        BlockPos origin = controller.getBlockPos();
        ms.pushPose();
        ms.translate(center.getX() - origin.getX() + 0.5, center.getY() - origin.getY() + 0.5, center.getZ() - origin.getZ() + 0.5);
        PoseStack.Pose pose = ms.last();
        if (buffers instanceof SuperRenderTypeBuffer) {
            VertexConsumer buffer = buffers.getBuffer(RenderType.translucent());
            TextureAtlasSprite white = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(WHITE);
            drawLit(buffer, pose, halo, look, look.haloAlpha() * 0.8f, white);
            drawLit(buffer, pose, core, look, Math.min(1, look.coreAlpha() * 1.2f), white);
            ms.popPose();
            return;
        }
        // One render type and no text: a single buffer for both shells.
        VertexConsumer buffer = buffers.getBuffer(PlasmaRenderTypes.PLASMA);
        draw(buffer, pose, halo, look, look.haloAlpha());
        draw(buffer, pose, core, look, look.coreAlpha());
        ms.popPose();
    }

    private void draw(VertexConsumer buffer, PoseStack.Pose pose, TorusMesh mesh, PlasmaLook look, float alpha) {
        if (alpha <= 0.004f) {
            return;
        }
        for (int q = 0; q < mesh.quadCount(); q++) {
            mesh.quad(q, corners);
            for (int c = 0; c < 4; c++) {
                buffer.addVertex(pose, corners[c * 3], corners[c * 3 + 1], corners[c * 3 + 2])
                        .setColor(look.red(), look.green(), look.blue(), alpha);
            }
        }
    }

    /** A shell in the block vertex format, for Ponder: both windings, since its view may mirror the scene. */
    private void drawLit(VertexConsumer buffer, PoseStack.Pose pose, TorusMesh mesh, PlasmaLook look, float alpha, TextureAtlasSprite s) {
        if (alpha <= 0.004f) {
            return;
        }
        float u = (s.getU0() + s.getU1()) / 2, v = (s.getV0() + s.getV1()) / 2;
        for (int q = 0; q < mesh.quadCount(); q++) {
            mesh.quad(q, corners);
            for (int pass = 0; pass < 2; pass++) {
                for (int k = 0; k < 4; k++) {
                    int c = pass == 0 ? k : 3 - k;
                    buffer.addVertex(pose, corners[c * 3], corners[c * 3 + 1], corners[c * 3 + 2])
                            .setColor(look.red(), look.green(), look.blue(), alpha).setUv(u, v)
                            .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0, 1, 0);
                }
            }
        }
    }

    @Override
    public boolean shouldRenderOffScreen(FusionReactorBlockEntity controller) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return VIEW_DISTANCE;
    }

    @Override
    public boolean shouldRender(FusionReactorBlockEntity controller, Vec3 camera) {
        int radius = controller.getMajorRadius();
        if (radius <= 0) {
            return false;
        }
        double reach = VIEW_DISTANCE + radius;
        Vec3 view = controller.getLevel() == null ? camera : org.papiricoh.create_nuclearindustry.integration.sable.ShipSpace.localView(controller.getLevel(), controller.getCenter(), camera);
        return Vec3.atCenterOf(controller.getCenter()).closerThan(view, reach);
    }

    /** The whole torus, so the plasma is not culled when the controller itself is off screen. */
    @Override
    public AABB getRenderBoundingBox(FusionReactorBlockEntity controller) {
        int radius = controller.getMajorRadius();
        if (radius <= 0) {
            return new AABB(controller.getBlockPos());
        }
        BlockPos center = controller.getCenter();
        int extent = radius + TokamakGeometry.HALF_HEIGHT + 1;
        return new AABB(center.getX() - extent, center.getY() - TokamakGeometry.HALF_HEIGHT, center.getZ() - extent,
                center.getX() + extent + 1, center.getY() + TokamakGeometry.HALF_HEIGHT + 1, center.getZ() + extent + 1);
    }
}
