package org.papiricoh.create_nuclearindustry.missile.flight;

/**
 * Moments in a flight the client marks with effects and sounds. Sent by id: only ever add new ones at the end with the next id.
 */
public enum FlightFxEvent {
    /** Lift-off, at the silo. */
    LAUNCH(0),
    /** A spent stage falls away (the event's stage is the one dropped). */
    STAGE_SEPARATION(1),
    /** The next stage lights. */
    STAGE_IGNITION(2),
    /** The engines stop for good: guidance cut them or the propellant ran out. */
    BURNOUT(3),
    /** The warhead leaves the stages still attached at a guided cutoff. */
    PAYLOAD_SEPARATION(4),
    /** Back into the air after coasting above it. */
    REENTRY(5),
    /** The last seconds before impact. */
    TERMINAL(6),
    /** Down, just before the warhead goes off. */
    IMPACT(7),
    /** A falling spent stage reaches the ground. */
    DEBRIS_IMPACT(8),
    /** Taken out of the sky without landing. */
    ABORT(9),
    /** Destroyed by an interceptor: the event's position is where, its velocity the missile's. */
    INTERCEPT(10),
    /** An interceptor that reached its point without a kill destroys itself. */
    INTERCEPTOR_MISS(11),
    /**
     * A launch vehicle reaches orbit and lets its satellite go: the event's id is the flight's, and the stage the clients are sent
     * is the satellite's kind ({@code SatelliteKind} ordinal).
     */
    ORBIT_INSERTION(12),
    /** A satellite destroyed by an interceptor, high up: the event's id is the satellite's. */
    SATELLITE_KILLED(13),
    /** A low satellite that decayed burns up coming down: the event's id is the satellite's. */
    SATELLITE_REENTRY(14);

    private static final FlightFxEvent[] BY_ID = new FlightFxEvent[15];

    static {
        for (FlightFxEvent e : values()) BY_ID[e.id] = e;
    }

    private final byte id;

    FlightFxEvent(int id) {
        this.id = (byte) id;
    }

    public byte id() {
        return id;
    }

    /** The event sent as {@code id}, or null for one this version does not know. */
    public static FlightFxEvent byId(int id) {
        return id >= 0 && id < BY_ID.length ? BY_ID[id] : null;
    }
}
