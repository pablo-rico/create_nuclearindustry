package org.papiricoh.create_nuclearindustry.advancement;

import java.util.Locale;

/** Moments in the game that advancements listen for, through {@link NuclearEventTrigger}. */
public enum NuclearEvent {
    REACTOR_FORMED,
    REACTOR_RUNNING,
    MODERATED_CORE,
    SCRAM,
    IODINE_PIT,
    MELTDOWN,
    CONTROL_ROOM_LINKED,
    TURBINE_FORMED,
    COOLING_TOWER_FORMED,
    TOKAMAK_IGNITION,
    RADIATION_SICKNESS,
    RADIATION_PURGE,
    NUCLEAR_DETONATION,
    DIRTY_DETONATION;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }
}
