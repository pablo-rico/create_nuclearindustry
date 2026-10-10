package org.papiricoh.create_nuclearindustry.fusion.api;

import java.util.Locale;

/**
 * Where the tokamak is in its startup sequence. Saved by name, so new phases may go anywhere.
 * A start request walks through them in order; a trip or a shutdown request goes to
 * {@link #RAMP_DOWN} and then {@link #OFFLINE}.
 */
public enum TokamakPhase {
    OFFLINE,
    /** Vacuum pumps evacuate the chamber. */
    PUMP_DOWN,
    /** Liquid helium brings the coils below their superconducting temperature. */
    COOL_DOWN,
    /** The coil current ramps up to the field set by the magnet drives. */
    ENERGIZE,
    /** A central solenoid pulse ionises a puff of deuterium into plasma. */
    BREAKDOWN,
    /** Ohmic and auxiliary heating raise the plasma to burning temperature. */
    HEATING,
    /** Fusion: the plasma burns fuel and makes plasma steam. */
    BURN,
    /** Fuel and heating are cut and the field ramps down safely. */
    RAMP_DOWN;

    public boolean hasPlasma() {
        return this == BREAKDOWN || this == HEATING || this == BURN;
    }

    public String langKey() {
        return "create_nuclearindustry.tokamak.phase." + name().toLowerCase(Locale.ROOT);
    }
}
