package org.papiricoh.create_nuclearindustry.antimatter.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;

/**
 * Soft camera-facing layers of light for the generator's core and the injector's dose and beam, in
 * the cloud render types ({@link AntimatterRenderTypes}) the traps use. The plane is taken from the
 * whole transform (model-view times pose), as the trap renderer does, so the layers face the viewer.
 */
final class CloudLayers {
    static final ResourceLocation CLOUD = tex("antimatter_cloud");
    static final ResourceLocation WISP = tex("antimatter_cloud_wisp");
    static final ResourceLocation LIT = tex("penning_trap_led_lit");

    private CloudLayers() {}

    /** The plane facing the viewer at the pose's origin, as two unit axes in the pose's space. */
    record View(Vector3f right, Vector3f up) {
        static View of(PoseStack ms) {
            Matrix3f toLocal = new Matrix3f();
            new Matrix4f(RenderSystem.getModelViewMatrix()).mul(ms.last().pose()).get3x3(toLocal);
            toLocal.transpose();
            return new View(toLocal.transform(new Vector3f(1, 0, 0)).normalize(), toLocal.transform(new Vector3f(0, 1, 0)).normalize());
        }
    }

    /** One square layer of half-size {@code half} round the pose's origin, turned {@code spin} radians in the view plane. */
    static void layer(VertexConsumer buffer, PoseStack ms, View view, TextureAtlasSprite sprite, float half, float spin, float r, float g,
                      float b, float alpha) {
        if (alpha <= 0.004f || half <= 0) {
            return;
        }
        float cos = (float) Math.cos(spin) * half, sin = (float) Math.sin(spin) * half;
        Vector3f ax = view.right(), ay = view.up();
        float ux = ax.x() * cos + ay.x() * sin, uy = ax.y() * cos + ay.y() * sin, uz = ax.z() * cos + ay.z() * sin;
        float vx = ay.x() * cos - ax.x() * sin, vy = ay.y() * cos - ax.y() * sin, vz = ay.z() * cos - ax.z() * sin;
        quad(buffer, ms.last(), sprite, -ux - vx, -uy - vy, -uz - vz, ux - vx, uy - vy, uz - vz, ux + vx, uy + vy, uz + vz,
                -ux + vx, -uy + vy, -uz + vz, r, g, b, alpha);
    }

    /**
     * A streak from the pose's origin {@code length} along +z, {@code half} wide, its broad side turned
     * to the viewer as far as a ribbon along a fixed axis can be.
     */
    static void streak(VertexConsumer buffer, PoseStack ms, TextureAtlasSprite sprite, float length, float half, float r, float g,
                       float b, float alpha) {
        if (alpha <= 0.004f || half <= 0 || length <= 0) {
            return;
        }
        // The camera's position in the pose's space: the ribbon's side is across both the axis and the view.
        Matrix4f toLocal = new Matrix4f(RenderSystem.getModelViewMatrix()).mul(ms.last().pose()).invert();
        Vector3f eye = toLocal.transformPosition(new Vector3f(0, 0, 0));
        Vector3f side = new Vector3f(0, 0, 1).cross(new Vector3f(eye.x(), eye.y(), eye.z() - length / 2));
        if (side.lengthSquared() < 1.0e-8f) {
            side.set(1, 0, 0);
        }
        side.normalize(half);
        quad(buffer, ms.last(), sprite, -side.x(), -side.y(), 0, side.x(), side.y(), 0, side.x(), side.y(), length,
                -side.x(), -side.y(), length, r, g, b, alpha);
    }

    /**
     * A small lit orb facing the viewer, for the cutout (block) buffer: three stacked discs, the
     * {@code rgb} rim, a paler middle and a white heart, each a hair nearer the viewer than the last.
     * Pixel-crisp light that reads in daylight and under any other pass. {@code flare} 0..1 whitens it.
     */
    static void orb(VertexConsumer buffer, PoseStack.Pose pose, View view, TextureAtlasSprite sprite, float radius, int rgb, float flare,
                    float time) {
        if (radius <= 0) {
            return;
        }
        Vector3f toward = new Vector3f(view.right()).cross(view.up()).normalize(radius * 0.02f);
        float spin = time * 0.05f;
        float[] sizes = {1.0f, 0.62f, 0.3f};
        float[] whites = {0.0f, 0.45f, 0.9f};
        for (int i = 0; i < sizes.length; i++) {
            float white = whites[i] + (1 - whites[i]) * flare * 0.8f;
            int colour = lerp(rgb, 0xFFFFFF, white);
            disc(buffer, pose, view, sprite, radius * sizes[i], spin, toward.x() * i, toward.y() * i, toward.z() * i, colour);
        }
    }

