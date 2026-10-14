package org.papiricoh.create_nuclearindustry.controlroom.servo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Where a servo puts its actuator for a signal, and what its comparator reads back. */
class ServoPlanTest {

    @Test
    void theSignalIsAShareOfTheStroke() {
        assertEquals(0, ServoPlan.target(0, 8, 8, true), "no signal: retracted");
        assertEquals(8, ServoPlan.target(15, 8, 8, true), "full signal: the whole stroke");
        assertEquals(4, ServoPlan.target(8, 8, 8, true), "8/15 of 8 blocks rounds to 4");
        assertEquals(2, ServoPlan.target(8, 4, 4, true), "8/15 of 4 blocks rounds to 2");
        assertEquals(1, ServoPlan.target(1, 15, 15, true), "with a 15-block stroke every level is a block");
    }

    @Test
    void retractingWithTheSignalTurnsTheScaleOver() {
        assertEquals(8, ServoPlan.target(0, 8, 8, false), "no signal: the whole stroke");
        assertEquals(0, ServoPlan.target(15, 8, 8, false), "full signal: retracted");
        for (int signal = 0; signal <= 15; signal++) {
            assertEquals(ServoPlan.target(15 - signal, 8, 8, true), ServoPlan.target(signal, 8, 8, false),
                    "the retracting scale is the extending one upside down, at " + signal);
        }
    }

    @Test
    void theStrokeStopsWhereTheActuatorEnds() {
        assertEquals(4, ServoPlan.target(15, 10, 4, true), "a four-pole piston goes no further than 4");
        assertEquals(2, ServoPlan.target(15, 2, 4, true), "a shorter stroke stops short of the poles");
        assertEquals(0, ServoPlan.target(15, 8, 0, true), "an actuator with no reach stays put");
        assertEquals(0, ServoPlan.usableStroke(-3, 4), "the stroke is never negative");
    }

    @Test
    void outOfRangeSignalsAreClamped() {
        assertEquals(8, ServoPlan.target(40, 8, 8, true));
        assertEquals(0, ServoPlan.target(-2, 8, 8, true));
    }

    @Test
    void theTargetNeverDecreasesWithTheSignal() {
        for (int stroke = 1; stroke <= 16; stroke++) {
            int previous = -1;
            for (int signal = 0; signal <= 15; signal++) {
                int target = ServoPlan.target(signal, stroke, stroke, true);
                assertTrue(target >= previous, "stroke " + stroke + " signal " + signal);
                assertTrue(target >= 0 && target <= stroke, "inside the stroke");
                previous = target;
            }
        }
    }

    @Test
    void aComparatorReadsBackTheSignalThatPutItThere() {
        for (boolean extend : new boolean[]{true, false}) {
            for (int signal = 0; signal <= 15; signal++) {
                int target = ServoPlan.target(signal, 15, 15, extend);
                assertEquals(signal, ServoPlan.comparator(target, 15, 15, extend),
                        "a 15-block stroke reads back exactly, signal " + signal + (extend ? " extending" : " retracting"));
            }
        }
        assertEquals(15, ServoPlan.comparator(4, 4, 4, true), "at the end of the stroke");
        assertEquals(0, ServoPlan.comparator(0, 4, 4, true), "retracted");
        assertEquals(15, ServoPlan.comparator(0, 4, 4, false), "retracted, on the retracting scale");
        assertEquals(15, ServoPlan.comparator(9, 4, 4, true), "past the stroke still reads full");
        assertEquals(0, ServoPlan.comparator(3, 0, 4, true), "no stroke reads nothing");
    }

    @Test
    void movesGoWholeBlocks() {
        assertEquals(3, ServoPlan.stepsTo(1, 4));
        assertEquals(-2, ServoPlan.stepsTo(3, 1));
        assertEquals(0, ServoPlan.stepsTo(2.02f, 2), "a hair off a block is at it");
        assertTrue(ServoPlan.arrived(2.01f, 2));
        assertFalse(ServoPlan.arrived(1.5f, 2));
    }

    @Test
    void aMoveTheWrongWayIsNoticed() {
        assertFalse(ServoPlan.movingAway(1, 1.4f, 4, 0.25f), "closing in");
        assertFalse(ServoPlan.movingAway(1, 0.9f, 4, 0.25f), "within the slack");
        assertTrue(ServoPlan.movingAway(1, 0.5f, 4, 0.25f), "backing off");
        assertTrue(ServoPlan.movingAway(3, 3.5f, 1, 0.25f), "the other way round");
    }
}
