package org.papiricoh.create_nuclearindustry.accelerator.physics;

import org.junit.jupiter.api.Test;
import org.papiricoh.create_nuclearindustry.accelerator.api.BeamPhase;
import org.papiricoh.create_nuclearindustry.accelerator.api.BeamTripCause;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The accelerator's startup state machine: what blocks a start, the order of the phases and the trips. */
class BeamSequenceTest {
    private static final BeamSequence.Params P = BeamSequence.Params.DEFAULT;

    /** An accelerator's state; each method returns a changed copy. */
    private record Plant(boolean formed, boolean pumps, boolean vacuum, boolean sc, double helium, boolean cold,
                         boolean quench, double field, double magnetPower, double rf, boolean gas, boolean interlocks,
                         boolean ring, boolean injection, boolean atEnergy, double loss) {
        static Plant linac() {
            return new Plant(true, true, false, false, 0, true, false, 0, 1, 1, true, true, false, false, false, 0);
        }

        static Plant synchrotron() {
            return new Plant(true, true, false, true, 1000, false, false, 0, 1, 1, true, true, true, true, false, 0);
        }

        BeamSequence.Conditions c() {
            return new BeamSequence.Conditions(formed, pumps, vacuum, sc, helium, cold, quench, field, magnetPower, rf, gas,
                    interlocks, ring, injection, atEnergy, loss);
        }

        Plant formed(boolean v) { return new Plant(v, pumps, vacuum, sc, helium, cold, quench, field, magnetPower, rf, gas, interlocks, ring, injection, atEnergy, loss); }
        Plant pumps(boolean v) { return new Plant(formed, v, vacuum, sc, helium, cold, quench, field, magnetPower, rf, gas, interlocks, ring, injection, atEnergy, loss); }
        Plant vacuum(boolean v) { return new Plant(formed, pumps, v, sc, helium, cold, quench, field, magnetPower, rf, gas, interlocks, ring, injection, atEnergy, loss); }
        Plant helium(double v) { return new Plant(formed, pumps, vacuum, sc, v, cold, quench, field, magnetPower, rf, gas, interlocks, ring, injection, atEnergy, loss); }
        Plant cold(boolean v) { return new Plant(formed, pumps, vacuum, sc, helium, v, quench, field, magnetPower, rf, gas, interlocks, ring, injection, atEnergy, loss); }
        Plant quench(boolean v) { return new Plant(formed, pumps, vacuum, sc, helium, cold, v, field, magnetPower, rf, gas, interlocks, ring, injection, atEnergy, loss); }
        Plant field(double v) { return new Plant(formed, pumps, vacuum, sc, helium, cold, quench, v, magnetPower, rf, gas, interlocks, ring, injection, atEnergy, loss); }
        Plant magnetPower(double v) { return new Plant(formed, pumps, vacuum, sc, helium, cold, quench, field, v, rf, gas, interlocks, ring, injection, atEnergy, loss); }
        Plant rf(double v) { return new Plant(formed, pumps, vacuum, sc, helium, cold, quench, field, magnetPower, v, gas, interlocks, ring, injection, atEnergy, loss); }
        Plant gas(boolean v) { return new Plant(formed, pumps, vacuum, sc, helium, cold, quench, field, magnetPower, rf, v, interlocks, ring, injection, atEnergy, loss); }
        Plant interlocks(boolean v) { return new Plant(formed, pumps, vacuum, sc, helium, cold, quench, field, magnetPower, rf, gas, v, ring, injection, atEnergy, loss); }
        Plant injection(boolean v) { return new Plant(formed, pumps, vacuum, sc, helium, cold, quench, field, magnetPower, rf, gas, interlocks, ring, v, atEnergy, loss); }
        Plant atEnergy(boolean v) { return new Plant(formed, pumps, vacuum, sc, helium, cold, quench, field, magnetPower, rf, gas, interlocks, ring, injection, v, loss); }
        Plant loss(double v) { return new Plant(formed, pumps, vacuum, sc, helium, cold, quench, field, magnetPower, rf, gas, interlocks, ring, injection, atEnergy, v); }
    }

