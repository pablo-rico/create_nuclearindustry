package org.papiricoh.create_nuclearindustry.missile.orbit;

import java.util.Locale;

/** What a satellite does for its network. Saved by name, sent by ordinal: add new kinds at the end, never rename one. */
public enum SatelliteKind {
    /** Sees launches under it, beyond its radars' horizon, by their exhaust. */
    EARLY_WARNING,
    /** Relays its network past the link ranges and guides its missiles: launch site and target under it narrow their spread. */
    COMMS;

    public static SatelliteKind byName(String name) {
        try {
            return valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return EARLY_WARNING;
        }
    }

    public static SatelliteKind byOrdinal(int ordinal) {
        return values()[Math.floorMod(ordinal, values().length)];
    }
}
