package org.papiricoh.create_nuclearindustry.fusion.heating;

import org.junit.jupiter.api.Test;
import org.papiricoh.create_nuclearindustry.fusion.api.TokamakPhase;
import org.papiricoh.create_nuclearindustry.fusion.api.TokamakReadings;
import org.papiricoh.create_nuclearindustry.fusion.api.TripCause;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeatingDriveTest {
    private static final HeatingDrive.Params NBI = new HeatingDrive.Params(20, 128, 32);
    private static final HeatingDrive.Params RF = new HeatingDrive.Params(12, 64, 16);

    @Test
    void ratedPowerAtAndAboveRatedSpeed() {
        assertEquals(20, HeatingDrive.power(128, NBI), 1e-9);
        assertEquals(20, HeatingDrive.power(256, NBI), 1e-9);
        assertEquals(12, HeatingDrive.power(64, RF), 1e-9);
        assertEquals(12, HeatingDrive.power(-200, RF), 1e-9, "either direction");
    }

    @Test
    void nothingAtOrBelowMinimumSpeed() {
        assertEquals(0, HeatingDrive.power(0, NBI));
        assertEquals(0, HeatingDrive.power(32, NBI));
        assertEquals(0, HeatingDrive.power(16, RF));
    }

    @Test
    void linearInBetween() {
        assertEquals(10, HeatingDrive.power(80, NBI), 1e-9);
        assertEquals(6, HeatingDrive.power(40, RF), 1e-9);
        assertEquals(0.5, HeatingDrive.load(80, NBI), 1e-9);
        double previous = 0;
        for (int rpm = 0; rpm <= 256; rpm += 8) {
            double power = HeatingDrive.power(rpm, NBI);
            assertTrue(power >= previous, "power never falls with speed");
            previous = power;
        }
    }

    @Test
    void beamsDrawDeuteriumPerMegawatt() {
        assertEquals(0.4, HeatingDrive.beamDeuterium(20, 0.02), 1e-12);
        assertEquals(0, HeatingDrive.beamDeuterium(-5, 0.02));
        assertEquals(0, HeatingDrive.beamDeuterium(20, -1));
    }

    /** The shared balance: about 1600 SU/RPM per MW of beam at rated speed. */
    @Test
    void defaultBeamCostMatchesTheBalance() {
        double perMw = PlasmaHeatingConfig.neutralBeamStress / PlasmaHeatingConfig.neutralBeamPower;
        assertTrue(perMw > 1500 && perMw < 1700, "got " + perMw);
        double rfPerMw = PlasmaHeatingConfig.radioFrequencyStress / PlasmaHeatingConfig.radioFrequencyPower;
        assertTrue(rfPerMw > 1000 && rfPerMw < 1700, "got " + rfPerMw);
    }

    @Test
    void antennaNeedsDensityToCouple() {
        assertEquals(0, HeatingDrive.rfCoupling(0));
        assertEquals(0, HeatingDrive.rfCoupling(HeatingDrive.RF_MIN_COUPLING_DENSITY));
        assertEquals(1, HeatingDrive.rfCoupling(HeatingDrive.RF_FULL_COUPLING_DENSITY));
        assertEquals(1, HeatingDrive.rfCoupling(1.1));
        double middle = HeatingDrive.rfCoupling((HeatingDrive.RF_MIN_COUPLING_DENSITY + HeatingDrive.RF_FULL_COUPLING_DENSITY) / 2);
        assertEquals(0.5, middle, 1e-9);
    }

    @Test
    void ratedSpeedMustBeAboveMinimum() {
        assertThrows(IllegalArgumentException.class, () -> new HeatingDrive.Params(10, 32, 32));
        assertThrows(IllegalArgumentException.class, () -> new HeatingDrive.Params(10, 64, -1));
    }

    // ------------------------------------------------------------------ viewport glow

    private static TokamakReadings readings(boolean formed, TokamakPhase phase, double temperature) {
        TokamakReadings none = TokamakReadings.NONE;
        return new TokamakReadings(formed, false, formed ? 4 : 0, phase, TripCause.NONE, temperature,
                none.instability(), none.confinement(), 0.5, 0, 0, 0, 0, 0, 0, false, 1, 1, 4.5, -4,
                0, 0.5, true, 1, 0, 0, 0, 0, 0, 0, false);
    }

    @Test
    void viewportIsDarkWithoutPlasma() {
        assertEquals(0, ViewportGlow.level(TokamakReadings.NONE));
        assertEquals(0, ViewportGlow.level(null));
        for (TokamakPhase phase : TokamakPhase.values()) {
            if (!phase.hasPlasma()) {
                assertEquals(0, ViewportGlow.level(readings(true, phase, 3000)), "dark in " + phase);
            }
        }
        assertEquals(0, ViewportGlow.level(readings(false, TokamakPhase.BURN, 3000)), "dark when not formed");
    }

    @Test
    void viewportGlowsDimAtBreakdownAndBrighterWithHeat() {
        int breakdown = ViewportGlow.level(readings(true, TokamakPhase.BREAKDOWN, 50));
        int cold = ViewportGlow.level(readings(true, TokamakPhase.HEATING, 300));
        int warm = ViewportGlow.level(readings(true, TokamakPhase.HEATING, 2000));
        int burning = ViewportGlow.level(readings(true, TokamakPhase.BURN, 4500));
        assertEquals(ViewportGlow.BREAKDOWN, breakdown);
        assertTrue(breakdown > 0 && breakdown < cold, "breakdown is a dim glow");
        assertTrue(cold < warm && warm < burning, "brighter as it heats");
        assertEquals(ViewportGlow.MAX, burning);
    }
}
