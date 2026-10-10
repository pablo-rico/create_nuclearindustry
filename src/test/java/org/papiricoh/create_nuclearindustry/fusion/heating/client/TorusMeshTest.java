package org.papiricoh.create_nuclearindustry.fusion.heating.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.papiricoh.create_nuclearindustry.fusion.multiblock.TokamakGeometry;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TorusMeshTest {

    @ParameterizedTest
    @ValueSource(ints = {4, 6, 9, 16})
    void everyCornerLiesOnTheEllipticalCrossSection(int radius) {
        double a = 1.15;
        double b = 1.3;
        TorusMesh mesh = TorusMesh.build(radius, a, b, TorusMesh.majorSegmentsFor(radius), 16);
        float[] corners = new float[12];
        for (int q = 0; q < mesh.quadCount(); q++) {
            mesh.quad(q, corners);
            for (int c = 0; c < 4; c++) {
                double x = corners[c * 3];
                double y = corners[c * 3 + 1];
                double z = corners[c * 3 + 2];
                double rho = Math.sqrt(x * x + z * z);
                double ellipse = Math.pow((rho - radius) / a, 2) + Math.pow(y / b, 2);
                assertEquals(1.0, ellipse, 1e-4, "corner off the surface at quad " + q);
            }
        }
    }

    @Test
    void quadCountIsTheGrid() {
        TorusMesh mesh = TorusMesh.build(5, 1, 1, 40, 12);
        assertEquals(480, mesh.quadCount());
        assertEquals(40, mesh.majorSegments());
        assertEquals(12, mesh.minorSegments());
    }

    @Test
    void ringClosesWithoutASeam() {
        int major = 30;
        int minor = 10;
        TorusMesh mesh = TorusMesh.build(6, 1, 1.2, major, minor);
        float[] last = new float[12];
        float[] first = new float[12];
        for (int j = 0; j < minor; j++) {
            mesh.quad((major - 1) * minor + j, last);
            mesh.quad(j, first);
            // The last column's far edge is the first column's near edge.
            assertArrayEquals(new float[]{first[0], first[1], first[2]}, new float[]{last[9], last[10], last[11]}, 1e-5f);
            assertArrayEquals(new float[]{first[3], first[4], first[5]}, new float[]{last[6], last[7], last[8]}, 1e-5f);
        }
        // And round the cross-section: quad j's far poloidal edge is quad j+1's near one.
        float[] next = new float[12];
        mesh.quad(minor - 1, last);
        mesh.quad(0, next);
        assertArrayEquals(new float[]{next[0], next[1], next[2]}, new float[]{last[3], last[4], last[5]}, 1e-5f);
    }

    @ParameterizedTest
    @ValueSource(ints = {4, 8, 16, 40})
    void bigRingsGetEnoughSegments(int radius) {
        int segments = TorusMesh.majorSegmentsFor(radius);
        assertTrue(segments >= TorusMesh.MIN_MAJOR_SEGMENTS && segments <= TorusMesh.MAX_MAJOR_SEGMENTS);
        double chord = 2 * Math.PI * radius / segments;
        assertTrue(chord <= TorusMesh.MAX_CHORD || segments == TorusMesh.MAX_MAJOR_SEGMENTS,
                "edges of " + chord + " blocks at R=" + radius);
        assertTrue(TorusMesh.majorSegmentsFor(radius + 4) >= segments, "more radius never means fewer segments");
    }

    /** The core must sit in the chamber's air, and nearly all of the halo too; the shell hides the rest. */
    @ParameterizedTest
    @ValueSource(ints = {4, 5, 8, 12})
    void plasmaFitsTheChamber(int radius) {
        TokamakGeometry geometry = TokamakGeometry.of(radius);
        int segments = TorusMesh.majorSegmentsFor(radius);
        TorusMesh core = TorusMesh.build(radius, PlasmaTorusRenderer.CORE_HALF_WIDTH, PlasmaTorusRenderer.CORE_HALF_HEIGHT, segments, 16);
        TorusMesh halo = TorusMesh.build(radius, PlasmaTorusRenderer.HALO_HALF_WIDTH, PlasmaTorusRenderer.HALO_HALF_HEIGHT, segments, 16);
        assertEquals(1.0, insideShare(core, geometry), 1e-9, "core outside the chamber");
        assertTrue(insideShare(halo, geometry) > 0.9, "halo mostly in the chamber, got " + insideShare(halo, geometry));
        // About 2.4 blocks across, as the chamber is 3.
        double width = 2 * PlasmaTorusRenderer.HALO_HALF_WIDTH;
        assertTrue(width > 2.0 && width < 3.0);
    }

    private static double insideShare(TorusMesh mesh, TokamakGeometry geometry) {
        float[] corners = new float[12];
        int inside = 0;
        int total = 0;
        for (int q = 0; q < mesh.quadCount(); q++) {
            mesh.quad(q, corners);
            // Mesh coordinates are relative to the middle of the centre block.
            int x = (int) Math.floor(corners[0] + 0.5);
            int y = (int) Math.floor(corners[1] + 0.5);
            int z = (int) Math.floor(corners[2] + 0.5);
            total++;
            if (geometry.classify(x, y, z) == TokamakGeometry.Cell.PLASMA) {
                inside++;
            }
        }
        return inside / (double) total;
    }

    @Test
    void rejectsImpossibleRings() {
        assertThrows(IllegalArgumentException.class, () -> TorusMesh.build(1, 1.5, 1, 24, 8));
        assertThrows(IllegalArgumentException.class, () -> TorusMesh.build(4, 1, 1, 2, 8));
        assertThrows(IllegalArgumentException.class, () -> TorusMesh.build(4, 1, 0, 24, 8));
    }

    @Test
    void meshIsReusedOnlyForTheSameShape() {
        TorusMesh mesh = TorusMesh.build(4, 1, 1, 24, 8);
        assertTrue(mesh.matches(4, 1, 1, 24, 8));
        assertTrue(!mesh.matches(5, 1, 1, 24, 8));
    }
}
