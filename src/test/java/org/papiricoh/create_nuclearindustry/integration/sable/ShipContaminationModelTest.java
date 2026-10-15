package org.papiricoh.create_nuclearindustry.integration.sable;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A ship taking up contamination from what it flies through. */
class ShipContaminationModelTest {
    @Test
    void aShipClosesPartOfTheGapEachCheck() {
        assertEquals(10f, ShipContaminationModel.uptake(0, 100, 0.1), 1.0e-5);
        assertEquals(19f, ShipContaminationModel.uptake(10, 100, 0.1), 1.0e-5);
    }

    @Test
    void itNeverGoesBeyondWhatSurroundsIt() {
        float level = 0;
        for (int i = 0; i < 1000; i++) {
            level = ShipContaminationModel.uptake(level, 50, 0.3);
        }
        assertTrue(level <= 50 && level > 49.9, "it approaches the air around it: " + level);
        assertEquals(100f, ShipContaminationModel.uptake(1.0f, 100, 2.0), 1.0e-5, "a share above one takes it all");
    }

    @Test
    void cleanAirOrADirtierShipChangesNothing() {
        assertEquals(30f, ShipContaminationModel.uptake(30, 10, 0.5));
        assertEquals(30f, ShipContaminationModel.uptake(30, 0, 0.5));
        assertEquals(5f, ShipContaminationModel.uptake(5, 100, 0));
    }

    @Test
    void theHigherItFliesTheLessReachesIt() {
        assertEquals(1.0, ShipContaminationModel.heightFactor(0), 1.0e-9);
        assertEquals(1.0, ShipContaminationModel.heightFactor(-5), 1.0e-9, "below the ground counts as on it");
        assertEquals(0.5, ShipContaminationModel.heightFactor(ShipContaminationModel.HALF_HEIGHT), 1.0e-9);
        assertTrue(ShipContaminationModel.heightFactor(200) < 0.05);
    }
}
