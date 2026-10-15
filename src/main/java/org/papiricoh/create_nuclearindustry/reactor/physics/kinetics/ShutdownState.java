package org.papiricoh.create_nuclearindustry.reactor.physics.kinetics;

/**
 * Where a fission core stands on its way from running to a safe, cold shutdown:
 * <ul>
 *   <li>{@link #OPERATING}: the chain reaction runs, or the core is critical and will;</li>
 *   <li>{@link #SUBCRITICAL}: the chain reaction is off, but the rods and boron would not hold it off
 *       once the core is cold and its xenon gone: it will restart as it cools;</li>
 *   <li>{@link #HOT_SHUTDOWN}: held off with margin to spare, still hot;</li>
 *   <li>{@link #COLD_SHUTDOWN}: held off with margin, fuel and water below {@link #COLD_TEMPERATURE}
 *       and the core under water: safe to leave, refuel and mend.</li>
 * </ul>
 * Ordinals are synced to clients: new states go at the end. Pure arithmetic.
 */
public enum ShutdownState {
    OPERATING,
    SUBCRITICAL,
    HOT_SHUTDOWN,
    COLD_SHUTDOWN;

    /** Below this the fuel and the water count as cold, Celsius. */
    public static final double COLD_TEMPERATURE = 100.0;
    /** Share of the core under water it takes to count as covered. */
    public static final double COVERED = 0.999;

    /**
     * @param running            the chain reaction runs
     * @param netReactivity      the core's reactivity now, pcm
     * @param coldMargin         reactivity the rods and boron would hold back cold and without xenon, pcm
     * @param requiredMargin     margin a shutdown needs to count as safe, pcm
     * @param fuelTemperature    Celsius
     * @param coolantTemperature Celsius
     * @param coverage           share of the core under water, 0..1
     */
    public static ShutdownState classify(boolean running, double netReactivity, double coldMargin, double requiredMargin,
                                         double fuelTemperature, double coolantTemperature, double coverage) {
        if (running || netReactivity >= 0) {
            return OPERATING;
        }
        if (!(coldMargin >= requiredMargin)) {
            return SUBCRITICAL;
        }
        if (fuelTemperature < COLD_TEMPERATURE && coolantTemperature < COLD_TEMPERATURE && coverage >= COVERED) {
            return COLD_SHUTDOWN;
        }
        return HOT_SHUTDOWN;
    }

    /** Held off with margin to spare, hot or cold. */
    public boolean safe() {
        return this == HOT_SHUTDOWN || this == COLD_SHUTDOWN;
    }

    public static ShutdownState byOrdinal(int ordinal) {
        ShutdownState[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : OPERATING;
    }
}
