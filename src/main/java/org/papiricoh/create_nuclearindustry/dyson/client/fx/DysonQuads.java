package org.papiricoh.create_nuclearindustry.dyson.client.fx;

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
 * One frame's camera-facing quads of one render type, in flat arrays so a sky full of sails costs no garbage. The same contract as
 * the rocket effects' builder (which is private to its package): world positions in, kept relative to the camera and pulled in
 * past {@code limit} along their line of sight ({@link FxBudget#pullIn}); shaded quads carry their place in the shape as UV for
 * {@code rocket_glow}, plain ones carry their soft edges in the vertex colours.
 */
final class DysonQuads {
    /** Sides of a soft disc's fan, and of a solid dot's, when drawn without the shaders. */
    private static final int FAN = 10, DOT = 6;
    private static final double[] FAN_COS = new double[FAN + 1], FAN_SIN = new double[FAN + 1];
    private static final double[] DOT_COS = new double[DOT + 1], DOT_SIN = new double[DOT + 1];

    static {
        for (int k = 0; k <= FAN; k++) {
            FAN_COS[k] = Math.cos(k * 2 * Math.PI / FAN);
            FAN_SIN[k] = Math.sin(k * 2 * Math.PI / FAN);
        }
        for (int k = 0; k <= DOT; k++) {
            DOT_COS[k] = Math.cos(k * 2 * Math.PI / DOT);
            DOT_SIN[k] = Math.sin(k * 2 * Math.PI / DOT);
        }
    }

    private float[] xyz = new float[12 * 1024];
    private float[] uv = new float[8 * 1024];
    private int[] argb = new int[4 * 1024];
    private int capacity = 1024;
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

    Vector3f left() {
        return left;
    }

    Vector3f up() {
        return up;
    }

    /** Distance from the camera to a point. */
    double distance(double x, double y, double z) {
        double dx = x - ex, dy = y - ey, dz = z - ez;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private void grow() {
        if (count < capacity) return;
        capacity *= 2;
        xyz = Arrays.copyOf(xyz, capacity * 12);
        uv = Arrays.copyOf(uv, capacity * 8);
        argb = Arrays.copyOf(argb, capacity * 4);
    }

    private void put(double x, double y, double z, float u, float v, int colour) {
        if (vertex == 0) grow();
        double rx = x - ex, ry = y - ey, rz = z - ez;
        double s = FxBudget.pullIn(Math.sqrt(rx * rx + ry * ry + rz * rz), limit);
        int i = count * 4 + vertex;
        xyz[i * 3] = (float) (rx * s);
        xyz[i * 3 + 1] = (float) (ry * s);
        xyz[i * 3 + 2] = (float) (rz * s);
        uv[i * 2] = u;
        uv[i * 2 + 1] = v;
        argb[i] = colour;
        if (++vertex == 4) {
            vertex = 0;
            count++;
        }
    }

    /** Draws everything added this frame with {@code type}: a shaded type if begun shaded, a position–colour one if not. */
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
        count = 0;
    }

    // ------------------------------------------------------------------ shapes

    /**
     * A strip from {@code a} to {@code b} turned to face the camera, {@code ra} and {@code rb} blocks to either side of its line,
     * coloured {@code ca} to {@code cb} along its middle and fading to its edges. {@code va}, {@code vb}: blocks along it, which the
     * glow shader's flicker hangs on.
     */
    void strip(double ax, double ay, double az, double bx, double by, double bz, double ra, double rb, int ca, int cb, float va, float vb) {
        double dx = bx - ax, dy = by - ay, dz = bz - az;
        double mx = (ax + bx) * 0.5 - ex, my = (ay + by) * 0.5 - ey, mz = (az + bz) * 0.5 - ez;
        double sx = dy * mz - dz * my, sy = dz * mx - dx * mz, sz = dx * my - dy * mx;
        double len = Math.sqrt(sx * sx + sy * sy + sz * sz);
        if (len < 1.0e-9) return;
        sx /= len;
        sy /= len;
        sz /= len;
        float v0 = 4 + va, v1 = 4 + vb;
        int ea = shaded ? ca : ca & 0x00FFFFFF, eb = shaded ? cb : cb & 0x00FFFFFF;
        put(ax - sx * ra, ay - sy * ra, az - sz * ra, -1, v0, ea);
        put(ax, ay, az, 0, v0, ca);
        put(bx, by, bz, 0, v1, cb);
        put(bx - sx * rb, by - sy * rb, bz - sz * rb, -1, v1, eb);
        put(ax, ay, az, 0, v0, ca);
        put(ax + sx * ra, ay + sy * ra, az + sz * ra, 1, v0, ea);
        put(bx + sx * rb, by + sy * rb, bz + sz * rb, 1, v1, eb);
        put(bx, by, bz, 0, v1, cb);
    }

    /** A soft disc of {@code radius} round a point, facing the camera, its pattern turned by {@code spin} radians. */
    void disc(double x, double y, double z, double radius, int colour, double spin) {
        round(x, y, z, radius, colour, spin, false);
    }

    /** A soft ring of {@code radius} round a point, facing the camera. */
    void ring(double x, double y, double z, double radius, int colour) {
        round(x, y, z, radius, colour, 0, true);
    }

    private void round(double x, double y, double z, double r, int colour, double spin, boolean ring) {
        double c = Math.cos(spin), s = Math.sin(spin);
        double ux = (c * left.x() + s * up.x()) * r, uy = (c * left.y() + s * up.y()) * r, uz = (c * left.z() + s * up.z()) * r;
        double wx = (-s * left.x() + c * up.x()) * r, wy = (-s * left.y() + c * up.y()) * r, wz = (-s * left.z() + c * up.z()) * r;
        if (shaded) {
            float shift = ring ? -8 : 0;
            put(x - ux - wx, y - uy - wy, z - uz - wz, -1, -1 + shift, colour);
            put(x + ux - wx, y + uy - wy, z + uz - wz, 1, -1 + shift, colour);
            put(x + ux + wx, y + uy + wy, z + uz + wz, 1, 1 + shift, colour);
            put(x - ux + wx, y - uy + wy, z - uz + wz, -1, 1 + shift, colour);
            return;
        }
        int clear = colour & 0x00FFFFFF;
        double inner = ring ? 0.6 : 0;
        for (int k = 0; k < FAN; k++) {
            double c0 = FAN_COS[k], s0 = FAN_SIN[k], c1 = FAN_COS[k + 1], s1 = FAN_SIN[k + 1];
            put(x + (ux * c0 + wx * s0) * inner, y + (uy * c0 + wy * s0) * inner, z + (uz * c0 + wz * s0) * inner, 0, 0, ring ? clear : colour);
            put(x + ux * c0 + wx * s0, y + uy * c0 + wy * s0, z + uz * c0 + wz * s0, 0, 0, ring ? colour : clear);
            put(x + ux * c1 + wx * s1, y + uy * c1 + wy * s1, z + uz * c1 + wz * s1, 0, 0, ring ? colour : clear);
            put(x + (ux * c1 + wx * s1) * inner, y + (uy * c1 + wy * s1) * inner, z + (uz * c1 + wz * s1) * inner, 0, 0, ring ? clear : colour);
        }
    }

    /**
     * A solid dot facing the camera, {@code centre} in the middle and {@code rim} at its edge: paint rather than light, for what
     * must read against the daylit sky. Always a plain fan; begin this builder unshaded.
     */
    void dot(double x, double y, double z, double r, int centre, int rim) {
        double ux = left.x() * r, uy = left.y() * r, uz = left.z() * r;
        double wx = up.x() * r, wy = up.y() * r, wz = up.z() * r;
        for (int k = 0; k < DOT; k++) {
            double c0 = DOT_COS[k], s0 = DOT_SIN[k], c1 = DOT_COS[k + 1], s1 = DOT_SIN[k + 1];
            put(x, y, z, 0, 0, centre);
            put(x + ux * c0 + wx * s0, y + uy * c0 + wy * s0, z + uz * c0 + wz * s0, 0, 0, rim);
            put(x + ux * c1 + wx * s1, y + uy * c1 + wy * s1, z + uz * c1 + wz * s1, 0, 0, rim);
            put(x, y, z, 0, 0, centre);
        }
    }

    /** {@code rgb} with {@code alpha} (0 to 1, clamped) as its alpha byte. */
    static int argb(double alpha, int rgb) {
        int a = (int) Math.round(Math.max(0, Math.min(1, alpha)) * 255);
        return a << 24 | (rgb & 0xFFFFFF);
    }
}
