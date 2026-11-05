package org.papiricoh.create_nuclearindustry.explosive.fx.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.papiricoh.create_nuclearindustry.explosive.fx.model.BlastFxModel;
import org.papiricoh.create_nuclearindustry.explosive.fx.model.CloudEvolution;
import org.papiricoh.create_nuclearindustry.explosive.fx.model.CloudLighting;
import org.papiricoh.create_nuclearindustry.missile.fx.FxBudget;

import java.util.Arrays;

/**
 * Puffs gathered for drawing, in flat arrays that are reused (a cloud's hundreds of puffs ten times a second cost no garbage),
 * lit, sorted far to near by an index sort, and written as billboards facing the camera's position: textured quads on the cloud
 * atlas, or, under a shader pack, soft vertex-coloured fans. Positions are kept in the list's own frame (a cloud's ground zero,
 * or the world) and written relative to a reference point (the cloud's origin for its cached buffer, the camera for a batch drawn
 * at once).
 */
final class PuffList implements CloudEvolution.PuffSink, BlastFxModel.SmokeSink {
    /** Sides of a fan under a shader pack. */
    private static final int FAN = 8;
    /** The share of a fan's radius that is solid before it fades out. */
    private static final float CORE = 0.5f;
    /**
     * How much bigger than a puff's radius its textured quad is: the atlas' puffs fill only the middle of their tiles, with soft
     * edges, so a puff's quad is drawn this much bigger for what shows to be its radius.
     */
    private static final double QUAD_SCALE = 1.5;
    /** Half a texel of the atlas, so a tile never bleeds into the next. */
    private static final float INSET = 0.5f / 512;

    private int size;
    private double[] x = new double[256], y = new double[256], z = new double[256];
    private float[] radius = new float[256], spin = new float[256], red = new float[256], green = new float[256], blue = new float[256],
            alpha = new float[256], heat = new float[256], nx = new float[256], ny = new float[256], nz = new float[256],
            occlusion = new float[256];
    private int[] tile = new int[256], flags = new int[256];
    private long[] order = new long[256];
    private boolean sorted;

    private double ox, oy, oz;
    private float growth = 1, fade = 1;
    private final float[] lit = new float[3];

    void clear() {
        size = 0;
        sorted = false;
        ox = oy = oz = 0;
        growth = 1;
        fade = 1;
    }

    int size() {
        return size;
    }

    /** Puffs added from now on are moved by this much (a source's place in the list's frame). */
    void offset(double x, double y, double z) {
        this.ox = x;
        this.oy = y;
        this.oz = z;
    }

    /** Puffs added from now on are this much bigger (fewer of them, far off) and this much fainter. */
    void scale(float growth, float fade) {
        this.growth = growth;
        this.fade = fade;
    }

    @Override
    public void puff(double px, double py, double pz, double r, int t, float s, float cr, float cg, float cb, float a, float h, float fx,
                     float fy, float fz, float occ, int f) {
        if (a * fade <= 0.004f || r <= 0) return;
        if (size == x.length) grow();
        int i = size++;
        x[i] = px + ox;
        y[i] = py + oy;
        z[i] = pz + oz;
        radius[i] = (float) r * growth;
        tile[i] = t;
        spin[i] = s;
        red[i] = cr;
        green[i] = cg;
        blue[i] = cb;
        alpha[i] = Math.min(1, a * fade);
        heat[i] = h;
        nx[i] = fx;
        ny[i] = fy;
        nz[i] = fz;
        occlusion[i] = occ;
        flags[i] = f;
        sorted = false;
    }

    @Override
    public void puff(double px, double py, double pz, double r, int t, float s, float cr, float cg, float cb, float a) {
        puff(px, py, pz, r, t, s, cr, cg, cb, a, 0, 0, 0.7f, 0, 0.3f, 0);
    }

    private void grow() {
        int n = x.length * 2;
        x = Arrays.copyOf(x, n);
        y = Arrays.copyOf(y, n);
        z = Arrays.copyOf(z, n);
        radius = Arrays.copyOf(radius, n);
        spin = Arrays.copyOf(spin, n);
        red = Arrays.copyOf(red, n);
        green = Arrays.copyOf(green, n);
        blue = Arrays.copyOf(blue, n);
        alpha = Arrays.copyOf(alpha, n);
        heat = Arrays.copyOf(heat, n);
        nx = Arrays.copyOf(nx, n);
        ny = Arrays.copyOf(ny, n);
        nz = Arrays.copyOf(nz, n);
        occlusion = Arrays.copyOf(occlusion, n);
        tile = Arrays.copyOf(tile, n);
        flags = Arrays.copyOf(flags, n);
        order = Arrays.copyOf(order, n);
    }

