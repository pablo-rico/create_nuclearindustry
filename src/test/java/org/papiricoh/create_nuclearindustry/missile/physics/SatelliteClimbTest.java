package org.papiricoh.create_nuclearindustry.missile.physics;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SatelliteClimbTest {
    private static MissileDesign liquid(int stages) {
        var stage = PropellantModel.stage(Collections.nCopies(4, new PropellantModel.Tank(false, 1000, 1000))).design();
        return new MissileDesign(Collections.nCopies(stages, stage), 1, 0);
    }

    @Test
    void fullLiquidStagesReachInsertionAfterSeparating() {
        var params = BallisticModel.Params.DEFAULT;
        for (int stages : new int[]{2, 3}) {
            for (double heading : new double[]{0, Math.PI / 2, -2.4}) {
                var start = new V3(123, 64, -321);
                var climb = SatelliteClimb.flight(liquid(stages), start, heading, 1263, 600, params);
                assertEquals(1263, climb.aim().y());
                assertEquals(600, climb.aim().sub(start).horizontalLength(), 1e-8);
                climb.step(params);
                assertEquals(start.x(), climb.position().x());
                assertEquals(start.z(), climb.position().z());
                assertTrue(climb.position().y() > start.y());
                int drops = 0;
                while (!climb.impacted() && climb.tick() < 10000) {
                    if (climb.step(params).stageDropped()) drops++;
                }
                assertTrue(climb.impacted());
                assertTrue(climb.guidedCutoff());
                assertEquals(climb.arrivalTick(), climb.tick());
                assertTrue(climb.position().distance(climb.aim()) < 1e-8);
                assertTrue(drops >= stages - 1);
            }
        }
    }

    @Test
    void emptyVehicleCannotReachInsertion() {
        var climb = SatelliteClimb.flight(new MissileDesign(List.of(), 1, 0), new V3(0, 64, 0), 0,
                1263, 600, BallisticModel.Params.DEFAULT);
        climb.step(BallisticModel.Params.DEFAULT);
        assertTrue(climb.burnedOut());
        assertFalse(climb.guidedCutoff());
        assertTrue(climb.velocity().y() <= 0);
        assertTrue(climb.position().distance(climb.aim()) > 1000);
    }
}
