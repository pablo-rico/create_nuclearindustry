package org.papiricoh.create_nuclearindustry.ruins;

/**
 * Whether the ground under a ruin is flat enough for it: a big plant on a hillside would hang over
 * the slope or be buried in it. Pure arithmetic over sampled heights, so it is tested without the game.
 */
public final class Flatness {
    private Flatness() {}

    /** Highest minus lowest of the samples; 0 for none. */
    public static int spread(int... heights) {
        if (heights.length == 0) {
            return 0;
        }
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int h : heights) {
            min = Math.min(min, h);
            max = Math.max(max, h);
        }
        return max - min;
    }

    /** Flat when the samples differ by at most {@code maxSlope} blocks; a negative limit accepts anything. */
    public static boolean isFlat(int maxSlope, int... heights) {
        return maxSlope < 0 || spread(heights) <= maxSlope;
    }

    /**
     * Dry when no sample has water over its ground: the surface (top of any block or fluid) and the
     * ocean floor (top of a solid block) match at every pair of samples.
     */
    public static boolean isDry(int[] surface, int[] floor) {
        if (surface.length != floor.length) {
            throw new IllegalArgumentException("one floor height per surface height");
        }
        for (int i = 0; i < surface.length; i++) {
            if (surface[i] > floor[i]) {
                return false;
            }
        }
        return true;
    }
}