    /** Turns every unlit puff's colour into the colour it is seen in under {@code sky}. */
    void light(CloudLighting.Sky sky) {
        for (int i = 0; i < size; i++) {
            if ((flags[i] & CloudEvolution.EMISSIVE) != 0) continue;
            CloudLighting.light(sky, red[i], green[i], blue[i], nx[i], ny[i], nz[i], occlusion[i], heat[i], lit);
            red[i] = lit[0];
            green[i] = lit[1];
            blue[i] = lit[2];
        }
    }

    /**
     * Sorts far to near from {@code (cx, cy, cz)} in the list's frame. A glowing puff sorts a little behind the smoke at the same
     * spot, so the smoke drawn after it veils it.
     */
    void sort(double cx, double cy, double cz) {
        for (int i = 0; i < size; i++) {
            double dx = x[i] - cx, dy = y[i] - cy, dz = z[i] - cz;
            double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if ((flags[i] & CloudEvolution.EMISSIVE) != 0) d += 0.3 * radius[i];
            // Largest distance first: a float's bits order like the float for positive values.
            long key = 0x7FFFFFFFL - Float.floatToIntBits((float) Math.max(0, d));
            order[i] = key << 32 | i;
        }
        Arrays.sort(order, 0, size);
        sorted = true;
    }

    /** The nearest any puff comes to {@code (cx, cy, cz)}, its own radius taken off. */
    double nearest(double cx, double cy, double cz) {
        double best = Double.POSITIVE_INFINITY;
        for (int i = 0; i < size; i++) {
            double dx = x[i] - cx, dy = y[i] - cy, dz = z[i] - cz;
            best = Math.min(best, Math.sqrt(dx * dx + dy * dy + dz * dz) - radius[i]);
        }
        return best;
    }

    private int index(int k) {
        return sorted ? (int) order[k] : k;
    }

    /** Which puffs a pass writes. */
    enum Filter {
        ALL, SMOKE, LIGHT;

        boolean takes(int flags) {
            boolean light = (flags & CloudEvolution.EMISSIVE) != 0;
            return this == ALL || (this == LIGHT) == light;
        }
    }

    /**
     * Writes textured billboards for the puffs {@code filter} takes, in sorted order, each facing {@code (cx, cy, cz)} and placed
     * relative to {@code (rx, ry, rz)} (all in the list's frame). Past {@code pull} blocks from the camera (0 for never) a puff is
     * drawn nearer and smaller in the same proportion ({@link FxBudget#pullIn}), so the far plane never cuts it. Returns how many
     * quads were written.
     */
    int writeTextured(VertexConsumer out, double rx, double ry, double rz, double cx, double cy, double cz, double pull, Filter filter) {
        int written = 0;
        for (int k = 0; k < size; k++) {
            int i = index(k);
            if (!filter.takes(flags[i])) continue;
            double px = x[i] - cx, py = y[i] - cy, pz = z[i] - cz;
            double d = Math.sqrt(px * px + py * py + pz * pz);
            double s = pull > 0 ? FxBudget.pullIn(d, pull) : 1;
            // Facing the camera: towards it, across, and up as seen.
            double fx = -px / Math.max(1e-6, d), fy = -py / Math.max(1e-6, d), fz = -pz / Math.max(1e-6, d);
            double rxa = fz, rza = -fx;
            double rl = Math.sqrt(rxa * rxa + rza * rza);
            double ax, ay, az, bx, by, bz;
            if (rl < 1e-4) {
                ax = 1;
                ay = 0;
                az = 0;
            } else {
                ax = rxa / rl;
                ay = 0;
                az = rza / rl;
            }
            if ((flags[i] & CloudEvolution.UPRIGHT) != 0) {
                bx = 0;
                by = 1;
                bz = 0;
            } else {
                bx = fy * az - fz * ay;
                by = fz * ax - fx * az;
                bz = fx * ay - fy * ax;
                double c = Math.cos(spin[i]), sn = Math.sin(spin[i]);
                double ax2 = c * ax + sn * bx, ay2 = c * ay + sn * by, az2 = c * az + sn * bz;
                double bx2 = -sn * ax + c * bx, by2 = -sn * ay + c * by, bz2 = -sn * az + c * bz;
                ax = ax2;
                ay = ay2;
                az = az2;
                bx = bx2;
                by = by2;
                bz = bz2;
            }
            double r = radius[i] * s * QUAD_SCALE;
            double qx = cx + px * s - rx, qy = cy + py * s - ry, qz = cz + pz * s - rz;
            int t = tile[i];
            float u0 = (t % NukeRenderTypes.TILES) / (float) NukeRenderTypes.TILES + INSET;
            float v0 = (t / NukeRenderTypes.TILES) / (float) NukeRenderTypes.TILES + INSET;
            float u1 = u0 + 1f / NukeRenderTypes.TILES - 2 * INSET, v1 = v0 + 1f / NukeRenderTypes.TILES - 2 * INSET;
            int colour = argb(alpha[i], red[i], green[i], blue[i]);
            out.addVertex((float) (qx - (ax + bx) * r), (float) (qy - (ay + by) * r), (float) (qz - (az + bz) * r)).setUv(u0, v1).setColor(colour);
            out.addVertex((float) (qx + (ax - bx) * r), (float) (qy + (ay - by) * r), (float) (qz + (az - bz) * r)).setUv(u1, v1).setColor(colour);
            out.addVertex((float) (qx + (ax + bx) * r), (float) (qy + (ay + by) * r), (float) (qz + (az + bz) * r)).setUv(u1, v0).setColor(colour);
            out.addVertex((float) (qx - (ax - bx) * r), (float) (qy - (ay - by) * r), (float) (qz - (az - bz) * r)).setUv(u0, v0).setColor(colour);
            written++;
        }
        return written;
    }

