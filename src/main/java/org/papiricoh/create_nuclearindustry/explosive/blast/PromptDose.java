package org.papiricoh.create_nuclearindustry.explosive.blast;

/**
 * The burst of neutrons and gamma rays at the moment of the detonation: a dose taken all at once, falling
 * with the square of the distance from the fireball's edge and further with the air in between. Walls
 * cut it as they cut other radiation. Measured in the dose players carry ({@code radiation.*}: nausea
 * from 100, sickness from 600). No game classes.
 */
public final class PromptDose {
    /** Below this a dose is not worth giving. */
    public static final double NEGLIGIBLE = 5;

    private PromptDose() {}

    /**
     * The dose {@code distance} blocks from ground zero of {@code profile}, in the open: {@code edgeDose}
     * at the fireball's edge (at least 10 blocks out).
     */
    public static double dose(DetonationProfile profile, double distance, double edgeDose) {
        double edge = Math.max(10, profile.fireballRadius());
        if (distance > profile.promptRadius()) {
            return 0;
        }
        double r = Math.max(edge, distance);
        double air = Math.exp(-(r - edge) / Math.max(1.0, 0.3 * profile.horizontalRadius()));
        double dose = edgeDose * (edge / r) * (edge / r) * air;
        return dose < NEGLIGIBLE ? 0 : dose;
    }
}
