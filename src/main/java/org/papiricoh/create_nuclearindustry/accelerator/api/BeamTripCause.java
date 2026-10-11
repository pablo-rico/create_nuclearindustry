package org.papiricoh.create_nuclearindustry.accelerator.api;

import java.util.Locale;

/** Why the accelerator last dumped its beam, or why it refuses to start. Saved by name. */
public enum BeamTripCause {
    NONE,
    /** The operator asked for a stop (GUI, desk lever or redstone). */
    OPERATOR,
    /** The structure broke or was unloaded. */
    STRUCTURE,
    /** The beam pipe pressure rose too high, or the pumps cannot reach beam vacuum. */
    VACUUM_LOST,
    /** Superconducting parts are warm and there is no liquid helium to cool them. */
    MAGNETS_WARM,
    /** A superconducting magnet quenched. */
    MAGNET_QUENCH,
    /** Too few magnet power supplies, or too little power to them. */
    NO_MAGNET_POWER,
    /** Too little radio-frequency power for the cavities. */
    NO_RF_POWER,
    /** The ion source has no hydrogen. */
    NO_ION_GAS,
    /** A personnel interlock is open: a door into the beam area. */
    INTERLOCK,
    /** The linac is too weak for the ring it feeds. */
    INJECTION_ENERGY_LOW,
    /** Too much of the beam was lost on the way. */
    BEAM_LOSS;

    public String langKey() {
        return "create_nuclearindustry.accelerator.trip." + name().toLowerCase(Locale.ROOT);
    }
}
