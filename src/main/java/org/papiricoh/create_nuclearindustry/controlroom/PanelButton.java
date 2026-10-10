package org.papiricoh.create_nuclearindustry.controlroom;

import org.jetbrains.annotations.Nullable;

/**
 * The six buttons on the face of a Reactor Control Panel, in two columns and three rows below a
 * status strip. Bounds are in block units on the front face: u to the viewer's right, v up.
 */
public enum PanelButton {
    SCRAM(0, 0),
    AUTO_SCRAM(1, 0),
    RODS_IN(0, 1),
    RODS_OUT(1, 1),
    LIMIT_DOWN(0, 2),
    LIMIT_UP(1, 2);

    /** How far one press moves the rod target. */
    public static final float ROD_STEP = 0.1f;
    public static final int LIMIT_STEP = 100;
    /** Bottom of the status strip above the buttons. */
    public static final float STRIP_BOTTOM = 11 / 16f;
    public static final float STRIP_TOP = 15 / 16f;

    private final int column;
    private final int row;

    PanelButton(int column, int row) {
        this.column = column;
        this.row = row;
    }

    public float u0() {
        return column == 0 ? 1 / 16f : 8.25f / 16f;
    }

    public float u1() {
        return column == 0 ? 7.75f / 16f : 15 / 16f;
    }

    public float v1() {
        return (10.5f - row * 3.25f) / 16f;
    }

    public float v0() {
        return v1() - 3 / 16f;
    }

    public boolean contains(double u, double v) {
        return u >= u0() && u <= u1() && v >= v0() && v <= v1();
    }

    @Nullable
    public static PanelButton at(double u, double v) {
        for (PanelButton button : values()) {
            if (button.contains(u, v)) {
                return button;
            }
        }
        return null;
    }
}
