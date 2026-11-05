package org.papiricoh.create_nuclearindustry.dyson.gui;

import org.papiricoh.create_nuclearindustry.missile.gui.LayoutRect;

import java.util.List;

/**
 * Where everything sits on an EM rail ejector's screen, in texture pixels: its sail magazine (two rows of eight) with the charge
 * gauge beside it, the four orbit buttons, two lines of status, and the player's inventory. The sprites (the orbit button in its
 * three states, stacked: open, picked, closed; and the full gauge) sit right of the screen on the same sheet.
 * {@code em_rail_ejector_screen.png} is painted from {@code tools/texturegen/dyson_layouts.py} ({@code EJECTOR_*}); JUnit keeps both
 * in step.
 */
public final class EjectorLayout {
    public static final int WIDTH = 176, HEIGHT = 186;
    public static final int SHEET = 256;

    public static final LayoutRect TITLE = new LayoutRect("title", 6, 4, 164, 12);
    /** The magazine's slot frames: items at {@link #SAIL_X} + 18 col, {@link #SAIL_Y} + 18 row. */
    public static final LayoutRect SAILS = new LayoutRect("sails", 7, 17, 144, 36);
    /** The charge gauge, filled from the bottom as the rails charge. */
    public static final LayoutRect CHARGE = new LayoutRect("charge", 153, 17, 16, 36);
    public static final LayoutRect ORBIT_0 = new LayoutRect("orbit_0", 7, 57, 38, 14);
    public static final LayoutRect ORBIT_1 = new LayoutRect("orbit_1", 48, 57, 38, 14);
    public static final LayoutRect ORBIT_2 = new LayoutRect("orbit_2", 89, 57, 38, 14);
    public static final LayoutRect ORBIT_3 = new LayoutRect("orbit_3", 130, 57, 38, 14);
    public static final LayoutRect STATUS = new LayoutRect("status", 7, 74, 162, 24);
    public static final LayoutRect INVENTORY = new LayoutRect("inventory", 7, 101, 162, 77);

    public static final List<LayoutRect> ALL = List.of(TITLE, SAILS, CHARGE, ORBIT_0, ORBIT_1, ORBIT_2, ORBIT_3, STATUS, INVENTORY);
    public static final List<LayoutRect> ORBITS = List.of(ORBIT_0, ORBIT_1, ORBIT_2, ORBIT_3);

    /** The first sail slot's item corner, and the player's inventory's and hotbar's. */
    public static final int SAIL_X = 8, SAIL_Y = 18, INV_X = 8, INV_Y = 102, HOTBAR_Y = 160;

    /** The orbit button's three states, stacked 14 apart: open, picked, closed. */
    public static final LayoutRect SPRITE_ORBIT = new LayoutRect("sprite_orbit", 180, 0, 38, 42);
    /** The gauge full, to be cut from the bottom. */
    public static final LayoutRect SPRITE_CHARGE = new LayoutRect("sprite_charge", 220, 0, 16, 36);
    public static final List<LayoutRect> SPRITES = List.of(SPRITE_ORBIT, SPRITE_CHARGE);

    private EjectorLayout() {
    }
}
