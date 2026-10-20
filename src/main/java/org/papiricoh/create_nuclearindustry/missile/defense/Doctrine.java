package org.papiricoh.create_nuclearindustry.missile.defense;

import java.util.Locale;

/**
 * How many interceptors go up against one missile. Saved by name and sent by ordinal: add new ones at the end, never rename one.
 */
public enum Doctrine {
    /** One at a time: another only once the first has missed, while there is still time. */
    SHOOT_LOOK_SHOOT(1),
    /** Two at once. */
    SALVO_TWO(2);

    private final int volley;

    Doctrine(int volley) {
        this.volley = volley;
    }

    /** Interceptors fired at a missile none is flying at yet. */
    public int volley() {
        return volley;
    }

    public static Doctrine byName(String name) {
        try {
            return valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return SHOOT_LOOK_SHOOT;
        }
    }

    public static Doctrine byOrdinal(int ordinal) {
        Doctrine[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : SHOOT_LOOK_SHOOT;
    }
}
