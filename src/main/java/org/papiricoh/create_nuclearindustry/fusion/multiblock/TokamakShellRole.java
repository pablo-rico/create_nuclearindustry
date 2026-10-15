package org.papiricoh.create_nuclearindustry.fusion.multiblock;

/**
 * What a block in the tokamak shell is for. The validator only asks a block for its role, so a
 * new shell part needs no change to {@link TokamakStructureValidator}: its block implements
 * {@link TokamakShellBlock}, and {@link TokamakStructure} counts and lists it by role.
 */
public enum TokamakShellRole {
    /** Plain vacuum vessel or cryostat casing. */
    VESSEL,
    /** Toroidal field coil; counted per sector for the coil coverage. */
    COIL,
    /** Lithium-6 breeding blanket. */
    BLANKET,
    /** Kinetic drive that energises the coils. */
    MAGNET_DRIVE,
    /** Fluid port taking coolant water in. */
    COOLANT_PORT,
    /** Fluid port giving plasma steam out. */
    STEAM_PORT,
    /** Fuel injector taking deuterium and tritium in. */
    FUEL_IN,
    /** Fuel injector giving surplus tritium out. */
    FUEL_OUT,
    /** Kinetic pump that holds the chamber vacuum. */
    VACUUM_PUMP,
    /** Cryogenic port taking liquid helium in to cool the coils. */
    CRYO_IN,
    /** Cryogenic port giving warm helium gas back to the cryogenic plant. */
    CRYO_OUT,
    /** Neutral beam injector: auxiliary heating. */
    NBI,
    /** Radio-frequency antenna: auxiliary heating. */
    RF_ANTENNA,
    /** Window into the plasma chamber; glows while the plasma is lit. */
    VIEWPORT,
    /** Antiproton injector: fires a dose of antimatter that lifts a heating plasma past ignition. */
    ANTIPROTON_INJECTOR
}
