package org.papiricoh.create_nuclearindustry.antimatter.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/**
 * Full-bright boxes in the block vertex format (the cutout buffer): solid light that still reads in
 * daylight, where additive glow washes out. Used for the quench flash of a trap's field bands and
 * the pulses running down a conduit.
 */
final class LitBoxes {
    private LitBoxes() {}

    /** A box from (x0, y0, z0) to (x1, y1, z1), all six faces showing {@code sprite} tinted {@code rgb}. */
    static void box(VertexConsumer b, PoseStack.Pose pose, TextureAtlasSprite s, int rgb, float x0, float y0, float z0, float x1,
                    float y1, float z1) {
        int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, bl = rgb & 0xFF;
        float u0 = s.getU0(), u1 = s.getU1(), v0 = s.getV0(), v1 = s.getV1();
        quad(b, pose, r, g, bl, u0, v0, u1, v1, 0, 0, -1, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0);
        quad(b, pose, r, g, bl, u0, v0, u1, v1, 0, 0, 1, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
        quad(b, pose, r, g, bl, u0, v0, u1, v1, -1, 0, 0, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
        quad(b, pose, r, g, bl, u0, v0, u1, v1, 1, 0, 0, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1);
        quad(b, pose, r, g, bl, u0, v0, u1, v1, 0, -1, 0, x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1);
        quad(b, pose, r, g, bl, u0, v0, u1, v1, 0, 1, 0, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0);
    }

    private static void quad(VertexConsumer b, PoseStack.Pose pose, int r, int g, int bl, float u0, float v0, float u1, float v1,
                             float nx, float ny, float nz, float... v) {
        float[] us = {u0, u1, u1, u0};
        float[] vs = {v1, v1, v0, v0};
        // Both windings: Ponder's view mirrors the scene, which turns front faces into back faces.
        for (int pass = 0; pass < 2; pass++) {
            for (int k = 0; k < 4; k++) {
                int c = pass == 0 ? k : 3 - k;
                b.addVertex(pose, v[c * 3], v[c * 3 + 1], v[c * 3 + 2]).setColor(r, g, bl, 255).setUv(us[c], vs[c])
                        .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, nx, ny, nz);
            }
        }
    }
}
