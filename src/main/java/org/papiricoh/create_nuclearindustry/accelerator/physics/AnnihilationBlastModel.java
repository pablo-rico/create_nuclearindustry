package org.papiricoh.create_nuclearindustry.accelerator.physics;

/**
 * What happens when trapped antimatter touches matter all at once. Every nanogram annihilates with a
 * nanogram of matter, {@code 2 m c^2} = about 180 kJ, mostly as pions and gamma rays. The game keeps
 * it fun rather than true to scale (a real gram would be a 43 kiloton bomb):
 *
 * <ul>
 *   <li>The blast radius grows with the cube root of the charge, like any explosion's:
 *       {@code clamp(2 + 1.2 cbrt(ng), 3, 8)}. A vial is a large TNT block; a full trap tops out.</li>
 *   <li>A gamma pulse reaches twice that radius. The centre dose grows with the charge up to a cap,
 *       and falls as {@code r^2 / (r^2 + d^2)} with the distance {@code d}, softened inside the blast.</li>
 *   <li>Annihilation leaves no fission products: no fallout and no contamination.</li>
 *   <li>Less than {@code fizzleNg} only crackles; it does not explode.</li>
 * </ul>
 * Doses are in the units players carry ({@code radiation.*}). Pure arithmetic.
 */
public final class AnnihilationBlastModel {
    /** kJ released by annihilating one nanogram of antimatter with one of matter: {@code 2 * 1e-12 kg * c^2}. */
    public static final double KJ_PER_NANOGRAM = 2 * 1.0e-12 * 299_792_458.0 * 299_792_458.0 / 1000.0;
    /** kJ in a kilogram of TNT. */
    public static final double KJ_PER_KG_TNT = 4184.0;
    /** Below this a dose is not worth giving. */
    public static final double NEGLIGIBLE_DOSE = 1.0;

    /**
     * @param baseRadius        radius before the charge's term, blocks
     * @param radiusPerCbrtNg   blocks per cube root of a nanogram
     * @param minRadius         smallest blast, blocks
     * @param maxRadius         largest blast, blocks
     * @param pulseRadiusFactor gamma pulse radius over the blast radius
     * @param dosePerNg         centre dose per nanogram
     * @param maxCentreDose     cap on the centre dose
     * @param fizzleNg          charge below which nothing explodes, ng
     */
    public record Params(double baseRadius, double radiusPerCbrtNg, double minRadius, double maxRadius,
                         double pulseRadiusFactor, double dosePerNg, double maxCentreDose, double fizzleNg) {
        /** A 4 ng vial: radius 3.9, 100 at the centre (nausea). A full small trap: radius 5.8, 800. */
        public static final Params DEFAULT = new Params(2.0, 1.2, 3.0, 8.0, 2.0, 25.0, 1000.0, 1.0e-4);
    }

    private AnnihilationBlastModel() {}

    /** Whether this much antimatter makes an explosion at all. */
    public static boolean explodes(Params p, double ng) {
        return ng >= Math.max(1.0e-12, p.fizzleNg());
    }

    /** Explosion radius (vanilla explosion power), blocks; 0 for a fizzle. */
    public static double radius(Params p, double ng) {
        if (!explodes(p, ng)) {
            return 0;
        }
        double r = p.baseRadius() + p.radiusPerCbrtNg() * Math.cbrt(ng);
        return Math.max(p.minRadius(), Math.min(p.maxRadius(), r));
    }

    /** How far the gamma pulse reaches, blocks. */
    public static double pulseRadius(Params p, double ng) {
        return radius(p, ng) * Math.max(0, p.pulseRadiusFactor());
    }

    /** Dose at the centre of the blast. */
    public static double centreDose(Params p, double ng) {
        if (!explodes(p, ng)) {
            return 0;
        }
        return Math.min(p.maxCentreDose(), Math.max(0, p.dosePerNg()) * ng);
    }

    /** Gamma dose {@code distance} blocks from the centre, in the open; 0 beyond the pulse radius. */
    public static double dose(Params p, double ng, double distance) {
        double pulse = pulseRadius(p, ng);
        double d = Math.max(0, distance);
        if (pulse <= 0 || d > pulse) {
            return 0;
        }
        double r = radius(p, ng);
        double dose = centreDose(p, ng) * r * r / (r * r + d * d);
        return dose < NEGLIGIBLE_DOSE ? 0 : dose;
    }

    /** Energy released, kJ. */
    public static double energyKj(double ng) {
        return Math.max(0, ng) * KJ_PER_NANOGRAM;
    }

    /** TNT equivalent, kg. */
    public static double tntKg(double ng) {
        return energyKj(ng) / KJ_PER_KG_TNT;
    }
}
