package org.papiricoh.create_nuclearindustry.accelerator.physics;

import org.papiricoh.create_nuclearindustry.accelerator.api.BeamPhase;
import org.papiricoh.create_nuclearindustry.accelerator.api.BeamTripCause;

/**
 * The accelerator's startup and shutdown sequence as a pure state machine over {@link BeamPhase},
 * on the pattern of the tokamak's {@link org.papiricoh.create_nuclearindustry.fusion.physics.StartupSequence}.
 *
 * <p>A start request from {@link BeamPhase#OFFLINE} checks what the machine needs and either starts
 * pumping down or reports the first thing missing. The phases then follow each other as their
 * conditions are met: vacuum, cold superconducting parts (only if there are any), magnets at field,
 * radio-frequency power, and beam. A linac goes straight from {@link BeamPhase#RF_ON} to
 * {@link BeamPhase#BEAM_ON}; a ring fills ({@link BeamPhase#INJECTION}) and ramps
 * ({@link BeamPhase#RAMP}) first. A stop or a trip with beam in the machine kicks it into the dump
 * ({@link BeamPhase#BEAM_DUMP}), then the magnets ramp down. Trips keep their cause for the operator.
 */
public final class BeamSequence {

    /**
     * @param minimumMagnetPower share of the magnet power supplies needed, 0..1
     * @param minimumRfDrive     share of the radio-frequency power needed for beam, 0..1
     * @param vacuumGraceTicks   ticks the pressure may stay too high before a trip
     * @param heliumGraceTicks   ticks a cool-down may go without liquid helium before a trip
     * @param powerGraceTicks    ticks magnets or cavities may be short of power before a trip
     * @param gasGraceTicks      ticks the source may go without hydrogen before a trip
     * @param lossGraceTicks     ticks the losses may stay above the limit before a trip
     * @param lossTripFraction   share of the source current lost above which the beam trips
     * @param injectionTicks     length of a ring's fill
     * @param dumpTicks          length of the beam dump
     */
    public record Params(double minimumMagnetPower, double minimumRfDrive, int vacuumGraceTicks, int heliumGraceTicks,
                         int powerGraceTicks, int gasGraceTicks, int lossGraceTicks, double lossTripFraction,
                         int injectionTicks, int dumpTicks) {
        public static final Params DEFAULT = new Params(0.95, 0.5, 60, 100, 40, 40, 40, 0.5, 40, 20);
    }

    /**
     * What the accelerator looks like this tick.
     *
     * @param formed          the structure is valid and loaded
     * @param pumpsCanReach   the pumps can bring the pipe to beam vacuum
     * @param atBeamVacuum    the pipe is at or below beam vacuum
     * @param superconducting the machine has superconducting magnets or cavities
     * @param liquidHelium    liquid helium in the controller, mB
     * @param magnetsReady    the superconducting parts are cold enough to energise (true without any)
     * @param quenching       a superconducting magnet carries field above its superconducting temperature
     * @param field           magnet field, fraction of rated
     * @param magnetPower     share of the magnet power supplies the magnets need that are running, 0..1
     * @param rfDrive         share of the cavities' radio-frequency power that is there, 0..1
     * @param ionGas          the ion source has hydrogen
     * @param interlocksClear every personnel interlock is closed
     * @param ring            the machine has a ring
     * @param injectionOk     the linac is strong enough for the ring
     * @param atEnergy        the ring reached its energy setpoint
     * @param lossFraction    share of the source current lost on the way, 0..1
     */
    public record Conditions(boolean formed, boolean pumpsCanReach, boolean atBeamVacuum, boolean superconducting,
                             double liquidHelium, boolean magnetsReady, boolean quenching, double field,
                             double magnetPower, double rfDrive, boolean ionGas, boolean interlocksClear, boolean ring,
                             boolean injectionOk, boolean atEnergy, double lossFraction) {}

    /** A change of phase this tick; {@code cause} is set when it was a trip. */
    public record Transition(BeamPhase from, BeamPhase to, BeamTripCause cause) {}

    private BeamPhase phase = BeamPhase.OFFLINE;
    private BeamTripCause tripCause = BeamTripCause.NONE;
    private int phaseTicks;
    private int vacuumTicks;
    private int heliumTicks;
    private int magnetTicks;
    private int rfTicks;
    private int gasTicks;
    private int lossTicks;
    private boolean pendingStart;

