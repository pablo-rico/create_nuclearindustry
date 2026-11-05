package org.papiricoh.create_nuclearindustry.explosive.fx.model;

import org.papiricoh.create_nuclearindustry.explosive.blast.BlastHash;
import org.papiricoh.create_nuclearindustry.explosive.blast.ExplosionPalette;
import org.papiricoh.create_nuclearindustry.explosive.blast.ExplosionPalette.Rgb;
import org.papiricoh.create_nuclearindustry.explosive.conventional.ConventionalKind;

/**
 * How a conventional blast looks over time, from its charge alone: a brief fireball, a column of black smoke that rises, leans
 * with the wind and hangs about for a minute or two (the part a player arriving late still sees), and a little ground dust. A
 * thermobaric charge first sprays a haze of fuel, then lights it: a fireball much wider than tall, rolling out along the ground
 * and burning longer, with less debris. A bomblet is a small blast with a short-lived puff. A dirty bomb is plain high explosive.
 * Everything scales with the cube root of the charge, as the blast's reach does. No game classes.
 */
public final class BlastFxModel {
    /** Blocks a second the smoke drifts. */
    public static final double DRIFT = 0.6;

    /** Where smoke puffs go; colours before the light. */
    public interface SmokeSink {
        void puff(double x, double y, double z, double radius, int tile, float spin, float red, float green, float blue, float alpha);
    }

    private final ConventionalKind kind;
    private final double charge;
    private final double root;
    private final double fireballRadius;
    private final double fireRadius;
    private final boolean airburst;
    private final int seedHash;
    private final double driftX, driftZ;
    private final Rgb ground;

    /**
     * @param kind           what the charge was made of
     * @param charge         TNT-block equivalents
     * @param fireballRadius blocks, as the server worked it out
     * @param fireRadius     how far it sets fires (a thermobaric cloud), 0 for none
     * @param airburst       whether it went off clear of the ground (no dust, no crater smoke)
     * @param seed           picks the puffs and which way the smoke drifts
     * @param ground         the ground's colour where it went off, for the dust
     */
    public BlastFxModel(ConventionalKind kind, double charge, double fireballRadius, double fireRadius, boolean airburst, long seed,
                        Rgb ground) {
        this.kind = kind == ConventionalKind.DIRTY ? ConventionalKind.HIGH_EXPLOSIVE : kind;
        this.charge = Math.max(0.1, charge);
        this.root = Math.cbrt(this.charge);
        this.fireballRadius = Math.max(0.6, fireballRadius);
        this.fireRadius = Math.max(0, fireRadius);
        this.airburst = airburst;
        this.seedHash = (int) seed ^ (int) (seed >>> 32);
        double a = BlastHash.unit(seedHash, 0, 199) * 2 * Math.PI;
        this.driftX = Math.cos(a);
        this.driftZ = Math.sin(a);
        this.ground = ground == null ? ExplosionPalette.DUST : ExplosionPalette.DUST.lerp(ground, 0.5);
    }

    public ConventionalKind kind() {
        return kind;
    }

    public boolean thermobaric() {
        return kind == ConventionalKind.THERMOBARIC;
    }

    /** The share of the charge against a full bay of the strongest explosive (192), by cube root: 0 to 1 and past. */
    public double size() {
        return root / Math.cbrt(192);
    }

    /** Seconds a thermobaric charge's fuel hangs as a haze before it lights; 0 for anything else. */
    public double hazeSeconds() {
        return thermobaric() ? 0.25 + 0.03 * root : 0;
    }

    /** Seconds the fireball burns once lit. */
    public double fireballSeconds() {
        return switch (kind) {
            case THERMOBARIC -> 1.2 + 0.25 * root;
            case BOMBLET -> 0.25;
            default -> 0.25 + 0.12 * root;
        };
    }

    /** Seconds from the detonation until the fireball is out. */
    public double fireEnd() {
        return hazeSeconds() + fireballSeconds();
    }

    /** The fireball's horizontal radius {@code t} seconds after the detonation (0 before it lights or after it is out). */
    public double fireballWidth(double t) {
        double s = t - hazeSeconds();
        if (s < 0 || s > fireballSeconds()) return 0;
        double grow = Math.sqrt(1 - Math.exp(-s / (thermobaric() ? 0.12 : 0.05)));
        if (thermobaric()) {
            // Rolling out along the ground as the cloud burns through.
            double reach = Math.max(2.2 * fireballRadius, Math.min(fireRadius * 0.7, 2.2 * fireballRadius + 0.5 * fireRadius));
            return reach * grow * (0.7 + 0.3 * Math.min(1, s / fireballSeconds()));
        }
        return fireballRadius * grow;
    }

    /** Its vertical radius: as wide as tall for high explosive, flattened for a thermobaric cloud. */
    public double fireballHeight(double t) {
        double w = fireballWidth(t);
        return thermobaric() ? Math.min(w * 0.38, 0.9 * fireballRadius + 0.05 * w) : w;
    }