    /**
     * As {@link #writeTextured}, but each puff a soft fan of vertex colours (no texture): solid to half its radius, fading to
     * clear at its edge. For shader packs, which draw only plain colours unlit. Light (fire, lightning) is written with its
     * colour premultiplied by its strength, for adding on.
     */
    int writeFans(VertexConsumer out, double rx, double ry, double rz, double cx, double cy, double cz, double pull, Filter filter) {
        int written = 0;
        for (int k = 0; k < size; k++) {
            int i = index(k);
            if (!filter.takes(flags[i])) continue;
            double px = x[i] - cx, py = y[i] - cy, pz = z[i] - cz;
            double d = Math.sqrt(px * px + py * py + pz * pz);
            double s = pull > 0 ? FxBudget.pullIn(d, pull) : 1;
            double fx = -px / Math.max(1e-6, d), fy = -py / Math.max(1e-6, d), fz = -pz / Math.max(1e-6, d);
            double ax = fz, az = -fx;
            double al = Math.sqrt(ax * ax + az * az);
            if (al < 1e-4) {
                ax = 1;
                az = 0;
            } else {
                ax /= al;
                az /= al;
            }
            double bx = fy * az, by = fz * ax - fx * az, bz = -fy * ax;
            double r = radius[i] * s * 1.15;
            double qx = cx + px * s - rx, qy = cy + py * s - ry, qz = cz + pz * s - rz;
            boolean light = (flags[i] & CloudEvolution.EMISSIVE) != 0;
            float a = alpha[i] * (light ? 1 : 0.9f);
            int centre = argb(a, red[i], green[i], blue[i]);
            int clear = centre & 0x00FFFFFF;
            double core = r * CORE;
            for (int f = 0; f < FAN; f++) {
                double a0 = f * 2 * Math.PI / FAN, a1 = (f + 1) * 2 * Math.PI / FAN;
                double c0 = Math.cos(a0), s0 = Math.sin(a0), c1 = Math.cos(a1), s1 = Math.sin(a1);
                float e0x = (float) (c0 * ax + s0 * bx), e0y = (float) (s0 * by), e0z = (float) (c0 * az + s0 * bz);
                float e1x = (float) (c1 * ax + s1 * bx), e1y = (float) (s1 * by), e1z = (float) (c1 * az + s1 * bz);
                out.addVertex((float) qx, (float) qy, (float) qz).setColor(centre);
                out.addVertex((float) (qx + e0x * core), (float) (qy + e0y * core), (float) (qz + e0z * core)).setColor(centre);
                out.addVertex((float) (qx + e1x * core), (float) (qy + e1y * core), (float) (qz + e1z * core)).setColor(centre);
                out.addVertex((float) qx, (float) qy, (float) qz).setColor(centre);
                out.addVertex((float) (qx + e0x * core), (float) (qy + e0y * core), (float) (qz + e0z * core)).setColor(centre);
                out.addVertex((float) (qx + e0x * r), (float) (qy + e0y * r), (float) (qz + e0z * r)).setColor(clear);
                out.addVertex((float) (qx + e1x * r), (float) (qy + e1y * r), (float) (qz + e1z * r)).setColor(clear);
                out.addVertex((float) (qx + e1x * core), (float) (qy + e1y * core), (float) (qz + e1z * core)).setColor(centre);
                written += 2;
            }
        }
        return written;
    }

    /** Whether any puff {@code filter} takes is here. */
    boolean has(Filter filter) {
        for (int i = 0; i < size; i++) {
            if (filter.takes(flags[i])) return true;
        }
        return false;
    }

    static int argb(float a, float r, float g, float b) {
        return Math.round(clamp(a) * 255) << 24 | Math.round(clamp(r) * 255) << 16 | Math.round(clamp(g) * 255) << 8 | Math.round(clamp(b) * 255);
    }

    private static float clamp(float v) {
        return Math.max(0, Math.min(1, v));
    }
}
