package org.papiricoh.create_nuclearindustry.fusion.physics;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.papiricoh.create_nuclearindustry.fusion.multiblock.TokamakGeometry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Long-run behaviour of the plasma: heating to a burn, Q, ignition, ash, the density limit and coolant. */
class PlasmaModelTest {
    private static final PlasmaModel.Params P = PlasmaModel.Params.DEFAULT;
    private static final int MINUTE = 1200;

    /** A plasma's operating conditions, fed the same each tick. */
    private record Run(int radius, double coverage, double density, double beams, double rf, double coolant,
                       double deuteriumShare, double blankets) {
        static Run of(int radius) {
            return new Run(radius, 1.0, 0.7, 40, 0, 1e9, 0.5, 0.4);
        }

        Run beams(double mw) {
            return new Run(radius, coverage, density, mw, rf, coolant, deuteriumShare, blankets);
        }

        Run rf(double mw) {
            return new Run(radius, coverage, density, beams, mw, coolant, deuteriumShare, blankets);
        }

        Run density(double fraction) {
            return new Run(radius, coverage, fraction, beams, rf, coolant, deuteriumShare, blankets);
        }

        Run coverage(double share) {
            return new Run(radius, share, density, beams, rf, coolant, deuteriumShare, blankets);
        }

        Run dry() {
            return new Run(radius, coverage, density, beams, rf, 0, deuteriumShare, blankets);
        }

        Run deuteriumOnly() {
            return new Run(radius, coverage, density, beams, rf, coolant, 1.0, blankets);
        }

        int volume() {
            return TokamakGeometry.of(radius).plasmaCount();
        }

        PlasmaModel.Inputs inputs() {
            double fuel = PlasmaModel.fuelRateFor(P, volume(), density);
            return new PlasmaModel.Inputs(volume(), radius, coverage, 1.0, blankets, fuel * deuteriumShare,
                    fuel * (1 - deuteriumShare), true, beams, beams * 0.02, rf, coolant, 1e9, false);
        }
    }

    private static PlasmaModel start() {
        PlasmaModel model = new PlasmaModel();
        model.breakdown(10);
        return model;
    }

    private static PlasmaModel.Outputs run(PlasmaModel model, Run run, int ticks) {
        PlasmaModel.Outputs out = PlasmaModel.Outputs.NONE;
        for (int tick = 0; tick < ticks; tick++) {
            out = model.tick(P, run.inputs());
        }
        return out;
    }

    @Test
    void reactivityPeaksNearFourThousandDegrees() {
        assertEquals(1.0, PlasmaModel.reactivity(4000), 1e-9);
        assertTrue(PlasmaModel.reactivity(3000) < 1 && PlasmaModel.reactivity(5000) < 1);
        assertTrue(PlasmaModel.reactivity(1000) < 0.2, "a cool plasma barely fuses");
        assertEquals(0, PlasmaModel.reactivity(PlasmaModel.AMBIENT_TEMP));
    }

    @Test
    void ohmicHeatingAloneNeverReachesBurn() {
        PlasmaModel model = start();
        Run run = Run.of(4).beams(0);
        for (int tick = 0; tick < 3 * MINUTE; tick++) {
            model.tick(P, run.inputs());
            assertTrue(model.getTemperature() < PlasmaModel.IGNITION_TEMP, "the solenoid alone must not light a burn (tick " + tick + ")");
        }
        assertTrue(model.getOhmicPower() == 0, "the solenoid's volt-seconds run out");
    }

    @Test
    void twoNeutralBeamsBringAnR4ToBurnWithQBetweenThreeAndEight() {
        PlasmaModel model = start();
        Run run = Run.of(4);
        int burnTick = -1;
        for (int tick = 0; tick < 3 * MINUTE; tick++) {
            model.tick(P, run.inputs());
            assertFalse(model.isDisrupting(), "a cooled, heated plasma must not disrupt");
            if (burnTick < 0 && model.getTemperature() >= PlasmaModel.IGNITION_TEMP) {
                burnTick = tick;
            }
        }
        assertTrue(burnTick >= 0 && burnTick < 20 * 20, "should reach burning temperature within 20 s, took " + burnTick);
        assertEquals(PlasmaModel.Mode.DT, model.getMode());
        assertTrue(model.getTemperature() < PlasmaModel.DANGER_TEMP, "settled at " + model.getTemperature());
        assertTrue(model.gain() >= 3 && model.gain() <= 8, "sub-ignited Q should be 3..8, got " + model.gain());
    }

