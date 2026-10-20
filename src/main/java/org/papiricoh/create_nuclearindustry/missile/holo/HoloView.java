package org.papiricoh.create_nuclearindustry.missile.holo;

/**
 * The command table's hologram geometry, pure: which view (blocks across) holds everything worth showing, and how heights are
 * squeezed onto the table so the highest arc stands at most {@link #MAX_HEIGHT} blocks over the glass.
 */
public final class HoloView {
    /** The views the table switches between, blocks across. */
    public static final int[] VIEWS = {1024, 2048, 4096, 8192, 16384};
    /** The highest an arc rises over the glass, blocks. */
    public static final float MAX_HEIGHT = 1.5f;
    /** Heights are exaggerated over the map's scale at most this much. */
    public static final float EXAGGERATION = 6;

    private HoloView() {
    }

    /** The smallest view that shows a point {@code farthest} blocks (along x or z) from the table, with a margin. */
    public static int pick(double farthest) {
        for (int view : VIEWS) if (farthest <= view * 0.45) return view;
        return VIEWS[VIEWS.length - 1];
    }

    /** Blocks of hologram per block of height, for a view {@code view} across a glass {@code size} blocks wide, with arcs up to {@code top}. */
    public static float heightScale(int view, float size, double top) {
        return (float) Math.min(MAX_HEIGHT / Math.max(50, top), size / view * EXAGGERATION);
    }

    /** The level of terrain detail for a view: about one sample per pixel of a tile drawn over the table. */
    public static int lod(int view) {
        return view <= 1024 ? 0 : view <= 4096 ? 1 : view <= 8192 ? 2 : 3;
    }
}
