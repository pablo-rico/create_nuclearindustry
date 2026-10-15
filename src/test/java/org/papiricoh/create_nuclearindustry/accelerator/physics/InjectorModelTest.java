package org.papiricoh.create_nuclearindustry.accelerator.physics;

import org.junit.jupiter.api.Test;
import org.papiricoh.create_nuclearindustry.fusion.multiblock.TokamakGeometry;
import org.papiricoh.create_nuclearindustry.fusion.physics.PlasmaModel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** When the antiproton injector fires into a tokamak's plasma, and what a shot costs. */
class InjectorModelTest {
    private static final InjectorModel.Params P = InjectorModel.Params.DEFAULT;
    private static final InjectorModel.Plasma READY_PLASMA = new InjectorModel.Plasma(true, 0.9, 0.5, 400);
    private static final int FE = P.shotFe();

    private static InjectorModel.Verdict verdict(boolean armed, double stored, int fe, boolean field, int cooldown,
                                                 InjectorModel.Plasma plasma) {
        return InjectorModel.verdict(P, armed, stored, fe, field, cooldown, plasma);
    }

    @Test
    void firesIntoAWellHeldHeatingPlasma() {
        InjectorModel.Verdict v = verdict(true, 2, FE, true, 0, READY_PLASMA);
        assertSame(InjectorModel.Verdict.READY, v);
        assertTrue(v.fires());
        assertTrue(InjectorModel.plasmaReady(P, READY_PLASMA));
    }

    @Test
    void theOperatorsProblemsComeFirst() {
        assertSame(InjectorModel.Verdict.SAFE, verdict(false, 0, 0, false, 50, InjectorModel.Plasma.NONE));
        assertSame(InjectorModel.Verdict.COOLDOWN, verdict(true, 0, 0, false, 50, InjectorModel.Plasma.NONE));
        assertSame(InjectorModel.Verdict.NO_DOSE, verdict(true, 1.9, 0, false, 0, InjectorModel.Plasma.NONE));
        assertSame(InjectorModel.Verdict.NO_POWER, verdict(true, 2, FE - 1, true, 0, READY_PLASMA));
        assertSame(InjectorModel.Verdict.NO_POWER, verdict(true, 2, FE, false, 0, READY_PLASMA), "a chamber without field cannot fire");
        assertSame(InjectorModel.Verdict.WAITING, verdict(true, 2, FE, true, 0, InjectorModel.Plasma.NONE));
        assertSame(InjectorModel.Verdict.WAITING, verdict(true, 2, FE, true, 0, null));
    }

    @Test
    void itWaitsForAPlasmaThatTakesTheDose() {
        assertSame(InjectorModel.Verdict.IGNITED, verdict(true, 2, FE, true, 0,
                new InjectorModel.Plasma(true, 0.9, 0.5, PlasmaModel.IGNITION_TEMP)));
        assertSame(InjectorModel.Verdict.LOW_CONFINEMENT, verdict(true, 2, FE, true, 0,
                new InjectorModel.Plasma(true, PlasmaModel.STABLE_CONFINEMENT - 0.01, 0.5, 400)));
        assertSame(InjectorModel.Verdict.LOW_DENSITY, verdict(true, 2, FE, true, 0,
                new InjectorModel.Plasma(true, 0.9, PlasmaModel.ANTIPROTON_IGNITION_DENSITY - 0.001, 400)));
        assertSame(InjectorModel.Verdict.READY, verdict(true, 2, FE, true, 0,
                new InjectorModel.Plasma(true, PlasmaModel.STABLE_CONFINEMENT, PlasmaModel.ANTIPROTON_IGNITION_DENSITY, 400)),
                "the limits themselves are enough");
        assertTrue(InjectorModel.Verdict.LOW_DENSITY.armedAndLoaded());
        assertTrue(InjectorModel.Verdict.READY.armedAndLoaded());
        assertFalse(InjectorModel.Verdict.NO_POWER.armedAndLoaded());
    }

    @Test
    void armingFollowsTheSetting() {
        assertTrue(InjectorModel.armed(InjectorModel.Arming.AUTOMATIC, false));
        assertTrue(InjectorModel.armed(InjectorModel.Arming.REDSTONE, true));
        assertFalse(InjectorModel.armed(InjectorModel.Arming.REDSTONE, false));
        assertFalse(InjectorModel.armed(InjectorModel.Arming.SAFE, true));
        assertTrue(InjectorModel.armed(null, false));
        assertSame(InjectorModel.Arming.REDSTONE, InjectorModel.Arming.AUTOMATIC.next());
        assertSame(InjectorModel.Arming.AUTOMATIC, InjectorModel.Arming.SAFE.next());
        assertSame(InjectorModel.Arming.SAFE, InjectorModel.Arming.byName("safe"));
        assertSame(InjectorModel.Arming.AUTOMATIC, InjectorModel.Arming.byName("?"));
    }

