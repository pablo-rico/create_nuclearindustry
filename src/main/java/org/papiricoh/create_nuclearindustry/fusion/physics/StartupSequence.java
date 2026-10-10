package org.papiricoh.create_nuclearindustry.fusion.physics;

import org.papiricoh.create_nuclearindustry.fusion.api.TokamakPhase;
import org.papiricoh.create_nuclearindustry.fusion.api.TripCause;

/**
 * The tokamak's startup and shutdown sequence as a pure state machine over {@link TokamakPhase}.
 *
 * <p>A start request from {@link TokamakPhase#OFFLINE} checks what the tokamak needs (structure,
 * enough pumps, liquid helium, field, deuterium) and either starts pumping down or reports the first
 * thing missing. The phases then follow each other as their conditions are met: vacuum, cold coils,
 * full field, a breakdown pulse, heating and burn. A shutdown request, or a trip (vacuum lost,
 * coils warm, field lost, fuel out, structure broken, disruption), goes to
 * {@link TokamakPhase#RAMP_DOWN}: the plasma is pumped out, then the field ramps down, then the
 * tokamak is offline. Trips never damage anything; they only keep their cause for the operator.
 */
public final class StartupSequence {

    /**
     * @param minimumField       field the magnet drives must ask for, fraction of rated
     * @param vacuumGraceTicks   ticks the pressure may stay above the plasma limit before a trip
     * @param heliumGraceTicks   ticks a cool-down may go without any liquid helium before a trip
     * @param fieldGraceTicks    ticks the drives may ask for too little field before a trip
     * @param breakdownTicks     length of the breakdown pulse
     * @param breakdownDeuterium deuterium puffed in at breakdown, mB
     * @param plasmaLossTicks    ticks the plasma may stay below the minimum density before it is lost
     * @param minimumDensity     Greenwald fraction below which the plasma is going out
     * @param burnExitTemperature a burning plasma colder than this goes back to heating
     */
    public record Params(double minimumField, int vacuumGraceTicks, int heliumGraceTicks, int fieldGraceTicks,
                         int breakdownTicks, double breakdownDeuterium, int plasmaLossTicks, double minimumDensity,
                         double burnExitTemperature) {
        public static final Params DEFAULT = new Params(0.5, 60, 100, 40, 20, 10.0, 40, 0.01, 1200.0);
    }

    /**
     * What the tokamak looks like this tick.
     *
     * @param formed          the structure is valid and loaded
     * @param pumpsCanReach   the pumps that are running can bring the chamber to plasma vacuum
     * @param atPlasmaVacuum  the chamber is at or below the plasma pressure limit
     * @param liquidHelium    liquid helium in the controller, mB
     * @param coilsReady      the coils are cold enough to energise
     * @param coilsQuenching  the coils carry field above their superconducting temperature
     * @param field           field, fraction of rated
     * @param fieldTarget     field the magnet drives ask for
     * @param deuterium       deuterium in the controller, mB
     * @param plasmaPresent   the chamber holds a plasma
     * @param temperature     plasma temperature, degrees
     * @param density         Greenwald fraction
     * @param fusionPower     MW
     */
    public record Conditions(boolean formed, boolean pumpsCanReach, boolean atPlasmaVacuum, double liquidHelium,
                             boolean coilsReady, boolean coilsQuenching, double field, double fieldTarget,
                             double deuterium, boolean plasmaPresent, double temperature, double density,
                             double fusionPower) {}

    /** A change of phase this tick; {@code cause} is set when it was a trip. */
    public record Transition(TokamakPhase from, TokamakPhase to, TripCause cause) {}

    private TokamakPhase phase = TokamakPhase.OFFLINE;
    private TripCause tripCause = TripCause.NONE;
    private int phaseTicks;
    private int vacuumTicks;
    private int heliumTicks;
    private int fieldTicks;
    private int lossTicks;
    private boolean pendingStart;

