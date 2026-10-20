package org.papiricoh.create_nuclearindustry.missile.physics;

/** Fictional TNT load scaling: volume grows with charge, radius with its cube root. */
public final class ConventionalYield {
    private ConventionalYield() {}

    public static double power(int tnt, double cap) {
        return Math.min(Math.max(0, cap), 4 * Math.cbrt(Math.max(0, tnt)));
    }
}