    /**
     * Asks for a start. From {@link BeamPhase#OFFLINE} this begins the sequence or reports what is
     * missing (which also becomes the shown cause); while dumping or ramping down it queues a
     * restart; while running it does nothing.
     *
     * @return {@link BeamTripCause#NONE} if accepted, otherwise the first thing missing
     */
    public BeamTripCause requestStart(Params p, Conditions c) {
        if (phase == BeamPhase.RAMP_DOWN || phase == BeamPhase.BEAM_DUMP) {
            pendingStart = true;
            return BeamTripCause.NONE;
        }
        if (phase != BeamPhase.OFFLINE) {
            return BeamTripCause.NONE;
        }
        BeamTripCause missing = missingForStart(p, c);
        tripCause = missing;
        if (missing == BeamTripCause.NONE) {
            enter(BeamPhase.PUMP_DOWN);
        }
        return missing;
    }

    /** What stops a start right now, or {@link BeamTripCause#NONE}. */
    public static BeamTripCause missingForStart(Params p, Conditions c) {
        if (!c.formed()) {
            return BeamTripCause.STRUCTURE;
        }
        if (!c.pumpsCanReach()) {
            return BeamTripCause.VACUUM_LOST;
        }
        if (c.superconducting() && c.liquidHelium() <= 0 && !c.magnetsReady()) {
            return BeamTripCause.MAGNETS_WARM;
        }
        if (c.magnetPower() < p.minimumMagnetPower()) {
            return BeamTripCause.NO_MAGNET_POWER;
        }
        if (c.rfDrive() < p.minimumRfDrive()) {
            return BeamTripCause.NO_RF_POWER;
        }
        if (!c.ionGas()) {
            return BeamTripCause.NO_ION_GAS;
        }
        if (!c.interlocksClear()) {
            return BeamTripCause.INTERLOCK;
        }
        if (c.ring() && !c.injectionOk()) {
            return BeamTripCause.INJECTION_ENERGY_LOW;
        }
        return BeamTripCause.NONE;
    }

    /** An operator stop: dumps the beam and ramps down from any running phase. */
    public Transition requestStop() {
        pendingStart = false;
        if (phase == BeamPhase.OFFLINE || phase == BeamPhase.RAMP_DOWN || phase == BeamPhase.BEAM_DUMP) {
            return null;
        }
        return trip(BeamTripCause.OPERATOR);
    }

    /** Trips the accelerator for {@code cause}; from offline it only records the cause. */
    public Transition trip(BeamTripCause cause) {
        pendingStart = false;
        if (phase == BeamPhase.OFFLINE) {
            if (cause != BeamTripCause.OPERATOR) {
                tripCause = cause;
            }
            return null;
        }
        BeamPhase from = phase;
        tripCause = cause;
        if (phase == BeamPhase.RAMP_DOWN || phase == BeamPhase.BEAM_DUMP) {
            return null;
        }
        BeamPhase next = phase.hasBeam() ? BeamPhase.BEAM_DUMP : BeamPhase.RAMP_DOWN;
        enter(next);
        return new Transition(from, next, cause);
    }

