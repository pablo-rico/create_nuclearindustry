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
    NUCLEAR_SHELL_FIRED,
    // Particle accelerator
    ACCELERATOR_FORMED,
    FIRST_BEAM,
    RING_FORMED,
    GEV_BEAM,
    // Thermonuclear weapons
    THERMONUCLEAR_DETONATION,
    // Antimatter
    FIRST_NANOGRAM,
    ANNIHILATION_IGNITION,
    QUENCH,
    ANNIHILATION_POWER,
    // Isotopes and nuclear medicine
    FIRST_ISOTOPE,
    FOOD_STERILISED,
    TECHNETIUM_ELUTED,
    // Operation and maintenance
    MAINTENANCE_DONE,
    SEISMIC_SCRAM,
    // Nuclear engines on Create Aeronautics ships
    NUCLEAR_FLIGHT,
    ANTIMATTER_THRUST,
    // Safe shutdown and loss of cooling
    /** Boric acid went into a reactor whose water held no boron. */
    BORON_INJECTED,
    /** Hydrogen from overheated cladding blew up the room around a reactor with no containment. */
    HYDROGEN_EXPLOSION,
    // Missile defence
    /** An interceptor destroyed a missile in flight. */
    MISSILE_INTERCEPTED,
    // Satellites
    /** A launch vehicle put its satellite in orbit. */
    SATELLITE_IN_ORBIT,
    /** An interceptor destroyed a satellite in orbit. */
    SATELLITE_DESTROYED;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }
}
