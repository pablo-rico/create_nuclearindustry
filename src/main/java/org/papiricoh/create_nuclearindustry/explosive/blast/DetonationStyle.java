package org.papiricoh.create_nuclearindustry.explosive.blast;

/**
 * What kind of device went off, which decides how its explosion looks ({@link ExplosionPalette}) and what
 * listeners of the detonation event make of it: a fission warhead, a thermonuclear one (a fission
 * primary driving a fusion secondary) or a dirty bomb's conventional blast. Sent to clients by its
 * {@link #id()}. No game classes.
 */
public enum DetonationStyle {
    FISSION,
    THERMONUCLEAR,
    DIRTY;

    private static final DetonationStyle[] VALUES = values();

    public byte id() {
        return (byte) ordinal();
    }

    /** The style sent as {@code id}; anything unknown reads as fission. */
    public static DetonationStyle byId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : FISSION;
    }
}
