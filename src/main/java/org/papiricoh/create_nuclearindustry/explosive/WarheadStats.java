package org.papiricoh.create_nuclearindustry.explosive;

/**
 * Formulas compartidas para la cabeza nuclear (bomba y misil): a partir de la cantidad de uranio
 * valido y su enriquecimiento medio calcula potencia, radios e intensidad de la explosion.
 * Centralizado aqui para que la bomba ({@code NuclearBombBlockEntity}) y la plataforma de
 * lanzamiento ({@code LaunchPadBlockEntity}) produzcan exactamente los mismos resultados.
 */
public final class WarheadStats {
    /** Enriquecimiento minimo (%) para que el uranio sea apto como combustible de armamento. */
    public static final float REQUIRED_ENRICHMENT = 90.0f;
    /** Unidades de uranio de referencia para saturar la potencia/radio (3 slots * 16). */
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

    public static int verticalRadius(int units, float averageEnrichment) {
        return Math.max(28, Math.round(horizontalRadius(units, averageEnrichment) * 0.34f));
    }

    public static float intensity(float averageEnrichment) {
        return enrichmentFactor(averageEnrichment);
    }
}
