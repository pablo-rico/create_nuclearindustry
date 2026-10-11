package org.papiricoh.create_nuclearindustry.reactor.pwr.containment.door;

/**
 * How a containment door moves as it opens, from 0 (shut) to 1 (open): first it pulls out of its
 * frame like a plug, then it slides sideways along the wall, most of its thickness behind the wall
 * beside the doorway. Closing runs the same path backwards. Pure, so JUnit can check the curve; the
 * renderer turns it into an offset.
 */
public final class ContainmentDoorMotion {
    /** Ticks a full stroke takes. */
    public static final int STROKE_TICKS = 24;
    /** Share of the stroke spent pulling out of the frame before sliding. */
    public static final float PULL_SHARE = 0.3f;
    /** How far the door pulls out of the wall, in blocks. */
    public static final float PULL_DEPTH = 5 / 16f;
    /**
     * How far the open door's edge still stands in the doorway, in blocks: enough to click it shut
     * and bump into, little enough to walk past.
     */
    public static final float EDGE = 3 / 16f;
    /** How far it slides sideways, in blocks: all but its edge, clear of the doorway. */
    public static final float SLIDE_DISTANCE = 1.0f - EDGE;
    /**
     * How much smaller than its block the moving door is drawn, on each side, so that the part slid
     * into the wall beside it hides behind the wall's faces instead of flickering through them.
     */
    public static final float INSET = 1 / 256f;

    private ContainmentDoorMotion() {}

    /** Animation progress gained per tick. */
    public static float speed() {
        return 1.0f / STROKE_TICKS;
    }

    /** How far the door has pulled out of its frame, in blocks. */
    public static float pull(float progress) {
        return PULL_DEPTH * smooth(clamp(progress / PULL_SHARE));
    }

    /** How far the door has slid sideways, in blocks. */
    public static float slide(float progress) {
        return SLIDE_DISTANCE * smooth(clamp((progress - PULL_SHARE) / (1 - PULL_SHARE)));
    }

    /**
     * Across the wall, the open door's edge left in its block, as {@code {min, max}}; {@code towards}
     * is the sign of the slide along that axis.
     */
    public static float[] edgeAcross(int towards) {
        return towards > 0 ? new float[]{SLIDE_DISTANCE, 1} : new float[]{0, EDGE};
    }

    /**
     * Through the wall, the open door as {@code {min, max}}: pulled out of its block, so it reaches
     * past it where it sticks out; {@code towards} is the sign of the pull along that axis.
     */
    public static float[] edgeThrough(int towards) {
        return towards > 0 ? new float[]{PULL_DEPTH, 1 + PULL_DEPTH} : new float[]{-PULL_DEPTH, 1 - PULL_DEPTH};
    }

    /** Smoothstep: starts and stops gently, like a heavy door on a geared drive. */
    static float smooth(float t) {
        return t * t * (3 - 2 * t);
    }

    private static float clamp(float t) {
        return Math.max(0, Math.min(1, t));
    }
}
