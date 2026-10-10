package org.papiricoh.create_nuclearindustry.fusion.api;

/**
 * How the outside world drives a tokamak: the controller GUI, a redstone pulse on the controller,
 * control desk levers and the AUTO controller module all go through this. Server side only;
 * every setter clamps its argument.
 */
public interface TokamakControl {

    /**
     * Starts the sequence from {@link TokamakPhase#OFFLINE}.
     *
     * @return {@link TripCause#NONE} if the start was accepted, otherwise what is missing
     */
    TripCause requestStart();

    /** Cuts fuel and heating and ramps the field down; does nothing when already offline. */
    void requestShutdown();

    /** Fuelling setpoint, mB/t of deuterium plus tritium. */
    void setFuelRate(double millibucketsPerTick);

    /** Deuterium share of the fuelling, 0..1 (0.5 is an even D-T mix). */
    void setDtRatio(double deuteriumShare);

    /** Auxiliary heating on or off, and its level 0..1. */
    void setHeating(boolean enabled, double level);

    /** Highest fuelling setpoint the GUI and the AUTO module may ask for, mB/t. */
    double maxFuelRate();

    TokamakPhase phase();

    TripCause tripCause();

    TokamakReadings readings();
}
