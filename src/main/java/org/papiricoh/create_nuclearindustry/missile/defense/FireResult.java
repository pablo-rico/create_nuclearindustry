package org.papiricoh.create_nuclearindustry.missile.defense;

/** How a launcher took an order to engage. Add new ones at the end. */
public enum FireResult {
    /** Taken: it launches once it is ready (turned, counted down). */
    QUEUED,
    /** Launched at once. */
    LAUNCHED,
    /** Busy or not set up to launch. */
    NOT_READY,
    /** Nothing loaded. */
    NO_AMMO,
    /** It cannot reach the target in time. */
    NO_SOLUTION,
    /** Not formed, not turning or unloaded. */
    OFFLINE,
    /** Held by redstone. */
    HOLD
}
