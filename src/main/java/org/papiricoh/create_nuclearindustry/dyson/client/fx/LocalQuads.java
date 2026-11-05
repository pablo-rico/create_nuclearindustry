package org.papiricoh.create_nuclearindustry.dyson.client.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * Soft shapes for the machines' renderers, in the block's own frame (the pose a renderer is handed) and the position–colour format
 * of {@link DysonRenderTypes}. Billboards face a point given in that same frame, the camera's, so a machine on a ship or in Ponder
 * (where there is no world camera to ask) passes whatever stands in for it. Colours are ARGB; edges fade to clear.
 */
public final class LocalQuads {
    private static final int SIDES = 24;
    private static final float[] COS = new float[SIDES + 1], SIN = new float[SIDES + 1];

    static {
        for (int k = 0; k <= SIDES; k++) {
            COS[k] = (float) Math.cos(k * 2 * Math.PI / SIDES);
            SIN[k] = (float) Math.sin(k * 2 * Math.PI / SIDES);
        }
    }

    private LocalQuads() {
    }

    /**
     * A ribbon from {@code a} to {@code b} turned to face the camera at {@code c}, {@code ra} and {@code rb} to either side of its
     * line, coloured {@code ca} to {@code cb} along its middle and clear at its edges.
     */
    public static void strip(VertexConsumer vc, PoseStack.Pose pose, float ax, float ay, float az, float bx, float by, float bz,
                             float ra, float rb, int ca, int cb, float cx, float cy, float cz) {
        float dx = bx - ax, dy = by - ay, dz = bz - az;
        float mx = (ax + bx) * 0.5f - cx, my = (ay + by) * 0.5f - cy, mz = (az + bz) * 0.5f - cz;
        float sx = dy * mz - dz * my, sy = dz * mx - dx * mz, sz = dx * my - dy * mx;
        float len = (float) Math.sqrt(sx * sx + sy * sy + sz * sz);
        if (len < 1.0e-7f) return;
        sx /= len;
        sy /= len;
        sz /= len;
        int ea = ca & 0x00FFFFFF, eb = cb & 0x00FFFFFF;
        v(vc, pose, ax - sx * ra, ay - sy * ra, az - sz * ra, ea);
        v(vc, pose, ax, ay, az, ca);
        v(vc, pose, bx, by, bz, cb);
        v(vc, pose, bx - sx * rb, by - sy * rb, bz - sz * rb, eb);
        v(vc, pose, ax, ay, az, ca);
        v(vc, pose, ax + sx * ra, ay + sy * ra, az + sz * ra, ea);
        v(vc, pose, bx + sx * rb, by + sy * rb, bz + sz * rb, eb);
        v(vc, pose, bx, by, bz, cb);
    }

    /** A soft disc of radius {@code r} round {@code (x, y, z)} facing the camera at {@code c}: {@code colour} at its centre. */
    public static void disc(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float z, float r, int colour, float cx, float cy,
                            float cz) {
        float[] f = frame(x - cx, y - cy, z - cz);
        if (f == null) return;
        // A bright middle and a long soft skirt, close to a Gaussian, so the edge never shows as a rim.
        float k = SOFT_KNEE;
        int knee = argb(((colour >>> 24) / 255f) * SOFT_KNEE_ALPHA, colour);
        fan(vc, pose, x, y, z, f[0] * r * k, f[1] * r * k, f[2] * r * k, f[3] * r * k, f[4] * r * k, f[5] * r * k, colour, knee);
        for (int s = 0; s < SIDES; s++) {
            float c0 = COS[s], s0 = SIN[s], c1 = COS[s + 1], s1 = SIN[s + 1];
            band(vc, pose, x, y, z, f[0] * c0 + f[3] * s0, f[1] * c0 + f[4] * s0, f[2] * c0 + f[5] * s0,
                    f[0] * c1 + f[3] * s1, f[1] * c1 + f[4] * s1, f[2] * c1 + f[5] * s1, r * k, r, knee, colour & 0x00FFFFFF);
        }
    }

    /** Where a soft disc's bright middle ends, as a share of its radius, and how much of its alpha is left there. */
    private static final float SOFT_KNEE = 0.35f, SOFT_KNEE_ALPHA = 0.3f;

    /** A solid dot: {@code centre} in the middle, {@code rim} at the edge. */
    public static void dot(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float z, float r, int centre, int rim, float cx,
                           float cy, float cz) {
        float[] f = frame(x - cx, y - cy, z - cz);
        if (f == null) return;
        fan(vc, pose, x, y, z, f[0] * r, f[1] * r, f[2] * r, f[3] * r, f[4] * r, f[5] * r, centre, rim);
    }

