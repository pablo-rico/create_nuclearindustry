package org.papiricoh.create_nuclearindustry.missile.defense;

/** How an order to engage one missile went. Add new ones at the end. */
public enum EngageResult {
    /** At least one launcher took it. */
    QUEUED,
    /** None of the radars sees it. */
    NOT_TRACKED,
    /** It is one of ours. */
    FRIENDLY,
    /** No working launcher. */
    NO_SHOOTER,
    /** No launcher has anything loaded. */
    NO_AMMO,
    /** No launcher can reach it in time. */
    NO_SOLUTION,
    /** Enough interceptors are already on their way. */
    ALREADY_ENGAGED,
    /** It is down or gone. */
    GONE
}
