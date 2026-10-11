package org.papiricoh.create_nuclearindustry.accelerator.multiblock;

/**
 * What a block does on the beamline. The controller walks the beamline by these roles, so a new
 * part needs only its block's {@link BeamElementBlock#beamRole}, no validator change. Not saved.
 */
public enum BeamElementRole {
    /** Makes the beam; sits right in front of the controller. */
    ION_SOURCE,
    /** Plain beam pipe. */
    DRIFT,
    /** Copper radio-frequency cavity: adds energy. */
    RF_CAVITY,
    /** Superconducting cavity: adds more energy for less power, needs liquid helium. */
    SC_RF_CAVITY,
    /** Focuses the beam. */
    QUADRUPOLE,
    /** Turns the beamline 90 degrees. */
    BEND,
    /** Beam position monitor: reads the current. */
    DIAGNOSTIC,
    /** Normal-conducting ring dipole: bends the beam round the ring. */
    DIPOLE,
    /** Superconducting ring dipole: a stronger field, needs liquid helium. */
    SC_DIPOLE,
    /** Where the injector line feeds a ring. */
    INJECTION,
    /** Where a ring's beam leaves for its extraction line. */
    EXTRACTION,
    /** End of a line: a target station. */
    TARGET,
    /** End of a line: a beam dump. */
    DUMP;

    /** The walk along a line ends here. */
    public boolean terminal() {
        return this == TARGET || this == DUMP || this == INJECTION;
    }

    public boolean magnet() {
        return this == QUADRUPOLE || this == BEND || this == DIPOLE || this == SC_DIPOLE;
    }

    public boolean cavity() {
        return this == RF_CAVITY || this == SC_RF_CAVITY;
    }

    public boolean superconducting() {
        return this == SC_RF_CAVITY || this == SC_DIPOLE;
    }

    /** May sit on a straight beamline (injector or extraction line). */
    public boolean lineElement() {
        return this == DRIFT || this == RF_CAVITY || this == SC_RF_CAVITY || this == QUADRUPOLE || this == BEND
                || this == DIAGNOSTIC;
    }

    /** May sit in a ring. */
    public boolean ringElement() {
        return this == DRIFT || this == RF_CAVITY || this == SC_RF_CAVITY || this == QUADRUPOLE || this == DIAGNOSTIC
                || this == DIPOLE || this == SC_DIPOLE || this == INJECTION || this == EXTRACTION;
    }
}
