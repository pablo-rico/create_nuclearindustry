package org.papiricoh.create_nuclearindustry.waste.repository;

import org.papiricoh.create_nuclearindustry.waste.WasteForm;

/**
 * How well a spot holds a waste package, from 0 (open air) to 1 (a proper geological
 * repository). Four barriers add up: depth below the surface, the host rock around the
 * package, the backfill packed against it and the waste form itself; groundwater (any fluid
 * source nearby) takes a large share away, because water is what carries waste out.
 *
 * <p>Pure: the world is sampled by {@link RepositoryProbe} into a {@link Site}, and the
 * weights come from the config, so the scoring can be unit tested.
 */
public final class RepositoryQuality {

    private RepositoryQuality() {}

    /**
     * Weight of each barrier. With the defaults the four positive weights add up to 1, so a
     * perfect site scores 1, and a fluid nearby costs {@code fluidPenalty}.
     */
    public record Weights(double depth, double hostRock, double backfill, double form, double fluidPenalty) {
        public static final Weights DEFAULT = new Weights(0.35, 0.25, 0.25, 0.15, 0.4);
    }

    /**
     * What the world looks like around one package.
     *
     * @param depthBelowSurface blocks between the package and the solid surface above it
     * @param hostRockFraction  0 to 1, share of the surrounding natural blocks that are sound host rock
     * @param backfilledFaces   0 to 6, faces covered by backfill (bentonite) or other packages
     * @param fluidNearby       a fluid source within reach of the package
     */
    public record Site(int depthBelowSurface, double hostRockFraction, int backfilledFaces, boolean fluidNearby) {}

    /**
     * Each barrier's weighted contribution and the final score, so goggles and displays can
     * show which barrier is missing.
     */
    public record Breakdown(double depth, double hostRock, double backfill, double form, double fluidPenalty, double score) {
        public static final Breakdown NONE = new Breakdown(0, 0, 0, 0, 0, 0);
    }

    /** 0 at the surface, 1 at {@code depthSpan} blocks down or deeper. */
    public static double depthFactor(int depthBelowSurface, int depthSpan) {
        if (depthSpan <= 0) {
            return depthBelowSurface > 0 ? 1 : 0;
        }
        return clamp(depthBelowSurface / (double) depthSpan);
    }

    public static Breakdown assess(Site site, WasteForm form, Weights weights, int depthSpan) {
        double depth = weights.depth() * depthFactor(site.depthBelowSurface(), depthSpan);
        double host = weights.hostRock() * clamp(site.hostRockFraction());
        double backfill = weights.backfill() * clamp(Math.max(0, Math.min(6, site.backfilledFaces())) / 6.0);
        double formTerm = weights.form() * form.quality();
        double penalty = site.fluidNearby() ? weights.fluidPenalty() : 0;
        double score = clamp(depth + host + backfill + formTerm - penalty);
        return new Breakdown(depth, host, backfill, formTerm, penalty, score);
    }

    /** A package at {@code score} counts as geologically stored: it neither leaks nor irradiates. */
    public static boolean isStored(double score, double minScore) {
        return score >= minScore;
    }

    /**
     * Contamination a package adds to its chunk on one random tick: the base barrel leak, scaled
     * by how leaky the package is and by what the repository fails to hold back.
     */
    public static double leak(double barrelLeak, double leakFactor, double score, double minScore) {
        if (isStored(score, minScore)) {
            return 0;
        }
        return Math.max(0, barrelLeak * leakFactor * (1 - clamp(score)));
    }

    /** Dose rate a placed package gives off, from its carried activity {@code perSecond}. */
    public static double emission(double perSecond, double emissionFactor, double score, double minScore) {
        if (isStored(score, minScore)) {
            return 0;
        }
        return Math.max(0, perSecond * emissionFactor * (1 - clamp(score)));
    }

    private static double clamp(double value) {
        return Math.max(0, Math.min(1, value));
    }
}