    private static BeamTripCause start(Plant plant) {
        return new BeamSequence().requestStart(P, plant.c());
    }

    @Test
    void eachMissingPieceIsReported() {
        assertEquals(BeamTripCause.STRUCTURE, start(Plant.linac().formed(false)));
        assertEquals(BeamTripCause.VACUUM_LOST, start(Plant.linac().pumps(false)));
        assertEquals(BeamTripCause.MAGNETS_WARM, start(Plant.synchrotron().helium(0)));
        assertEquals(BeamTripCause.NONE, start(Plant.synchrotron().helium(0).cold(true)), "cold magnets need no helium to start");
        assertEquals(BeamTripCause.NONE, start(Plant.linac().helium(0)), "a normal-conducting linac needs no helium");
        assertEquals(BeamTripCause.NO_MAGNET_POWER, start(Plant.linac().magnetPower(0.5)));
        assertEquals(BeamTripCause.NO_RF_POWER, start(Plant.linac().rf(0.2)));
        assertEquals(BeamTripCause.NO_ION_GAS, start(Plant.linac().gas(false)));
        assertEquals(BeamTripCause.INTERLOCK, start(Plant.linac().interlocks(false)));
        assertEquals(BeamTripCause.INJECTION_ENERGY_LOW, start(Plant.synchrotron().injection(false)));
        assertEquals(BeamTripCause.NONE, start(Plant.linac()));
    }

    @Test
    void aLinacSkipsCoolingInjectionAndRamp() {
        BeamSequence s = new BeamSequence();
        Plant plant = Plant.linac();
        assertEquals(BeamTripCause.NONE, s.requestStart(P, plant.c()));
        assertEquals(BeamPhase.PUMP_DOWN, s.phase());
        plant = plant.vacuum(true);
        assertEquals(BeamPhase.ENERGIZE, s.tick(P, plant.c()).to(), "no superconducting parts, no cool-down");
        assertNull(s.tick(P, plant.c()), "waits for the field");
        assertFalse(s.rfOn());
        plant = plant.field(1);
        assertEquals(BeamPhase.RF_ON, s.tick(P, plant.c()).to());
        assertTrue(s.rfOn());
        assertFalse(s.sourceOn());
        assertEquals(BeamPhase.BEAM_ON, s.tick(P, plant.c()).to(), "a linac goes straight to beam");
        assertTrue(s.delivering());
        assertTrue(s.sourceOn());
    }

    @Test
    void aRingCoolsFillsAndRamps() {
        BeamSequence s = new BeamSequence();
        Plant plant = Plant.synchrotron();
        s.requestStart(P, plant.c());
        plant = plant.vacuum(true);
        assertEquals(BeamPhase.COOL_DOWN, s.tick(P, plant.c()).to());
        assertTrue(s.cooling());
        plant = plant.cold(true);
        assertEquals(BeamPhase.ENERGIZE, s.tick(P, plant.c()).to());
        plant = plant.field(1);
        assertEquals(BeamPhase.RF_ON, s.tick(P, plant.c()).to());
        assertEquals(BeamPhase.INJECTION, s.tick(P, plant.c()).to());
        for (int i = 1; i < P.injectionTicks(); i++) {
            assertNull(s.tick(P, plant.c()));
        }
        assertEquals(BeamPhase.RAMP, s.tick(P, plant.c()).to());
        assertNull(s.tick(P, plant.c()), "ramping until the energy is reached");
        assertFalse(s.delivering());
        plant = plant.atEnergy(true);
        assertEquals(BeamPhase.BEAM_ON, s.tick(P, plant.c()).to());
    }

    private static BeamSequence running(Plant plant) {
        BeamSequence s = new BeamSequence();
        s.restore(BeamPhase.BEAM_ON, BeamTripCause.NONE, 0, false);
        return s;
    }

