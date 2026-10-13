package org.papiricoh.create_nuclearindustry.reactor.pwr;

/**
 * What a reactor reports about its pressurized water loop and its containment, for monitors,
 * desks, alarms and goggles. A reactor that boils its coolant directly reports {@link #NONE}.
 *
 * @param pwr                  whether the reactor runs a pressurized primary loop
 * @param pressure             primary pressure, bar
 * @param hotLeg               hot leg temperature, °C
 * @param coldLeg              cold leg temperature, °C
 * @param subcooling           saturation temperature at the primary pressure minus the hot leg, °C
 * @param pressurizerLevel     pressurizer water level, 0-1
 * @param flow                 primary flow through the core, mB/t
 * @param vesselLevel          water covering the core, 0-1
 * @param containmentPressure  pressure in the containment around the reactor, bar (0 without one)
 * @param hydrogen             hydrogen fraction of the containment atmosphere, 0-1
 */
public record PlantReadings(boolean pwr, float pressure, float hotLeg, float coldLeg, float subcooling,
                            float pressurizerLevel, float flow, float vesselLevel, float containmentPressure,
                            float hydrogen) {
    public static final PlantReadings NONE = new PlantReadings(false, 0, 0, 0, 0, 0, 0, 0, 0, 0);
}
