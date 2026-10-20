package org.papiricoh.create_nuclearindustry.missile.defense;

import java.util.Locale;

/** What launches an interceptor. Saved by name and sent by ordinal: add new ones at the end, never rename one. */
public enum ShooterType {
    /** An interceptor battery: short reach, inside the air, reloads. */
    BATTERY,
    /** A launch controller whose rocket carries a kill vehicle: long reach, above the air, once. */
    SILO;

    public static ShooterType byName(String name) {
        try {
            return valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return BATTERY;
        }
    }
}
