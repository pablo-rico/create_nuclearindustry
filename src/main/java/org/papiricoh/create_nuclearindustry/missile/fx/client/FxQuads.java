package org.papiricoh.create_nuclearindustry.missile.fx.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.papiricoh.create_nuclearindustry.missile.fx.FxBudget;

import java.util.Arrays;

/**
 * One frame's quads of one kind (light or smoke), in flat arrays so a sky full of trails costs no garbage. Positions go in in
 * world space and are kept relative to the camera; anything past the far plane is pulled in along its line of sight
 * ({@link FxBudget#pullIn}), so it is drawn the same size in the same place instead of being cut off.
 * <p>
 * The shapes carry their place in the shape as UV for the shaders ({@link RocketRenderTypes}); without them, the soft edges
 * go into the vertex colours instead: strips fade to their edges and discs are drawn as fans.
 */
final class FxQuads {
    /** Sides of a disc's fan when drawn without the shaders. */
    private static final int FAN = 10;

    private float[] xyz = new float[12 * 1024];
    private float[] uv = new float[8 * 1024];
    private int[] argb = new int[4 * 1024];
    private double[] depth = new double[1024];
    private int count;
    private int vertex;

    private double ex, ey, ez;
    private double limit;
    private boolean shaded;
    private Vector3f left = new Vector3f(1, 0, 0), up = new Vector3f(0, 1, 0);

    /** Starts a frame seen from {@code eye}, pulling in what lies past {@code limit} blocks. */
    void begin(Vec3 eye, double limit, boolean shaded, Vector3f left, Vector3f up) {
        this.ex = eye.x;
        this.ey = eye.y;
        this.ez = eye.z;
        this.limit = limit;
        this.shaded = shaded;
        this.left = left;
        this.up = up;
        count = 0;
        vertex = 0;
    }

    int size() {
        return count;
    }

    boolean shaded() {
        return shaded;
    }

    Vector3f left() {
        return left;
    }

    Vector3f up() {
        return up;
    }

    /** The camera's position. */
    Vec3 eye() {
        return new Vec3(ex, ey, ez);
    }

    private void grow() {
        if (count < depth.length) return;
        int n = depth.length * 2;
        xyz = Arrays.copyOf(xyz, n * 12);
        uv = Arrays.copyOf(uv, n * 8);
        argb = Arrays.copyOf(argb, n * 4);
        depth = Arrays.copyOf(depth, n);
    }

    /** Adds one vertex of the quad being built; the fourth closes it. */
    private void put(double x, double y, double z, float u, float v, int colour) {
        if (vertex == 0) grow();
        double rx = x - ex, ry = y - ey, rz = z - ez;
        double d = Math.sqrt(rx * rx + ry * ry + rz * rz);
        double s = FxBudget.pullIn(d, limit);
        int q = count;
        xyz[q * 12 + vertex * 3] = (float) (rx * s);
        xyz[q * 12 + vertex * 3 + 1] = (float) (ry * s);
        xyz[q * 12 + vertex * 3 + 2] = (float) (rz * s);
        uv[q * 8 + vertex * 2] = u;
        uv[q * 8 + vertex * 2 + 1] = v;
        argb[q * 4 + vertex] = colour;
        if (++vertex == 4) {
            vertex = 0;
            depth[q] = d;
            count++;
        }
    }

    /** Sorts so far quads draw first, for blending; light added on needs no order. */
    void sortFarToNear() {
        if (count < 2) return;
        Integer[] order = new Integer[count];
        for (int i = 0; i < count; i++) order[i] = i;
        Arrays.sort(order, (a, b) -> Double.compare(depth[b], depth[a]));
        float[] nx = new float[xyz.length];
        float[] nu = new float[uv.length];
        int[] nc = new int[argb.length];
        double[] nd = new double[depth.length];
        for (int i = 0; i < count; i++) {
            int o = order[i];
            System.arraycopy(xyz, o * 12, nx, i * 12, 12);
            System.arraycopy(uv, o * 8, nu, i * 8, 8);
            System.arraycopy(argb, o * 4, nc, i * 4, 4);
            nd[i] = depth[o];
        }
        xyz = nx;
        uv = nu;
        argb = nc;
        depth = nd;
    }

