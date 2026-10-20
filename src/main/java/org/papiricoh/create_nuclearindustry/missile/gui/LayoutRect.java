package org.papiricoh.create_nuclearindustry.missile.gui;

/**
 * A rectangle of a screen texture, in texture pixels: {@code x, y} its top left, {@code w × h} its size. The missile screens
 * keep theirs in layout classes ({@link SiloLayout}, {@link HoloLayout}) that {@code tools/texturegen/missile_layouts.py}
 * mirrors, so the painted sheet and the drawing code agree.
 */
public record LayoutRect(String name, int x, int y, int w, int h) {
    public int right() { return x + w; }
    public int bottom() { return y + h; }

    public boolean contains(double px, double py) {
        return px >= x && px < x + w && py >= y && py < y + h;
    }

    public boolean overlaps(LayoutRect o) {
        return x < o.right() && o.x < right() && y < o.bottom() && o.y < bottom();
    }

    /** Whether {@code o} lies wholly inside it. */
    public boolean encloses(LayoutRect o) {
        return o.x >= x && o.y >= y && o.right() <= right() && o.bottom() <= bottom();
    }
}
