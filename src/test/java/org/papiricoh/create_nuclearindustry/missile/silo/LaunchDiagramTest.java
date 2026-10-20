package org.papiricoh.create_nuclearindustry.missile.silo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Cell ordinals are sent to the console and must leave room for the new payload sprite. */
class LaunchDiagramTest {
    @Test void killVehicleUsesItsSectionOrdinalAndForeignRemainsOutsideTheGrammar() {
        assertEquals(4, RocketComposition.Section.KILL_VEHICLE.ordinal());
        assertEquals(15, LaunchDiagram.Cell.FOREIGN);
    }
}
