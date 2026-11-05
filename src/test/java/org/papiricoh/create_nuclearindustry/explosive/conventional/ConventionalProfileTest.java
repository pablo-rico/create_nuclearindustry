package org.papiricoh.create_nuclearindustry.explosive.conventional;

import org.junit.jupiter.api.Test;
import org.papiricoh.create_nuclearindustry.civildefense.blast.BlastWaveModel;
import org.papiricoh.create_nuclearindustry.explosive.blast.DetonationProfile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** How far a conventional blast's crater, wave, fire and fragments reach. */
class ConventionalProfileTest {
    private static final ConventionalProfile.Settings S = ConventionalProfile.Settings.DEFAULT;

    private static ConventionalProfile he(double charge) {
        return ConventionalProfile.of(ConventionalKind.HIGH_EXPLOSIVE, charge, 0, S);
    }

    @Test
    void aFullBayReachesTheConfiguredMaximum() {
        ConventionalProfile p = he(192);
        assertEquals(20, p.craterRadius(), 0.5);
        assertEquals(64, p.waveRadius(), 0.5);
        assertFalse(p.airburst());
        assertTrue(p.fireballRadius() < p.craterRadius());
        assertTrue(p.severeRadius() >= p.craterRadius() && p.moderateRadius() >= p.severeRadius());
        assertTrue(p.moderateRadius() < p.waveRadius(), "the wave reaches past what it wrecks");
    }

    @Test
    void itsHarmStopsWhereTheWaveSays() {
        ConventionalProfile p = he(192);
        double kPa = BlastWaveModel.overpressure(ConventionalProfile.HARM_REACH * p.waveRadius(), p.yieldKg());
        assertEquals(S.damageThresholdKPa(), kPa, 0.05);
    }

    @Test
    void everythingGrowsWithTheCubeRootOfTheCharge() {
        assertEquals(2 * he(24).waveRadius(), he(192).waveRadius(), 1e-6, "eight times the charge, twice the reach");
        double last = 0;
        for (int c = 1; c <= 192; c++) {
            ConventionalProfile p = he(c);
            assertTrue(p.waveRadius() > last);
            last = p.waveRadius();
        }
        assertEquals(0, he(0).waveRadius());
        assertEquals(0, he(0).fragments());
    }

    @Test
    void aThermobaricChargePushesHarderAndDigsLess() {
        ConventionalProfile h = he(192);
        ConventionalProfile t = ConventionalProfile.of(ConventionalKind.THERMOBARIC, 192, 0, S);
        assertTrue(t.waveRadius() > h.waveRadius());
        assertTrue(t.yieldKg() > h.yieldKg());
        assertTrue(t.fireballRadius() > h.fireballRadius());
        assertTrue(t.fireRadius() > 3 * h.fireRadius(), "it sets far more alight");
        assertTrue(ConventionalProfile.of(ConventionalKind.THERMOBARIC, 24, 0, S).fragments() < he(24).fragments() / 4,
                "its thin case throws few fragments");
        assertTrue(t.shelterReach() > 0 && h.shelterReach() == 0);
    }

    @Test
    void anAirburstTradesItsCraterForReach() {
        ConventionalProfile ground = he(96);
        double height = ConventionalProfile.airburstHeight(ConventionalKind.HIGH_EXPLOSIVE, 96, S, 1.5);
        ConventionalProfile air = ConventionalProfile.of(ConventionalKind.HIGH_EXPLOSIVE, 96, height, S);
        assertTrue(air.airburst());
        assertEquals(0, air.craterRadius());
        assertTrue(air.waveRadius() > ground.waveRadius());
    }

    @Test
    void bombletsThrowMoreFragmentsForTheirCharge() {
        ConventionalProfile b = ConventionalProfile.of(ConventionalKind.BOMBLET, 1.5, 0, S);
        assertTrue(b.fragments() > he(1.5).fragments());
        assertTrue(b.waveRadius() > 0 && b.waveRadius() < 16, "a bomblet is small: " + b.waveRadius());
    }

    @Test
    void itDigsAsANuclearProfileWithoutGlassOrFallout() {
        DetonationProfile d = he(192).toDetonationProfile(100);
        assertEquals(20, d.craterRadius(), 0.5);
        assertEquals(0, d.glassRadius());
        assertEquals(0, d.promptRadius());
        assertEquals(0, d.falloutIntensity());
        assertEquals(100, d.blastProof(1200));
        assertEquals(d, DetonationProfile.decode(d.encode()));
        org.papiricoh.create_nuclearindustry.explosive.blast.CraterShape shape = new org.papiricoh.create_nuclearindustry.explosive.blast.CraterShape(d);
        for (int x = -3; x <= 3; x++) {
            assertEquals(org.papiricoh.create_nuclearindustry.explosive.blast.CraterShape.Floor.NONE, shape.floor(x, 0, x, 0),
                    "no fused floor, not even at the centre");
        }
    }
}
