package org.papiricoh.create_nuclearindustry.missile.gui;

import java.util.List;

/**
 * Where everything sits on the launch controller's screen, in texture pixels. It has two tabs over a shared frame: the
 * rocket (a cutaway of the column, its stages and the slots of the section picked in it) and the launch (target, arming and
 * countdown). {@code launch_controller_rocket.png} and {@code launch_controller_launch.png} paint each tab's sheet and
 * {@code launch_controller_widgets.png} the sprites drawn over them, from the numbers in
 * {@code tools/texturegen/missile_layouts.py} ({@code SILO_*}); JUnit keeps both in step.
 */
public final class SiloLayout {
    public static final int WIDTH = 336, HEIGHT = 256;

    // Shared by both tabs.
    public static final LayoutRect TITLE = new LayoutRect("title", 6, 4, 324, 13);
    public static final LayoutRect TAB_ROCKET = new LayoutRect("tab_rocket", 6, 20, 64, 14);
    public static final LayoutRect TAB_LAUNCH = new LayoutRect("tab_launch", 72, 20, 64, 14);
    /** The state lamp and a one-line status. */
    public static final LayoutRect STATUS = new LayoutRect("status", 140, 20, 190, 14);
    /** The cutaway of the rocket standing on the base, on both tabs. */
    public static final LayoutRect DIAGRAM = new LayoutRect("diagram", 6, 38, 92, 212);

    // Rocket tab.
    /** One header row of {@link #STAGE_HEADER} and up to three stages of {@link #STAGE_ROW}. */
    public static final LayoutRect STAGES = new LayoutRect("stages", 102, 38, 228, 44);
    /** The section picked in the diagram: a title strip of {@link #SECTION_TITLE} over its slots and figures. */
    public static final LayoutRect SECTION = new LayoutRect("section", 102, 86, 228, 64);
    /** Inside {@link #SECTION}, right of the slots: what the section holds adds up to. */
    public static final LayoutRect SECTION_STATS = new LayoutRect("section_stats", 228, 98, 98, 48);
    /** One line of advice: what to add or change next. */
    public static final LayoutRect ADVICE = new LayoutRect("advice", 102, 153, 228, 13);
    /** The player's inventory: three rows from {@link #INV_Y} and the hotbar at {@link #HOTBAR_Y}. */
    public static final LayoutRect INVENTORY = new LayoutRect("inventory", 134, 169, 164, 78);

    // Launch tab.
    public static final LayoutRect TARGET = new LayoutRect("target", 102, 38, 228, 58);
    public static final LayoutRect BOX_X = new LayoutRect("box_x", 110, 52, 64, 16);
    public static final LayoutRect BOX_Y = new LayoutRect("box_y", 180, 52, 64, 16);
    public static final LayoutRect BOX_Z = new LayoutRect("box_z", 250, 52, 64, 16);
    public static final LayoutRect SET = new LayoutRect("set", 110, 74, 100, 16);
    public static final LayoutRect AIRBURST = new LayoutRect("airburst", 216, 74, 106, 16);
    /** Reach, CEP, guidance, flight time and the salvo delay. */
    public static final LayoutRect READOUT = new LayoutRect("readout", 102, 100, 228, 64);
    public static final LayoutRect COUNTDOWN = new LayoutRect("countdown", 102, 168, 110, 40);
    public static final LayoutRect ARM = new LayoutRect("arm", 216, 168, 114, 18);
    public static final LayoutRect DISARM = new LayoutRect("disarm", 216, 190, 114, 18);
    /** A lever and the reminder that a new redstone signal fires it. */
    public static final LayoutRect REDSTONE = new LayoutRect("redstone", 102, 212, 228, 38);

    public static final List<LayoutRect> COMMON = List.of(TITLE, TAB_ROCKET, TAB_LAUNCH, STATUS, DIAGRAM);
    public static final List<LayoutRect> ROCKET = List.of(STAGES, SECTION, ADVICE, INVENTORY);
    public static final List<LayoutRect> LAUNCH = List.of(TARGET, READOUT, COUNTDOWN, ARM, DISARM, REDSTONE);
    /** Regions inside {@link #TARGET}. */
    public static final List<LayoutRect> TARGET_PARTS = List.of(BOX_X, BOX_Y, BOX_Z, SET, AIRBURST);

    public static final int STAGE_HEADER = 8, STAGE_ROW = 12;
    /** Column starts in the stage table, from its left edge. */
    public static final int C_STAGE = 3, C_TANKS = 26, C_TWR = 56, C_DV = 96, C_BURN = 140, C_REACH = 180;
    public static final int SECTION_TITLE = 11;

