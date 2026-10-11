package org.papiricoh.create_nuclearindustry.reactor.visual;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.ToIntFunction;

/**
 * What each fuel channel of a fission reactor should show, worked out from the controller's
 * fuel and power alone, so it can be tested without a world. The controller's assemblies fill
 * the channels in a fixed order (column by column, west to east then north to south, each from
 * the bottom up); a channel past the last assembly is empty. Every channel shares the core's glow
 * and coolant.
 */
public final class RodVisuals {
    /** Fuel looks, by the ordinal of {@code UraniumRodBlock.Fuel}. */
    public static final int EMPTY = 0;
    public static final int FRESH = 1;
    public static final int HALF = 2;
    public static final int SPENT = 3;

    /** Burn fraction below which an assembly still looks fresh, and below which it looks half burnt. */
    public static final double FRESH_BELOW = 0.33;
    public static final double HALF_BELOW = 0.75;

    /** Power fraction each glow step starts at, going up. */
    static final double[] GLOW_UP = {0.02, 0.35, 0.70};
    /** Power fraction each glow step ends below, going down: a little under where it started, so it does not flicker. */
    static final double[] GLOW_DOWN = {0.01, 0.30, 0.65};
    public static final int MAX_GLOW = GLOW_UP.length;

    private RodVisuals() {}

    /** One channel's looks: the fuel (see {@link #EMPTY} and others), the glow 0..3 and whether coolant fills it. */
    public record Look(int fuel, int glow, boolean coolant) {
        public static final Look DARK = new Look(EMPTY, 0, false);
    }

    /** The channels in fill order: by x, then z, then from the bottom up. */
    public static <P> List<P> order(Collection<P> rods, ToIntFunction<P> x, ToIntFunction<P> y, ToIntFunction<P> z) {
        List<P> ordered = new ArrayList<>(rods);
        ordered.sort(Comparator.comparingInt(x).thenComparingInt(z).thenComparingInt(y));
        return ordered;
    }

    /** How an assembly burnt this far (0 fresh, 1 spent) looks. */
    public static int fuel(double burnFraction) {
        if (Double.isNaN(burnFraction) || burnFraction < FRESH_BELOW) {
            return FRESH;
        }
        return burnFraction < HALF_BELOW ? HALF : SPENT;
    }

    /**
     * The glow step for a power fraction (0..1 of full power), given the step shown now. Rising
     * takes the step's threshold; falling needs the power a little below it.
     */
    public static int glow(int previous, double powerFraction) {
        double power = Double.isFinite(powerFraction) ? powerFraction : 0.0;
        int up = 0;
        while (up < MAX_GLOW && power >= GLOW_UP[up]) {
            up++;
        }
        int prev = Math.max(0, Math.min(MAX_GLOW, previous));
        if (up >= prev) {
            return up;
        }
        // Falling: keep each step until the power is clearly below where it started.
        int down = prev;
        while (down > 0 && power < GLOW_DOWN[down - 1]) {
            down--;
        }
        return Math.max(up, down);
    }

    /**
     * The looks of {@code channels} channels, in fill order, for the controller's assemblies'
     * burn fractions (in burn order), the glow step and whether the core holds coolant.
     */
    public static List<Look> looks(int channels, List<Double> burnFractions, int glow, boolean coolant) {
        List<Look> looks = new ArrayList<>(Math.max(0, channels));
        for (int i = 0; i < channels; i++) {
            int fuel = i < burnFractions.size() ? fuel(burnFractions.get(i)) : EMPTY;
            looks.add(new Look(fuel, glow, coolant));
        }
        return looks;
    }
}
