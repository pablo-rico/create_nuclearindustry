package org.papiricoh.create_nuclearindustry.missile.gui;

import java.util.List;

/**
 * Where everything sits on a ground station's screen, in texture pixels: a sky plot on the left (the station's network's
 * satellites round the zenith, by bearing and ground distance), and on the right its link code and lamp, the dish's speed against
 * what it needs, whether it works, and its network: how many satellites and the soonest pass. {@code ground_station_screen.png} is
 * painted from {@code tools/texturegen/missile_layouts.py} ({@code STATION_*}); JUnit keeps both in step.
 */
public final class StationLayout {
    public static final int WIDTH = 200, HEIGHT = 100;

    public static final LayoutRect TITLE = new LayoutRect("title", 6, 4, 188, 12);
    /** The sky over the station: a circle of {@link #SKY_RADIUS} round ({@link #SKY_CX}, {@link #SKY_CY}), the outer ring the satellites' horizon. */
    public static final LayoutRect SKY = new LayoutRect("sky", 6, 18, 76, 76);
    public static final LayoutRect CODE = new LayoutRect("code", 88, 18, 46, 14);
    /** Lit while the station works. */
    public static final LayoutRect CODE_LAMP = new LayoutRect("code_lamp", 136, 18, 14, 14);
    /** The dish's speed, red under what it needs. */
    public static final LayoutRect RPM = new LayoutRect("rpm", 152, 18, 42, 14);
    public static final LayoutRect STATUS = new LayoutRect("status", 88, 36, 106, 24);
    /** Satellites on its code and the soonest pass. */
    public static final LayoutRect NETWORK = new LayoutRect("network", 88, 64, 106, 30);

    public static final List<LayoutRect> ALL = List.of(TITLE, SKY, CODE, CODE_LAMP, RPM, STATUS, NETWORK);

    /** The sky plot's centre, the zenith, and its horizon ring's radius. */
    public static final int SKY_CX = 44, SKY_CY = 56, SKY_RADIUS = 34;

    private StationLayout() {
    }
}
