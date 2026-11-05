package org.papiricoh.create_nuclearindustry.explosive.fx.model;

import org.junit.jupiter.api.Test;
import org.papiricoh.create_nuclearindustry.explosive.blast.ExplosionPalette;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The mushroom cloud's phases: fireball, condensation, rise and torus, stem and surge, flattening, drift and dispersal. */
class CloudEvolutionTest {
    private record P(double x, double y, double z, double radius, int tile, float r, float g, float b, float alpha, int flags) {}

    private static CloudEvolution fission(double scale) {
        return new CloudEvolution(7, ExplosionPalette.FISSION, scale, 0.12 * scale, 0, false, 8, 1.5 * scale, 1.6 * scale, 0,
                ExplosionPalette.DUST);
    }

    private static CloudEvolution thermonuclear() {
        return new CloudEvolution(7, ExplosionPalette.THERMONUCLEAR, 750, 90, 0, false, 8, 768, 1050, 0, ExplosionPalette.DUST);
    }

    private static List<P> puffs(CloudEvolution c, double t) {
        List<P> out = new ArrayList<>();
        c.emit(t, CloudEvolution.Counts.of(QualityPreset.MEDIUM, 1, 1),
                (x, y, z, radius, tile, spin, r, g, b, alpha, heat, nx, ny, nz, occ, flags) -> out.add(new P(x, y, z, radius, tile, r, g, b, alpha, flags)));
        return out;
    }

    @Test
    void aFissionCloudStands250To400BlocksTall() {
        for (double scale : new double[]{80, 160, 250}) {
            CloudEvolution c = fission(scale);
            assertFalse(c.flattened(), "a fission cloud never reaches the tropopause");
            double top = puffs(c, 200).stream().mapToDouble(p -> p.y + p.radius * 0.5).max().orElse(0);
            assertTrue(top > 230 && top < 420, "scale " + scale + ": " + top);
        }
        assertTrue(fission(250).topHeight() > fission(80).topHeight(), "a bigger yield rises higher");
    }

    @Test
    void aThermonuclearCloudFlattensAtTheTropopause() {
        CloudEvolution h = thermonuclear();
        assertTrue(h.flattened());
        assertTrue(h.freeTop() > CloudEvolution.TROPOPAUSE);
        assertTrue(h.topHeight() <= CloudEvolution.TROPOPAUSE + 80);
        assertTrue(h.capRadius() > 1.5 * fission(250).capRadius(), "it spreads into a wide anvil instead");
        assertTrue(h.capRadius(300) > h.capRadius(120), "and keeps spreading");
    }

    @Test
    void theFireballCoolsFromWhiteThroughOrange() {
        CloudEvolution c = fission(250);
        List<P> first = puffs(c, 0.05);
        assertTrue(first.stream().anyMatch(p -> (p.flags & CloudEvolution.EMISSIVE) != 0), "it glows");
        double white = meanBlue(first);
        double later = meanBlue(puffs(c, 0.6 * c.fireballSeconds()));
        assertTrue(white > 0.6 && later < white, "blue falls as it cools: " + white + " -> " + later);
        assertTrue(c.fireballRadius(1) > c.fireballRadius(0.05));
        assertTrue(puffs(c, 120).stream().noneMatch(p -> (p.flags & CloudEvolution.EMISSIVE) != 0), "two minutes later it has cooled");
    }

    @Test
    void itRisesAndRollsIntoATorus() {
        CloudEvolution c = fission(250);
        assertTrue(c.capCentre(5) < c.capCentre(30) && c.capCentre(30) < c.capCentre(120));
        assertTrue(c.capCentre(300) <= c.topHeight());
        // The torus: at the cap's height the puffs ring its middle, which stays open.
        double r = c.capRadius(60);
        List<P> cap = puffs(c, 60).stream().filter(p -> Math.abs(p.y - c.capCentre(60)) < 0.2 * r).toList();
        double cx = cap.stream().mapToDouble(P::x).average().orElse(0), cz = cap.stream().mapToDouble(P::z).average().orElse(0);
        long middle = cap.stream().filter(p -> Math.hypot(p.x - cx, p.z - cz) < 0.15 * r).count();
        assertTrue(cap.size() > 20 && middle * 10 < cap.size(), "around " + cap.size() + ", in the middle " + middle);
    }

