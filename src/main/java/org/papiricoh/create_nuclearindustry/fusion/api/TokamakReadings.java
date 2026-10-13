package org.papiricoh.create_nuclearindustry.fusion.api;

/**
 * Everything the tokamak shows the outside world: the GUI, goggles, Display Link, control room
 * and the plasma renderer read this rather than the controller's internals. Works on both sides;
 * on the client it is built from the synced state.
 *
 * @param formed            the structure is valid
 * @param suspended         part of the torus is in unloaded chunks
 * @param majorRadius       0 when not formed
 * @param phase             startup phase
 * @param tripCause         why the plasma last went out, or why a start is blocked
 * @param temperature       plasma temperature, in the model's degrees (20..6000)
 * @param instability       0..100; disruption at 100
 * @param confinement       0..1
 * @param density           plasma density as a fraction of its limit (Greenwald fraction), 0..~1.2
 * @param ashFraction       helium ash share of the plasma, 0..1
 * @param fusionPower       MW
 * @param heatingPower      auxiliary plus ohmic heating absorbed by the plasma, MW
 * @param gain              Q = fusion power / heating power; {@link Double#POSITIVE_INFINITY} when burning with no heating
 * @param burnRate          mB/t of fuel burnt
 * @param tritiumBreeding   tritium breeding ratio
 * @param dtMode            true when burning D-T, false for D-D or cold
 * @param field             toroidal field as a fraction of its rated value, 0..1
 * @param fieldTarget       field the magnet drives ask for, 0..1
 * @param magnetTemperature coil temperature, in kelvin
 * @param vacuum            chamber pressure as log10 of pascals (5 at atmosphere, about -4 at plasma vacuum)
 * @param fuelRate          fuelling setpoint, mB/t of deuterium plus tritium
 * @param dtRatio           deuterium share of the fuelling, 0..1
 * @param heatingEnabled    whether auxiliary heating is on
 * @param heatingLevel      auxiliary heating setpoint, 0..1
 * @param deuterium         mB in the deuterium tank
 * @param tritium           mB in the tritium tank
 * @param coolant           mB in the coolant tank
 * @param plasmaSteam       mB in the plasma steam tank
 * @param liquidHelium      mB in the liquid helium tank
 * @param warmHelium        mB in the warm helium tank
 * @param tritiumOverflow   bred tritium had nowhere to go recently
 */
public record TokamakReadings(
        boolean formed,
        boolean suspended,
        int majorRadius,
        TokamakPhase phase,
        TripCause tripCause,
        double temperature,
        double instability,
        double confinement,
        double density,
        double ashFraction,
        double fusionPower,
        double heatingPower,
        double gain,
        double burnRate,
        double tritiumBreeding,
        boolean dtMode,
        double field,
        double fieldTarget,
        double magnetTemperature,
        double vacuum,
        double fuelRate,
        double dtRatio,
        boolean heatingEnabled,
        double heatingLevel,
        int deuterium,
        int tritium,
        int coolant,
        int plasmaSteam,
        int liquidHelium,
        int warmHelium,
        boolean tritiumOverflow) {

    public static final double AMBIENT_MAGNET_TEMPERATURE = 293.0;
    public static final double ATMOSPHERE_VACUUM = 5.0;

    public static final TokamakReadings NONE = new TokamakReadings(false, false, 0, TokamakPhase.OFFLINE, TripCause.NONE,
            20, 0, 0, 0, 0, 0, 0, 0, 0, 0, false, 0, 0, AMBIENT_MAGNET_TEMPERATURE, ATMOSPHERE_VACUUM,
            0, 0.5, false, 0, 0, 0, 0, 0, 0, 0, false);

    public boolean burning() {
        return phase == TokamakPhase.BURN && fusionPower > 0;
    }

    public boolean ignited() {
        return Double.isInfinite(gain);
    }
}
