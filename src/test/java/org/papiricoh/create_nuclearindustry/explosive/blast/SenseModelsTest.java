package org.papiricoh.create_nuclearindustry.explosive.blast;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** What a detonation does to a witness's eyes and ears. */
class SenseModelsTest {

    @Test
    void deafnessStartsAtItsThresholdAndWorsensWithPressure() {
        assertEquals(-1, DeafeningModel.grade(6.9, 7));
        assertEquals(0, DeafeningModel.grade(7, 7));
        assertEquals(1, DeafeningModel.grade(14, 7));
        assertEquals(DeafeningModel.MAX_GRADE, DeafeningModel.grade(1000, 7));
        assertEquals(0, DeafeningModel.seconds(3, 7));
        assertTrue(DeafeningModel.seconds(28, 7) > DeafeningModel.seconds(7, 7));
        assertTrue(DeafeningModel.seconds(1.0e6, 7) <= DeafeningModel.MAX_SECONDS);
    }

    @Test
    void lookingStraightAtTheFireballDazzlesLongest() {
        double straight = FlashBlindnessModel.seconds(1000, 30, 1, false, false, 30);
        double aside = FlashBlindnessModel.seconds(1000, 30, 0.3, false, false, 30);
        double away = FlashBlindnessModel.seconds(1000, 30, -0.9, false, false, 30);
        assertTrue(straight > aside && aside > 0, straight + " " + aside);
        assertEquals(0, away, "the back turned sees nothing");
    }

    @Test
    void gogglesNightAndDistanceCount() {
        assertEquals(0, FlashBlindnessModel.seconds(200, 30, 1, false, true, 30), "goggles protect");
        assertTrue(FlashBlindnessModel.seconds(3000, 30, 0.5, true, false, 30) > FlashBlindnessModel.seconds(3000, 30, 0.5, false, false, 30),
                "dark-adapted eyes suffer more");
        assertTrue(FlashBlindnessModel.seconds(500, 30, 1, false, false, 300) > FlashBlindnessModel.seconds(4000, 30, 1, false, false, 300));
        assertTrue(FlashBlindnessModel.seconds(10, 90, 1, true, false, 30) <= 30, "never past the cap");
    }

    @Test
    void theGradeFollowsTheShareOfTheCap() {
        assertEquals(-1, FlashBlindnessModel.grade(0, 30));
        assertEquals(0, FlashBlindnessModel.grade(3, 30));
        assertEquals(FlashBlindnessModel.MAX_GRADE, FlashBlindnessModel.grade(30, 30));
    }
}
