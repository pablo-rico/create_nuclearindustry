package org.papiricoh.create_nuclearindustry.controlroom;

/** What a control room device is watching. */
public enum TargetKind {
    /** Nothing linked yet. */
    NONE,
    /** Linked, but the target is out of range, unloaded or in another dimension. */
    NO_SIGNAL,
    REACTOR,
    TURBINE,
    COOLING_TOWER,
    TOKAMAK,
    /** Any other block: the radiation around it. */
    AREA,
    /** A {@link PlantReadable} steam generator. */
    STEAM_GENERATOR,
    /** A {@link PlantReadable} containment building. */
    CONTAINMENT,
    /** A {@link PlantReadable} Penning trap holding antimatter. */
    ANTIMATTER_TRAP,
    /** A {@link PlantReadable} annihilation generator. */
    ANNIHILATION_GENERATOR,
    /** A {@link PlantReadable} nuclear thermal engine. */
    THERMAL_ENGINE,
    /** A {@link PlantReadable} antimatter engine. */
    ANTIMATTER_ENGINE,
    /** A {@link PlantReadable} missile radar. Stored by ordinal elsewhere: new kinds go last. */
    RADAR;

    public String translationKey() {
        return "create_nuclearindustry.control_room.kind." + name().toLowerCase(java.util.Locale.ROOT);
    }

    public static TargetKind byName(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return NONE;
        }
    }
}
