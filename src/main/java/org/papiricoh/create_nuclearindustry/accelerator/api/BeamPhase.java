package org.papiricoh.create_nuclearindustry.accelerator.api;

import java.util.Locale;

/**
 * Where the particle accelerator is in its startup sequence. Saved by name, so new phases may go
 * anywhere. A start request walks through them in order; a stop or a trip with beam in the machine
 * goes through {@link #BEAM_DUMP}, then {@link #RAMP_DOWN} and {@link #OFFLINE}.
 */
public enum BeamPhase {
    OFFLINE,
    /** Vacuum pumps evacuate the beam pipe. */
    PUMP_DOWN,
    /** Liquid helium brings the superconducting magnets and cavities cold (skipped without any). */
    COOL_DOWN,
    /** The magnet power supplies ramp the magnets up. */
    ENERGIZE,
    /** The klystrons fill the cavities with radio-frequency power. */
    RF_ON,
    /** The ion source fills the ring with beam from the linac (rings only). */
    INJECTION,
    /** The ring's magnets and cavities raise the beam to its energy (rings only). */
    RAMP,
    /** Beam on target. */
    BEAM_ON,
    /** The beam is kicked into the dump. */
    BEAM_DUMP,
    /** Radio-frequency power off and the magnets ramp down. */
    RAMP_DOWN;

    /** Particles are in the machine. */
    public boolean hasBeam() {
        return this == INJECTION || this == RAMP || this == BEAM_ON;
    }

    /** The magnets carry current. */
    public boolean energized() {
        return this == ENERGIZE || this == RF_ON || hasBeam() || this == BEAM_DUMP;
    }

    /** The cavities carry radio-frequency power. */
    public boolean rfOn() {
        return this == RF_ON || hasBeam();
    }

    public String langKey() {
        return "create_nuclearindustry.accelerator.phase." + name().toLowerCase(Locale.ROOT);
    }
}