    @Test
    void oneBeamStillBurnsWithHigherQ() {
        PlasmaModel two = start();
        run(two, Run.of(4), 3 * MINUTE);
        PlasmaModel one = start();
        run(one, Run.of(4).beams(20), 3 * MINUTE);
        assertTrue(one.getTemperature() >= PlasmaModel.IGNITION_TEMP, "one beam reaches burn too, at " + one.getTemperature());
        assertTrue(one.gain() > two.gain(), "less heating for nearly the same fusion means a higher Q");
    }

    @Test
    void lotsOfHeatingIsCutBackBeforeTheDangerTemperature() {
        PlasmaModel model = start();
        run(model, Run.of(4).beams(160), 3 * MINUTE);
        assertFalse(model.isDisrupting(), "the heating cut-back keeps an over-heated plasma below danger");
        assertTrue(model.getTemperature() < PlasmaModel.DANGER_TEMP, "settled at " + model.getTemperature());
    }

    @Test
    void anR4IgnitesWithEnoughDensityAndKeepsBurningWithHeatingOff() {
        PlasmaModel model = start();
        Run dense = Run.of(4).density(0.9);
        run(model, dense, 2 * MINUTE);
        run(model, dense.beams(0), 5 * MINUTE);
        assertTrue(model.getTemperature() >= PlasmaModel.IGNITION_TEMP, "an ignited plasma burns on its own, at " + model.getTemperature());
        assertTrue(Double.isInfinite(model.gain()), "with no heating at all Q is infinite, got " + model.gain());
        assertFalse(model.isDisrupting());
    }

    @Test
    void aSubIgnitedPlasmaGoesOutWithoutHeating() {
        PlasmaModel model = start();
        run(model, Run.of(4), 2 * MINUTE);
        run(model, Run.of(4).beams(0), 3 * MINUTE);
        assertTrue(model.getTemperature() < PlasmaModel.IGNITION_TEMP, "at the default density the plasma needs its heating");
    }

    @Test
    void radioFrequencyNeedsDensityToCouple() {
        assertEquals(0, PlasmaModel.rfCoupling(P, 0.1));
        assertEquals(0, PlasmaModel.rfCoupling(P, P.rfMinDensity()));
        assertEquals(1, PlasmaModel.rfCoupling(P, 0.4));
        double half = PlasmaModel.rfCoupling(P, (P.rfMinDensity() + P.rfFullDensity()) / 2);
        assertEquals(0.5, half, 1e-9);

        // An antenna alone on a thin plasma barely heats it; on a dense one it does.
        PlasmaModel thin = start();
        run(thin, Run.of(4).beams(0).rf(24).density(0.1), MINUTE);
        PlasmaModel dense = start();
        run(dense, Run.of(4).beams(0).rf(24).density(0.6), MINUTE);
        assertTrue(dense.getHeatingPower() > 20 && thin.getHeatingPower() < 1,
                "RF absorbed: thin " + thin.getHeatingPower() + " MW, dense " + dense.getHeatingPower() + " MW");
    }

    @Test
    void ashSettlesAtAnEquilibriumAndLeavesAsHelium() {
        PlasmaModel model = start();
        Run run = Run.of(4);
        run(model, run, 3 * MINUTE);
        double ash = model.ashFraction();
        PlasmaModel.Outputs out = run(model, run, MINUTE);
        assertEquals(ash, model.ashFraction(), 0.005, "the ash share should have settled");
        assertTrue(ash > 0.02 && ash < 0.3, "some ash dilutes the fuel, got " + ash);
        assertEquals(out.deuteriumBurnt(), out.heliumRemoved(), 0.02 * out.deuteriumBurnt() + 1e-6,
                "at equilibrium as much helium is pumped out as the burn makes");
    }

