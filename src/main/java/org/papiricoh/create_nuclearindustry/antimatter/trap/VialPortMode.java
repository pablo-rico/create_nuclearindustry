package org.papiricoh.create_nuclearindustry.antimatter.trap;

import java.util.Locale;

/** What a trap's vial port does with the vial in it; the wrench switches it. Saved by name. */
public enum VialPortMode {
    /** Fills the vial from the trap. */
    FILL,
    /** Empties the vial into the trap. */
    DRAIN;

    public VialPortMode next() {
        return this == FILL ? DRAIN : FILL;
    }

    public String langKey() {
        return "create_nuclearindustry.penning_trap.mode." + name().toLowerCase(Locale.ROOT);
    }

    public static VialPortMode byName(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            return FILL;
        }
    }
}
