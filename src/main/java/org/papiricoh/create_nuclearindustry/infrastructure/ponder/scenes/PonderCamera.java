package org.papiricoh.create_nuclearindustry.infrastructure.ponder.scenes;

import net.createmod.ponder.api.scene.SceneBuilder;

/**
 * Camera moves shared by the mod's scenes. Ponder starts looking from the north-west
 * (north, west and top faces visible): turning +90 shows the east side, -90 the south.
 */
final class PonderCamera {
    private static final int TURN_TICKS = 20;

    private PonderCamera() {}

    /** Turns the view by {@code degrees} and waits for the turn to finish. */
    static void turn(SceneBuilder scene, float degrees) {
        scene.rotateCameraY(degrees);
        scene.idle(TURN_TICKS);
    }

    static void showEast(SceneBuilder scene) {
        turn(scene, 90);
    }

    static void backFromEast(SceneBuilder scene) {
        turn(scene, -90);
    }
}
