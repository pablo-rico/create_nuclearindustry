package org.papiricoh.create_nuclearindustry.fusion.heating.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.papiricoh.create_nuclearindustry.fusion.multiblock.TokamakGeometry;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokamakSkinMeshTest {
    private static final int QUADS_PER_SEGMENT = 2 * 16;
    private static final int QUADS_PER_BAND = 16 + 2 * TokamakSkinMesh.SIDES;

    @ParameterizedTest
    @ValueSource(ints = {4, 7, 16})
    void aBareRingIsSkinLinerAndBands(int radius) {
        TokamakSkinMesh mesh = TokamakSkinMesh.build(radius, List.of(), List.of());
        int expected = TokamakSkinMesh.segmentsFor(radius) * QUADS_PER_SEGMENT + TokamakSkinMesh.bandsFor(radius) * QUADS_PER_BAND;
        assertEquals(expected, mesh.quads().size());
    }

    @ParameterizedTest
    @ValueSource(ints = {4, 9})
    void skinFacesOutAndLinerFacesIn(int radius) {
        for (TokamakSkinMesh.Quad quad : TokamakSkinMesh.build(radius, List.of(), List.of()).quads()) {
            float[] p = quad.xyz();
            double cx = (p[0] + p[3] + p[6] + p[9]) / 4;
            double cy = (p[1] + p[4] + p[7] + p[10]) / 4;
            double cz = (p[2] + p[5] + p[8] + p[11]) / 4;
            double rho = Math.hypot(cx, cz);
            // From the centreline of the tube to the quad.
            double ox = cx - cx / rho * radius;
            double oz = cz - cz / rho * radius;
            double out = ox * quad.nx() + cy * quad.ny() + oz * quad.nz();
            if (quad.kind() == TokamakSkinMesh.Kind.LINER) {
                assertTrue(out < 0, "liner faces out at R=" + radius);
            } else if (quad.kind() != TokamakSkinMesh.Kind.BAND || quad.ny() != 0 || Math.abs(quad.nx()) + Math.abs(quad.nz()) < 0.99) {
                assertTrue(out > 0, quad.kind() + " faces in at R=" + radius);
            }
            assertWindingMatchesNormal(quad);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {4, 12})
    void skinStaysInsideTheShellAndOutsideTheChamber(int radius) {
        for (TokamakSkinMesh.Quad quad : TokamakSkinMesh.build(radius, List.of(), List.of()).quads()) {
            if (quad.kind() == TokamakSkinMesh.Kind.BAND) {
                continue;
            }
            float[] p = quad.xyz();
            for (int c = 0; c < 4; c++) {
                double offset = Math.hypot(p[c * 3], p[c * 3 + 2]) - radius;
                double y = p[c * 3 + 1];
                assertTrue(Math.abs(offset) <= 2.5 && Math.abs(y) <= 2.5, "skin outside the shell at R=" + radius);
                assertTrue(Math.abs(offset) > 1.5 || Math.abs(y) > 1.5, "skin inside the chamber at R=" + radius);
            }
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {4, 8, 16})
    void everyShellCellLiesUnderAPatchAndNothingElseDoes(int radius) {
        TokamakGeometry geometry = TokamakGeometry.of(radius);
        Set<Long> patches = new HashSet<>();
        for (int i = 0; i < geometry.shellCount(); i++) {
            long patch = TokamakSkinMesh.patchFor(radius, geometry.shellX(i), geometry.shellY(i), geometry.shellZ(i));
            assertTrue(patch >= 0, "shell cell without a patch at R=" + radius);
            patches.add(patch);
        }
        for (int i = 0; i < geometry.cornerCount(); i++) {
            assertEquals(-1, TokamakSkinMesh.patchFor(radius, geometry.cornerX(i), geometry.cornerY(i), geometry.cornerZ(i)));
        }
        for (int i = 0; i < geometry.plasmaCount(); i++) {
            assertEquals(-1, TokamakSkinMesh.patchFor(radius, geometry.plasmaX(i), geometry.plasmaY(i), geometry.plasmaZ(i)));
        }
        // Flat-side patches of the whole ring, most of them over at least one cell.
        int flatPatches = TokamakSkinMesh.segmentsFor(radius) * 12;
        assertTrue(patches.size() > flatPatches / 3, "too few patches covered at R=" + radius + ": " + patches.size());
    }

    @Test
    void aViewportOpensAWindow() {
        int radius = 4;
        int[] viewport = {TokamakGeometry.controllerToAxis(radius), 0, 1};
        TokamakSkinMesh mesh = TokamakSkinMesh.build(radius, List.of(viewport), List.of());
        assertEquals(1, mesh.holeCount());
        long windows = mesh.quads().stream().filter(q -> q.kind() == TokamakSkinMesh.Kind.WINDOW).count();
        long glass = mesh.quads().stream().filter(q -> q.kind() == TokamakSkinMesh.Kind.GLASS).count();
        long liner = mesh.quads().stream().filter(q -> q.kind() == TokamakSkinMesh.Kind.LINER).count();
        assertEquals(1, windows);
        assertEquals(1, glass);
        assertEquals(TokamakSkinMesh.segmentsFor(radius) * 16 - 1, liner);
    }

    @Test
    void bandsKeepClearOfNozzles() {
        int radius = 4;
        int bands = TokamakSkinMesh.bandsFor(radius);
        double theta = 2 * Math.PI * 0.5 / bands;
        int d = TokamakGeometry.controllerToAxis(radius);
        int[] nozzle = {(int) Math.round(d * Math.cos(theta)), 0, (int) Math.round(d * Math.sin(theta))};
        int bare = TokamakSkinMesh.build(radius, List.of(), List.of()).quads().size();
        int clear = TokamakSkinMesh.build(radius, List.of(), List.<int[]>of(nozzle)).quads().size();
        assertEquals(bare - QUADS_PER_BAND, clear);
    }

    private static void assertWindingMatchesNormal(TokamakSkinMesh.Quad quad) {
        float[] p = quad.xyz();
        double ax = p[3] - p[0], ay = p[4] - p[1], az = p[5] - p[2];
        double bx = p[6] - p[0], by = p[7] - p[1], bz = p[8] - p[2];
        double dot = (ay * bz - az * by) * quad.nx() + (az * bx - ax * bz) * quad.ny() + (ax * by - ay * bx) * quad.nz();
        assertTrue(dot > 0, quad.kind() + " winds against its normal");
    }
}
