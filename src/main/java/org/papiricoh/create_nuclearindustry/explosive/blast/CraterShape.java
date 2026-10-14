package org.papiricoh.create_nuclearindustry.explosive.blast;

/**
 * The shape of a nuclear crater, column by column: a bowl dug below the ground zero, its lip raised
 * above the surrounding ground and falling away outside it, ejecta scattered beyond, a floor of fused
 * glass and basalt with magma at the centre. Heights are blocks relative to ground zero inside the bowl
 * and to each column's own ground outside it. The edge waves with the angle, more for a hotter warhead.
 * No game classes.
 */
public final class CraterShape {
    /** How much the edge waves, a fraction of the radius at full intensity. */
    public static final double EDGE_NOISE = 0.06;

    public enum Floor {NONE, TRINITITE, OBSIDIAN, BASALT, MAGMA}

    public enum Debris {COARSE_DIRT, GRAVEL, COBBLESTONE, TUFF}

    private final DetonationProfile profile;

    public CraterShape(DetonationProfile profile) {
        this.profile = profile;
    }

    /** The waving of the edge for a column, about [-1, 1] times the warhead's intensity. */
    public double edgeNoise(int dx, int dz) {
        double angle = Math.atan2(dz, dx);
        double distance = Math.sqrt((double) dx * dx + (double) dz * dz);
        double n = 0.55 * Math.sin(angle * 5.0 + distance * 0.06)
                + 0.30 * Math.sin(angle * 11.0 - distance * 0.03 + 1.7)
                + 0.15 * Math.sin(angle * 23.0 + 3.1);
        return Math.max(-1.0, Math.min(1.0, n * profile.intensity()));
    }

    /** The bowl's radius along this column's direction. */
    public double radius(int dx, int dz) {
        return profile.craterRadius() * (1.0 + EDGE_NOISE * edgeNoise(dx, dz));
    }

    public boolean inBowl(int dx, int dz) {
        return profile.craterRadius() > 0 && distance(dx, dz) < radius(dx, dz);
    }

    /**
     * The bowl's surface below ground zero for a column in it: deepest at the centre, rising to the
     * rim's height at the edge.
     */
    public double bowlSurface(int dx, int dz) {
        double t = distance(dx, dz) / radius(dx, dz);
        if (t >= 1) {
            return profile.rimHeight();
        }
        return -profile.craterDepth() * (1 - t * t) + profile.rimHeight() * t * t * t * t;
    }

    /**
     * Blocks the rim piles on a column's ground outside the bowl: its full height at the bowl's edge,
     * falling away ever faster to nothing at the rim's end.
     */
    public int rimPile(int dx, int dz) {
        double r = distance(dx, dz);
        double inner = radius(dx, dz);
        double outer = profile.rimRadius() * (1.0 + EDGE_NOISE * edgeNoise(dx, dz));
        if (r < inner || r >= outer || outer <= inner) {
            return 0;
        }
        double t = (outer - r) / (outer - inner);
        return (int) Math.round(profile.rimHeight() * t * (2 - t));
    }

    /** Whether a lone block of ejecta lands on a column beyond the rim. */
    public boolean ejecta(int x, int z, int dx, int dz) {
        double r = distance(dx, dz);
        double rim = profile.rimRadius();
        double end = profile.ejectaRadius();
        if (r < rim || r >= end || end <= rim) {
            return false;
        }
        double t = 1 - (r - rim) / (end - rim);
        return BlastHash.unit(x, z, 11) < 0.45 * t * t;
    }

    /** What the bowl's floor becomes at a column: fused sand glass, with basalt, obsidian and at the centre magma. */
    public Floor floor(int x, int z, int dx, int dz) {
        if (!inBowl(dx, dz)) {
            return Floor.NONE;
        }
        double t = distance(dx, dz) / Math.max(1.0e-6, profile.craterRadius());
        double roll = BlastHash.unit(x, z, 23);
        if (t < 0.2) {
            return roll < 0.35 ? Floor.MAGMA : roll < 0.5 ? Floor.OBSIDIAN : Floor.TRINITITE;
        }
        if (distance(dx, dz) < profile.glassRadius()) {
            return roll < 0.45 ? Floor.TRINITITE : roll < 0.7 ? Floor.BASALT : roll < 0.8 ? Floor.OBSIDIAN : Floor.NONE;
        }
        return Floor.NONE;
    }

    /** What a block of the rim or the ejecta is. */
    public static Debris debris(int x, int y, int z) {
        double roll = BlastHash.unit(x, y, z, 31);
        return roll < 0.35 ? Debris.COARSE_DIRT : roll < 0.6 ? Debris.GRAVEL : roll < 0.85 ? Debris.COBBLESTONE : Debris.TUFF;
    }

    public static double distance(int dx, int dz) {
        return Math.sqrt((double) dx * dx + (double) dz * dz);
    }
}