    /** How hot the fireball still is, 1 to 0 ({@link ExplosionPalette#fire}). */
    public double fireballHeat(double t) {
        double s = t - hazeSeconds();
        if (s < 0) return 0;
        return Math.exp(-1.8 * s / fireballSeconds());
    }

    /** The fuel haze's radius and opacity {@code t} seconds in (thermobaric only), as {radius, alpha}, or null. */
    public double[] haze(double t) {
        if (!thermobaric() || t > hazeSeconds() + 0.15) return null;
        double r = fireballRadius * 2.4 * Math.sqrt(Math.min(1, t / Math.max(0.05, hazeSeconds())));
        double alpha = 0.45 * (1 - CloudEvolution.smooth((t - hazeSeconds()) / 0.15));
        return new double[]{r, alpha};
    }

    /** Seconds the smoke hangs about: a minute or two for a big charge, half a minute for a stick of TNT, less for a bomblet. */
    public double smokeSeconds() {
        if (kind == ConventionalKind.BOMBLET) return 18 + 10 * root;
        return 20 + 100 * Math.min(1, size());
    }

    /** How high the smoke column climbs, blocks above the blast. */
    public double columnHeight() {
        return (kind == ConventionalKind.BOMBLET ? 6 : 4) + 14 * root;
    }

    /** Smoke puffs this blast keeps when one full-charge blast may keep {@code perBlast}. */
    public int smokePuffs(int perBlast) {
        double share = kind == ConventionalKind.BOMBLET ? 0.08 : Math.max(0.15, Math.min(1, 1.2 * size()));
        return Math.max(3, (int) Math.round(perBlast * share));
    }

    /** Whether anything of the blast still shows {@code t} seconds in. */
    public boolean over(double t) {
        return t > smokeSeconds() + 1;
    }

    /**
     * The smoke {@code t} seconds in, {@code count} puffs: born over the first seconds from the fireball (black at first, greyer
     * later and as each thins out), each rising, swelling and drifting, and fading out over its life. A ground burst adds a few
     * low puffs of dust in the ground's colour that settle sooner.
     */
    public void smoke(double t, int count, SmokeSink sink) {
        double life = smokeSeconds();
        if (t < 0 || t > life) return;
        double emit = (kind == ConventionalKind.BOMBLET ? 0.6 : 1.5) + 6 * Math.min(1, size());
        double height = columnHeight();
        double rise = 3 + 3 * Math.min(1.5, size());
        double r0 = 0.5 * fireballRadius + 0.6;
        int dust = airburst ? 0 : Math.max(1, count / 5);
        int smoke = count - dust;
        for (int i = 0; i < smoke; i++) {
            double born = Math.pow((i + 0.5) / smoke, 1.5) * emit;
            double age = t - born - hazeSeconds();
            if (age <= 0) continue;
            double span = life * (0.65 + 0.35 * rand(i, 1)) - born;
            if (age > span) continue;
            double x = age / span;
            double up = height * (1 - Math.exp(-age / rise)) * (0.75 + 0.35 * rand(i, 2));
            double lean = DRIFT * age * (0.3 + 0.7 * Math.min(1, up / height));
            double spread = r0 * (0.3 + 0.9 * x) * (rand(i, 3) - 0.5) * 2;
            double a = rand(i, 4) * 2 * Math.PI;
            double px = driftX * lean + Math.cos(a) * spread, pz = driftZ * lean + Math.sin(a) * spread;
            double radius = r0 * (1 + 1.8 * Math.sqrt(age / life)) * (0.8 + 0.4 * rand(i, 5));
            double alpha = 0.85 * Math.min(1, age / 0.3) * (1 - x * x);
            float grey = (float) (0.1 + 0.14 * (i / (double) smoke) + 0.22 * x);
            sink.puff(px, up + fireballRadius * 0.3, pz, radius, CloudEvolution.TILE_DENSE + (i & 3), (float) (rand(i, 6) * 6.283 + age * 0.05),
                    grey, grey * 0.97f, grey * 0.94f, (float) alpha);
        }
        for (int i = 0; i < dust; i++) {
            double age = t - 0.05;
            double span = Math.min(life, 12 + 10 * Math.min(1, size())) * (0.7 + 0.3 * rand(i, 11));
            if (age <= 0 || age > span) continue;
            double x = age / span;
            double a = rand(i, 12) * 2 * Math.PI;
            double out = r0 * (1.2 + 1.5 * Math.sqrt(x)) * (0.6 + 0.6 * rand(i, 13)) + fireballRadius * 0.8;
            double radius = r0 * (1.2 + 1.2 * Math.sqrt(x));
            double lean = DRIFT * age * 0.3;
            sink.puff(Math.cos(a) * out + driftX * lean, radius * 0.5 + 0.4 * age * 0.2, Math.sin(a) * out + driftZ * lean, radius,
                    CloudEvolution.TILE_DENSE + (i & 3), (float) (rand(i, 14) * 6.283), ground.red(), ground.green(), ground.blue(),
                    (float) (0.6 * Math.min(1, age / 0.2) * (1 - x)));
        }
    }

    private double rand(int index, int salt) {
        return BlastHash.unit(seedHash, index, salt);
    }
}
