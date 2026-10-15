package org.papiricoh.create_nuclearindustry.controlroom.annunciator;

/**
 * Where everything sits on the face of an annunciator board, a rectangle of panels hung side by side
 * and one above the other: the one source the renderer draws from and clicks are read against.
 * Coordinates are face pixels (16 to a block) from the board's top-left corner as seen from the front,
 * y down; blocks are counted from the left and from the top.
 * <p>
 * Each panel holds its six windows, two columns of three, in its upper twelve pixels; under them runs
 * a strip. The strip of the bottom row carries the board's one set of buttons (TEST, SILENCE, ACK,
 * RESET) at its right end, and every strip shows its panel's engraved group nameplate where the
 * buttons leave room. No game classes.
 */
public final class AnnunciatorBoardLayout {
    public static final int BLOCK = 16;
    public static final int COLUMNS = 2;
    public static final int ROWS = 3;
    /** Narrowest a nameplate is drawn. */
    static final float MIN_NAMEPLATE = 6;

    public enum Button {
        TEST, SILENCE, ACK, RESET;

        private static final Button[] VALUES = values();
    }

    /** A rectangle, x0/y0 inclusive, x1/y1 exclusive. */
    public record Box(float x0, float y0, float x1, float y1) {
        public boolean contains(float x, float y) {
            return x >= x0 && x < x1 && y >= y0 && y < y1;
        }

        public float width() {
            return x1 - x0;
        }

        public float height() {
            return y1 - y0;
        }
    }

    public enum HitKind { NONE, WINDOW, BUTTON, NAMEPLATE }

    /**
     * What a click on the face landed on.
     *
     * @param blockX the panel, from the left
     * @param blockY the panel, from the top
     * @param index  the window (0-5) of that panel, or the button's ordinal
     */
    public record Hit(HitKind kind, int blockX, int blockY, int index) {
        public static final Hit NONE = new Hit(HitKind.NONE, -1, -1, -1);

        public Button button() {
            return kind == HitKind.BUTTON ? Button.VALUES[index] : null;
        }
    }

    private final int width;
    private final int height;

    /** A board {@code width} panels wide and {@code height} high. */
    public AnnunciatorBoardLayout(int width, int height) {
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    /** Window {@code index} (left to right, top to bottom) of the panel at {@code blockX, blockY}. */
    public Box window(int blockX, int blockY, int index) {
        int column = index % COLUMNS;
        int row = index / COLUMNS;
        float x0 = blockX * BLOCK + (column == 0 ? 1 : 9);
        float y0 = blockY * BLOCK + 1 + row * 4;
        return new Box(x0, y0, x0 + 6, y0 + 3);
    }

    /** The strip under a panel's windows. */
    public Box strip(int blockX, int blockY) {
        float y0 = blockY * BLOCK + 13;
        return new Box(blockX * BLOCK + 1, y0, blockX * BLOCK + 15, y0 + 2);
    }

    /** How wide one button is: a lone panel squeezes four into its strip. */
    private float buttonWidth() {
        return width == 1 ? 3 : 5;
    }

    private float buttonGap() {
        return width == 1 ? 0.5f : 1;
    }

    /** One of the board's buttons, at the right end of the bottom strip. */
    public Box button(Button button) {
        float w = buttonWidth();
        float gap = buttonGap();
        int fromRight = Button.VALUES.length - 1 - button.ordinal();
        float x1 = width * BLOCK - 1 - fromRight * (w + gap);
        float y0 = (height - 1) * BLOCK + 13;
        return new Box(x1 - w, y0, x1, y0 + 2);
    }

    /** Where the buttons start, the left edge of the leftmost. */
    public float buttonsLeft() {
        return button(Button.TEST).x0();
    }

    /** A panel's nameplate, where the buttons leave room for one, or null. */
    public Box nameplate(int blockX, int blockY) {
        Box strip = strip(blockX, blockY);
        float x1 = strip.x1();
        if (blockY == height - 1) {
            x1 = Math.min(x1, buttonsLeft() - 1);
        }
        return x1 - strip.x0() >= MIN_NAMEPLATE ? new Box(strip.x0(), strip.y0(), x1, strip.y1()) : null;
    }

    /** What lies under face pixel {@code x, y}. */
    public Hit hit(float x, float y) {
        if (x < 0 || y < 0 || x >= width * BLOCK || y >= height * BLOCK) {
            return Hit.NONE;
        }
        for (Button button : Button.VALUES) {
            if (button(button).contains(x, y)) {
                return new Hit(HitKind.BUTTON, -1, -1, button.ordinal());
            }
        }
        int blockX = (int) Math.floor(x / BLOCK);
        int blockY = (int) Math.floor(y / BLOCK);
        for (int i = 0; i < COLUMNS * ROWS; i++) {
            if (window(blockX, blockY, i).contains(x, y)) {
                return new Hit(HitKind.WINDOW, blockX, blockY, i);
            }
        }
        Box nameplate = nameplate(blockX, blockY);
        if (nameplate != null && nameplate.contains(x, y)) {
            return new Hit(HitKind.NAMEPLATE, blockX, blockY, -1);
        }
        return Hit.NONE;
    }
}
