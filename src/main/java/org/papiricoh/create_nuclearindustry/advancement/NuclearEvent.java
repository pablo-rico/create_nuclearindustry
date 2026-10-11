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
    DIRTY_DETONATION,
    // Visible fission reactor
    REACTOR_FULL_POWER,
    COLD_SHUTDOWN,
    RELIEF_VALVE_LIFTED,
    STEAM_GENERATOR_FORMED,
    // Guide: the plant, waste, civil defense, fusion and weapons
    CONTAINMENT_HELD,
    CORIUM_CAUGHT,
    REPOSITORY_SEALED,
    SIREN_SOUNDED,
    SEISMIC_DETECTED,
    SHELTERED_FROM_BLAST,
    BLAST_DOOR_SHUT,
    TOKAMAK_FORMED,
    CRYOPLANT_FORMED,
    FUSION_BREAKEVEN,
    DISRUPTION,
    MISSILE_LAUNCHED,
    NUCLEAR_SHELL_FIRED;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }
}
