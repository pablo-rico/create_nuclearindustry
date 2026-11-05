package org.papiricoh.create_nuclearindustry.dyson.receiver;

import java.util.Locale;

/** What a ray receiver is doing, or why it takes nothing. Sent to clients by ordinal: add new ones at the end. */
public enum ReceiverStatus {
    RECEIVING,
    /** Not in the Overworld. */
    WRONG_DIMENSION,
    /** Something stands over it. */
    NO_SKY,
    /** The sun is down, too low, or behind a storm. */
    NO_SUN,
    /** No sails in orbit. */
    NO_SWARM,
    /** Its store is full: nothing takes its energy. */
    FULL,
    /** Asking, but the swarm's power is all shared out (or this is its first second). */
    WAITING;

    public String key() {
        return "create_nuclearindustry.receiver.status." + name().toLowerCase(Locale.ROOT);
    }

    public static ReceiverStatus byOrdinal(int ordinal) {
        ReceiverStatus[] all = values();
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : WAITING;
    }
}
