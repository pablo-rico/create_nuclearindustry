package org.papiricoh.create_nuclearindustry.missile.orbit;

import java.util.Locale;

/** Saved by name, sent by ordinal: add new kinds at the end, never rename one. */
public enum OrbitType {
    /** Low: its ground track sweeps the world, over a place only in passes, and it slowly comes down. */
    LEO,
    /** Geostationary: parked for good over one place, seeing far but needing a bigger rocket. */
    GEO;

    public static OrbitType byName(String name) {
        try {
            return valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return LEO;
        }
    }

    public static OrbitType byOrdinal(int ordinal) {
        return values()[Math.floorMod(ordinal, values().length)];
    }
}
