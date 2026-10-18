package org.papiricoh.create_nuclearindustry.missile.physics;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Staging, guidance to burnout and the free fall after it. */
class MissileFlightTest {
    private static final BallisticModel.Params P = BallisticModel.Params.DEFAULT;
    private static final MissileDesign PAD = MissileDesign.LEGACY_PAD;

    private static MissileFlight.Prediction fly(MissileDesign design, V3 start, V3 aim) {
        MissileFlight flight = new MissileFlight(design, start, aim, V3.ZERO);
        MissileFlight.Prediction predicted = flight.predict(P);
        int ticks = 0;
        while (!flight.impacted() && ticks < 100_000) {
            flight.step(P);
            ticks++;
        }
        assertEquals(Math.ceil(predicted.ticks()), ticks, 1.0, "the prediction matches the flight");
        return new MissileFlight.Prediction(ticks, flight.position(), predicted.apogee(), flight.guidedCutoff());
    }

    @Test
    void theRocketEquationStageByStage() {
        MissileDesign one = new MissileDesign(List.of(new MissileDesign.Stage(1, 8, 0.12, 3)), 1, 0);
        assertEquals(3 * Math.log(10.0 / 2.0), one.deltaV(), 1.0e-9);
        // The same propellant split over two stages drops the first one's dead weight on the way.
        MissileDesign two = new MissileDesign(List.of(new MissileDesign.Stage(0.5, 4, 0.12, 3),
                new MissileDesign.Stage(0.5, 4, 0.06, 3)), 1, 0);
        assertEquals(one.launchMass(), two.launchMass(), 1.0e-9);
        assertTrue(two.deltaV() > one.deltaV(), two.deltaV() + " vs " + one.deltaV());
    }

    @Test
    void aGuidedShotLandsOnItsAim() {
        V3 start = new V3(0, 64, 0);
        for (double range : new double[] {150, 800, 2000}) {
            V3 aim = new V3(range * 0.6, 70, range * 0.8);
            MissileFlight.Prediction shot = fly(PAD, start, aim);
            assertTrue(shot.onTarget(), "cut off by its guidance at " + range);
            assertEquals(0, shot.impact().distance(aim), 1.0e-6, "lands on its aim at " + range);
        }
    }

    @Test
    void longerShotsFlyLongerAndHigher() {
        V3 start = new V3(0, 64, 0);
        MissileFlight.Prediction near = fly(PAD, start, new V3(500, 64, 0));
        MissileFlight.Prediction far = fly(PAD, start, new V3(2000, 64, 0));
        assertTrue(far.ticks() > near.ticks());
        assertTrue(far.apogee() > near.apogee());
        assertTrue(far.apogee() > 400, "a long shot leaves the air, apogee " + far.apogee());
    }

    @Test
    void anOutOfReachTargetIsShort() {
        double reach = MissileFlight.maxRange(PAD, P);
        assertTrue(reach > 2000 && reach < 6000, "the pad's missile reaches about 3000 blocks, got " + reach);
        V3 aim = new V3(reach * 1.5, 0, 0);
        MissileFlight.Prediction shot = fly(PAD, V3.ZERO, aim);
        assertFalse(shot.onTarget(), "it burns out before reaching the speed it needs");
        assertTrue(shot.impact().x() < aim.x(), "and comes down short");
    }

    @Test
    void moreStagesReachFurther() {
        MissileDesign big = new MissileDesign(List.of(new MissileDesign.Stage(1.5, 14, 0.3, 3),
                new MissileDesign.Stage(0.6, 5, 0.1, 3.5)), 1, 0);
        assertTrue(MissileFlight.maxRange(big, P) > 1.5 * MissileFlight.maxRange(PAD, P));
    }

    @Test
    void stagesDropAsTheyEmpty() {
        MissileDesign two = new MissileDesign(List.of(new MissileDesign.Stage(1, 3, 0.2, 3),
                new MissileDesign.Stage(0.5, 3, 0.08, 3)), 1, 0);
        MissileFlight flight = new MissileFlight(two, V3.ZERO, new V3(4000, 0, 0), V3.ZERO);
        double before = flight.mass();
        int dropped = 0;
        while (!flight.burnedOut()) {
            if (flight.step(P).stageDropped()) {
                dropped++;
            }
        }
        assertTrue(dropped >= 1, "the first stage drops off");
        assertTrue(flight.mass() < before - 1, "lighter by the stage and its propellant");
    }

    @Test
    void tooLittleThrustNeverLeavesThePad() {
        MissileDesign weak = new MissileDesign(List.of(new MissileDesign.Stage(1, 8, 0.01, 3)), 1, 0);
        assertTrue(weak.liftOffThrustToWeight(P.gravity()) < 1);
        MissileFlight flight = new MissileFlight(weak, new V3(0, 64, 0), new V3(500, 64, 0), V3.ZERO);
        for (int i = 0; i < 200; i++) {
            flight.step(P);
        }
        assertEquals(64, flight.position().y(), 1.0e-9, "it sits on the pad");
    }

    @Test
    void itClimbsClearOfItsSiloBeforeTurning() {
        MissileFlight flight = new MissileFlight(PAD, new V3(0, 40, 0), new V3(3000, 64, 0), V3.ZERO);
        while (flight.rising(P)) {
            flight.step(P);
            assertEquals(0, flight.position().horizontalLength(), 1.0e-9, "straight up while in the silo");
        }
        assertTrue(flight.position().y() >= 40 + P.riseHeight());
    }

    @Test
    void phasesFollowTheFlight() {
        MissileFlight flight = new MissileFlight(PAD, new V3(0, 64, 0), new V3(2500, 64, 0), V3.ZERO);
        assertEquals(MissileFlight.Phase.BOOST, flight.phase(P));
        boolean midcourse = false;
        boolean reentry = false;
        boolean terminal = false;
        while (!flight.impacted()) {
            flight.step(P);
            switch (flight.phase(P)) {
                case MIDCOURSE -> midcourse = true;
                case REENTRY -> reentry = true;
                case TERMINAL -> terminal = true;
                default -> {
                }
            }
        }
        assertTrue(midcourse && reentry && terminal);
        assertEquals(MissileFlight.Phase.IMPACT, flight.phase(P));
    }

    @Test
    void aSavedFlightResumesTheSame() {
        MissileFlight a = new MissileFlight(PAD, new V3(0, 64, 0), new V3(1200, 64, 300), V3.ZERO);
        for (int i = 0; i < 150; i++) {
            a.step(P);
        }
        MissileFlight b = MissileFlight.restore(a.design(), a.aim(), a.launchY(), a.position(), a.velocity(), a.stage(), a.stagePropellant(),
                a.tick(), a.burnedOut(), a.guidedCutoff(), a.burnoutTick(), a.burnoutPosition(), a.burnoutVelocity(),
                a.fallTicks(), a.impacted());
        for (int i = 0; i < 400; i++) {
            a.step(P);
            b.step(P);
        }
        assertEquals(0, a.position().distance(b.position()), 1.0e-9);
    }

    @Test
    void theSpreadHasTheRightCep() {
        double cep = 20;
        int inside = 0;
        int shots = 20_000;
        for (int i = 0; i < shots; i++) {
            if (AccuracyModel.miss(cep, i).horizontalLength() <= cep) {
                inside++;
            }
        }
        assertEquals(0.5, inside / (double) shots, 0.02, "half the shots land within the CEP");
        assertEquals(AccuracyModel.miss(cep, 7), AccuracyModel.miss(cep, 7), "a seed always misses the same way");
        assertEquals(V3.ZERO, AccuracyModel.miss(0, 7));
    }
}
