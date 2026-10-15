package org.papiricoh.create_nuclearindustry.maintenance.physics;

/**
 * The International Nuclear and Radiological Event Scale, 0 to 7, as the incident log rates what
 * happens at a plant. A release is measured against a whole core's airborne inventory
 * ({@code ReleasePath.CORE_REFERENCE}). No game classes.
 * <ul>
 *   <li>0 deviation: an automatic SCRAM, a turbine trip.</li>
 *   <li>1 anomaly: a stuck control rod, a relief valve stuck open.</li>
 *   <li>2 incident: a steam generator tube rupture (contamination inside the plant).</li>
 *   <li>4 accident with local consequences: fuel damaged by overheating.</li>
 *   <li>5 accident with wider consequences: severe core damage (a meltdown) or a limited release.</li>
 *   <li>6 serious accident: a significant release.</li>
 *   <li>7 major accident: a large release.</li>
 * </ul>
 */
public final class InesScale {
    public static final int NONE = -1;
    /** Shares of a core's inventory released outside that rate 5, 6 and 7. */
    public static final double LIMITED_RELEASE = 0.01, SIGNIFICANT_RELEASE = 0.1, MAJOR_RELEASE = 0.5;
    /** Core damage that rates a 4. */
    public static final double FUEL_DAMAGE = 0.05;

    private InesScale() {}

    /** The level of a release of {@code share} of a core outside the plant, or {@link #NONE} below a limited one. */
    public static int release(double share) {
        if (share >= MAJOR_RELEASE) {
            return 7;
        }
        if (share >= SIGNIFICANT_RELEASE) {
            return 6;
        }
        return share >= LIMITED_RELEASE ? 5 : NONE;
    }

    /** The level of a damaged core: 5 melted, 4 damaged, {@link #NONE} below that; a release raises it. */
    public static int core(double damage, boolean meltdown, double releasedShare) {
        int level = meltdown ? 5 : damage >= FUEL_DAMAGE ? 4 : NONE;
        return Math.max(level, release(releasedShare));
    }

    /** Whether {@code level} is an accident (4 and up) rather than an incident. */
    public static boolean accident(int level) {
        return level >= 4;
    }
}
