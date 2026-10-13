package org.papiricoh.create_nuclearindustry.waste.repository;

import org.junit.jupiter.api.Test;
import org.papiricoh.create_nuclearindustry.waste.WasteForm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RepositoryQualityTest {
    private static final RepositoryQuality.Weights W = RepositoryQuality.Weights.DEFAULT;
    private static final int SPAN = 64;
    private static final double MIN = 0.8;

    private static RepositoryQuality.Breakdown assess(int depth, double host, int faces, boolean fluid, WasteForm form) {
        return RepositoryQuality.assess(new RepositoryQuality.Site(depth, host, faces, fluid), form, W, SPAN);
    }

    @Test
    void defaultWeightsAddUpToOne() {
        assertEquals(1.0, W.depth() + W.hostRock() + W.backfill() + W.form(), 1e-9);
    }

    @Test
    void perfectRepositoryScoresOne() {
        RepositoryQuality.Breakdown b = assess(200, 1.0, 6, false, WasteForm.VITRIFIED);
        assertEquals(1.0, b.score(), 1e-9);
        assertEquals(0.35, b.depth(), 1e-9);
        assertEquals(0.25, b.hostRock(), 1e-9);
        assertEquals(0.25, b.backfill(), 1e-9);
        assertEquals(0.15, b.form(), 1e-9);
        assertTrue(RepositoryQuality.isStored(b.score(), MIN));
    }

    @Test
    void depthGrowsLinearlyThenSaturates() {
        assertEquals(0.0, RepositoryQuality.depthFactor(0, SPAN), 1e-9);
        assertEquals(0.5, RepositoryQuality.depthFactor(32, SPAN), 1e-9);
        assertEquals(1.0, RepositoryQuality.depthFactor(64, SPAN), 1e-9);
        assertEquals(1.0, RepositoryQuality.depthFactor(500, SPAN), 1e-9);
        assertEquals(0.0, RepositoryQuality.depthFactor(-3, SPAN), 1e-9);
    }

    @Test
    void formsRankRawCompactedCementedVitrified() {
        double raw = assess(0, 0, 0, false, WasteForm.RAW).score();
        double compacted = assess(0, 0, 0, false, WasteForm.COMPACTED).score();
        double cemented = assess(0, 0, 0, false, WasteForm.CEMENTED).score();
        double vitrified = assess(0, 0, 0, false, WasteForm.VITRIFIED).score();
        assertEquals(0.045, raw, 1e-9);
        assertEquals(compacted, cemented, 1e-9);
        assertTrue(raw < compacted && cemented < vitrified);
    }

    @Test
    void fluidNearbyTakesThePenaltyAndFloorsAtZero() {
        RepositoryQuality.Breakdown wet = assess(200, 1.0, 6, true, WasteForm.VITRIFIED);
        assertEquals(0.6, wet.score(), 1e-9);
        assertEquals(0.4, wet.fluidPenalty(), 1e-9);
        assertFalse(RepositoryQuality.isStored(wet.score(), MIN));
        assertEquals(0.0, assess(0, 0, 0, true, WasteForm.RAW).score(), 1e-9);
    }

    @Test
    void deepRockWithoutBackfillIsNotEnoughButTwoFacesAre() {
        assertFalse(RepositoryQuality.isStored(assess(100, 1.0, 0, false, WasteForm.VITRIFIED).score(), MIN));
        assertTrue(RepositoryQuality.isStored(assess(100, 1.0, 2, false, WasteForm.VITRIFIED).score(), MIN));
    }

    @Test
    void aWellBuriedRawBarrelCanStillBeStored() {
        // 0.35 + 0.25 + 0.25 + 0.045: the old barrels are fine in a proper repository.
        assertTrue(RepositoryQuality.isStored(assess(100, 1.0, 6, false, WasteForm.RAW).score(), MIN));
        // ...but not merely below Y=0 in open rock.
        assertFalse(RepositoryQuality.isStored(assess(100, 0.5, 1, false, WasteForm.RAW).score(), MIN));
    }

    @Test
    void outOfRangeInputsAreClamped() {
        RepositoryQuality.Breakdown b = assess(200, 3.0, 12, false, WasteForm.VITRIFIED);
        assertEquals(1.0, b.score(), 1e-9);
        assertEquals(0.25, b.backfill(), 1e-9);
    }

    @Test
    void leakAndEmissionShrinkWithTheScoreAndStopWhenStored() {
        assertEquals(0.02, RepositoryQuality.leak(0.02, 1.0, 0.0, MIN), 1e-12);
        assertEquals(0.01, RepositoryQuality.leak(0.02, 1.0, 0.5, MIN), 1e-12);
        assertEquals(0.005, RepositoryQuality.leak(0.02, 0.5, 0.5, MIN), 1e-12);
        assertEquals(0.0, RepositoryQuality.leak(0.02, 1.0, 0.8, MIN), 1e-12);
        assertEquals(3.0, RepositoryQuality.emission(8.0, 0.5, 0.25, MIN), 1e-12);
        assertEquals(0.0, RepositoryQuality.emission(8.0, 0.5, 0.9, MIN), 1e-12);
    }

    @Test
    void customWeightsAreHonoured() {
        RepositoryQuality.Weights depthOnly = new RepositoryQuality.Weights(1.0, 0, 0, 0, 0);
        RepositoryQuality.Breakdown b = RepositoryQuality.assess(new RepositoryQuality.Site(16, 1.0, 6, true),
                WasteForm.VITRIFIED, depthOnly, 32);
        assertEquals(0.5, b.score(), 1e-9);
    }

    @Test
    void formsRoundTripByName() {
        for (WasteForm form : WasteForm.values()) {
            assertEquals(form, WasteForm.byId(form.id()));
        }
        assertEquals(null, WasteForm.byId("glass"));
    }
}
