package org.papiricoh.create_nuclearindustry.explosive.fx.model;

import org.papiricoh.create_nuclearindustry.explosive.fx.ExplosionQuality;

/**
 * What one {@link ExplosionQuality} spends on explosions: how many puffs each part of a mushroom cloud is drawn with, how often
 * its mesh is rebuilt, how many particles a blast may keep alive, whether the air ripples over a fireball, how many layers its
 * far impostor has and how far the cloud keeps its full detail. Puffs fall off with distance ({@link #lod}): a far cloud is drawn
 * with fewer, bigger puffs covering the same sky, so it costs little and looks the same. No game classes.
 *
 * @param capPuffs         puffs in the rolling cap at full detail
 * @param stemPuffs        puffs in the stem
 * @param surgePuffs       puffs in the base surge and the dust front
 * @param extraPuffs       condensation, fire columns, the pyrocumulus
 * @param rebuildHz        times a second the cloud's mesh is rebuilt once its fireball is gone
 * @param particleBudget   explosion particles alive at once, at most
 * @param particleScale    how many particles a blast throws, against MEDIUM
 * @param heatHaze         whether the air ripples over fireballs and along shock fronts
 * @param impostorLayers   layers of the far impostor: cap and stem, plus a rear cap at 3
 * @param fullDetail       blocks out to which a cloud keeps all its puffs
 * @param smokePerBlast    smoke puffs one conventional blast may keep at full charge
 * @param lightning        whether lightning flickers inside a young cloud
 */
public record QualityPreset(int capPuffs, int stemPuffs, int surgePuffs, int extraPuffs, double rebuildHz, int particleBudget,
                            double particleScale, boolean heatHaze, int impostorLayers, double fullDetail, int smokePerBlast,
                            boolean lightning) {

    public static final QualityPreset LOW = new QualityPreset(90, 30, 40, 40, 5, 400, 0.35, false, 2, 400, 24, false);
    public static final QualityPreset MEDIUM = new QualityPreset(180, 60, 80, 80, 10, 1200, 0.7, true, 3, 700, 48, true);
    public static final QualityPreset HIGH = new QualityPreset(320, 100, 140, 140, 15, 2500, 1.0, true, 3, 1100, 80, true);
    public static final QualityPreset ULTRA = new QualityPreset(520, 160, 220, 220, 20, 4500, 1.4, true, 3, 1600, 120, true);

    /** The fewest of a cloud's puffs a far LOD keeps, as a share of its full count. */
    public static final double MIN_LOD = 0.3;

    public static QualityPreset of(ExplosionQuality quality) {
        return switch (quality == null ? ExplosionQuality.MEDIUM : quality) {
            case LOW -> LOW;
            case MEDIUM -> MEDIUM;
            case HIGH -> HIGH;
            case ULTRA -> ULTRA;
        };
    }

    /** Every puff a cloud has at full detail. */
    public int totalPuffs() {
        return capPuffs + stemPuffs + surgePuffs + extraPuffs;
    }

    /**
     * The share of its puffs a cloud {@code distance} blocks off is drawn with: all within {@link #fullDetail}, falling with the
     * distance past it down to {@link #MIN_LOD}. A puff of a far cloud covers a pixel or two, so fewer and bigger ones look the
     * same.
     */
    public double lod(double distance) {
        if (distance <= fullDetail) {
            return 1;
        }
        return Math.max(MIN_LOD, fullDetail / distance);
    }

    /** How much bigger each puff is drawn when only {@code lod} of them are, so the cloud covers the same area. */
    public static double lodGrowth(double lod) {
        return 1 / Math.sqrt(Math.max(MIN_LOD, Math.min(1, lod)));
    }

    /** Seconds between rebuilds of a cloud's mesh, {@code age} seconds after it went off: every frame while the fireball burns. */
    public double rebuildSeconds(double age, double fireballSeconds) {
        if (age < fireballSeconds * 1.5 + 1) {
            return 0;
        }
        return 1 / rebuildHz;
    }

    /** {@code count} puffs scaled by the player's fine detail ({@code detail}, 0.25-2) and the LOD, never fewer than a handful. */
    public static int scaled(int count, double detail, double lod) {
        return Math.max(4, (int) Math.round(count * Math.max(0.25, Math.min(2, detail)) * Math.max(MIN_LOD, Math.min(1, lod))));
    }
}