    public Transition tick(Params p, Conditions c) {
        phaseTicks++;
        switch (phase) {
            case OFFLINE -> {
                return null;
            }
            case BEAM_DUMP -> {
                return phaseTicks >= p.dumpTicks() ? advance(BeamPhase.RAMP_DOWN) : null;
            }
            case RAMP_DOWN -> {
                if (c.field() <= 1.0e-3) {
                    enter(BeamPhase.OFFLINE);
                    Transition done = new Transition(BeamPhase.RAMP_DOWN, BeamPhase.OFFLINE, null);
                    if (pendingStart) {
                        pendingStart = false;
                        requestStart(p, c);
                    }
                    return done;
                }
                return null;
            }
            default -> {
            }
        }
        if (!c.formed()) {
            return trip(BeamTripCause.STRUCTURE);
        }
        if (c.quenching()) {
            return trip(BeamTripCause.MAGNET_QUENCH);
        }
        if (phase != BeamPhase.PUMP_DOWN) {
            vacuumTicks = c.atBeamVacuum() ? 0 : vacuumTicks + 1;
            if (vacuumTicks > p.vacuumGraceTicks()) {
                return trip(BeamTripCause.VACUUM_LOST);
            }
        }
        if (phase.energized()) {
            magnetTicks = c.magnetPower() < p.minimumMagnetPower() ? magnetTicks + 1 : 0;
            if (magnetTicks > p.powerGraceTicks()) {
                return trip(BeamTripCause.NO_MAGNET_POWER);
            }
        }
        if (phase.rfOn()) {
            rfTicks = c.rfDrive() < p.minimumRfDrive() ? rfTicks + 1 : 0;
            if (rfTicks > p.powerGraceTicks()) {
                return trip(BeamTripCause.NO_RF_POWER);
            }
        }
        if (phase.hasBeam()) {
            if (!c.interlocksClear()) {
                return trip(BeamTripCause.INTERLOCK);
            }
            gasTicks = c.ionGas() ? 0 : gasTicks + 1;
            if (gasTicks > p.gasGraceTicks()) {
                return trip(BeamTripCause.NO_ION_GAS);
            }
            lossTicks = c.lossFraction() > p.lossTripFraction() ? lossTicks + 1 : 0;
            if (lossTicks > p.lossGraceTicks()) {
                return trip(BeamTripCause.BEAM_LOSS);
            }
        }

        switch (phase) {
            case PUMP_DOWN -> {
                if (c.atBeamVacuum()) {
                    return advance(c.superconducting() ? BeamPhase.COOL_DOWN : BeamPhase.ENERGIZE);
                }
                vacuumTicks = c.pumpsCanReach() ? 0 : vacuumTicks + 1;
                if (vacuumTicks > p.vacuumGraceTicks()) {
                    return trip(BeamTripCause.VACUUM_LOST);
                }
            }
            case COOL_DOWN -> {
                if (c.magnetsReady()) {
                    return advance(BeamPhase.ENERGIZE);
                }
                heliumTicks = c.liquidHelium() > 0 ? 0 : heliumTicks + 1;
                if (heliumTicks > p.heliumGraceTicks()) {
                    return trip(BeamTripCause.MAGNETS_WARM);
                }
            }
            case ENERGIZE -> {
                if (c.field() >= 0.99) {
                    return advance(BeamPhase.RF_ON);
                }
            }
            case RF_ON -> {
                if (c.rfDrive() >= p.minimumRfDrive()) {
                    if (!c.interlocksClear()) {
                        return trip(BeamTripCause.INTERLOCK);
                    }
                    if (!c.ionGas()) {
                        return trip(BeamTripCause.NO_ION_GAS);
                    }
                    if (c.ring()) {
                        if (!c.injectionOk()) {
                            return trip(BeamTripCause.INJECTION_ENERGY_LOW);
                        }
                        return advance(BeamPhase.INJECTION);
                    }
                    return advance(BeamPhase.BEAM_ON);
                }
            }
            case INJECTION -> {
                if (!c.injectionOk()) {
                    return trip(BeamTripCause.INJECTION_ENERGY_LOW);
                }
                if (phaseTicks >= p.injectionTicks()) {
                    return advance(BeamPhase.RAMP);
                }
            }
            case RAMP -> {
                if (c.atEnergy()) {
                    return advance(BeamPhase.BEAM_ON);
                }
            }
            default -> {
            }
        }
        return null;
    }

    private Transition advance(BeamPhase next) {
        BeamPhase from = phase;
        enter(next);
        return new Transition(from, next, null);
    }

    private void enter(BeamPhase next) {
        phase = next;
        phaseTicks = 0;
        vacuumTicks = heliumTicks = magnetTicks = rfTicks = gasTicks = lossTicks = 0;
    }

    // ------------------------------------------------------------------ what the phase asks of the plant

    /** Liquid helium flows through the superconducting parts. */
    public boolean cooling() {
        return phase != BeamPhase.OFFLINE && phase != BeamPhase.PUMP_DOWN;
    }

    /** The magnets carry current; while ramping down they lose it. */
    public boolean energize() {
        return phase.energized();
    }

    /** The klystrons drive the cavities. */
    public boolean rfOn() {
        return phase.rfOn();
    }

    /** The ion source makes beam. */
    public boolean sourceOn() {
        return phase.hasBeam();
    }

    /** Beam reaches the target or the dump at the end of the line. */
    public boolean delivering() {
        return phase == BeamPhase.BEAM_ON;
    }

    public BeamPhase phase() {
        return phase;
    }

    public BeamTripCause tripCause() {
        return tripCause;
    }

    public int phaseTicks() {
        return phaseTicks;
    }

    public boolean pendingStart() {
        return pendingStart;
    }

    public void clearCause() {
        tripCause = BeamTripCause.NONE;
    }

    public void restore(BeamPhase phase, BeamTripCause cause, int phaseTicks, boolean pendingStart) {
        this.phase = phase == null ? BeamPhase.OFFLINE : phase;
        this.tripCause = cause == null ? BeamTripCause.NONE : cause;
        this.phaseTicks = Math.max(0, phaseTicks);
        this.pendingStart = pendingStart;
        vacuumTicks = heliumTicks = magnetTicks = rfTicks = gasTicks = lossTicks = 0;
    }
}
