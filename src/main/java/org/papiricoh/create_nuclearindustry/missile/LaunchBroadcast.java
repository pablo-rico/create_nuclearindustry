package org.papiricoh.create_nuclearindustry.missile;

/** Who hears of a missile launch ({@code missile.launchBroadcast}). */
public enum LaunchBroadcast {
    /** Nobody: only the sirens of the target know. */
    NONE,
    /** Players near where the missile comes down. */
    IMPACT_AREA,
    /** Every player on the server. */
    SERVER
}