    /**
     * Asks for a start. From {@link TokamakPhase#OFFLINE} this begins the sequence or reports what
     * is missing (which also becomes the shown trip cause); while ramping down it queues a restart for
     * when the tokamak is offline; while running it does nothing.
     *
     * @return {@link TripCause#NONE} if accepted, otherwise the first thing missing
     */
    public TripCause requestStart(Params p, Conditions c) {
        if (phase == TokamakPhase.RAMP_DOWN) {
            pendingStart = true;
            return TripCause.NONE;
        }
        if (phase != TokamakPhase.OFFLINE) {
            return TripCause.NONE;
        }
        TripCause missing = missingForStart(p, c);
        tripCause = missing;
        if (missing == TripCause.NONE) {
            enter(TokamakPhase.PUMP_DOWN);
        }
        return missing;
    }

    /** What stops a start right now, or {@link TripCause#NONE}. */
    public static TripCause missingForStart(Params p, Conditions c) {
        if (!c.formed()) {
            return TripCause.STRUCTURE;
        }
        if (!c.pumpsCanReach()) {
            return TripCause.VACUUM_LOST;
        }
        if (c.liquidHelium() <= 0 && !c.coilsReady()) {
            return TripCause.MAGNETS_WARM;
        }
        if (c.fieldTarget() < p.minimumField()) {
            return TripCause.NO_FIELD;
        }
        if (c.deuterium() < p.breakdownDeuterium()) {
            return TripCause.NO_FUEL;
        }
        return TripCause.NONE;
    }

    /** An operator shutdown: ramps down from any running phase. */
    public Transition requestShutdown() {
        pendingStart = false;
        if (phase == TokamakPhase.OFFLINE || phase == TokamakPhase.RAMP_DOWN) {
            return null;
        }
        return trip(TripCause.OPERATOR);
    }

    /** Trips the tokamak for {@code cause}; from offline it only records the cause. */
    public Transition trip(TripCause cause) {
        pendingStart = false;
        if (phase == TokamakPhase.OFFLINE) {
            if (cause != TripCause.OPERATOR) {
                tripCause = cause;
            }
            return null;
        }
        TokamakPhase from = phase;
        tripCause = cause;
        if (phase != TokamakPhase.RAMP_DOWN) {
            enter(TokamakPhase.RAMP_DOWN);
        }
        return new Transition(from, TokamakPhase.RAMP_DOWN, cause);
    }

