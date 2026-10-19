package org.papiricoh.create_nuclearindustry.missile.flight;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** A rocket's outline: built from a silo column, and kept within bounds whatever it is given. */
class RocketShapeTest {

    @Test
    void aColumnSplitsIntoItsStagesUnderThePayload() {
        // Engine, two tanks | engine, tank | guidance, warhead.
        RocketShape shape = RocketShape.column(7, List.of(0, 3));
        assertEquals(7, shape.height());
        assertEquals(5, shape.payloadBottom());
        assertEquals(2, shape.stages().size());
        assertEquals(0, shape.stages().get(0).bottom());
        assertEquals(2, shape.stages().get(0).top());
        assertEquals(3, shape.stages().get(1).bottom());
        assertEquals(4, shape.stages().get(1).top());
        assertEquals(3f, shape.stages().get(1).nozzles().get(0).y());
    }

    @Test
    void whatStaysAttachedFollowsTheStages() {
        RocketShape shape = RocketShape.column(7, List.of(0, 3));
        assertEquals(0, shape.attachedBottom(0, false));
        assertEquals(3, shape.attachedBottom(1, false));
        assertEquals(5, shape.attachedBottom(2, false));
        assertEquals(5, shape.attachedBottom(0, true), "once burnt out only the payload is left");
        assertNull(shape.stage(2));
    }

    @Test
    void anOversizedShapeIsCutDown() {
        List<RocketShape.Nozzle> nozzles = new ArrayList<>();
        for (int i = 0; i < 20; i++) nozzles.add(new RocketShape.Nozzle(0, 0, 0));
        List<RocketShape.Stage> stages = new ArrayList<>();
        for (int i = 0; i < 6; i++) stages.add(new RocketShape.Stage(i * 100, i * 100 + 5, nozzles, null));
        RocketShape shape = new RocketShape(500, 9f, stages, 900);
        assertEquals(RocketShape.MAX_HEIGHT, shape.height());
        assertEquals(RocketShape.MAX_STAGES, shape.stages().size());
        assertEquals(RocketShape.MAX_NOZZLES, shape.stages().get(0).nozzles().size());
        assertEquals(RocketShape.Propellant.SOLID, shape.stages().get(0).propellant());
        assertEquals(RocketShape.MAX_HEIGHT, shape.payloadBottom());
        assertEquals(RocketShape.MAX_HEIGHT - 1, shape.stages().get(2).bottom());
    }

    @Test
    void anEmptyColumnIsTheOldMissile() {
        assertEquals(RocketShape.LEGACY, RocketShape.column(0, List.of()));
    }
}
