package org.papiricoh.create_nuclearindustry.explosive.conventional;

/**
 * What a non-nuclear blast was made of, which decides how far each of its effects reaches and how it looks: high explosive
 * (TNT, plastic explosive), a thermobaric charge's fuel-air cloud, one of a cluster warhead's bomblets, or a dirty bomb's
 * high-explosive charge scattering its radioactive load (it looks exactly like high explosive). Sent to clients by
 * {@link #id()}: only ever add new kinds at the end. No game classes.
 */
public enum ConventionalKind {
    HIGH_EXPLOSIVE,
    THERMOBARIC,
    BOMBLET,
    DIRTY;

    private static final ConventionalKind[] VALUES = values();

    public byte id() {
        return (byte) ordinal();
    }

    /** The kind sent as {@code id}; anything unknown reads as high explosive. */
    public static ConventionalKind byId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : HIGH_EXPLOSIVE;
    }
}
