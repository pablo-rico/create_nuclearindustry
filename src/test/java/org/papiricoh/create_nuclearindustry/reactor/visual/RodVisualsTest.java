package org.papiricoh.create_nuclearindustry.reactor.visual;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RodVisualsTest {

    private record P(int x, int y, int z) {}

    @Test
    void channelsFillColumnByColumnFromTheBottom() {
        List<P> rods = List.of(new P(3, 2, 3), new P(1, 1, 1), new P(3, 1, 3), new P(1, 2, 1), new P(1, 1, 3), new P(2, 1, 1));
        List<P> ordered = RodVisuals.order(rods, P::x, P::y, P::z);
        assertEquals(List.of(new P(1, 1, 1), new P(1, 2, 1), new P(1, 1, 3), new P(2, 1, 1), new P(3, 1, 3), new P(3, 2, 3)), ordered);
    }

    @Test
    void fuelDarkensWithBurnup() {
        assertEquals(RodVisuals.FRESH, RodVisuals.fuel(0.0));
        assertEquals(RodVisuals.FRESH, RodVisuals.fuel(0.32));
        assertEquals(RodVisuals.HALF, RodVisuals.fuel(0.33));
        assertEquals(RodVisuals.HALF, RodVisuals.fuel(0.74));
        assertEquals(RodVisuals.SPENT, RodVisuals.fuel(0.75));
        assertEquals(RodVisuals.SPENT, RodVisuals.fuel(1.0));
    }

    @Test
    void assembliesMapToChannelsInOrderAndTheRestAreEmpty() {
        List<RodVisuals.Look> looks = RodVisuals.looks(5, List.of(0.9, 0.5, 0.0), 2, true);
        assertEquals(5, looks.size());
        assertEquals(RodVisuals.SPENT, looks.get(0).fuel(), "the burning assembly is the first channel");
        assertEquals(RodVisuals.HALF, looks.get(1).fuel());
        assertEquals(RodVisuals.FRESH, looks.get(2).fuel());
        assertEquals(RodVisuals.EMPTY, looks.get(3).fuel());
        assertEquals(RodVisuals.EMPTY, looks.get(4).fuel());
        assertTrue(looks.stream().allMatch(look -> look.glow() == 2 && look.coolant()), "glow and coolant are the core's");
        assertTrue(RodVisuals.looks(2, List.of(0.1, 0.1, 0.1), 0, false).size() == 2, "never more looks than channels");
    }

    @Test
    void glowRisesWithPowerAndHoldsOnTheWayDown() {
        assertEquals(0, RodVisuals.glow(0, 0.0));
        assertEquals(1, RodVisuals.glow(0, 0.05));
        assertEquals(2, RodVisuals.glow(0, 0.4));
        assertEquals(3, RodVisuals.glow(0, 0.95));
        assertEquals(3, RodVisuals.glow(3, 0.68), "a little below the step keeps the glow");
        assertEquals(2, RodVisuals.glow(3, 0.6), "well below drops a step");
        assertEquals(0, RodVisuals.glow(3, 0.0), "a shut-down core goes dark at once");
        assertEquals(1, RodVisuals.glow(1, 0.015), "the first step holds just under its threshold");
        assertEquals(0, RodVisuals.glow(1, 0.005));
        assertEquals(1, RodVisuals.glow(2, 0.2), "drops only the steps the power fell below");
    }
}