    @Test
    void tooMuchFuelHitsTheDensityLimitAndDisrupts() {
        PlasmaModel model = start();
        Run run = Run.of(4).density(1.5);
        boolean disrupted = false;
        for (int tick = 0; tick < 3 * MINUTE && !disrupted; tick++) {
            model.tick(P, run.inputs());
            disrupted = model.isDisrupting();
        }
        assertTrue(disrupted, "a plasma pushed past the density limit must disrupt");
    }

    @Test
    void dryTokamakOverheatsAndDisrupts() {
        PlasmaModel model = start();
        Run run = Run.of(4).dry();
        boolean disrupted = false;
        for (int tick = 0; tick < 3 * MINUTE && !disrupted; tick++) {
            model.tick(P, run.inputs());
            disrupted = model.isDisrupting();
        }
        assertTrue(disrupted, "without coolant the plasma must overheat and disrupt");
    }

    @Test
    void onePairOfPortsCarriesADefaultR4() {
        // The controller lets a coolant and steam port pair take 400 mB/t of plasma steam.
        Run run = Run.of(4);
        PlasmaModel model = start();
        for (int tick = 0; tick < 5 * MINUTE; tick++) {
            PlasmaModel.Inputs in = run.inputs();
            model.tick(P, new PlasmaModel.Inputs(in.volume(), in.majorRadius(), in.coilCoverage(), in.field(),
                    in.blanketFraction(), in.deuteriumFeed(), in.tritiumFeed(), in.ohmic(), in.beamPower(),
                    in.beamDeuterium(), in.rfPower(), in.coolant(), 400, false));
            assertFalse(model.isDisrupting(), "one port pair must carry an R=4 at its defaults (tick " + tick + ")");
        }
        assertTrue(model.getTemperature() < PlasmaModel.DANGER_TEMP, "settled at " + model.getTemperature());
    }

    @Test
    void deuteriumOnlyRunsDdAndBreedsTritium() {
        PlasmaModel model = start();
        Run run = Run.of(4).deuteriumOnly();
        double bred = 0;
        for (int tick = 0; tick < 10 * MINUTE; tick++) {
            bred += model.tick(P, run.inputs()).tritiumBred();
        }
        assertEquals(PlasmaModel.Mode.DD, model.getMode());
        assertTrue(bred > 100, "ten minutes of D-D should breed over 100 mB of tritium, got " + bred);
    }

    @Test
    void breedingNeedsBlankets() {
        Run bare = new Run(4, 1.0, 0.7, 40, 0, 1e9, 0.5, 0.0);
        Run blanketed = Run.of(4);
        double bredBare = 0;
        double bredBlanketed = 0;
        PlasmaModel a = start();
        PlasmaModel b = start();
        for (int tick = 0; tick < 3 * MINUTE; tick++) {
            bredBare += a.tick(P, bare.inputs()).tritiumBred();
            bredBlanketed += b.tick(P, blanketed.inputs()).tritiumBred();
        }
        // Only the few D-D reactions while the first tritium arrives make any directly.
        assertTrue(bredBare < 1, "D-T without blankets breeds almost nothing, got " + bredBare);
        assertTrue(bredBlanketed > 100, "blankets breed, got " + bredBlanketed);
    }

    @ParameterizedTest
    @ValueSource(ints = {4, 8, 16})
    void steamScalesWithSizeAndBurningIsStable(int radius) {
        PlasmaModel model = start();
        Run run = Run.of(radius).beams(40 * Math.ceil(radius / 4.0));
        PlasmaModel.Outputs out = PlasmaModel.Outputs.NONE;
        for (int tick = 0; tick < 5 * MINUTE; tick++) {
            out = model.tick(P, run.inputs());
            assertFalse(model.isDisrupting(), "R=" + radius + " must not disrupt (tick " + tick + ")");
        }
        assertTrue(model.getTemperature() >= PlasmaModel.IGNITION_TEMP && model.getTemperature() < PlasmaModel.DANGER_TEMP,
                "R=" + radius + " settled at " + model.getTemperature());
        double steamPerCell = out.steamProduced() / run.volume();
        assertTrue(steamPerCell > 0.7 && steamPerCell < 2.5, "steam per plasma cell " + steamPerCell);
    }

