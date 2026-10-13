package org.papiricoh.create_nuclearindustry.radiation;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.create_nuclearindustry.civildefense.shelter.ShelterRegistry;

/**
 * Adjustments to what a body takes from its surroundings, applied by {@link RadiationHandler}:
 * a filtered shelter keeps fallout dust and contaminated ground air out.
 */
public final class ExposureModifiers {

    private ExposureModifiers() {}

    /**
     * Fraction of fallout and ground contamination dose that reaches a body at {@code body}, 0-1:
     * {@code civilDefense.shelter.filterFactor} inside a certified shelter whose filtration unit runs.
     */
    public static double shelter(Level level, Vec3 body) {
        return ShelterRegistry.doseFactor(level, body);
    }
}
