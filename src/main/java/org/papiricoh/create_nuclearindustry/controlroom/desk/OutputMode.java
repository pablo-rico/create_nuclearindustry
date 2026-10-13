package org.papiricoh.create_nuclearindustry.controlroom.desk;

import java.util.Locale;

/** Where a control module sends its state as redstone. */
public enum OutputMode {
    NONE,
    /** Out of the bottom and back of the desk it sits on. */
    BELOW,
    /** On a Create Redstone Link frequency. */
    LINK,
    /** On an Advanced Redstone Link code. Saved by name; new modes go last. */
    ADVANCED_LINK;

    public OutputMode next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public String translationKey() {
        return "create_nuclearindustry.control_module.output." + name().toLowerCase(Locale.ROOT);
    }

    public static OutputMode byName(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return NONE;
        }
    }
}