    @Test
    void dosesAndSpending() {
        assertEquals(3, InjectorModel.doses(P, 6.5));
        assertEquals(1, InjectorModel.doses(P, 2.0));
        assertEquals(0, InjectorModel.doses(P, 1.99));
        assertEquals(2, InjectorModel.spend(P, 7), 1e-12);
        assertEquals(1.5, InjectorModel.spend(P, 1.5), 1e-12);
        assertEquals(3, InjectorModel.comparator(P, 6.5));
        assertEquals(15, InjectorModel.comparator(P, 1000));
        InjectorModel.Params free = P.withDose(0);
        assertTrue(InjectorModel.hasDose(free, 0), "a zero dose never runs out");
        assertEquals(Integer.MAX_VALUE, InjectorModel.doses(free, 0));
        assertEquals(0, InjectorModel.spend(free, 5), 1e-12);
        assertEquals(5, P.withDose(5).dose(), 1e-12);
        assertEquals(P.cooldownTicks(), P.withDose(5).cooldownTicks());
    }

    @Test
    void cooldownCountsDownToZero() {
        int left = InjectorModel.cooldownAfterShot(P);
        assertEquals(200, left);
        int ticks = 0;
        while (left > 0) {
            left = InjectorModel.cooldownTick(left);
            ticks++;
        }
        assertEquals(200, ticks);
        assertEquals(0, InjectorModel.cooldownTick(0));
    }

    @Test
    void theChamberHoldsAtLeastTwoDoses() {
        assertEquals(InjectorModel.CHAMBER.capacity(), InjectorModel.chamber(P).capacity(), 1e-12, "four 2 ng doses fit the default");
        assertEquals(40, InjectorModel.chamber(P.withDose(20)).capacity(), 1e-12);
        assertEquals(InjectorModel.CHAMBER.idleFe(), InjectorModel.chamber(P.withDose(20)).idleFe(), 1e-12);
        assertEquals(P.shotFe() + PenningTrapModel.bufferCapacity(InjectorModel.CHAMBER), InjectorModel.bufferCapacity(P, InjectorModel.CHAMBER));
        assertEquals(0.5, InjectorModel.pull(InjectorModel.CHAMBER, 7.5, 1), 1e-12);
        assertEquals(0.1, InjectorModel.pull(InjectorModel.CHAMBER, 0, 0.1), 1e-12);
    }

    @Test
    void aShotReleasesTheAnnihilationEnergy() {
        assertEquals(2 * AnnihilationBlastModel.energyKj(1), InjectorModel.shotEnergyKj(P), 1e-6);
        assertEquals(359.5, InjectorModel.shotEnergyKj(P), 0.1, "two nanograms meet two of matter: about 360 kJ");
    }

    @Test
    void readyMeansThePlasmaTakesTheDose() {
        PlasmaModel model = new PlasmaModel();
        model.breakdown(10);
        int radius = 6;
        int volume = TokamakGeometry.of(radius).plasmaCount();
        PlasmaModel.Params pp = PlasmaModel.Params.DEFAULT;
        double fuel = PlasmaModel.fuelRateFor(pp, volume, 0.7);
        PlasmaModel.Inputs in = new PlasmaModel.Inputs(volume, radius, 1.0, 1.0, 0.4, fuel / 2, fuel / 2, true,
                0, 0, 0, 1e9, 1e9, false);
        for (int tick = 0; tick < 20; tick++) {
            model.tick(pp, in);
        }
        InjectorModel.Plasma plasma = new InjectorModel.Plasma(true, model.getConfinement(), model.getDensity(), model.getTemperature());
        assertSame(InjectorModel.Verdict.READY, InjectorModel.verdict(P, true, 2, FE, true, 0, plasma));
        assertTrue(model.antiprotonIgnite(), "what the injector judges ready, the plasma takes");
        InjectorModel.Plasma after = new InjectorModel.Plasma(true, model.getConfinement(), model.getDensity(), model.getTemperature());
        assertSame(InjectorModel.Verdict.IGNITED, InjectorModel.verdict(P, true, 2, FE, true, 0, after), "it does not fire twice");
    }

    @Test
    void aLooselyHeldPlasmaIsNotFired() {
        PlasmaModel model = new PlasmaModel();
        model.breakdown(10);
        int radius = 6;
        int volume = TokamakGeometry.of(radius).plasmaCount();
        PlasmaModel.Params pp = PlasmaModel.Params.DEFAULT;
        double fuel = PlasmaModel.fuelRateFor(pp, volume, 0.7);
        PlasmaModel.Inputs in = new PlasmaModel.Inputs(volume, radius, 0.3, 1.0, 0.4, fuel / 2, fuel / 2, true,
                0, 0, 0, 1e9, 1e9, false);
        for (int tick = 0; tick < 20; tick++) {
            model.tick(pp, in);
        }
        InjectorModel.Plasma plasma = new InjectorModel.Plasma(true, model.getConfinement(), model.getDensity(), model.getTemperature());
        assertSame(InjectorModel.Verdict.LOW_CONFINEMENT, InjectorModel.verdict(P, true, 2, FE, true, 0, plasma));
        assertFalse(model.antiprotonIgnite(), "and the plasma would indeed waste it");
    }

    @Test
    void verdictsRoundTripByOrdinal() {
        for (InjectorModel.Verdict v : InjectorModel.Verdict.values()) {
            assertSame(v, InjectorModel.Verdict.byOrdinal(v.ordinal()));
        }
        assertSame(InjectorModel.Verdict.WAITING, InjectorModel.Verdict.byOrdinal(-1));
        assertEquals("low_confinement", InjectorModel.Verdict.LOW_CONFINEMENT.id());
    }
}
