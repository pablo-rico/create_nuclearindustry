package org.papiricoh.create_nuclearindustry.missile.physics;

/**
 * Where a shot really lands around its aim point. Guidance errors scatter impacts in a round normal
 * spread whose circular error probable (CEP) is the radius holding half the shots. The miss is drawn
 * from a seed, so one flight always misses the same way, and a save and reload change nothing.
 */
public final class AccuracyModel {
    /** CEP over the spread's standard deviation: half a Rayleigh distribution lies within sigma * sqrt(2 ln 2). */
    public static final double CEP_PER_SIGMA = Math.sqrt(2.0 * Math.log(2.0));

    private AccuracyModel() {}

    /** The horizontal miss, blocks, of a shot with {@code cep} drawn from {@code seed}. */
    public static V3 miss(double cep, long seed) {
        if (cep <= 0) {
            return V3.ZERO;
        }
        double sigma = cep / CEP_PER_SIGMA;
        long a = mix(seed);
        long b = mix(a);
        double u = Math.max(1.0e-12, unit(a));
        double angle = 2.0 * Math.PI * unit(b);
        double radius = sigma * Math.sqrt(-2.0 * Math.log(u));
        return new V3(radius * Math.cos(angle), 0, radius * Math.sin(angle));
    }

    private static double unit(long bits) {
        return (bits >>> 11) * 0x1.0p-53;
    }

    /** SplitMix64's finaliser: spreads a seed's bits so nearby seeds miss in unrelated ways. */
    private static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
        z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
        return z ^ (z >>> 31);
    }
}
