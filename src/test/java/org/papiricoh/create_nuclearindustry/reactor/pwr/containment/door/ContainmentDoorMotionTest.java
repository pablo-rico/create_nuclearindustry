package org.papiricoh.create_nuclearindustry.reactor.pwr.containment.door;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContainmentDoorMotionTest {

    @Test
    void shutDoorSitsInItsFrame() {
        assertEquals(0, ContainmentDoorMotion.pull(0), 1e-6);
        assertEquals(0, ContainmentDoorMotion.slide(0), 1e-6);
    }

    @Test
    void itPullsOutBeforeItSlides() {
        float pulled = ContainmentDoorMotion.PULL_SHARE;
        assertEquals(ContainmentDoorMotion.PULL_DEPTH, ContainmentDoorMotion.pull(pulled), 1e-6, "fully out of the frame");
        assertEquals(0, ContainmentDoorMotion.slide(pulled), 1e-6, "and only then starts sliding");
        assertTrue(ContainmentDoorMotion.pull(pulled / 2) > 0, "pulling out takes part of the stroke");
    }

    @Test
    void openDoorHasClearedTheDoorway() {
        assertEquals(ContainmentDoorMotion.SLIDE_DISTANCE, ContainmentDoorMotion.slide(1), 1e-6);
        assertEquals(1.0f, ContainmentDoorMotion.SLIDE_DISTANCE + ContainmentDoorMotion.EDGE, 1e-6,
                "all but its edge, into the wall beside it");
        assertTrue(ContainmentDoorMotion.SLIDE_DISTANCE > 0.6f + 0.1f, "a player (0.6 wide) walks past the edge");
        assertEquals(ContainmentDoorMotion.PULL_DEPTH, ContainmentDoorMotion.pull(1), 1e-6, "still pulled out");
    }

    @Test
    void theOpenEdgeIsWhereTheDoorWasDrawn() {
        // Slid towards +: the edge is the door's trailing side, at the far end of its own block.
        float[] across = ContainmentDoorMotion.edgeAcross(1);
        assertEquals(ContainmentDoorMotion.SLIDE_DISTANCE, across[0], 1e-6);
        assertEquals(1, across[1], 1e-6);
        float[] back = ContainmentDoorMotion.edgeAcross(-1);
        assertEquals(0, back[0], 1e-6);
        assertEquals(ContainmentDoorMotion.EDGE, back[1], 1e-6);
        // Pulled towards -: it sticks out of the wall by the pull and is that much short of the back.
        float[] through = ContainmentDoorMotion.edgeThrough(-1);
        assertEquals(-ContainmentDoorMotion.PULL_DEPTH, through[0], 1e-6);
        assertEquals(1 - ContainmentDoorMotion.PULL_DEPTH, through[1], 1e-6);
        assertEquals(1, through[1] - through[0], 1e-6, "a whole block thick");
    }

    @Test
    void theDrawnDoorHidesInsideTheWallBesideIt() {
        assertTrue(ContainmentDoorMotion.INSET > 0 && ContainmentDoorMotion.INSET <= 1 / 128f,
                "a hair smaller than a block: behind the wall's faces, too little to see");
    }

    @Test
    void movementIsSmoothAndNeverBacktracks() {
        float lastPull = 0;
        float lastSlide = 0;
        for (int step = 0; step <= 100; step++) {
            float progress = step / 100f;
            float pull = ContainmentDoorMotion.pull(progress);
            float slide = ContainmentDoorMotion.slide(progress);
            assertTrue(pull >= lastPull - 1e-6 && slide >= lastSlide - 1e-6, "monotonic at " + progress);
            assertTrue(slide - lastSlide < 0.05f, "no jumps at " + progress);
            lastPull = pull;
            lastSlide = slide;
        }
        assertTrue(ContainmentDoorMotion.slide(0.31f) < 0.01f, "it eases into the slide");
    }

    @Test
    void aStrokeTakesAboutASecond() {
        assertEquals(1.0f / ContainmentDoorMotion.STROKE_TICKS, ContainmentDoorMotion.speed(), 1e-6);
        assertTrue(ContainmentDoorMotion.STROKE_TICKS >= 15 && ContainmentDoorMotion.STROKE_TICKS <= 40);
    }
}