    /**
     * A soft ring of radius {@code r} round {@code (x, y, z)} in the plane of {@code u} and {@code w} (unit, square to each other),
     * {@code width} across, brightest along its middle.
     */
    public static void ring(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float z, float r, float width, int colour,
                            float ux, float uy, float uz, float wx, float wy, float wz) {
        float r0 = Math.max(0, r - width * 0.5f), r1 = r + width * 0.5f;
        int clear = colour & 0x00FFFFFF;
        for (int k = 0; k < SIDES * 2; k++) {
            double a0 = k * Math.PI / SIDES, a1 = (k + 1) * Math.PI / SIDES;
            float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0), c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
            float px0 = ux * c0 + wx * s0, py0 = uy * c0 + wy * s0, pz0 = uz * c0 + wz * s0;
            float px1 = ux * c1 + wx * s1, py1 = uy * c1 + wy * s1, pz1 = uz * c1 + wz * s1;
            band(vc, pose, x, y, z, px0, py0, pz0, px1, py1, pz1, r0, r, clear, colour);
            band(vc, pose, x, y, z, px0, py0, pz0, px1, py1, pz1, r, r1, colour, clear);
        }
    }

    /** A box's six faces, for glow round a part. */
    public static void box(VertexConsumer vc, PoseStack.Pose pose, float x0, float y0, float z0, float x1, float y1, float z1, int colour) {
        quad(vc, pose, colour, x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0);
        quad(vc, pose, colour, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
        quad(vc, pose, colour, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
        quad(vc, pose, colour, x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0);
        quad(vc, pose, colour, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
        quad(vc, pose, colour, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1);
    }

    private static void band(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float z, float px0, float py0, float pz0,
                             float px1, float py1, float pz1, float ri, float ro, int ci, int co) {
        v(vc, pose, x + px0 * ri, y + py0 * ri, z + pz0 * ri, ci);
        v(vc, pose, x + px0 * ro, y + py0 * ro, z + pz0 * ro, co);
        v(vc, pose, x + px1 * ro, y + py1 * ro, z + pz1 * ro, co);
        v(vc, pose, x + px1 * ri, y + py1 * ri, z + pz1 * ri, ci);
    }

    private static void fan(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float z, float ux, float uy, float uz, float wx,
                            float wy, float wz, int centre, int rim) {
        for (int k = 0; k < SIDES; k++) {
            float c0 = COS[k], s0 = SIN[k], c1 = COS[k + 1], s1 = SIN[k + 1];
            v(vc, pose, x, y, z, centre);
            v(vc, pose, x + ux * c0 + wx * s0, y + uy * c0 + wy * s0, z + uz * c0 + wz * s0, rim);
            v(vc, pose, x + ux * c1 + wx * s1, y + uy * c1 + wy * s1, z + uz * c1 + wz * s1, rim);
            v(vc, pose, x, y, z, centre);
        }
    }

    private static final float[] FRAME = new float[6];

    /** Two unit axes square to the line of sight {@code (dx, dy, dz)}, into a shared scratch array (render thread only). */
    private static float[] frame(float dx, float dy, float dz) {
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1.0e-6f) return null;
        dx /= len;
        dy /= len;
        dz /= len;
        // Across: up × sight, or east × sight looking straight up or down.
        float ax, ay, az;
        if (Math.abs(dy) > 0.95f) {
            ax = 0;
            ay = -dz;
            az = dy;
        } else {
            ax = -dz;
            ay = 0;
            az = dx;
        }
        float al = (float) Math.sqrt(ax * ax + ay * ay + az * az);
        ax /= al;
        ay /= al;
        az /= al;
        FRAME[0] = ax;
        FRAME[1] = ay;
        FRAME[2] = az;
        FRAME[3] = dy * az - dz * ay;
        FRAME[4] = dz * ax - dx * az;
        FRAME[5] = dx * ay - dy * ax;
        return FRAME;
    }

    private static void quad(VertexConsumer vc, PoseStack.Pose pose, int colour, float x0, float y0, float z0, float x1, float y1, float z1,
                             float x2, float y2, float z2, float x3, float y3, float z3) {
        v(vc, pose, x0, y0, z0, colour);
        v(vc, pose, x1, y1, z1, colour);
        v(vc, pose, x2, y2, z2, colour);
        v(vc, pose, x3, y3, z3, colour);
    }

    private static void v(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float z, int colour) {
        vc.addVertex(pose, x, y, z).setColor(colour);
    }

    /** {@code rgb} with {@code alpha} (0 to 1, clamped) as its alpha byte. */
    public static int argb(float alpha, int rgb) {
        int a = Math.round(Math.max(0, Math.min(1, alpha)) * 255);
        return a << 24 | (rgb & 0xFFFFFF);
    }

    /** Linear blend of two RGB colours. */
    public static int mix(int a, int b, float t) {
        t = Math.max(0, Math.min(1, t));
        int r = Math.round(((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
        int g = Math.round(((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
        int bl = Math.round((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
        return r << 16 | g << 8 | bl;
    }
}