    @Test
    void terminationPumpsThePlasmaOutQuickly() {
        PlasmaModel model = start();
        run(model, Run.of(4), MINUTE);
        assertTrue(model.hasPlasma());
        Run run = Run.of(4);
        PlasmaModel.Inputs off = new PlasmaModel.Inputs(run.volume(), 4, 1.0, 1.0, 0.4, 0, 0, false, 0, 0, 0, 1e9, 1e9, true);
        double recycled = 0;
        double inventory = model.getDeuterium() + model.getTritium();
        int ticks = 0;
        while (model.hasPlasma() && ticks < 400) {
            PlasmaModel.Outputs out = model.tick(P, off);
            recycled += out.deuteriumRecycled() + out.tritiumRecycled();
            assertFalse(model.isDisrupting(), "a ramp-down is safe");
            ticks++;
        }
        assertFalse(model.hasPlasma(), "the plasma should be pumped out");
        assertTrue(ticks < 200, "within ten seconds, took " + ticks);
        assertTrue(recycled >= inventory * 0.8, "its fuel goes back to the tanks: " + recycled + " of " + inventory);
    }

    @Test
    void theStructureWarningHasNoCoverageGap() {
        // For every coverage the outlook either warns, or the plasma really burns, stays stable and
        // keeps burning with the reference heating: no coverage forms quietly and then never burns.
        Run base = Run.of(4);
        boolean seenBurn = false;
        for (int percent = 0; percent <= 100; percent++) {
            double coverage = percent / 100.0;
            PlasmaModel.Outlook outlook = PlasmaModel.outlook(P, base.volume(), 4, coverage, 40, 0.7);
            if (outlook != PlasmaModel.Outlook.CAN_BURN) {
                assertFalse(seenBurn, "more coils must never make it worse (" + percent + "%)");
                continue;
            }
            seenBurn = true;
            PlasmaModel model = start();
            Run run = base.coverage(coverage);
            for (int tick = 0; tick < 3 * MINUTE; tick++) {
                model.tick(P, run.inputs());
                assertFalse(model.isDisrupting(), "coverage " + percent + "% was not warned about but disrupts");
            }
            assertTrue(model.getTemperature() >= PlasmaModel.IGNITION_TEMP,
                    "coverage " + percent + "% was not warned about but does not burn: " + model.getTemperature());
        }
        assertTrue(seenBurn, "full coverage must burn");
        assertEquals(PlasmaModel.Outlook.UNSTABLE, PlasmaModel.outlook(P, base.volume(), 4, 0.5, 40, 0.7));
    }

    @Test
    void solenoidRechargesWhileOffline() {
        PlasmaModel model = start();
        run(model, Run.of(4), MINUTE);
        assertTrue(model.getSolenoidFlux() == 0, "a full pulse uses the budget up");
        model.quench();
        for (int tick = 0; tick < P.solenoidRechargeTicks(); tick++) {
            model.rechargeSolenoid(P);
        }
        assertEquals(1.0, model.getSolenoidFlux(), 1e-9);
    }

    @Test
    void anAntiprotonDoseIgnitesAWellHeldPlasma() {
        PlasmaModel model = start();
        run(model, Run.of(6).beams(0), 20);
        assertTrue(model.getTemperature() < PlasmaModel.IGNITION_TEMP, "unheated, it stays cold");
        assertTrue(model.antiprotonIgnite(), "a confined plasma of fair density takes the dose");
        assertTrue(model.getTemperature() >= PlasmaModel.IGNITION_TEMP);
    }

    @Test
    void anAntiprotonDoseIsWastedOnAPoorlyHeldPlasma() {
        PlasmaModel model = start();
        run(model, Run.of(6).beams(0).coverage(0.3), 20);
        double before = model.getTemperature();
        assertFalse(model.antiprotonIgnite(), "too little confinement");
        assertEquals(before, model.getTemperature(), 1e-9);
        assertFalse(new PlasmaModel().antiprotonIgnite(), "no plasma at all");
    }
}
