package org.papiricoh.create_nuclearindustry.radiation;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Adjustments to what a body takes from its surroundings, applied by {@link RadiationHandler}:
 * a filtered shelter keeps fallout dust and contaminated ground air out.
 */
public final class ExposureModifiers {

    private ExposureModifiers() {}

    /** Fraction of fallout and ground contamination dose that reaches a body at {@code body}, 0-1. */
    public static double shelter(Level level, Vec3 body) {
        return 1.0;
    }
}
