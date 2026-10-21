package org.papiricoh.create_nuclearindustry.fusion.heating.client;

import org.papiricoh.create_nuclearindustry.fusion.multiblock.TokamakGeometry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The faceted skin a formed tokamak wears over its hidden hull: an octagonal cross-section (the 5x5
 * shell with its corners cut, pulled {@link #INSET} inside the blocks so nozzles poke out) swept
 * around the major radius in flat segments, so the ring reads round from above too. Each side of the
 * octagon is split into patches of about a block, each mapping one whole texture; a viewport cuts
 * its patch out and gets a window instead. White coil bands sit a pixel proud of the skin, and a
 * dark liner faces inwards so the far wall shows through a window. Pure geometry, relative to the
 * centre of the solenoid's middle block; built once per radius and set of windows.
 */
public final class TokamakSkinMesh {
    /** How far inside the outer block faces the skin lies, in blocks. */
    public static final double INSET = 0.25;
    /** Coil bands stand this far proud of the skin. */
    public static final double BAND_RISE = 1 / 16.0;
    /** Half the width of a coil band at the outer equator, in blocks. */
    public static final double BAND_HALF_WIDTH = 0.3;
    /** How far behind the window rim the glass sits. */
    public static final double GLASS_DEPTH = 0.06;
    public static final int SIDES = 8;
    /** Patches along each side: flat sides span three blocks, chamfers one. */
    static final int[] PATCHES = {3, 1, 3, 1, 3, 1, 3, 1};

    public enum Kind { SKIN, CHAMFER, BAND, LINER, WINDOW, GLASS }

    /**
     * One quad, corners counter-clockwise seen from the side its normal points to.
     *
     * @param xyz   four corners, x y z each
     * @param uv    four corners, u v each in 0..1 of the sprite
     * @param light the block to sample light from, relative to the centre
     */
    public record Quad(Kind kind, float[] xyz, float[] uv, float nx, float ny, float nz, int[] light) {}

    private final int radius;
    private final int segments;
    private final Set<Long> holes;
    private final List<Quad> quads = new ArrayList<>();

    private TokamakSkinMesh(int radius, Set<Long> holes) {
        this.radius = radius;
        this.segments = segmentsFor(radius);
        this.holes = holes;
    }

    /**
     * @param viewports cells, {dx, dy, dz} from the centre, that open a window
     * @param features  cells of parts that poke out of the skin; coil bands keep clear of them
     */
    public static TokamakSkinMesh build(int radius, Collection<int[]> viewports, Collection<int[]> features) {
        Set<Long> holes = new HashSet<>();
        for (int[] cell : viewports) {
            long patch = patchFor(radius, cell[0], cell[1], cell[2]);
            if (patch >= 0) {
                holes.add(patch);
            }
        }
        TokamakSkinMesh mesh = new TokamakSkinMesh(radius, holes);
        mesh.buildSkin();
        List<int[]> clear = new ArrayList<>(viewports);
        clear.addAll(features);
        mesh.buildBands(clear);
        return mesh;
    }

    /** Two flat segments per toroidal sector of {@link TokamakGeometry}. */
    public static int segmentsFor(int radius) {
        return 8 * radius;
    }

    /** Coil bands: one every two sectors. */
    public static int bandsFor(int radius) {
        return 2 * radius;
    }

    public int radius() {
        return radius;
    }

    public int segments() {
        return segments;
    }

    public List<Quad> quads() {
        return quads;
    }

    public int holeCount() {
        return holes.size();
    }

    /** Half-extent of the octagon's flat sides: the outer wall reaches {@code R + a}. */
    static double a(double rise) {
        return 2.5 - INSET + rise;
    }

    /** Where a flat side meets a chamfer, keeping the chamfer at 45 degrees. */
    static double c(double rise) {
        return 1.5 - (INSET - rise) * (Math.sqrt(2) - 1);
    }

    /** Octagon corner {@code k} as {radial offset from the major radius, height}, counter-clockwise from the outer bottom. */
    static double[] corner(int k, double rise) {
        double a = a(rise);
        double c = c(rise);
        return switch (Math.floorMod(k, SIDES)) {
            case 0 -> new double[]{a, -c};
            case 1 -> new double[]{a, c};
            case 2 -> new double[]{c, a};
            case 3 -> new double[]{-c, a};
            case 4 -> new double[]{-a, c};
            case 5 -> new double[]{-a, -c};
            case 6 -> new double[]{-c, -a};
            default -> new double[]{c, -a};
        };
    }

    /** Patch key, or -1 when the cell is not on a flat side of the shell. */
    static long key(int segment, int side, int patch) {
        return ((long) segment * SIDES + side) * 4 + patch;
    }

    /**
     * The patch of skin over the shell cell {@code (dx, dy, dz)}, or -1 when the cell is not shell.
     * Shell cells only ever sit under the flat sides; the chamfers run over the empty corners.
     */
    public static long patchFor(int radius, int dx, int dy, int dz) {
        int offset = TokamakGeometry.rho(dx, dz) - radius;
        int side;
        int patch;
        if (offset == 2 && Math.abs(dy) <= 1) {
            side = 0;
            patch = dy + 1;
        } else if (dy == 2 && Math.abs(offset) <= 1) {
            side = 2;
            patch = 1 - offset;
        } else if (offset == -2 && Math.abs(dy) <= 1) {
            side = 4;
            patch = 1 - dy;
        } else if (dy == -2 && Math.abs(offset) <= 1) {
            side = 6;
            patch = offset + 1;
        } else {
            return -1;
        }
        return key(segmentOf(radius, dx, dz), side, patch);
    }

    static int segmentOf(int radius, int dx, int dz) {
        double angle = Math.atan2(dz, dx);
        if (angle < 0) {
            angle += 2 * Math.PI;
        }
        int segments = segmentsFor(radius);
        return Math.min(segments - 1, (int) Math.floor(angle / (2 * Math.PI) * segments));
    }

    private void buildSkin() {
        for (int j = 0; j < segments; j++) {
            double t0 = 2 * Math.PI * j / segments;
            double t1 = 2 * Math.PI * (j + 1) / segments;
            for (int side = 0; side < SIDES; side++) {
                double[] from = corner(side, 0);
                double[] to = corner(side + 1, 0);
                int patches = PATCHES[side];
                for (int p = 0; p < patches; p++) {
                    double[] s0 = lerp(from, to, p / (double) patches);
                    double[] s1 = lerp(from, to, (p + 1) / (double) patches);
                    if (holes.contains(key(j, side, p))) {
                        window(t0, t1, s0, s1, side);
                        continue;
                    }
                    Kind kind = side % 2 == 1 ? Kind.CHAMFER : Kind.SKIN;
                    add(kind, t0, t1, s0, s1, 0, 1, 0, 1, side, false);
                    add(Kind.LINER, t0, t1, s0, s1, 0, 1, 0, 1, side, true);
                }
            }
        }
    }

    /** A brass porthole rim flush with the skin and the glass just behind it. */
    private void window(double t0, double t1, double[] s0, double[] s1, int side) {
        add(Kind.WINDOW, t0, t1, s0, s1, 0, 1, 0, 1, side, false);
        double[] inward = outward(side);
        double[] g0 = {s0[0] - inward[0] * GLASS_DEPTH, s0[1] - inward[1] * GLASS_DEPTH};
        double[] g1 = {s1[0] - inward[0] * GLASS_DEPTH, s1[1] - inward[1] * GLASS_DEPTH};
        add(Kind.GLASS, t0, t1, g0, g1, 0, 1, 0, 1, side, false);
    }

    private void buildBands(List<int[]> clear) {
        int bands = bandsFor(radius);
        double half = BAND_HALF_WIDTH / (radius + a(0));
        double margin = half + 2 * Math.PI / segments;
        for (int b = 0; b < bands; b++) {
            double theta = 2 * Math.PI * (b + 0.5) / bands;
            if (blocked(theta, margin, clear)) {
                continue;
            }
            double t0 = theta - half;
            double t1 = theta + half;
            for (int side = 0; side < SIDES; side++) {
                double[] from = corner(side, BAND_RISE);
                double[] to = corner(side + 1, BAND_RISE);
                int patches = PATCHES[side];
                for (int p = 0; p < patches; p++) {
                    add(Kind.BAND, t0, t1, lerp(from, to, p / (double) patches), lerp(from, to, (p + 1) / (double) patches),
                            0, 1, 0, 1, side, false);
                }
                // The band's two edges, from the skin up to its face.
                double[] skinFrom = corner(side, 0);
                double[] skinTo = corner(side + 1, 0);
                edge(t0, skinFrom, skinTo, from, to, side, true);
                edge(t1, skinFrom, skinTo, from, to, side, false);
            }
        }
    }

    private static boolean blocked(double theta, double margin, List<int[]> clear) {
        for (int[] cell : clear) {
            double angle = Math.atan2(cell[2], cell[0]);
            double diff = Math.abs(Math.IEEEremainder(angle - theta, 2 * Math.PI));
            if (diff < margin) {
                return true;
            }
        }
        return false;
    }

    /** A thin wall at angle {@code t} between the skin side and the band side above it. */
    private void edge(double t, double[] skinFrom, double[] skinTo, double[] bandFrom, double[] bandTo, int side, boolean facingBack) {
        float[] xyz = new float[12];
        put(xyz, 0, t, skinFrom);
        put(xyz, 1, t, skinTo);
        put(xyz, 2, t, bandTo);
        put(xyz, 3, t, bandFrom);
        // Tangent to the ring: the edge at the start of the band faces back along it.
        double sign = facingBack ? -1 : 1;
        double nx = -Math.sin(t) * sign;
        double nz = Math.cos(t) * sign;
        float[] uv = {0, 0, 1, 0, 1, BAND_RISE_V, 0, BAND_RISE_V};
        orient(xyz, uv, nx, 0, nz);
        quads.add(new Quad(Kind.BAND, xyz, uv, (float) nx, 0, (float) nz, lightCell(t, lerp(skinFrom, skinTo, 0.5), side)));
    }

    /** The band's edge shows only a sliver of the band texture. */
    private static final float BAND_RISE_V = 1 / 16f;

    private void add(Kind kind, double t0, double t1, double[] s0, double[] s1, float u0, float u1, float v0, float v1,
                     int side, boolean inward) {
        float[] xyz = new float[12];
        put(xyz, 0, t0, s0);
        put(xyz, 1, t1, s0);
        put(xyz, 2, t1, s1);
        put(xyz, 3, t0, s1);
        float[] uv = {u0, v0, u1, v0, u1, v1, u0, v1};
        double[] n = outward(side);
        double mid = (t0 + t1) / 2;
        double sign = inward ? -1 : 1;
        double nx = n[0] * Math.cos(mid) * sign;
        double ny = n[1] * sign;
        double nz = n[0] * Math.sin(mid) * sign;
        orient(xyz, uv, nx, ny, nz);
        double[] centre = lerp(s0, s1, 0.5);
        quads.add(new Quad(kind, xyz, uv, (float) nx, (float) ny, (float) nz, lightCell(mid, centre, inward ? (side + 4) % SIDES : side)));
    }

    /** Outward unit normal of an octagon side in the (radial, height) plane. */
    static double[] outward(int side) {
        double[] from = corner(side, 0);
        double[] to = corner(side + 1, 0);
        double dr = to[0] - from[0];
        double dy = to[1] - from[1];
        double length = Math.hypot(dr, dy);
        return new double[]{dy / length, -dr / length};
    }

    /** The open block just outside the patch (inside the chamber for the liner), for its light. */
    private int[] lightCell(double theta, double[] section, int side) {
        double[] n = outward(side);
        double r = radius + section[0] + n[0] * 0.5;
        double y = section[1] + n[1] * 0.5;
        return new int[]{(int) Math.floor(r * Math.cos(theta) + 0.5), (int) Math.floor(y + 0.5), (int) Math.floor(r * Math.sin(theta) + 0.5)};
    }

    private void put(float[] xyz, int corner, double theta, double[] section) {
        double r = radius + section[0];
        xyz[corner * 3] = (float) (r * Math.cos(theta));
        xyz[corner * 3 + 1] = (float) section[1];
        xyz[corner * 3 + 2] = (float) (r * Math.sin(theta));
    }

    /** Reverses the corners when their winding faces away from {@code n}. */
    private static void orient(float[] xyz, float[] uv, double nx, double ny, double nz) {
        double ax = xyz[3] - xyz[0], ay = xyz[4] - xyz[1], az = xyz[5] - xyz[2];
        double bx = xyz[6] - xyz[0], by = xyz[7] - xyz[1], bz = xyz[8] - xyz[2];
        double cx = ay * bz - az * by;
        double cy = az * bx - ax * bz;
        double cz = ax * by - ay * bx;
        if (cx * nx + cy * ny + cz * nz < 0) {
            swap(xyz, uv, 1, 3);
        }
    }

    private static void swap(float[] xyz, float[] uv, int i, int j) {
        for (int k = 0; k < 3; k++) {
            float t = xyz[i * 3 + k];
            xyz[i * 3 + k] = xyz[j * 3 + k];
            xyz[j * 3 + k] = t;
        }
        for (int k = 0; k < 2; k++) {
            float t = uv[i * 2 + k];
            uv[i * 2 + k] = uv[j * 2 + k];
            uv[j * 2 + k] = t;
        }
    }

    private static double[] lerp(double[] from, double[] to, double f) {
        return new double[]{from[0] + (to[0] - from[0]) * f, from[1] + (to[1] - from[1]) * f};
    }
}
