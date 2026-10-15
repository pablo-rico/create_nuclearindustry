package org.papiricoh.create_nuclearindustry.reactor.pwr.accident;

import org.junit.jupiter.api.Test;
import org.papiricoh.create_nuclearindustry.reactor.pwr.containment.ContainmentModel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReactorBuildingTest {

    @Test
    void blastPowerGrowsWithThePressureRise() {
        assertEquals(2.0, ReactorBuilding.blastPower(0.0, 8.0), 1e-9, "a burn with no rise still pops");
        assertEquals(3.5, ReactorBuilding.blastPower(1.0, 8.0), 1e-9);
        assertEquals(6.5, ReactorBuilding.blastPower(3.0, 8.0), 1e-9);
        assertEquals(8.0, ReactorBuilding.blastPower(10.0, 8.0), 1e-9, "capped at the configured power");
    }

    @Test
    void blastPowerStaysInItsRange() {
        assertEquals(2.0, ReactorBuilding.blastPower(-5.0, 8.0), 1e-9);
        assertEquals(2.0, ReactorBuilding.blastPower(Double.NaN, 8.0), 1e-9);
        assertEquals(2.0, ReactorBuilding.blastPower(4.0, 1.0), 1e-9, "never below 2, whatever the cap");
    }

    @Test
    void aBurnAtTheIgnitionLimitBlowsTheRoof() {
        // 8% hydrogen burning in a room of ordinary blocks (1.5 bar): well past what the walls hold
        ContainmentModel room = ContainmentModel.atmospheric(1_000, 600);
        room.addHydrogen(room.totalMoles() * 0.087, ContainmentModel.WALL_TEMPERATURE);
        double rise = room.burn();
        assertTrue(ContainmentModel.ATMOSPHERE + rise > 1.5, "the burn beats an ordinary wall, rise " + rise);
        double power = ReactorBuilding.blastPower(rise, 8.0);
        assertTrue(power > 4.0, "stronger than TNT, " + power);
        assertTrue(ReactorBuilding.roofRadius(power) >= 3);
    }

    @Test
    void inesRisesWithTheFailedCladding() {
        assertEquals(3, ReactorBuilding.ines(0.0));
        assertEquals(3, ReactorBuilding.ines(0.099));
        assertEquals(5, ReactorBuilding.ines(0.1));
        assertEquals(5, ReactorBuilding.ines(1.0));
    }

    @Test
    void roofRadiusIsHalfThePowerRoundedUp() {
        assertEquals(1, ReactorBuilding.roofRadius(2.0));
        assertEquals(2, ReactorBuilding.roofRadius(3.5));
        assertEquals(4, ReactorBuilding.roofRadius(8.0));
    }
}
