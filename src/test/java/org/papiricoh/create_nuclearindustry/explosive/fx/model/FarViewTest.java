package org.papiricoh.create_nuclearindustry.explosive.fx.model;

import org.junit.jupiter.api.Test;
import org.papiricoh.create_nuclearindustry.explosive.blast.ExplosionPalette;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A cloud past the far plane looks exactly as big as it is: pulled in by distance and size alike, its impostor painted to size. */
class FarViewTest {
    /** A 12-chunk render distance: the far plane at four times 192 blocks, pulled in at nine tenths of it. */
    private static final double LIMIT = 0.9 * 4 * 192;

    private static CloudEvolution fission() {
        return new CloudEvolution(7, ExplosionPalette.FISSION, 250, 30, 0, false, 8, 375, 400, 0, ExplosionPalette.DUST);
    }

    /** How far the cap's puffs reach from its middle, as drawn. */
    private static double meshCapRadius(CloudEvolution c, double t) {
        double centre = c.capCentre(t);
        double[] sum = new double[3];
        double[] max = {0};
        c.emit(t, CloudEvolution.Counts.of(QualityPreset.MEDIUM, 1, 1), (x, y, z, r, tile, spin, red, g, b, a, heat, nx, ny, nz, occ, flags) -> {
            if (Math.abs(y - centre) < c.capHalfThickness(t)) {
                sum[0] += x;
                sum[1] += z;
                sum[2]++;
            }
        });
        double cx = sum[0] / sum[2], cz = sum[1] / sum[2];
        c.emit(t, CloudEvolution.Counts.of(QualityPreset.MEDIUM, 1, 1), (x, y, z, r, tile, spin, red, g, b, a, heat, nx, ny, nz, occ, flags) -> {
            if (Math.abs(y - centre) < c.capHalfThickness(t) && tile < CloudEvolution.TILE_FIRE) {
                max[0] = Math.max(max[0], Math.hypot(x - cx, z - cz) + r);
            }
        });
        return max[0];
    }

    @Test
    void theVisibleCapIsWhereItsPuffsReach() {
        CloudEvolution c = fission();
        for (double t : new double[]{30, 60, 90, 180}) {
            double mesh = meshCapRadius(c, t);
            assertEquals(mesh, c.visibleCapRadius(t), 0.12 * mesh, "at " + t + " s");
        }
    }

    @Test
    void pulledInOrImpostorTheCapSubtendsItsTrueAngle() {
        CloudEvolution c = fission();
        double t = 60;
        double r = meshCapRadius(c, t);
        FarView.Sheet sheet = FarView.capSheet(c, t);
        for (double d : new double[]{350, 800, 1600}) {
            double truth = FarView.angularDiameter(r, d);
            double k = FarView.pullIn(d, LIMIT);
            double pulled = FarView.angularDiameter(r * k, d * k);
            assertEquals(truth, pulled, 1e-9, "the mesh pulled in at " + d);
            double painted = FarView.angularDiameter(sheet.halfWidth() * FarView.CAP_FILL_WIDTH * k, d * k);
            assertEquals(truth, painted, 0.12 * truth, "the impostor at " + d);
        }
        assertTrue(Math.toDegrees(FarView.angularDiameter(r, 350)) > 35, "about R across, seen from 350 blocks");
    }

    @Test
    void theImpostorsSheetsCoverTheCloud() {
        CloudEvolution c = fission();
        double t = 90;
        FarView.Sheet cap = FarView.capSheet(c, t);
        double paintedTop = cap.centreY() + (FarView.CAP_RAISED + FarView.CAP_FILL_HEIGHT) * cap.halfHeight();
        double paintedBottom = cap.centreY() + (FarView.CAP_RAISED - FarView.CAP_FILL_HEIGHT) * cap.halfHeight();
        assertEquals(c.visibleCapTop(t), paintedTop, 1e-6);
        assertEquals(c.visibleCapBottom(t), paintedBottom, 1e-6);
        FarView.Sheet stem = FarView.stemSheet(c, t);
        assertTrue(stem.centreY() - stem.halfHeight() * FarView.STEM_FILL_HEIGHT < 1, "the stem stands on the ground");
        assertTrue(stem.centreY() + stem.halfHeight() * FarView.STEM_FILL_HEIGHT > c.visibleCapBottom(t), "and reaches into the cap");
    }

    @Test
    void theFireballRisesFastAndLeavesAStem() {
        CloudEvolution c = fission();
        assertTrue(c.capCentre(3) > 30, "off the ground in seconds: " + c.capCentre(3));
        assertTrue(c.capCentre(10) > 0.25 * c.capCentre(300), "a third of the way up by ten seconds: " + c.capCentre(10));
        assertTrue(c.capHalfThickness(10) < 0.75 * c.capRadius(10), "already rolling flat into a torus");
    }
}