    @Test
    void condensationComesAndGoes() {
        CloudEvolution c = fission(250);
        assertTrue(veils(c, 1.2) > 0, "the Wilson cloud forms behind the shock");
        assertEquals(0, puffs(c, 12).stream().filter(p -> p.tile == CloudEvolution.TILE_VEIL && p.y < 60).count(), "and is gone");
        double pileus = 0;
        for (double t = 2; t < 60; t += 0.5) pileus = Math.max(pileus, count(c, t, CloudEvolution.TILE_PILEUS));
        assertTrue(pileus > 0, "a pileus hood rides over the rising cap");
        assertEquals(0, count(c, 200, CloudEvolution.TILE_PILEUS));
    }

    @Test
    void itDriftsDownwindDarkensAndDisperses() {
        CloudEvolution c = new CloudEvolution(7, ExplosionPalette.FISSION, 250, 30, 0, false, 8, 375, 400, 0, ExplosionPalette.DUST);
        double early = meanX(puffs(c, 30), 200);
        double late = meanX(puffs(c, 250), 200);
        assertTrue(late > early + 30, "the wind blows along +x: " + early + " -> " + late);
        ExplosionPalette.Rgb young = c.capColour(5);
        assertTrue(young.red() > young.green() * 1.3 && young.green() > young.blue(), "reddish-brown while hot: " + young);
        for (double t = 0; t < c.lifetime(); t += 10) {
            assertTrue(c.capColour(t).luminance() > 0.4 && c.stemColour(t).luminance() > 0.35, "never soot-black at " + t);
        }
        assertTrue(c.capColour(60).luminance() > c.capColour(5).luminance() + 0.2, "tan and beige once condensation forms");
        assertTrue(c.life(330) > 0.01 && c.life(330) < 0.1, "a faint smear of haze near its end");
        assertTrue(c.lifetime() >= 300 && c.lifetime() <= 360);
        assertTrue(puffs(c, c.lifetime() + 1).isEmpty(), "and is gone");
        assertTrue(c.life(c.lifetime() - 10) < 0.1 && c.life(100) == 1);
    }

    /** At stabilisation the cap is about as wide as the destruction radius, its top 1.3-1.6 of it, the stem a fifth of it. */
    @Test
    void itFillsTheSkyAsBigAsItsYield() {
        CloudEvolution c = fission(250);
        List<P> late = puffs(c, 90);
        double cx = late.stream().filter(p -> p.y > c.capCentre(90) - 20).mapToDouble(P::x).average().orElse(0);
        double cz = late.stream().filter(p -> p.y > c.capCentre(90) - 20).mapToDouble(P::z).average().orElse(0);
        double wide = late.stream().filter(p -> Math.abs(p.y - c.capCentre(90)) < 60)
                .mapToDouble(p -> Math.hypot(p.x - cx, p.z - cz) + p.radius).max().orElse(0);
        assertTrue(2 * wide > 0.8 * 250 && 2 * wide < 1.4 * 250, "cap " + 2 * wide);
        double top = late.stream().mapToDouble(p -> p.y + p.radius * 0.6).max().orElse(0);
        assertTrue(top > 1.3 * 250 && top < 1.75 * 250, "top " + top);
        assertTrue(c.capCentre(60) > 0.75 * c.capCentre(300), "most of its height within a minute");
        CloudEvolution h = thermonuclear();
        assertTrue(2 * h.capRadius(120) > 1.3 * 750, "an anvil a thousand blocks and more across: " + 2 * h.capRadius(120));
    }

