package org.papiricoh.create_nuclearindustry.accelerator.physics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Activation builds up under lost beam, decays with its half-life and leaves waste. */
class ActivationModelTest {
    private static final ActivationModel.Params P = ActivationModel.Params.DEFAULT;

    @Test
    void activityTendsToItsSaturation() {
        double a = 0;
        for (int i = 0; i < 20 * P.halfLifeTicks(); i++) {
            a = ActivationModel.tick(P, a, 10);
        }
        assertEquals(ActivationModel.saturation(P, 10), a, 1e-3);
        assertEquals(5.0, ActivationModel.saturation(P, 10), 1e-9);
    }

    @Test
    void afterOneHalfLifeOfBeamItIsHalfwayThere() {
        double a = 0;
        for (int i = 0; i < P.halfLifeTicks(); i++) {
            a = ActivationModel.tick(P, a, 10);
        }
        assertEquals(2.5, a, 1e-6);
    }

    @Test
    void withoutBeamItDecaysWithItsHalfLife() {
        assertEquals(4.0, ActivationModel.decay(P, 8.0, (long) P.halfLifeTicks()), 1e-9);
        assertEquals(1.0, ActivationModel.decay(P, 8.0, (long) (3 * P.halfLifeTicks())), 1e-9);
        double a = 8.0;
        for (int i = 0; i < P.halfLifeTicks(); i++) {
            a = ActivationModel.tick(P, a, 0);
        }
        assertEquals(4.0, a, 1e-6, "ticking without beam decays the same");
    }

    @Test
    void thePromptDoseOnlyExistsWithBeam() {
        assertEquals(20.0, ActivationModel.prompt(P, 10), 1e-9);
        assertEquals(3.0, ActivationModel.strength(P, 3.0, 0), 1e-9, "after shutdown only the activation remains");
        assertEquals(P.maxStrength(), ActivationModel.strength(P, 0, 1e6), 1e-9, "capped");
    }

    @Test
    void dismantlingLeavesWasteInBands() {
        assertTrue(ActivationModel.wasteFor(P, 0.5).isEmpty());
        assertEquals(new ActivationModel.Waste(1, 0), ActivationModel.wasteFor(P, 1.0));
        assertEquals(new ActivationModel.Waste(2, 0), ActivationModel.wasteFor(P, 7.0));
        assertEquals(new ActivationModel.Waste(4, 1), ActivationModel.wasteFor(P, 50.0));
    }
}
