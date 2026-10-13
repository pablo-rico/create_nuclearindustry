package org.papiricoh.create_nuclearindustry.accelerator.api;

/**
 * What a beamline ends in: a beam dump or a target station. The controller hands it the beam every
 * tick it delivers; a terminal that hears nothing for a tick has no beam.
 */
public interface BeamTerminal {

    /**
     * @param energyMev beam energy
     * @param current   beam current, uA
     * @param powerKw   beam power
     */
    void receiveBeam(double energyMev, double current, double powerKw);

    /** Activation of the terminal, dose per second next to it. */
    double activity();
}