    public Transition tick(Params p, Conditions c) {
        phaseTicks++;
        if (phase == TokamakPhase.OFFLINE) {
            return null;
        }
        if (phase == TokamakPhase.RAMP_DOWN) {
            if (!c.plasmaPresent() && c.field() <= 1.0e-3) {
                enter(TokamakPhase.OFFLINE);
                Transition done = new Transition(TokamakPhase.RAMP_DOWN, TokamakPhase.OFFLINE, null);
                if (pendingStart) {
                    pendingStart = false;
                    requestStart(p, c);
                }
                return done;
            }
            return null;
        }
        if (!c.formed()) {
            return trip(TripCause.STRUCTURE);
        }

        // Trips that apply once the chamber has been pumped down.
        if (phase != TokamakPhase.PUMP_DOWN) {
            vacuumTicks = c.atPlasmaVacuum() ? 0 : vacuumTicks + 1;
            if (vacuumTicks > p.vacuumGraceTicks()) {
                return trip(TripCause.VACUUM_LOST);
            }
        }
        if (c.coilsQuenching()) {
            return trip(TripCause.MAGNETS_WARM);
        }
        if (energized(phase)) {
            fieldTicks = c.fieldTarget() < p.minimumField() ? fieldTicks + 1 : 0;
            if (fieldTicks > p.fieldGraceTicks()) {
                return trip(TripCause.NO_FIELD);
            }
        }

        switch (phase) {
            case PUMP_DOWN -> {
                if (c.atPlasmaVacuum()) {
                    return advance(TokamakPhase.COOL_DOWN);
                }
                vacuumTicks = c.pumpsCanReach() ? 0 : vacuumTicks + 1;
                if (vacuumTicks > p.vacuumGraceTicks()) {
                    return trip(TripCause.VACUUM_LOST);
                }
            }
            case COOL_DOWN -> {
                if (c.coilsReady()) {
                    return advance(TokamakPhase.ENERGIZE);
                }
                heliumTicks = c.liquidHelium() > 0 ? 0 : heliumTicks + 1;
                if (heliumTicks > p.heliumGraceTicks()) {
                    return trip(TripCause.MAGNETS_WARM);
                }
            }
            case ENERGIZE -> {
                if (c.fieldTarget() >= p.minimumField() && c.field() >= Math.min(1.0, c.fieldTarget()) - 0.01) {
                    if (c.deuterium() < p.breakdownDeuterium()) {
                        return trip(TripCause.NO_FUEL);
                    }
                    return advance(TokamakPhase.BREAKDOWN);
                }
            }
            case BREAKDOWN -> {
                if (phaseTicks >= p.breakdownTicks()) {
                    return advance(TokamakPhase.HEATING);
                }
            }
            case HEATING, BURN -> {
                lossTicks = c.plasmaPresent() && c.density() >= p.minimumDensity() ? 0 : lossTicks + 1;
                if (lossTicks > p.plasmaLossTicks()) {
                    return trip(TripCause.NO_FUEL);
                }
                if (phase == TokamakPhase.HEATING && c.temperature() >= PlasmaModel.IGNITION_TEMP && c.fusionPower() > 0) {
                    return advance(TokamakPhase.BURN);
                }
                if (phase == TokamakPhase.BURN && c.temperature() < p.burnExitTemperature()) {
                    return advance(TokamakPhase.HEATING);
                }
            }
            default -> {
            }
        }
        return null;
    }

    private Transition advance(TokamakPhase next) {
        TokamakPhase from = phase;
        enter(next);
        return new Transition(from, next, null);
    }

    private void enter(TokamakPhase next) {
        phase = next;
        phaseTicks = 0;
        vacuumTicks = 0;
        heliumTicks = 0;
        fieldTicks = 0;
        lossTicks = 0;
    }

    private static boolean energized(TokamakPhase phase) {
        return phase == TokamakPhase.ENERGIZE || phase.hasPlasma();
    }

    // ------------------------------------------------------------------ what the phase asks of the plant

    /** Liquid helium flows through the coils. */
    public boolean cooling() {
        return phase != TokamakPhase.OFFLINE && phase != TokamakPhase.PUMP_DOWN;
    }

    /** The coils carry current; while ramping down they hold it until the plasma is gone. */
    public boolean energize(boolean plasmaPresent) {
        return energized(phase) || phase == TokamakPhase.RAMP_DOWN && plasmaPresent;
    }

    /** Fuel and auxiliary heating go in. */
    public boolean fuelling() {
        return phase == TokamakPhase.HEATING || phase == TokamakPhase.BURN;
    }

    /** The solenoid drives the plasma current. */
    public boolean ohmic() {
        return phase.hasPlasma();
    }

    /** The plasma is being pumped out. */
    public boolean terminating() {
        return phase == TokamakPhase.RAMP_DOWN;
    }

    public TokamakPhase phase() {
        return phase;
    }

    public TripCause tripCause() {
        return tripCause;
    }

    public int phaseTicks() {
        return phaseTicks;
    }

    public boolean pendingStart() {
        return pendingStart;
    }

    /** Clears the shown cause, e.g. once the operator has seen it and starts again. */
    public void clearCause() {
        tripCause = TripCause.NONE;
    }

    public void restore(TokamakPhase phase, TripCause cause, int phaseTicks, boolean pendingStart) {
        this.phase = phase == null ? TokamakPhase.OFFLINE : phase;
        this.tripCause = cause == null ? TripCause.NONE : cause;
        this.phaseTicks = Math.max(0, phaseTicks);
        this.pendingStart = pendingStart;
        vacuumTicks = heliumTicks = fieldTicks = lossTicks = 0;
    }
}