    @Test
    void anOpenInterlockTripsAtOnceAndDumpsTheBeam() {
        Plant plant = Plant.linac().vacuum(true).field(1);
        BeamSequence s = running(plant);
        BeamSequence.Transition t = s.tick(P, plant.interlocks(false).c());
        assertEquals(BeamPhase.BEAM_DUMP, t.to());
        assertEquals(BeamTripCause.INTERLOCK, t.cause());
        assertFalse(s.sourceOn());
        for (int i = 1; i < P.dumpTicks(); i++) {
            assertNull(s.tick(P, plant.c()));
        }
        assertEquals(BeamPhase.RAMP_DOWN, s.tick(P, plant.c()).to());
        assertNull(s.tick(P, plant.c()), "waits for the magnets to ramp down");
        assertEquals(BeamPhase.OFFLINE, s.tick(P, plant.field(0).c()).to());
        assertEquals(BeamTripCause.INTERLOCK, s.tripCause(), "the cause stays for the operator");
    }

    @Test
    void graceTimesTripOnlyWhenTheyRunOut() {
        Plant plant = Plant.linac().vacuum(true).field(1);
        assertTripsAfter(plant.rf(0.1), P.powerGraceTicks(), BeamTripCause.NO_RF_POWER);
        assertTripsAfter(plant.magnetPower(0.5), P.powerGraceTicks(), BeamTripCause.NO_MAGNET_POWER);
        assertTripsAfter(plant.gas(false), P.gasGraceTicks(), BeamTripCause.NO_ION_GAS);
        assertTripsAfter(plant.loss(0.6), P.lossGraceTicks(), BeamTripCause.BEAM_LOSS);
        assertTripsAfter(plant.vacuum(false), P.vacuumGraceTicks(), BeamTripCause.VACUUM_LOST);
    }

    private static void assertTripsAfter(Plant plant, int grace, BeamTripCause cause) {
        BeamSequence s = running(plant);
        for (int i = 0; i < grace; i++) {
            assertNull(s.tick(P, plant.c()), cause + " too early at tick " + i);
        }
        BeamSequence.Transition t = s.tick(P, plant.c());
        assertEquals(cause, t.cause());
        assertEquals(BeamPhase.BEAM_DUMP, t.to());
    }

    @Test
    void aQuenchOrABrokenStructureTripsAtOnce() {
        Plant plant = Plant.synchrotron().vacuum(true).field(1).cold(true).atEnergy(true);
        assertEquals(BeamTripCause.MAGNET_QUENCH, running(plant).tick(P, plant.quench(true).c()).cause());
        assertEquals(BeamTripCause.STRUCTURE, running(plant).tick(P, plant.formed(false).c()).cause());
    }

    @Test
    void aTripWithoutBeamSkipsTheDump() {
        BeamSequence s = new BeamSequence();
        s.restore(BeamPhase.ENERGIZE, BeamTripCause.NONE, 0, false);
        assertEquals(BeamPhase.RAMP_DOWN, s.trip(BeamTripCause.OPERATOR).to());
    }

    @Test
    void aStartWhileRampingDownWaitsForOffline() {
        Plant plant = Plant.linac().vacuum(true).field(0.5);
        BeamSequence s = new BeamSequence();
        s.restore(BeamPhase.RAMP_DOWN, BeamTripCause.OPERATOR, 0, false);
        assertEquals(BeamTripCause.NONE, s.requestStart(P, plant.c()));
        assertTrue(s.pendingStart());
        s.tick(P, plant.field(0).c());
        assertEquals(BeamPhase.PUMP_DOWN, s.phase(), "the queued start runs once offline");
    }

    @Test
    void anOperatorStopDumpsTheBeam() {
        Plant plant = Plant.linac().vacuum(true).field(1);
        BeamSequence s = running(plant);
        BeamSequence.Transition t = s.requestStop();
        assertEquals(BeamPhase.BEAM_DUMP, t.to());
        assertEquals(BeamTripCause.OPERATOR, t.cause());
        assertNull(s.requestStop(), "already stopping");
    }

    @Test
    void theInjectionStopsIfTheLinacWeakens() {
        Plant plant = Plant.synchrotron().vacuum(true).field(1).cold(true);
        BeamSequence s = new BeamSequence();
        s.restore(BeamPhase.INJECTION, BeamTripCause.NONE, 0, false);
        assertEquals(BeamTripCause.INJECTION_ENERGY_LOW, s.tick(P, plant.injection(false).c()).cause());
    }
}