    /**
     * Item positions (the slot's frame is one pixel further out, 18 px square) of the section's slots, indexed as
     * {@code WarheadBay}: three charge slots, four for the secondary, two for the tamper, then the whole bomb. A tank's
     * cartridge and a guidance module's tracker go in slot 0.
     */
    public static final int[] SLOT_X = {110, 128, 146, 110, 128, 146, 164, 190, 208, 190};
    public static final int[] SLOT_Y = {100, 100, 100, 122, 122, 122, 122, 122, 122, 100};
    public static final int INV_X = 135, INV_Y = 170, HOTBAR_Y = 228;

    /** The diagram's cells: {@link #CELL_W} × {@link #CELL_H}, centred on {@link #COL_CX}, standing on a base {@link #BASE_H} tall. */
    public static final int CELL_W = 18, CELL_H = 10, COL_CX = 52, BASE_H = 8, BASE_W = 54;
    /** The stage bracket's strip left of the cells and where the labels right of them start. */
    public static final int BRACKET_X = 9, BRACKET_W = 4, LABEL_X = 66;
    /** Cells the diagram shows: the tallest rocket and one ghost cell for what comes next. */
    public static final int CELLS = 18;

    /** The top of the base under the column. */
    public static int baseY() {
        return DIAGRAM.bottom() - 4 - BASE_H;
    }

    /** The top of the {@code i}th cell up the column, 0 the bottom one. */
    public static int cellY(int i) {
        return baseY() - (i + 1) * CELL_H;
    }

    public static int cellX() {
        return COL_CX - CELL_W / 2;
    }

    /** Stage tints, first stage first, and those of the payload and of an error. */
    public static final int[] STAGE_COLOURS = {0xFFE0883C, 0xFF4FA3D9, 0xFF9C6ADE};
    public static final int PAYLOAD_COLOUR = 0xFFD8D8D8, ERROR_COLOUR = 0xFFFF4A3A, GHOST_COLOUR = 0x80FFFFFF;

    // Sprites in launch_controller_widgets.png (256 × 256).
    public static final int WIDGETS_SIZE = 256;
    /** Cells: column = state ({@code CELL_NORMAL…CELL_GHOST}) × {@link #CELL_W}, row = section (engine, tank, guidance, warhead) × {@link #CELL_H}. */
    public static final int CELL_NORMAL = 0, CELL_SELECTED = 1, CELL_ERROR = 2, CELL_GHOST = 3;
    public static final LayoutRect SPRITE_CELLS = new LayoutRect("sprite_cells", 0, 0, 72, 40);
    public static final LayoutRect SPRITE_BASE_PLATFORM = new LayoutRect("sprite_base_platform", 0, 40, 54, 8);
    public static final LayoutRect SPRITE_BASE_SILO = new LayoutRect("sprite_base_silo", 0, 48, 54, 8);
    /**
     * Ghost icons for empty slots, 16 × 16 side by side: core, radiological, TNT, lithium deuteride, tamper, bomb,
     * propellant cartridge, star tracker.
     */
    public static final LayoutRect SPRITE_GHOSTS = new LayoutRect("sprite_ghosts", 0, 64, 128, 16);
    public static final int GHOST_CORE = 0, GHOST_RADIOLOGICAL = 1, GHOST_TNT = 2, GHOST_LITHIUM = 3, GHOST_TAMPER = 4,
            GHOST_BOMB = 5, GHOST_PROPELLANT = 6, GHOST_TRACKER = 7;
    public static final LayoutRect SPRITE_TAB_IDLE = new LayoutRect("sprite_tab_idle", 0, 80, 64, 14);
    public static final LayoutRect SPRITE_TAB_ACTIVE = new LayoutRect("sprite_tab_active", 64, 80, 64, 14);
    /** Buttons the size of {@link #ARM}: normal, hover, disabled and armed, one under another. */
    public static final LayoutRect SPRITE_BUTTONS = new LayoutRect("sprite_buttons", 0, 96, 114, 72);
    /** Toggles the size of {@link #AIRBURST}: off over on. */
    public static final LayoutRect SPRITE_TOGGLES = new LayoutRect("sprite_toggles", 128, 96, 106, 32);
    /** State lamps, 10 × 10 side by side, one per controller state in order. */
    public static final LayoutRect SPRITE_LAMPS = new LayoutRect("sprite_lamps", 0, 170, 60, 10);
    public static final List<LayoutRect> SPRITES = List.of(SPRITE_CELLS, SPRITE_BASE_PLATFORM, SPRITE_BASE_SILO, SPRITE_GHOSTS,
            SPRITE_TAB_IDLE, SPRITE_TAB_ACTIVE, SPRITE_BUTTONS, SPRITE_TOGGLES, SPRITE_LAMPS);

    private SiloLayout() {
    }
}
