package org.papiricoh.create_nuclearindustry.explosive.blast;

import org.papiricoh.create_nuclearindustry.explosive.fx.model.CloudEvolution;
import org.papiricoh.create_nuclearindustry.explosive.fx.model.QualityPreset;

import java.util.ArrayList;
import java.util.List;

/**
 * The fireball and mushroom cloud of a nuclear detonation as a plain list of puffs, for tests and for anything that wants the
 * cloud without drawing it. The cloud itself, phase by phase, is {@link CloudEvolution}; the renderer reads it straight from
 * there without building lists. Positions are blocks from ground zero. No game classes.
 */
public final class MushroomCloudModel {
    /** Seconds the longest-lived cloud lasts; a smaller one is gone sooner ({@link CloudEvolution#lifetime()}). */
    public static final double LIFETIME = 360;

    /** One puff: centre, radius, colour 0-1 (before the light), opacity and whether it gives light of its own. */
    public record Puff(double x, double y, double z, double radius, float red, float green, float blue, float alpha, boolean glowing) {}

    private final CloudEvolution cloud;

    /** A fission warhead's cloud. */
    public MushroomCloudModel(long seed, double scale, double fireball, boolean airburst, double burstHeight) {
        this(seed, scale, fireball, airburst, burstHeight, ExplosionPalette.FISSION);
    }

    /**
     * @param seed        picks the puffs
     * @param scale       the warhead's destruction radius, which sizes the cloud
     * @param fireball    the fireball's radius
     * @param airburst    whether it went off in the air
     * @param burstHeight blocks above the ground it went off at
     * @param palette     its physics
     */
    public MushroomCloudModel(long seed, double scale, double fireball, boolean airburst, double burstHeight, ExplosionPalette palette) {
        this.cloud = new CloudEvolution(seed, palette, scale, fireball, burstHeight, airburst, 8, 1.5 * scale, 1.6 * scale, 0,
                ExplosionPalette.DUST);
    }

    public CloudEvolution evolution() {
        return cloud;
    }

    /** How high the cloud's top rises in the end. */
    public double topHeight() {
        return cloud.topHeight();
    }

    /** The cap's radius at the end of its rise. */
    public double capRadius() {
        return cloud.capRadius();
    }

    /** Whether the fireball still glows {@code seconds} after the detonation. */
    public boolean hasFireball(double seconds) {
        return seconds < cloud.fireballSeconds();
    }

    /** How hot the cloud's gas still is, 1 at the start falling to 0. */
    public double heatAt(double seconds) {
        return cloud.heat(seconds);
    }

    /** How high the cap's centre is {@code seconds} after the detonation. */
    public double capHeight(double seconds) {
        return cloud.capCentre(seconds);
    }

    /** The puffs {@code seconds} after the detonation; {@code detail} 0.25-2 scales how many. */
    public List<Puff> puffs(double seconds, double detail) {
        List<Puff> puffs = new ArrayList<>();
        cloud.emit(seconds, CloudEvolution.Counts.of(QualityPreset.MEDIUM, detail, 1),
                (x, y, z, radius, tile, spin, red, green, blue, alpha, heat, nx, ny, nz, occlusion, flags) ->
                        puffs.add(new Puff(x, y, z, radius, red, green, blue, alpha, (flags & CloudEvolution.EMISSIVE) != 0)));
        return puffs;
    }
}
