package org.papiricoh.create_nuclearindustry.explosive.blast;

import java.util.ArrayList;
import java.util.List;

/**
 * The fireball and mushroom cloud of a nuclear detonation as a set of soft puffs, at any time after it
 * went off: the fireball glows white, yellow and orange and rises; a stem of dust follows it up and the
 * cap spreads into a rolling ring, cooling from orange to grey-brown; a skirt of dust spreads along the
 * ground. Everything is drawn from a seed, so every player sees the same cloud, and fades out at the
 * end of its life. Positions are blocks from ground zero. No game classes.
 */
public final class MushroomCloudModel {
    /** Seconds the cloud lasts, the last {@link #FADE} of them fading. */
    public static final double LIFETIME = 240, FADE = 60;

    /** One puff: centre, radius, colour 0-1, opacity and whether it glows. */
    public record Puff(double x, double y, double z, double radius, float red, float green, float blue, float alpha, boolean glowing) {}

    private final long seed;
    private final double scale;
    private final double fireball;
    private final boolean airburst;
    private final double burstHeight;

    /**
     * @param seed        picks the puffs
     * @param scale       the warhead's destruction radius, which sizes the cloud
     * @param fireball    the fireball's radius
     * @param airburst    whether it went off in the air: a thinner stem, no ground skirt at first
     * @param burstHeight blocks above the ground it went off at
     */
    public MushroomCloudModel(long seed, double scale, double fireball, boolean airburst, double burstHeight) {
        this.seed = seed;
        this.scale = scale;
        this.fireball = fireball;
        this.airburst = airburst;
        this.burstHeight = Math.max(0, burstHeight);
    }

    /** How high the cap rises in the end. */
    public double topHeight() {
        return Math.min(360, 60 + 1.2 * scale);
    }

    /** The cap's radius in the end. */
    public double capRadius() {
        return 25 + 0.55 * scale;
    }

    /** How high the cap's centre is {@code seconds} after the detonation. */
    public double capHeight(double seconds) {
        return burstHeight + (topHeight() - burstHeight) * (1 - Math.exp(-Math.max(0, seconds) / 18));
    }

    /** How hot the cloud still glows, 1 at the start falling to 0. */
    public static double heat(double seconds) {
        return Math.exp(-Math.max(0, seconds) / 6);
    }

    /** How opaque the whole cloud is: 1 until it starts to fade, 0 once its life is over. */
    public static double life(double seconds) {
        return Math.max(0, Math.min(1, (LIFETIME - seconds) / FADE));
    }

