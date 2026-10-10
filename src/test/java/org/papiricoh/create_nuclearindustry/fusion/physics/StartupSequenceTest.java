package org.papiricoh.create_nuclearindustry.fusion.physics;

import org.junit.jupiter.api.Test;
import org.papiricoh.create_nuclearindustry.fusion.api.TokamakPhase;
import org.papiricoh.create_nuclearindustry.fusion.api.TripCause;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The startup state machine: what blocks a start, the order of the phases, trips and the ramp-down. */
class StartupSequenceTest {
    private static final StartupSequence.Params P = StartupSequence.Params.DEFAULT;

    /** A tokamak's state, changed one field at a time. */
    private record Plant(boolean formed, boolean pumps, boolean vacuum, double helium, boolean cold, boolean quench,
                         double field, double target, double deuterium, boolean plasma, double temperature,
                         double density, double power) {
        static Plant ready() {
            return new Plant(true, true, false, 1000, false, false, 0, 1, 1000, false, 20, 0, 0);
        }

        StartupSequence.Conditions c() {
            return new StartupSequence.Conditions(formed, pumps, vacuum, helium, cold, quench, field, target, deuterium,
                    plasma, temperature, density, power);
        }

        Plant formed(boolean v) { return new Plant(v, pumps, vacuum, helium, cold, quench, field, target, deuterium, plasma, temperature, density, power); }
        Plant pumps(boolean v) { return new Plant(formed, v, vacuum, helium, cold, quench, field, target, deuterium, plasma, temperature, density, power); }
        Plant vacuum(boolean v) { return new Plant(formed, pumps, v, helium, cold, quench, field, target, deuterium, plasma, temperature, density, power); }
        Plant helium(double v) { return new Plant(formed, pumps, vacuum, v, cold, quench, field, target, deuterium, plasma, temperature, density, power); }
        Plant cold(boolean v) { return new Plant(formed, pumps, vacuum, helium, v, quench, field, target, deuterium, plasma, temperature, density, power); }
        Plant quench(boolean v) { return new Plant(formed, pumps, vacuum, helium, cold, v, field, target, deuterium, plasma, temperature, density, power); }
        Plant field(double v) { return new Plant(formed, pumps, vacuum, helium, cold, quench, v, target, deuterium, plasma, temperature, density, power); }
        Plant target(double v) { return new Plant(formed, pumps, vacuum, helium, cold, quench, field, v, deuterium, plasma, temperature, density, power); }
        Plant deuterium(double v) { return new Plant(formed, pumps, vacuum, helium, cold, quench, field, target, v, plasma, temperature, density, power); }
        Plant plasma(double temperature, double density, double power) {
            return new Plant(formed, pumps, vacuum, helium, cold, quench, field, target, deuterium, density > 0, temperature, density, power);
        }
    }

    @Test
    void eachMissingPieceIsReported() {
        assertEquals(TripCause.STRUCTURE, start(Plant.ready().formed(false)));
        assertEquals(TripCause.VACUUM_LOST, start(Plant.ready().pumps(false)));
        assertEquals(TripCause.MAGNETS_WARM, start(Plant.ready().helium(0)));
        assertEquals(TripCause.NONE, start(Plant.ready().helium(0).cold(true)), "coils still cold need no helium to start");
        assertEquals(TripCause.NO_FIELD, start(Plant.ready().target(0.3)));
        assertEquals(TripCause.NO_FUEL, start(Plant.ready().deuterium(5)));
        assertEquals(TripCause.NONE, start(Plant.ready()));
    }

    private static TripCause start(Plant plant) {
        StartupSequence sequence = new StartupSequence();
        TripCause cause = sequence.requestStart(P, plant.c());
        assertEquals(cause, sequence.tripCause(), "the blocked reason is what the tokamak shows");
        assertEquals(cause == TripCause.NONE ? TokamakPhase.PUMP_DOWN : TokamakPhase.OFFLINE, sequence.phase());
        return cause;
    }

    @Test
    void phasesFollowEachOtherAsTheirConditionsAreMet() {
        StartupSequence sequence = new StartupSequence();
        Plant plant = Plant.ready();
        sequence.requestStart(P, plant.c());
        assertEquals(TokamakPhase.PUMP_DOWN, sequence.phase());
        assertFalse(sequence.cooling(), "no helium flows before the vacuum");
        assertNull(sequence.tick(P, plant.c()), "waits for the vacuum");

        plant = plant.vacuum(true);
        assertEquals(TokamakPhase.COOL_DOWN, sequence.tick(P, plant.c()).to());
        assertTrue(sequence.cooling());
        assertNull(sequence.tick(P, plant.c()), "waits for cold coils");

        plant = plant.cold(true);
        assertEquals(TokamakPhase.ENERGIZE, sequence.tick(P, plant.c()).to());
        assertTrue(sequence.energize(false));
        assertNull(sequence.tick(P, plant.field(0.5).c()), "waits for the field");

        plant = plant.field(1.0);
        assertEquals(TokamakPhase.BREAKDOWN, sequence.tick(P, plant.c()).to());
        assertTrue(sequence.ohmic());
        assertFalse(sequence.fuelling());
        plant = plant.plasma(300, 0.05, 0);
        for (int i = 1; i < P.breakdownTicks(); i++) {
            assertNull(sequence.tick(P, plant.c()));
        }
        assertEquals(TokamakPhase.HEATING, sequence.tick(P, plant.c()).to());
        assertTrue(sequence.fuelling());

        assertNull(sequence.tick(P, plant.plasma(1200, 0.5, 20).c()), "heats until the burning temperature");
        assertEquals(TokamakPhase.BURN, sequence.tick(P, plant.plasma(1600, 0.6, 150).c()).to());
        assertEquals(TokamakPhase.HEATING, sequence.tick(P, plant.plasma(1000, 0.6, 30).c()).to(),
                "a burn that cools down goes back to heating");
        assertEquals(TripCause.NONE, sequence.tripCause());
    }

