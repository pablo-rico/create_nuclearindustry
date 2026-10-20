package org.papiricoh.create_nuclearindustry.missile.gui;

import java.util.List;

/**
 * Where everything sits on the command table's screen, in texture pixels: the map on the left, a tabbed list of linked
 * silos, radars or tracks on the right, and the salvo and command buttons under it. {@code holo_table_screen.png} and
 * {@code holo_table_widgets.png} are painted from {@code tools/texturegen/missile_layouts.py} ({@code HOLO_*}); JUnit keeps
 * both in step.
 */
public final class HoloLayout {
    public static final int WIDTH = 400, HEIGHT = 256;

    public static final LayoutRect TITLE = new LayoutRect("title", 6, 4, 388, 13);
    /** The map's bezel; the map itself is {@link #MAP}. */
    public static final LayoutRect MAP_FRAME = new LayoutRect("map_frame", 6, 20, 212, 212);
    public static final LayoutRect MAP = new LayoutRect("map", 12, 26, 200, 200);
    public static final LayoutRect ZOOM_OUT = new LayoutRect("zoom_out", 6, 236, 15, 15);
    public static final LayoutRect ZOOM_IN = new LayoutRect("zoom_in", 23, 236, 15, 15);
    public static final LayoutRect AUTO = new LayoutRect("auto", 40, 236, 30, 15);
    public static final LayoutRect DESIGNATE = new LayoutRect("designate", 72, 236, 70, 15);
    public static final LayoutRect AIRBURST = new LayoutRect("airburst", 144, 236, 74, 15);

    public static final LayoutRect TAB_SILOS = new LayoutRect("tab_silos", 224, 20, 54, 14);
    public static final LayoutRect TAB_RADARS = new LayoutRect("tab_radars", 280, 20, 54, 14);
    public static final LayoutRect TAB_TRACKS = new LayoutRect("tab_tracks", 336, 20, 58, 14);
    /** A header of {@link #LIST_HEADER} over {@link #LIST_ROWS} rows of {@link #LIST_ROW}. */
    public static final LayoutRect LIST = new LayoutRect("list", 224, 36, 170, 155);
    /** The selected row in more words. */
    public static final LayoutRect DETAIL = new LayoutRect("detail", 224, 194, 170, 24);
    /** Time on target or a sequence, and the spacing between impacts of a sequence. */
    public static final LayoutRect SALVO_MODE = new LayoutRect("salvo_mode", 224, 220, 84, 13);
    public static final LayoutRect SPACING_DOWN = new LayoutRect("spacing_down", 310, 220, 13, 13);
    public static final LayoutRect SPACING = new LayoutRect("spacing", 325, 220, 44, 13);
    public static final LayoutRect SPACING_UP = new LayoutRect("spacing_up", 371, 220, 13, 13);
    public static final LayoutRect ARM = new LayoutRect("arm", 224, 236, 40, 15);
    public static final LayoutRect DISARM = new LayoutRect("disarm", 266, 236, 40, 15);
    public static final LayoutRect SALVO = new LayoutRect("salvo", 308, 236, 44, 15);
    /** Self-destructs one of the table's own missiles in flight; painted as a guarded switch. */
    public static final LayoutRect ABORT = new LayoutRect("abort", 354, 236, 40, 15);

    public static final List<LayoutRect> ALL = List.of(TITLE, MAP_FRAME, ZOOM_OUT, ZOOM_IN, AUTO, DESIGNATE, AIRBURST, TAB_SILOS,
            TAB_RADARS, TAB_TRACKS, LIST, DETAIL, SALVO_MODE, SPACING_DOWN, SPACING, SPACING_UP, ARM, DISARM, SALVO, ABORT);

    public static final int LIST_HEADER = 11, LIST_ROW = 9, LIST_ROWS = 16;
    /** Column starts in the list, from its left edge: tick box, name, state lamp and word, detail, salvo delay, unlink. */
    public static final int COL_CHECK = 2, COL_NAME = 12, COL_STATE = 50, COL_INFO = 104, COL_DELAY = 138, COL_UNLINK = 160;

    // Sprites in holo_table_widgets.png (256 × 128).
    public static final int WIDGETS_W = 256, WIDGETS_H = 128;
    /** State lamps 7 × 7, one every 8 pixels, in the order of the launch controller's states. */
    public static final LayoutRect SPRITE_LAMPS = new LayoutRect("sprite_lamps", 0, 0, 48, 7);
    /** Tick boxes 8 × 8: unticked, ticked. */
    public static final LayoutRect SPRITE_CHECKS = new LayoutRect("sprite_checks", 0, 8, 16, 8);
    /** Unlink crosses 8 × 8: normal, hover. */
    public static final LayoutRect SPRITE_UNLINK = new LayoutRect("sprite_unlink", 0, 16, 16, 8);
    /** Buttons 40 × 15 one under another every 16 pixels: normal, hover, disabled, danger. */
    public static final LayoutRect SPRITE_BUTTONS = new LayoutRect("sprite_buttons", 0, 32, 40, 63);
    /** Tabs 58 × 14: idle over active, 16 pixels apart. */
    public static final LayoutRect SPRITE_TABS = new LayoutRect("sprite_tabs", 64, 0, 58, 30);
    public static final List<LayoutRect> SPRITES = List.of(SPRITE_LAMPS, SPRITE_CHECKS, SPRITE_UNLINK, SPRITE_BUTTONS, SPRITE_TABS);

    private HoloLayout() {
    }
}