    /** Draws everything added this frame with {@code type} (shaded or plain, as begun). */
    void draw(RenderType type) {
        if (count == 0) return;
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,
                shaded ? DefaultVertexFormat.POSITION_TEX_COLOR : DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i < count * 4; i++) {
            var v = builder.addVertex(xyz[i * 3], xyz[i * 3 + 1], xyz[i * 3 + 2]);
            if (shaded) v.setUv(uv[i * 2], uv[i * 2 + 1]);
            v.setColor(argb[i]);
        }
        MeshData mesh = builder.build();
        if (mesh != null) type.draw(mesh);
    }

    // ------------------------------------------------------------------ shapes

    /**
     * A strip from {@code a} to {@code b} turned to face the camera, {@code ra} and {@code rb} blocks to either side of its
     * line, coloured {@code ca} to {@code cb}. {@code va} and {@code vb} are its lengthwise coordinates (blocks), which the
     * shaders' noise hangs on.
     */
    void strip(double ax, double ay, double az, double bx, double by, double bz, double ra, double rb, int ca, int cb, float va, float vb) {
        strip(ax, ay, az, bx, by, bz, ra, rb, ca, cb, va, vb, false);
    }

    /**
     * A solid strip (a rocket's body seen from afar): opaque out to its edges and shaded round like a cylinder by the smoke
     * shader, which leaves its outline alone.
     */
    void solidStrip(double ax, double ay, double az, double bx, double by, double bz, double ra, double rb, int ca, int cb) {
        strip(ax, ay, az, bx, by, bz, ra, rb, ca, cb, 0, 0, true);
    }

    private void strip(double ax, double ay, double az, double bx, double by, double bz, double ra, double rb, int ca, int cb, float va,
                       float vb, boolean solid) {
        double dx = bx - ax, dy = by - ay, dz = bz - az;
        double mx = (ax + bx) * 0.5 - ex, my = (ay + by) * 0.5 - ey, mz = (az + bz) * 0.5 - ez;
        // Across: perpendicular to the strip and to the line of sight.
        double sx = dy * mz - dz * my, sy = dz * mx - dx * mz, sz = dx * my - dy * mx;
        double len = Math.sqrt(sx * sx + sy * sy + sz * sz);
        if (len < 1.0e-9) return;
        sx /= len;
        sy /= len;
        sz /= len;
        float v0 = 4 + va, v1 = 4 + vb;
        // A solid strip says so with U shifted up by 8.
        float u = solid ? 8 : 0;
        int ea = shaded || solid ? ca : ca & 0x00FFFFFF, eb = shaded || solid ? cb : cb & 0x00FFFFFF;
        // Two halves meeting along the middle, so even the plain look fades to the edges.
        put(ax - sx * ra, ay - sy * ra, az - sz * ra, u - 1, v0, ea);
        put(ax, ay, az, u, v0, ca);
        put(bx, by, bz, u, v1, cb);
        put(bx - sx * rb, by - sy * rb, bz - sz * rb, u - 1, v1, eb);
        put(ax, ay, az, u, v0, ca);
        put(ax + sx * ra, ay + sy * ra, az + sz * ra, u + 1, v0, ea);
        put(bx + sx * rb, by + sy * rb, bz + sz * rb, u + 1, v1, eb);
        put(bx, by, bz, u, v1, cb);
    }

    /** A soft disc of {@code radius} round {@code (x, y, z)} facing the camera, its pattern turned by {@code spin} radians. */
    void disc(double x, double y, double z, double radius, int colour, double spin) {
        disc(x, y, z, radius, radius, null, colour, spin, 0);
    }

    /**
     * A soft ellipse round {@code (x, y, z)} facing the camera, {@code along} blocks along {@code axis} (as seen) and
     * {@code across} blocks the other way; a disc of {@code across} if {@code axis} is null or points at the camera.
     */
    void ellipse(double x, double y, double z, double along, double across, Vec3 axis, int colour) {
        disc(x, y, z, along, across, axis, colour, 0, 0);
    }

    /** A soft ring of {@code radius} round {@code (x, y, z)} facing the camera. */
    void ring(double x, double y, double z, double radius, int colour) {
        disc(x, y, z, radius, radius, null, colour, 0, -8);
    }

    private void disc(double x, double y, double z, double along, double across, Vec3 axis, int colour, double spin, float vShift) {
        double ux, uy, uz, wx, wy, wz;
        if (axis != null) {
            double tx = x - ex, ty = y - ey, tz = z - ez;
            double sx = axis.y * tz - axis.z * ty, sy = axis.z * tx - axis.x * tz, sz = axis.x * ty - axis.y * tx;
            double len = Math.sqrt(sx * sx + sy * sy + sz * sz);
            double dist = Math.sqrt(tx * tx + ty * ty + tz * tz);
            if (len < 0.15 * dist * axis.length()) {
                // Seen end on: round.
                along = across;
                ux = left.x();
                uy = left.y();
                uz = left.z();
                wx = up.x();
                wy = up.y();
                wz = up.z();
            } else {
                // Across the axis as seen, and the axis itself squared up to it.
                wx = sx / len;
                wy = sy / len;
                wz = sz / len;
                double al = axis.length();
                ux = axis.x / al;
                uy = axis.y / al;
                uz = axis.z / al;
            }
        } else {
            double c = Math.cos(spin), s = Math.sin(spin);
            ux = c * left.x() + s * up.x();
            uy = c * left.y() + s * up.y();
            uz = c * left.z() + s * up.z();
            wx = -s * left.x() + c * up.x();
            wy = -s * left.y() + c * up.y();
            wz = -s * left.z() + c * up.z();
        }
        if (shaded) {
            put(x - ux * along - wx * across, y - uy * along - wy * across, z - uz * along - wz * across, -1, -1 + vShift, colour);
            put(x + ux * along - wx * across, y + uy * along - wy * across, z + uz * along - wz * across, 1, -1 + vShift, colour);
            put(x + ux * along + wx * across, y + uy * along + wy * across, z + uz * along + wz * across, 1, 1 + vShift, colour);
            put(x - ux * along + wx * across, y - uy * along + wy * across, z - uz * along + wz * across, -1, 1 + vShift, colour);
            return;
        }
        boolean ring = vShift != 0;
        int clear = colour & 0x00FFFFFF;
        for (int k = 0; k < FAN; k++) {
            double a0 = k * 2 * Math.PI / FAN, a1 = (k + 1) * 2 * Math.PI / FAN;
            double c0 = Math.cos(a0), s0 = Math.sin(a0), c1 = Math.cos(a1), s1 = Math.sin(a1);
            double inner = ring ? 0.6 : 0;
            double ix0 = c0 * inner, iy0 = s0 * inner, ix1 = c1 * inner, iy1 = s1 * inner;
            put(x + ux * along * ix0 + wx * across * iy0, y + uy * along * ix0 + wy * across * iy0, z + uz * along * ix0 + wz * across * iy0,
                    0, 0, ring ? clear : colour);
            put(x + ux * along * c0 + wx * across * s0, y + uy * along * c0 + wy * across * s0, z + uz * along * c0 + wz * across * s0,
                    0, 0, ring ? colour : clear);
            put(x + ux * along * c1 + wx * across * s1, y + uy * along * c1 + wy * across * s1, z + uz * along * c1 + wz * across * s1,
                    0, 0, ring ? colour : clear);
            put(x + ux * along * ix1 + wx * across * iy1, y + uy * along * ix1 + wy * across * iy1, z + uz * along * ix1 + wz * across * iy1,
                    0, 0, ring ? clear : colour);
        }
    }
}
