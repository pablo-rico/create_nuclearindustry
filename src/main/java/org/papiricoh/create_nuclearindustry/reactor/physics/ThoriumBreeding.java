package org.papiricoh.create_nuclearindustry.reactor.physics;

/**
 * Uranium-233 breeding in a thorium assembly, as a function of how much of the assembly is burnt
 * (0 fresh, 1 spent). Thorium-232 is fertile, not fissile: a fresh assembly only burns its small
 * seed of uranium, so it starts weak. Neutrons the seed releases turn thorium into uranium-233
 * (through protactinium-233), which is fissile and better than the seed, so the assembly grows
 * more reactive as it burns until the seed is gone and the bred uranium-233 settles towards an
 * equilibrium. Free of game classes, so it can be tested on its own.
 */
public final class ThoriumBreeding {
    /** Reactivity the seed gives a fresh assembly, relative to a low-enriched uranium assembly. */
    public static final double SEED_WORTH = 0.55;
    /** Reactivity of the uranium-233 an assembly holds once breeding reaches equilibrium. */
    public static final double BRED_WORTH = 1.15;
    /** Burn fraction over which the uranium-233 builds up (e-folding): protactinium decays in between. */
    public static final double BREEDING_BURNUP = 0.25;
    /** How much longer a thorium assembly lasts than the uranium it is rated as: it breeds its own fuel. */
    public static final double LIFE_FACTOR = 1.4;

    private ThoriumBreeding() {}

    /** Fraction of the uranium seed left after burning {@code burnFraction} of the assembly. */
    public static double seedFraction(double burnFraction) {
        return 1.0 - clamp(burnFraction);
    }

    /** Uranium-233 bred so far, as a fraction of its equilibrium amount. */
    public static double bredFraction(double burnFraction) {
        return 1.0 - Math.exp(-clamp(burnFraction) / BREEDING_BURNUP);
    }

    /**
     * Multiplier on the assembly's reactivity after burning {@code burnFraction} of it: low while
     * only the seed fissions, rising as uranium-233 breeds, a little lower again at the end.
     */
    public static double reactivityMultiplier(double burnFraction) {
        return SEED_WORTH * seedFraction(burnFraction) + BRED_WORTH * bredFraction(burnFraction);
    }

    /** The burn fraction at which the assembly is most reactive. */
    public static double peakBurnFraction() {
        return clamp(BREEDING_BURNUP * Math.log(BRED_WORTH / (SEED_WORTH * BREEDING_BURNUP)));
    }

    private static double clamp(double value) {
        return Double.isFinite(value) ? Math.max(0.0, Math.min(1.0, value)) : 0.0;
    }
}
