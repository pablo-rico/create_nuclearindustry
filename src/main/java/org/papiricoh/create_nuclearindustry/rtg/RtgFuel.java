package org.papiricoh.create_nuclearindustry.rtg;

/**
 * What an RTG pellet is made of. Plutonium-238 runs strong and long; vitrified waste is a
 * weaker, shorter source; strontium-90 (from PUREX) is weaker than plutonium but lasts twice as
 * long, and its beta rays make it harder to shield. A pellet is spent once a quarter of its charge is left.
 */
public enum RtgFuel {
    PLUTONIUM("plutonium", 256.0f, 240_000L, 1.5),
    WASTE("waste", 64.0f, 48_000L, 1.0),
    STRONTIUM("strontium", 96.0f, 480_000L, 2.0);

    public static final double SPENT_BELOW = 0.25;

    private final String id;
    private final float capacityPerRpm;
    private final long halfLifeTicks;
    private final double emission;

    RtgFuel(String id, float capacityPerRpm, long halfLifeTicks, double emission) {
        this.id = id;
        this.capacityPerRpm = capacityPerRpm;
        this.halfLifeTicks = halfLifeTicks;
        this.emission = emission;
    }

    public String id() {
        return id;
    }

    /** Stress capacity per RPM of a fresh pellet. */
    public float capacityPerRpm() {
        return capacityPerRpm;
    }

    public long halfLifeTicks() {
        return halfLifeTicks;
    }

    /** Dose per second next to an unshielded RTG with a fresh pellet. */
    public double emission() {
        return emission;
    }

    public double decay(double charge, long ticks) {
        return charge * Math.pow(2.0, -ticks / (double) halfLifeTicks);
    }

    public static RtgFuel fromId(String id) {
        for (RtgFuel fuel : values()) {
            if (fuel.id.equals(id)) {
                return fuel;
            }
        }
        return null;
    }
}
