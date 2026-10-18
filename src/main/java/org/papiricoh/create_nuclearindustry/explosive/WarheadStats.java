package org.papiricoh.create_nuclearindustry.explosive;

/**
 * The formulas of a fission warhead, shared by the bomb and the missile: from the warhead units loaded
 * and their average enrichment, the power, radii and intensity of the detonation. Kept in one place so
 * the bomb ({@code NuclearBombBlockEntity}) and the launch pad ({@code LaunchPadBlockEntity}) give the
 * same results.
 */
public final class WarheadStats {
    /** Lowest enrichment, %, at which uranium is weapons grade. */
    public static final float REQUIRED_ENRICHMENT = 90.0f;
    /** Warhead units that saturate power and radius: six cores of eight. */
    public static final int MAX_URANIUM_UNITS = 48;

    private static final int MAX_HORIZONTAL_RADIUS = 250;
    private static final int MIN_HORIZONTAL_RADIUS = 80;

    private WarheadStats() {}

    public static float enrichmentFactor(float averageEnrichment) {
        return Math.max(1.0f, averageEnrichment / REQUIRED_ENRICHMENT);
    }

    public static float power(int units, float averageEnrichment) {
        if (units <= 0) {
            return 0.0f;
        }
        return Math.min(160.0f, 36.0f + units * 2.6f * enrichmentFactor(averageEnrichment));
    }

    public static int horizontalRadius(int units, float averageEnrichment) {
        if (units <= 0) {
            return 0;
        }
        float fuelRatio = Math.min(1.0f, units / (float) MAX_URANIUM_UNITS);
        return Math.min(MAX_HORIZONTAL_RADIUS,
                Math.round(MIN_HORIZONTAL_RADIUS
                        + (MAX_HORIZONTAL_RADIUS - MIN_HORIZONTAL_RADIUS) * fuelRatio * enrichmentFactor(averageEnrichment)));
    }

    /** The crater a ground burst digs, as {@code DetonationProfile} works it out: three tenths of the destruction radius. */
    public static int craterRadius(int units, float averageEnrichment) {
        return Math.round(horizontalRadius(units, averageEnrichment) * 0.3f);
    }

    public static float intensity(float averageEnrichment) {
        return enrichmentFactor(averageEnrichment);
    }
}
