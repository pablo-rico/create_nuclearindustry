package org.papiricoh.create_nuclearindustry.accelerator.physics;

/**
 * What a Penning trap's field is doing, as {@link PenningTrapModel#tick} works it out. Saved and
 * synced by ordinal: new states go at the end.
 */
public enum TrapState {
    /** No field: an empty trap without power. Harmless. */
    OFFLINE,
    /** Fully fed with a healthy buffer. */
    STABLE,
    /** Fully fed, but the buffer is running down or the field is still recovering from a quench. */
    LOW_POWER,
    /** The field is collapsing on a loaded trap; when the countdown ends, the antimatter annihilates. */
    QUENCHING;

    /** Lights and sounds that warn the operator. */
    public boolean alarming() {
        return this == QUENCHING;
    }

    public static TrapState byOrdinal(int ordinal) {
        TrapState[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : OFFLINE;
    }
}