    /** The puffs {@code seconds} after the detonation; {@code detail} 0.25-2 scales how many. */
    public List<Puff> puffs(double seconds, double detail) {
        List<Puff> puffs = new ArrayList<>();
        double life = life(seconds);
        if (life <= 0) {
            return puffs;
        }
        double heat = heat(seconds);
        double grow = 1 - Math.exp(-Math.max(0, seconds) / 25);
        double cap = Math.max(fireball, capRadius() * grow);
        double height = capHeight(seconds);
        // The fireball: white-hot at first, yellowing and reddening as the cap swallows it.
        if (seconds < 4) {
            double r = fireball * (1.0 + 0.5 * Math.min(1, seconds * 2));
            float fade = (float) Math.min(1, (4 - seconds) / 2);
            double y = Math.max(burstHeight, Math.min(height, fireball + seconds * fireball * 0.6));
            puffs.add(new Puff(0, y, 0, r, 1.0f, (float) (1.0 - 0.45 * Math.min(1, seconds / 3)),
                    (float) (0.95 - 0.8 * Math.min(1, seconds / 2)), fade, true));
            // Its blinding core.
            puffs.add(new Puff(0, y, 0, r * 0.55, 1.0f, 1.0f, (float) (0.9 - 0.5 * Math.min(1, seconds / 2)), fade, true));
        }
        // The cap and the stem form out of the fireball over its first seconds.
        double formed = Math.max(0, Math.min(1, (seconds - 0.8) / 2.5));
        int capCount = Math.max(12, (int) Math.round(90 * detail));
        double roll = seconds * 0.12;
        for (int i = 0; i < capCount; i++) {
            double theta = rand(i, 1) * 2 * Math.PI;
            double phi = rand(i, 2) * 2 * Math.PI + roll;
            double ring = 0.62 * cap;
            double tube = 0.38 * cap * (0.7 + 0.3 * rand(i, 3));
            double x = Math.cos(theta) * (ring + tube * Math.cos(phi));
            double z = Math.sin(theta) * (ring + tube * Math.cos(phi));
            double y = height + 0.55 * tube * Math.sin(phi);
            double size = 0.32 * cap * (0.75 + 0.5 * rand(i, 4));
            // The underside and the core stay hot longest.
            double inner = Math.max(0, -Math.sin(phi)) * 0.5 + 0.5 * (1 - Math.cos(phi)) * 0.3;
            puffs.add(smoke(x, y, z, size, heat * (0.6 + 0.6 * inner), 0.85 * life * formed, i));
        }
        int coreCount = Math.max(4, (int) Math.round(16 * detail));
        for (int i = 0; i < coreCount; i++) {
            double theta = rand(i, 5) * 2 * Math.PI;
            double r = 0.4 * cap * rand(i, 6);
            puffs.add(smoke(Math.cos(theta) * r, height + 0.2 * cap * (rand(i, 7) - 0.3), Math.sin(theta) * r,
                    0.4 * cap, heat * 1.2, 0.9 * life * formed, i + 1000));
        }
        // The stem: dust drawn up from the ground behind the rising cap; an airburst's starts later and thinner.
        double stemTop = height - 0.3 * cap;
        double stemShare = airburst ? Math.max(0, Math.min(1, (seconds - 4) / 20)) : 1;
        int stemCount = (int) Math.round(Math.max(8, 34 * detail) * stemShare);
        double stem = (airburst ? 0.1 : 0.17) * cap + 3;
        for (int i = 0; i < stemCount; i++) {
            double t = (i + rand(i, 8)) / Math.max(1, stemCount);
            double y = t * stemTop * (airburst ? stemShare : 1);
            double wobble = stem * 0.35;
            double x = (rand(i, 9) - 0.5) * wobble + Math.sin(t * 6 + seed) * stem * 0.15;
            double z = (rand(i, 10) - 0.5) * wobble;
            puffs.add(smoke(x, y, z, stem * (1.1 - 0.4 * t), heat * 0.5 * t, 0.8 * life * formed, i + 2000));
        }
        // The base surge: a skirt of dust rolling out along the ground.
        if (!airburst || seconds > 6) {
            double skirt = Math.min(1.6 * scale, 12 + 9 * seconds);
            int skirtCount = (int) Math.round(Math.max(8, 36 * detail));
            for (int i = 0; i < skirtCount; i++) {
                double theta = rand(i, 11) * 2 * Math.PI;
                double r = skirt * (0.55 + 0.45 * rand(i, 12));
                double size = 4 + 0.08 * skirt * (0.7 + 0.6 * rand(i, 13));
                double fade = Math.max(0, 1 - seconds / 90);
                puffs.add(dust(Math.cos(theta) * r, size * 0.5, Math.sin(theta) * r, size, 0.55 * life * fade));
            }
        }
        return puffs;
    }

    /** A puff of the cloud, glowing orange while it is hot and grey-brown once it cools. */
    private Puff smoke(double x, double y, double z, double radius, double heat, double alpha, int index) {
        double h = Math.max(0, Math.min(1, heat));
        float shade = (float) (0.85 + 0.15 * rand(index, 14));
        float red = (float) lerp(0.52, 1.0, h) * shade;
        float green = (float) lerp(0.47, 0.5, h) * shade;
        float blue = (float) lerp(0.43, 0.18, h) * shade;
        return new Puff(x, y, z, radius, red, green, blue, (float) Math.max(0, Math.min(1, alpha)), h > 0.35);
    }

    private static Puff dust(double x, double y, double z, double radius, double alpha) {
        return new Puff(x, y, z, radius, 0.62f, 0.56f, 0.48f, (float) Math.max(0, Math.min(1, alpha)), false);
    }

    private double rand(int index, int salt) {
        return BlastHash.unit((int) seed ^ (int) (seed >>> 32), index, salt);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }
}
