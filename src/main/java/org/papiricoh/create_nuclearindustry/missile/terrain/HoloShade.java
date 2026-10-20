package org.papiricoh.create_nuclearindustry.missile.terrain;

/**
 * Paints a terrain tile the way a military display shows it, pure: land in green phosphor brightening with height, lit from the
 * north-west, a brighter contour every {@link #CONTOUR} blocks; water cyan in the shallows darkening offshore, with a bright
 * coastline; forests stippled, snow pale, deserts yellowed. Land only foretold from the generator (not yet seen in a loaded chunk)
 * is dimmer and scan-lined. Heights are interpolated between samples, so a tile paints smoothly at any {@code scale}. Ported from
 * SupernaturalCraft's war table, with its amber map turned into a radar's.
 */
public final class HoloShade {

    public static final int CONTOUR = 16;
    static final int LOW = 0x0B3A1C, MID = 0x2DB858, HIGH = 0xB8FFC6;
    static final int SHALLOW = 0x2BD6DC, DEEP = 0x05283A, COAST = 0xA0FFFF, LINE = 0x9CFFB0;

    private HoloShade() {
    }

    /** The tile ({@code spacing} blocks between samples) painted at {@code scale} pixels per sample: ARGB, row-major, {@code (SIZE * scale)²}. */
    public static int[] paint(byte[] tile, int spacing, int sea, int scale) {
        int n = TerrainTile.SIZE * scale;
        int side = n + 1;
        float[] h = new float[side * side];
        float[] wet = new float[side * side];
        for (int y = 0; y < side; y++) {
            for (int x = 0; x < side; x++) {
                float u = (float) x / scale, v = (float) y / scale;
                int i0 = Math.min((int) u, TerrainTile.SIZE - 1), j0 = Math.min((int) v, TerrainTile.SIZE - 1);
                float fx = u - i0, fy = v - j0;
                h[y * side + x] = lerp(lerp(TerrainTile.height(tile, i0, j0), TerrainTile.height(tile, i0 + 1, j0), fx),
                        lerp(TerrainTile.height(tile, i0, j0 + 1), TerrainTile.height(tile, i0 + 1, j0 + 1), fx), fy);
                wet[y * side + x] = lerp(lerp(water(tile, i0, j0), water(tile, i0 + 1, j0), fx),
                        lerp(water(tile, i0, j0 + 1), water(tile, i0 + 1, j0 + 1), fx), fy);
            }
        }
        float perPixel = (float) spacing / scale;
        // Contours every 16 blocks close up; sparser where a pixel spans more ground, so they never fill the land.
        int contour = CONTOUR * Math.max(1, (int) (perPixel / 4));
        int[] out = new int[n * n];
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                int k = y * side + x;
                int surface = TerrainTile.surface(tile, Math.min(TerrainTile.SIZE, Math.round((float) x / scale)),
                        Math.min(TerrainTile.SIZE, Math.round((float) y / scale)));
                boolean water = wet[k] >= 0.5f;
                boolean real = (surface & TerrainTile.REAL) != 0;
                int rgb;
                float alpha;
                if (water) {
                    boolean coast = wet[k + 1] < 0.5f || wet[k + side] < 0.5f || (x > 0 && wet[k - 1] < 0.5f) || (y > 0 && wet[k - side] < 0.5f);
                    float depth = clamp((sea - h[k]) / 30f);
                    rgb = coast ? COAST : mix(SHALLOW, DEEP, depth);
                    alpha = coast ? 0.95f : 0.8f;
                } else {
                    float t = clamp(0.15f + (h[k] - sea) / 80f);
                    rgb = t < 0.6f ? mix(LOW, MID, t / 0.6f) : mix(MID, HIGH, (t - 0.6f) / 0.4f);
                    rgb = tint(rgb, TerrainKind.of(surface & TerrainTile.KIND_MASK), x, y);
                    // Lit from the north-west: brighter on slopes facing it.
                    float slope = ((x > 0 ? h[k - 1] : h[k]) - h[k + 1] + (y > 0 ? h[k - side] : h[k]) - h[k + side]) / (perPixel * 4f);
                    rgb = scale(rgb, 0.95f + Math.max(-0.35f, Math.min(0.35f, slope * 0.6f)));
                    if (Math.floorDiv((int) h[k], contour) != Math.floorDiv((int) h[k + 1], contour)
                            || Math.floorDiv((int) h[k], contour) != Math.floorDiv((int) h[k + side], contour)) {
                        rgb = mix(rgb, LINE, 0.5f);
                    }
                    alpha = 0.88f;
                }
                if (!real) {
                    rgb = scale(rgb, (y & 1) == 0 ? 0.8f : 0.7f);
                    alpha *= 0.85f;
                }
                out[y * n + x] = ((int) (alpha * 255) << 24) | rgb;
            }
        }
        return out;
    }

    /** The sum of a colour's channels. */
    public static int brightness(int argb) {
        return ((argb >> 16) & 0xFF) + ((argb >> 8) & 0xFF) + (argb & 0xFF);
    }

    private static float water(byte[] tile, int i, int j) {
        return (TerrainTile.surface(tile, i, j) & TerrainTile.WATER) != 0 ? 1f : 0f;
    }

    private static int tint(int rgb, TerrainKind kind, int x, int y) {
        return switch (kind) {
            case SNOW -> mix(rgb, 0xDCFFEA, 0.45f);
            case DESERT, BADLANDS -> mix(rgb, 0xB8D860, 0.3f);
            case SWAMP -> mix(rgb, 0x3A6A30, 0.3f);
            case MUSHROOM -> mix(rgb, 0x70D0B0, 0.3f);
            case FOREST, JUNGLE -> (x * 3 + y * 5) % 7 == 0 ? scale(rgb, kind == TerrainKind.JUNGLE ? 0.55f : 0.65f) : rgb;
            default -> rgb;
        };
    }

    public static int mix(int a, int b, float t) {
        t = clamp(t);
        int r = (int) (((a >> 16) & 0xFF) + (((b >> 16) & 0xFF) - ((a >> 16) & 0xFF)) * t);
        int g = (int) (((a >> 8) & 0xFF) + (((b >> 8) & 0xFF) - ((a >> 8) & 0xFF)) * t);
        int bl = (int) ((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * t);
        return (r << 16) | (g << 8) | bl;
    }

    public static int scale(int rgb, float f) {
        int r = Math.min(255, (int) (((rgb >> 16) & 0xFF) * f));
        int g = Math.min(255, (int) (((rgb >> 8) & 0xFF) * f));
        int b = Math.min(255, (int) ((rgb & 0xFF) * f));
        return (r << 16) | (g << 8) | b;
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static float clamp(float t) {
        return Math.max(0f, Math.min(1f, t));
    }
}
