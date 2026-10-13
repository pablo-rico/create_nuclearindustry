package org.papiricoh.create_nuclearindustry.spentfuel;

/**
 * How far a spent assembly has cooled since it left the core, hottest first. Saved by {@link #id()},
 * so grades can be reordered; no game classes, so the decay model using it can be unit tested.
 */
public enum CoolingGrade {
    /** Just discharged: short-lived fission products still glow. Burns bare hands, triples the dose. */
    HOT("hot"),
    /** Days old. Also what spent fuel from before cooling existed counts as. */
    WARM("warm"),
    /** Weeks in a pool or cask: reprocesses better. */
    COOLED("cooled"),
    /** Only the long-lived products are left: the best for reprocessing and the mildest to handle. */
    AGED("aged");

    private final String id;

    CoolingGrade(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public boolean isHotterThan(CoolingGrade other) {
        return ordinal() < other.ordinal();
    }

    /** The cooler of the two: grades only ever move towards {@link #AGED}. */
    public static CoolingGrade cooler(CoolingGrade a, CoolingGrade b) {
        return a.ordinal() >= b.ordinal() ? a : b;
    }

    /** The grade saved as {@code id}, or null if there is none. */
    public static CoolingGrade fromId(String id) {
        for (CoolingGrade grade : values()) {
            if (grade.id.equals(id)) {
                return grade;
            }
        }
        return null;
    }
}
