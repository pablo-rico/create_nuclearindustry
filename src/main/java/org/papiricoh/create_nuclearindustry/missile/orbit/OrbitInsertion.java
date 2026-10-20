package org.papiricoh.create_nuclearindustry.missile.orbit;

import org.papiricoh.create_nuclearindustry.missile.physics.MissileDesign;

/**
 * Which orbit a rocket can put its satellite in: its ideal speed ({@link MissileDesign#deltaV()}, with the satellite as its payload)
 * against what each orbit needs. Pure: thresholds are passed in.
 */
public final class OrbitInsertion {
    private OrbitInsertion() {
    }

    /** What {@code design} reaches: {@link OrbitType#GEO}, {@link OrbitType#LEO}, or null if neither. */
    public static OrbitType reach(MissileDesign design, double leoDeltaV, double geoDeltaV) {
        throw new UnsupportedOperationException("orbit stream");
    }

    /** Whether {@code design} can put its satellite in an orbit of {@code type}. */
    public static boolean reaches(MissileDesign design, OrbitType type, double leoDeltaV, double geoDeltaV) {
        throw new UnsupportedOperationException("orbit stream");
    }
}
