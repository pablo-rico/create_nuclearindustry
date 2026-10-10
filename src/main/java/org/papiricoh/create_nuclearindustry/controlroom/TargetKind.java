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
    /** Any other block: the radiation around it. */
    AREA;

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
