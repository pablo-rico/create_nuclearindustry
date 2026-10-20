package org.papiricoh.create_nuclearindustry.missile.gui;

import java.util.List;

/**
 * Where everything sits on an interceptor battery's screen, in texture pixels: a top-down schematic of the turret on the left,
 * the four tubes and their state, then the battery's status, radar link code, automatic mode, doctrine and defended zone, over
 * the player's inventory. {@code interceptor_battery_screen.png} is painted from {@code tools/texturegen/missile_layouts.py}
 * ({@code BATTERY_*}); JUnit keeps both in step.
 */
public final class BatteryLayout {
    public static final int WIDTH = 232, HEIGHT = 236;

    public static final LayoutRect TITLE = new LayoutRect("title", 6, 4, 220, 12);
    /** The turret from above: its heading, its tubes' lamps and the direction it is turning to. */
    public static final LayoutRect SCHEMATIC = new LayoutRect("schematic", 6, 18, 100, 126);
    /** The four tube slots' frame. */
    public static final LayoutRect TUBES = new LayoutRect("tubes", 112, 18, 44, 44);
    /** Each tube's state: ready, reloading (with time left) or empty. */
    public static final LayoutRect TUBE_STATUS = new LayoutRect("tube_status", 158, 22, 68, 40);
    public static final LayoutRect STATUS = new LayoutRect("status", 112, 66, 114, 24);
    public static final LayoutRect CODE = new LayoutRect("code", 112, 94, 46, 14);
    /** Lit while radars on its code see anything. */
    public static final LayoutRect CODE_LAMP = new LayoutRect("code_lamp", 160, 94, 14, 14);
    public static final LayoutRect AUTO = new LayoutRect("auto", 112, 112, 54, 15);
    public static final LayoutRect DOCTRINE = new LayoutRect("doctrine", 168, 112, 58, 15);
    public static final LayoutRect ZONE_DOWN = new LayoutRect("zone_down", 112, 130, 13, 13);
    public static final LayoutRect ZONE = new LayoutRect("zone", 127, 130, 52, 13);
    public static final LayoutRect ZONE_UP = new LayoutRect("zone_up", 181, 130, 13, 13);
    /** Centres the defended zone on the battery. */
    public static final LayoutRect ZONE_HERE = new LayoutRect("zone_here", 196, 130, 30, 13);
    public static final LayoutRect INVENTORY = new LayoutRect("inventory", 34, 150, 166, 82);

    public static final List<LayoutRect> ALL = List.of(TITLE, SCHEMATIC, TUBES, TUBE_STATUS, STATUS, CODE, CODE_LAMP, AUTO, DOCTRINE,
            ZONE_DOWN, ZONE, ZONE_UP, ZONE_HERE, INVENTORY);

    /** Tube slots' item corners, in tube order. */
    public static final int[] SLOT_X = {116, 136, 116, 136}, SLOT_Y = {24, 24, 44, 44};
    public static final int INV_X = 36, INV_Y = 154, HOTBAR_Y = 212;

    private BatteryLayout() {
    }
}
