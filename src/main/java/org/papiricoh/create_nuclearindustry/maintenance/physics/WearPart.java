package org.papiricoh.create_nuclearindustry.maintenance.physics;

import java.util.Locale;

/**
 * The plant parts that wear, with their default wear and failure figures. Lives are in game days
 * of work at rated duty; checks for the parts that fail at random come once a second.
 * <ul>
 *   <li>Control rod drives wear with the reactor's power and at every SCRAM; worn, one may jam at
 *       a SCRAM and leave its rods where they were (a stuck rod).</li>
 *   <li>The relief valve wears at every lift; the more worn, the likelier it sticks open.</li>
 *   <li>Steam generator tubes wear while they move heat, four times faster boiling dry; worn, one
 *       may burst and leak primary coolant into the secondary side (a tube rupture).</li>
 *   <li>Turbine blades wear with the steam they expand; worn, the vibration trips the turbine.</li>
 * </ul>
 */
public enum WearPart {
    ROD_DRIVES(20, 0.04, 0.5, 0.5),
    RELIEF_VALVE(0, 0.08, 0.6, 0.0),
    SG_TUBES(30, 0.1, 0.4, 0.004),
    TURBINE_BLADES(25, 0.1, 0.4, 0.004);

    public static final long TICKS_PER_DAY = 24000L;
    /** How much likelier a worn-out relief valve sticks than a new one. */
    public static final double WORN_VALVE_STICK = 10.0;

    private final double lifeDays;
    private final double transientWear;
    private final double safeAbove;
    private final double failChance;

    WearPart(double lifeDays, double transientWear, double safeAbove, double failChance) {
        this.lifeDays = lifeDays;
        this.transientWear = transientWear;
        this.safeAbove = safeAbove;
        this.failChance = failChance;
    }

    public double lifeDays() {
        return lifeDays;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** The part's parameters with every life divided by {@code wearScale} (2 = wears twice as fast). */
    public ComponentHealth.Params params(double wearScale) {
        double scale = wearScale > 0 ? wearScale : 0;
        double life = scale > 0 && lifeDays > 0 ? lifeDays * TICKS_PER_DAY / scale : 0;
        return new ComponentHealth.Params(life, transientWear * scale, safeAbove, failChance);
    }

    /** Multiplier on the relief valve's sticking chance for a valve this healthy: 1 new, {@link #WORN_VALVE_STICK} worn out. */
    public static double valveStickScale(double health) {
        double h = Math.max(0, Math.min(1, health));
        return 1.0 + (WORN_VALVE_STICK - 1.0) * (1.0 - h);
    }
}