    private static StartupSequence burning() {
        StartupSequence sequence = new StartupSequence();
        sequence.restore(TokamakPhase.BURN, TripCause.NONE, 100, false);
        return sequence;
    }

    private static Plant burningPlant() {
        return Plant.ready().vacuum(true).cold(true).field(1).plasma(4000, 0.6, 200);
    }

    @Test
    void shutdownRampsDownThenGoesOffline() {
        StartupSequence sequence = burning();
        StartupSequence.Transition shutdown = sequence.requestShutdown();
        assertEquals(TokamakPhase.RAMP_DOWN, shutdown.to());
        assertEquals(TripCause.OPERATOR, sequence.tripCause());
        assertTrue(sequence.terminating());
        assertFalse(sequence.fuelling());
        assertTrue(sequence.energize(true), "the field holds while the plasma is pumped out");
        assertFalse(sequence.energize(false), "then ramps down");
        Plant plant = burningPlant();
        assertNull(sequence.tick(P, plant.c()));
        assertNull(sequence.tick(P, plant.plasma(20, 0, 0).field(0.5).c()), "waits for the field to ramp down");
        assertEquals(TokamakPhase.OFFLINE, sequence.tick(P, plant.plasma(20, 0, 0).field(0).c()).to());
        assertNull(sequence.requestShutdown(), "nothing to shut down when offline");
    }

    @Test
    void warmCoilsTripAtOnce() {
        StartupSequence sequence = burning();
        StartupSequence.Transition trip = sequence.tick(P, burningPlant().quench(true).c());
        assertEquals(TokamakPhase.RAMP_DOWN, trip.to());
        assertEquals(TripCause.MAGNETS_WARM, trip.cause());
    }

    @Test
    void lostVacuumTripsAfterAGracePeriod() {
        StartupSequence sequence = burning();
        Plant leaking = burningPlant().vacuum(false);
        for (int i = 0; i < P.vacuumGraceTicks(); i++) {
            assertNull(sequence.tick(P, leaking.c()), "a short pressure blip is tolerated");
        }
        StartupSequence.Transition trip = sequence.tick(P, leaking.c());
        assertEquals(TripCause.VACUUM_LOST, trip.cause());
    }

    @Test
    void stoppedMagnetDrivesTripForNoField() {
        StartupSequence sequence = burning();
        Plant slow = burningPlant().target(0.2);
        StartupSequence.Transition trip = null;
        for (int i = 0; i <= P.fieldGraceTicks() && trip == null; i++) {
            trip = sequence.tick(P, slow.c());
        }
        assertEquals(TripCause.NO_FIELD, trip.cause());
    }

    @Test
    void aPlasmaThatRunsOutOfFuelGoesOut() {
        StartupSequence sequence = burning();
        Plant empty = burningPlant().plasma(1600, 0.001, 0);
        StartupSequence.Transition trip = null;
        for (int i = 0; i <= P.plasmaLossTicks() + 1 && trip == null; i++) {
            trip = sequence.tick(P, empty.c());
        }
        assertEquals(TripCause.NO_FUEL, trip.cause());
    }

    @Test
    void pumpsThatCannotCopeAbortThePumpDown() {
        StartupSequence sequence = new StartupSequence();
        sequence.requestStart(P, Plant.ready().c());
        StartupSequence.Transition trip = null;
        for (int i = 0; i <= P.vacuumGraceTicks() + 1 && trip == null; i++) {
            trip = sequence.tick(P, Plant.ready().pumps(false).c());
        }
        assertEquals(TripCause.VACUUM_LOST, trip.cause());
    }

    @Test
    void aStartDuringRampDownRestartsOnceOffline() {
        StartupSequence sequence = burning();
        sequence.requestShutdown();
        assertEquals(TripCause.NONE, sequence.requestStart(P, burningPlant().c()), "queued");
        StartupSequence.Transition done = sequence.tick(P, burningPlant().plasma(20, 0, 0).field(0).c());
        assertEquals(TokamakPhase.OFFLINE, done.to());
        assertEquals(TokamakPhase.PUMP_DOWN, sequence.phase(), "and starts again");
    }

    @Test
    void aBrokenStructureTripsAnyPhase() {
        StartupSequence sequence = burning();
        StartupSequence.Transition trip = sequence.tick(P, burningPlant().formed(false).c());
        assertEquals(TripCause.STRUCTURE, trip.cause());
        assertEquals(TokamakPhase.RAMP_DOWN, sequence.phase());
    }
}
