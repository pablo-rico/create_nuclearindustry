package org.papiricoh.create_nuclearindustry.fusion.api;

import java.util.Locale;

/** Why the tokamak last shut its plasma down, or why it refuses to start. Saved by name. */
public enum TripCause {
    NONE,
    /** The operator asked for a shutdown (GUI, desk lever or redstone). */
    OPERATOR,
    /** The chamber pressure rose above the plasma limit, or the pumps cannot reach it. */
    VACUUM_LOST,
    /** The coils warmed above their superconducting temperature, or there is no liquid helium. */
    MAGNETS_WARM,
    /** The magnet drives give too little field. */
    NO_FIELD,
    /** No deuterium to start or keep the plasma. */
    NO_FUEL,
    /** The structure broke or was unloaded, or the world was updated with a lit plasma. */
    STRUCTURE,
    /** The plasma disrupted. */
    DISRUPTION;

    public String langKey() {
        return "create_nuclearindustry.tokamak.trip." + name().toLowerCase(Locale.ROOT);
    }
}
