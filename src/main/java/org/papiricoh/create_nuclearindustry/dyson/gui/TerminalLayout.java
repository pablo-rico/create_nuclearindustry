package org.papiricoh.create_nuclearindustry.dyson.gui;

import org.papiricoh.create_nuclearindustry.missile.gui.LayoutRect;

import java.util.List;

/**
 * Where everything sits on a Dyson swarm terminal's screen, in texture pixels. On the left, the sky round the sun with the four
 * orbits drawn over it (the sun at ({@link #SKY_CX}, {@link #SKY_CY}), {@link OrbitShapeScale#PIXELS_PER_DEGREE} pixels to the
 * degree). On the right, a tab per orbit and the picked orbit's panel: open or closed, its radius, inclination and node as
 * sliders, and its sails. Along the bottom, the whole swarm: sails, the power it beams down and how the receivers split it. The
 * sprites (tab, toggle, slider track and knob) sit under the screen on the same sheet. {@code swarm_terminal_screen.png} is painted
 * from {@code tools/texturegen/dyson_layouts.py} ({@code TERMINAL_*}); JUnit keeps both in step.
 */
public final class TerminalLayout {
    public static final int WIDTH = 256, HEIGHT = 182;
    public static final int SHEET = 256;

    public static final LayoutRect TITLE = new LayoutRect("title", 6, 4, 244, 12);
    public static final LayoutRect SKY = new LayoutRect("sky", 6, 18, 124, 124);
    public static final LayoutRect TAB_0 = new LayoutRect("tab_0", 134, 18, 28, 14);
    public static final LayoutRect TAB_1 = new LayoutRect("tab_1", 164, 18, 28, 14);
    public static final LayoutRect TAB_2 = new LayoutRect("tab_2", 194, 18, 28, 14);
    public static final LayoutRect TAB_3 = new LayoutRect("tab_3", 224, 18, 28, 14);
    public static final LayoutRect PANEL = new LayoutRect("panel", 134, 34, 118, 108);
    public static final LayoutRect SWARM = new LayoutRect("swarm", 6, 146, 246, 32);

    public static final List<LayoutRect> ALL = List.of(TITLE, SKY, TAB_0, TAB_1, TAB_2, TAB_3, PANEL, SWARM);
    public static final List<LayoutRect> TABS = List.of(TAB_0, TAB_1, TAB_2, TAB_3);

    /** Inside {@link #PANEL}: the open/closed toggle, the three sliders, and the orbit's sails. */
    public static final LayoutRect ENABLE = new LayoutRect("enable", 138, 38, 110, 14);
    public static final LayoutRect RADIUS = new LayoutRect("radius", 138, 56, 110, 14);
    public static final LayoutRect INCLINATION = new LayoutRect("inclination", 138, 74, 110, 14);
    public static final LayoutRect NODE = new LayoutRect("node", 138, 92, 110, 14);
    public static final LayoutRect ORBIT_STATS = new LayoutRect("orbit_stats", 138, 110, 110, 28);
    public static final List<LayoutRect> PANEL_PARTS = List.of(ENABLE, RADIUS, INCLINATION, NODE, ORBIT_STATS);

    /** The sun's place in the sky plot, and the plot's radius (the outermost orbit, 40 degrees, fits inside it). */
    public static final int SKY_CX = 68, SKY_CY = 80, SKY_RADIUS = 60;

    /** A tab: unpicked over picked, 14 apart. */
    public static final LayoutRect SPRITE_TAB = new LayoutRect("sprite_tab", 0, 188, 28, 28);
    /** The toggle: closed over open, 14 apart. */
    public static final LayoutRect SPRITE_TOGGLE = new LayoutRect("sprite_toggle", 30, 188, 110, 28);
    public static final LayoutRect SPRITE_SLIDER = new LayoutRect("sprite_slider", 142, 188, 110, 14);
    public static final LayoutRect SPRITE_KNOB = new LayoutRect("sprite_knob", 142, 204, 6, 14);
    public static final List<LayoutRect> SPRITES = List.of(SPRITE_TAB, SPRITE_TOGGLE, SPRITE_SLIDER, SPRITE_KNOB);

    private TerminalLayout() {
    }

    /** The sky plot's scale. */
    public static final class OrbitShapeScale {
        /** Pixels per degree out from the sun: 40 degrees lands 2 pixels inside the plot's edge. */
        public static final float PIXELS_PER_DEGREE = 58f / 40f;

        private OrbitShapeScale() {
        }
    }
}
