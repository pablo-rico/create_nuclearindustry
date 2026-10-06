package org.papiricoh.create_nuclearindustry.fusion.multiblock;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokamakGeometryTest {

    @Test
    void rhoRoundsHalvesUp() {
        assertEquals(0, TokamakGeometry.rho(0, 0));
        assertEquals(5, TokamakGeometry.rho(3, 4));
        assertEquals(1, TokamakGeometry.rho(1, 1));   // 1.414
        assertEquals(2, TokamakGeometry.rho(2, 1));   // 2.236
        assertEquals(3, TokamakGeometry.rho(2, 2));   // 2.828
        assertEquals(16, TokamakGeometry.rho(16, 0));
    }

    @ParameterizedTest
    @ValueSource(ints = {4, 5, 8, 12, 16})
    void shellSealsThePlasma(int radius) {
        TokamakGeometry geometry = TokamakGeometry.of(radius);
        int[][] neighbours = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
        for (int i = 0; i < geometry.plasmaCount(); i++) {
            int x = geometry.plasmaX(i);
            int y = geometry.plasmaY(i);
            int z = geometry.plasmaZ(i);
            for (int[] n : neighbours) {
                assertNotEquals(TokamakGeometry.Cell.OUTSIDE, geometry.classify(x + n[0], y + n[1], z + n[2]),
                        "plasma cell " + x + "," + y + "," + z + " leaks at R=" + radius);
            }
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {4, 8, 16})
    void volumesGrowLinearlyWithRadius(int radius) {
        TokamakGeometry geometry = TokamakGeometry.of(radius);
        double expectedPlasma = 2 * Math.PI * radius * 3 * 3;
        assertEquals(expectedPlasma, geometry.plasmaCount(), expectedPlasma * 0.12);
        assertTrue(geometry.shellCount() > geometry.plasmaCount(), "shell must outnumber plasma cells");
    }

    @ParameterizedTest
    @ValueSource(ints = {4, 9, 16})
    void everySectorHasShell(int radius) {
        TokamakGeometry geometry = TokamakGeometry.of(radius);
        int[] perSector = new int[geometry.sectorCount()];
        for (int i = 0; i < geometry.shellCount(); i++) {
            perSector[geometry.shellSector(i)]++;
        }
        for (int sector = 0; sector < perSector.length; sector++) {
            assertTrue(perSector[sector] > 4, "sector " + sector + " too thin at R=" + radius + ": " + perSector[sector]);
        }
    }

    @Test
    void controllerSitsOnTheOuterEquatorOfTheShell() {
        for (int radius = 4; radius <= 16; radius++) {
            TokamakGeometry geometry = TokamakGeometry.of(radius);
            int d = TokamakGeometry.controllerToAxis(radius);
            assertEquals(TokamakGeometry.Cell.SHELL, geometry.classify(d, 0, 0));
            assertEquals(TokamakGeometry.Cell.PLASMA, geometry.classify(d - 1, 0, 0));
            assertEquals(TokamakGeometry.Cell.PLASMA, geometry.classify(d - 3, 0, 0));
            assertEquals(TokamakGeometry.Cell.SHELL, geometry.classify(d - 4, 0, 0));
            assertEquals(TokamakGeometry.Cell.OUTSIDE, geometry.classify(d - 5, 0, 0));
        }
    }

    @Test
    void rejectsTinyRadii() {
        assertThrows(IllegalArgumentException.class, () -> TokamakGeometry.of(3));
    }
}
