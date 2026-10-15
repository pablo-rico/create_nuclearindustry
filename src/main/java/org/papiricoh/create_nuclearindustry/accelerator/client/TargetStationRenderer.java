package org.papiricoh.create_nuclearindustry.accelerator.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import org.papiricoh.create_nuclearindustry.accelerator.blockentity.TargetStationBlockEntity;
import org.papiricoh.create_nuclearindustry.fusion.heating.client.PlasmaRenderTypes;

/**
 * The Target Station at work: while the beam hits it, its window glows blue-white, brighter for a
 * stronger beam; while its horn collects antiprotons, the collector ring on the back glows green and
 * pulses. Additive light just outside the faces (the plasma's render type).
 */
public class TargetStationRenderer extends SafeBlockEntityRenderer<TargetStationBlockEntity> {
    private static final float PX = 1 / 16f;

    public TargetStationRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    protected void renderSafe(TargetStationBlockEntity station, float partialTicks, PoseStack ms, MultiBufferSource buffers, int light,
                              int overlay) {
        if (station.getLevel() == null) {
            return;
        }
        double power = station.incomingPower();
        double production = station.displayedProduction();
        if (power <= 0 && production <= 0) {
            return;
        }
        float time = AnimationTickHolder.getRenderTime(station.getLevel());
        Direction window = station.beamWindow();
        ms.pushPose();
        // Model north is the window; the blockstate turns it to FACING.
        ms.translate(0.5, 0.5, 0.5);
        ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-((window.toYRot() + 180) % 360)));
        ms.translate(-0.5, -0.5, -0.5);
        VertexConsumer buffer = buffers.getBuffer(PlasmaRenderTypes.PLASMA);
        PoseStack.Pose pose = ms.last();
        if (power > 0) {
            float strength = (float) Math.min(1, Math.log10(1 + power) / 3);
            float flicker = 0.9f + 0.1f * (float) Math.sin(time * 1.7);
            float a = (0.35f + 0.55f * strength) * flicker;
            // The aperture (texels 5..9 of the front, seen from the north: x 6..11, y 6..11) and its halo.
            north(buffer, pose, 6, 6, 11, 11, -0.03f, 0.75f, 0.85f, 1.0f, a);
            north(buffer, pose, 4.5f, 4.5f, 12.5f, 12.5f, -0.06f, 0.55f, 0.7f, 1.0f, a * 0.3f);
        }
        if (production > 0) {
            float pulse = 0.65f + 0.35f * (float) Math.sin(time * 0.35);
            // The collector ring on the back (texels 5..9: x 5..10, y 6..11).
            south(buffer, pose, 5, 6, 10, 11, 0.03f, 0x6B / 255f, 1.0f, 0x8A / 255f, 0.7f * pulse);
            south(buffer, pose, 3.5f, 4.5f, 11.5f, 12.5f, 0.06f, 0x6B / 255f, 1.0f, 0x8A / 255f, 0.25f * pulse);
        }
        ms.popPose();
    }

    private static void north(VertexConsumer b, PoseStack.Pose pose, float x0, float y0, float x1, float y1, float out, float r, float g,
                              float bl, float a) {
        float z = out * PX;
        quad(b, pose, r, g, bl, a, x1 * PX, y0 * PX, z, x0 * PX, y0 * PX, z, x0 * PX, y1 * PX, z, x1 * PX, y1 * PX, z);
    }

    private static void south(VertexConsumer b, PoseStack.Pose pose, float x0, float y0, float x1, float y1, float out, float r, float g,
                              float bl, float a) {
        float z = 1 + out * PX;
        quad(b, pose, r, g, bl, a, x0 * PX, y0 * PX, z, x1 * PX, y0 * PX, z, x1 * PX, y1 * PX, z, x0 * PX, y1 * PX, z);
    }

    private static void quad(VertexConsumer b, PoseStack.Pose pose, float r, float g, float bl, float a, float... v) {
        for (int c = 0; c < 4; c++) {
            b.addVertex(pose, v[c * 3], v[c * 3 + 1], v[c * 3 + 2]).setColor(r, g, bl, a);
        }
    }
}
