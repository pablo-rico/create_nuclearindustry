package org.papiricoh.create_nuclearindustry.missile.physics;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.papiricoh.create_nuclearindustry.missile.physics.*;
import org.papiricoh.create_nuclearindustry.missile.defense.*;
import java.util.*;

class MissileFlightTimedTest {
    @Test void arrivesExactlyAndNeverEarly() {
        var params = BallisticModel.Params.DEFAULT;
        V3 aim = new V3(100, 100, 0);
        var flight = MissileFlight.timed(InterceptorDesigns.battery(9, 0.5), V3.ZERO, aim, V3.UP.scale(0.6), 80, 0);
        var prediction = flight.predict(params);
        assertTrue(prediction.onTarget());
        assertEquals(80, prediction.ticks());
        assertTrue(prediction.impact().distance(aim) < 1e-8);
        for (int i = 1; i < 80; i++) assertFalse(flight.step(params).impact());
        assertTrue(flight.step(params).impact());
        assertEquals(80, flight.tick());
        assertTrue(flight.position().distance(aim) < 1e-8);
    }
    @Test void copyAndRestoreKeepTiming() {
        var flight = MissileFlight.timed(InterceptorDesigns.battery(9, 0.5), V3.ZERO, new V3(30, 80, 0), V3.UP, 90, 3);
        flight.step(BallisticModel.Params.DEFAULT);
        var copy = flight.copy();
        assertEquals(90, copy.arrivalTick());
        assertEquals(3, copy.rise());
        assertEquals(flight.predict(BallisticModel.Params.DEFAULT), copy.predict(BallisticModel.Params.DEFAULT));
    }
    @Test void exhaustedMotorArrivesOnItsScheduledTick() {
        var design = new MissileDesign(List.of(new MissileDesign.Stage(1, 0.001, 0.1, 6)), 0.1, 0);
        var flight = MissileFlight.timed(design, V3.ZERO, new V3(1000, 100, 0), V3.UP, 30, 0);
        var params = BallisticModel.Params.DEFAULT;
        assertFalse(flight.step(params).impact());
        V3 position = flight.position(), velocity = flight.velocity();
        assertFalse(flight.step(params).impact());
        assertTrue(flight.position().distance(BallisticModel.position(position, velocity, 1, params.gravity())) < 1e-10);
        for (int i = 3; i < 30; i++) assertFalse(flight.step(params).impact());
        assertTrue(flight.step(BallisticModel.Params.DEFAULT).impact());
        assertFalse(flight.guidedCutoff());
    }
}