    /** Fire columns stand on their fires' ground, not floating: their lowest puff sits at it. */
    @Test
    void fireColumnsStandOnTheGround() {
        CloudEvolution c = fission(250);
        for (int i = 0; i < c.fireCount(); i++) c.fireGround(i, 12);
        double[] at = new double[2];
        c.firePoint(0, at);
        double low = puffs(c, 150).stream().filter(p -> Math.hypot(p.x - at[0], p.z - at[1]) < 30).mapToDouble(p -> p.y).min().orElse(-1);
        assertTrue(low >= 12 && low < 25, "rooted at its ground: " + low);
        c.unknownFireGround();
        assertTrue(puffs(c, 150).stream().noneMatch(p -> Math.hypot(p.x - at[0], p.z - at[1]) < 5 && p.y < 40), "none where the ground is unknown");
    }

    @Test
    void anAirburstsStemIsThinner() {
        CloudEvolution ground = fission(250);
        CloudEvolution air = new CloudEvolution(7, ExplosionPalette.FISSION, 250, 30, 120, true, 8, 375, 400, 0, ExplosionPalette.DUST);
        assertTrue(stemAlpha(air, 30) < stemAlpha(ground, 30));
    }

    @Test
    void lightningOnlyInAYoungCloud() {
        CloudEvolution c = fission(250);
        int early = 0;
        for (double t = 0.5; t < 26; t += 0.01) {
            int[] n = {0};
            c.lightning(t, (x, y, z, r, tile, spin, red, g, b, a, heat, nx, ny, nz, occ, flags) -> n[0]++);
            early += n[0];
        }
        assertTrue(early > 0);
        int[] late = {0};
        c.lightning(60, (x, y, z, r, tile, spin, red, g, b, a, heat, nx, ny, nz, occ, flags) -> late[0]++);
        assertEquals(0, late[0]);
    }

    @Test
    void sameSeedSameCloudAndDetailAddsPuffs() {
        assertEquals(puffs(fission(250), 40), puffs(fission(250), 40));
        int few = countAll(fission(250), 60, CloudEvolution.Counts.of(QualityPreset.LOW, 1, 1));
        int many = countAll(fission(250), 60, CloudEvolution.Counts.of(QualityPreset.ULTRA, 1, 1));
        assertTrue(many > 2 * few, few + " vs " + many);
    }

    @Test
    void theDustFrontRidesTheShock() {
        CloudEvolution c = fission(250);
        double[] r = {0};
        c.dustFront(1.0, 32, (x, y, z, radius, tile, spin, red, g, b, a, heat, nx, ny, nz, occ, flags) -> r[0] = Math.max(r[0], Math.hypot(x, z)));
        assertEquals(c.shockRadius(1.0), r[0], 0.05 * c.shockRadius(1.0));
        int[] n = {0};
        c.dustFront(60, 32, (x, y, z, radius, tile, spin, red, g, b, a, heat, nx, ny, nz, occ, flags) -> n[0]++);
        assertEquals(0, n[0], "gone once the shock has died out");
    }

    private static int countAll(CloudEvolution c, double t, CloudEvolution.Counts counts) {
        int[] n = {0};
        c.emit(t, counts, (x, y, z, radius, tile, spin, r, g, b, alpha, heat, nx, ny, nz, occ, flags) -> n[0]++);
        return n[0];
    }

    private static long veils(CloudEvolution c, double t) {
        return count(c, t, CloudEvolution.TILE_VEIL);
    }

    private static long count(CloudEvolution c, double t, int tile) {
        return puffs(c, t).stream().filter(p -> p.tile == tile && p.alpha > 0.01).count();
    }

    private static double meanBlue(List<P> puffs) {
        return puffs.stream().filter(p -> (p.flags & CloudEvolution.EMISSIVE) != 0).mapToDouble(p -> p.b / Math.max(0.01, p.r)).average().orElse(0);
    }

    private static double meanX(List<P> puffs, double above) {
        return puffs.stream().filter(p -> p.y > above).mapToDouble(P::x).average().orElse(0);
    }

    private static double stemAlpha(CloudEvolution c, double t) {
        return puffs(c, t).stream().filter(p -> p.y > 20 && p.y < 100 && Math.hypot(p.x, p.z) < 40
                        && p.tile >= CloudEvolution.TILE_DENSE && p.tile < CloudEvolution.TILE_FIRE)
                .mapToDouble(p -> p.alpha).sum();
    }
}
