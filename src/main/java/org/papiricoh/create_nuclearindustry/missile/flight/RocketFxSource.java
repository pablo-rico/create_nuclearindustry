package org.papiricoh.create_nuclearindustry.missile.flight;

import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/**
 * Something in the world the rocket effects should smoke and light up besides a flight itself, such as a spent stage tumbling
 * down. Implemented by entities; the client effects find them among the entities being rendered.
 */
public interface RocketFxSource {
    /** Its pivot (the stage's own tail centre) at {@code partial}. */
    Vec3 fxPosition(float partial);

    /** Its local frame (+Y up its axis) to the world at {@code partial}. */
    Quaternionf fxAttitude(float partial);

    /** The part of the rocket it is, in its own frame: where its nozzles are and what it burned. */
    RocketShape.Stage fxStage();

    /** Still burning (a stage cut loose with propellant left trails flame). */
    boolean fxBurning();
}
