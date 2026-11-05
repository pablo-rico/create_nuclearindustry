package org.papiricoh.create_nuclearindustry.dyson.ejector;

import java.util.Locale;

/**
 * What an EM rail ejector is doing, or the first thing stopping it, in the order it checks them. Sent to clients by ordinal: add
 * new ones at the end.
 */
public enum EjectorStatus {
    /** Charging its rails for the next sail. */
    CHARGING,
    /** Not in the Overworld: there is no swarm under any other sky. */
    WRONG_DIMENSION,
    /** Turning too slowly, or not at all. */
    TOO_SLOW,
    OVERSTRESSED,
    /** Something stands over it. */
    NO_SKY,
    /** The sun is down or too low. */
    SUN_DOWN,
    /** Its orbit is closed at the terminal. */
    ORBIT_CLOSED,
    /** The swarm holds as many sails as it may. */
    SWARM_FULL,
    /** No sails loaded. */
    NO_SAILS;

    public String key() {
        return "create_nuclearindustry.ejector.status." + name().toLowerCase(Locale.ROOT);
    }

    public static EjectorStatus byOrdinal(int ordinal) {
        EjectorStatus[] all = values();
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : NO_SAILS;
    }
}