    /** An eight-sided disc round (ox, oy, oz) in the view plane, full-bright, both windings. */
    private static void disc(VertexConsumer b, PoseStack.Pose pose, View view, TextureAtlasSprite s, float half, float spin, float ox,
                             float oy, float oz, int rgb) {
        Vector3f ax = view.right(), ay = view.up();
        float u = (s.getU0() + s.getU1()) / 2, v = (s.getV0() + s.getV1()) / 2;
        int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, bl = rgb & 0xFF;
        int segments = 8;
        for (int k = 0; k < segments; k += 2) {
            float[] p = new float[9];
            for (int j = 0; j < 3; j++) {
                double angle = spin + 2 * Math.PI * (k + j) / segments;
                float c = (float) Math.cos(angle) * half, sn = (float) Math.sin(angle) * half;
                p[j * 3] = ox + ax.x() * c + ay.x() * sn;
                p[j * 3 + 1] = oy + ax.y() * c + ay.y() * sn;
                p[j * 3 + 2] = oz + ax.z() * c + ay.z() * sn;
            }
            float[][] fan = {{ox, oy, oz}, {p[0], p[1], p[2]}, {p[3], p[4], p[5]}, {p[6], p[7], p[8]}};
            for (int pass = 0; pass < 2; pass++) {
                for (int i = 0; i < 4; i++) {
                    float[] q = fan[pass == 0 ? i : (4 - i) % 4];
                    b.addVertex(pose, q[0], q[1], q[2]).setColor(r, g, bl, 255).setUv(u, v)
                            .setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
                            .setLight(net.minecraft.client.renderer.LightTexture.FULL_BRIGHT).setNormal(pose, 0, 1, 0);
                }
            }
        }
    }

    private static void quad(VertexConsumer buffer, PoseStack.Pose pose, TextureAtlasSprite sprite, float x0, float y0, float z0, float x1,
                             float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float r, float g, float b,
                             float alpha) {
        // Half a texel in, so the smooth filter never samples the atlas neighbours.
        float du = (sprite.getU1() - sprite.getU0()) / 32f, dv = (sprite.getV1() - sprite.getV0()) / 32f;
        float u0 = sprite.getU0() + du, u1 = sprite.getU1() - du, v0 = sprite.getV0() + dv, v1 = sprite.getV1() - dv;
        buffer.addVertex(pose, x0, y0, z0).setUv(u0, v1).setColor(r, g, b, alpha);
        buffer.addVertex(pose, x1, y1, z1).setUv(u1, v1).setColor(r, g, b, alpha);
        buffer.addVertex(pose, x2, y2, z2).setUv(u1, v0).setColor(r, g, b, alpha);
        buffer.addVertex(pose, x3, y3, z3).setUv(u0, v0).setColor(r, g, b, alpha);
    }

    static TextureAtlasSprite sprite(ResourceLocation texture) {
        return Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(texture);
    }

    /** Blends a channel towards white. */
    static float mix(float channel, float white) {
        return channel + (1 - channel) * white;
    }

    static float red(int rgb) {
        return ((rgb >> 16) & 0xFF) / 255f;
    }

    static float green(int rgb) {
        return ((rgb >> 8) & 0xFF) / 255f;
    }

    static float blue(int rgb) {
        return (rgb & 0xFF) / 255f;
    }

    /** Linear blend of two colours, {@code t} 0..1. */
    static int lerp(int from, int to, float t) {
        t = Math.max(0, Math.min(1, t));
        int r = Math.round(((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * t);
        int g = Math.round(((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * t);
        int b = Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
        return r << 16 | g << 8 | b;
    }

    /** {@code rgb} darkened to {@code share} of its brightness. */
    static int scale(int rgb, float share) {
        return lerp(0, rgb, share);
    }

    private static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "block/" + name);
    }
}
