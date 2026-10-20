package org.papiricoh.create_nuclearindustry.missile.defense;

import org.junit.jupiter.api.Test;
import org.papiricoh.create_nuclearindustry.missile.defense.battery.TurretAim;
import org.papiricoh.create_nuclearindustry.missile.physics.V3;

import static org.junit.jupiter.api.Assertions.*;

class TurretAimTest {
    @Test void yawIsClockwiseFromNorthAndPitchRaisesTheMuzzle() {
        assertEquals(0, TurretAim.fromDirection(new V3(0, 0, -1)).yaw(), 0.001);
        assertEquals(90, TurretAim.fromDirection(new V3(1, 0, 0)).yaw(), 0.001);
        assertEquals(-90, TurretAim.fromDirection(new V3(-1, 0, 0)).yaw(), 0.001);
        assertEquals(180, Math.abs(TurretAim.fromDirection(new V3(0, 0, 1)).yaw()), 0.001);
        assertEquals(45, TurretAim.fromDirection(new V3(0, 1, -1)).pitch(), 0.001);
    }

    @Test void pitchStaysWithinTheCanistersTravel() {
        assertEquals(TurretAim.MIN_PITCH, TurretAim.fromDirection(new V3(1, 0, 0)).pitch(), 0.001);
        assertEquals(TurretAim.MAX_PITCH, TurretAim.fromDirection(new V3(0, 1, 0.001)).pitch(), 0.001);
    }

    @Test void pointingAlongAnAimedDirectionGivesItBack() {
        for (V3 wanted : new V3[]{new V3(3, 2, -1), new V3(-2, 5, 4), new V3(1, 1, 1), new V3(-4, 3, -3)}) {
            TurretAim.Angles angles = TurretAim.fromDirection(wanted);
            V3 pointed = TurretAim.direction(angles.yaw(), angles.pitch());
            V3 unit = wanted.normalize();
            assertEquals(unit.x(), pointed.x(), 1e-6);
            assertEquals(unit.y(), pointed.y(), 1e-6);
            assertEquals(unit.z(), pointed.z(), 1e-6);
        }
    }

    @Test void slewTakesTheShortWayAndClampsPitch() {
        assertEquals(175, TurretAim.yawStep(170, -170, 5), 0.001);
        assertEquals(-175, TurretAim.yawStep(-170, 170, 5), 0.001);
        assertEquals(TurretAim.MAX_PITCH, TurretAim.pitchStep(84, 100, 4), 0.001);
    }

    @Test void alignmentChecksBothAxes() {
        TurretAim.Angles target = new TurretAim.Angles(10, 30);
        assertTrue(TurretAim.aligned(11, 31, target, 2));
        assertFalse(TurretAim.aligned(13, 30, target, 2));
        assertFalse(TurretAim.aligned(10, 33, target, 2));
    }
}
