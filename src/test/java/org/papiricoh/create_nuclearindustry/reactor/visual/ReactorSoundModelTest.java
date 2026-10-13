package org.papiricoh.create_nuclearindustry.reactor.visual;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReactorSoundModelTest {

    /** The sound engine clamps pitch to 0.5..2 and volume to 0..1: everything must stay inside. */
    private static void inRange(float volume, float pitch) {
        assertTrue(volume > 0 && volume <= 1, "volume " + volume);
        assertTrue(pitch >= 0.5f && pitch <= 2.0f, "pitch " + pitch);
    }

    @Test
    void humFollowsPower() {
        assertFalse(ReactorSoundModel.humWanted(true, 0.0), "a shut-down core is silent");
        assertFalse(ReactorSoundModel.humWanted(false, 0.5), "an unformed one too");
        assertTrue(ReactorSoundModel.humWanted(true, 0.01));
        assertTrue(ReactorSoundModel.humVolume(0.9) > ReactorSoundModel.humVolume(0.1));
        assertTrue(ReactorSoundModel.humPitch(0.9) > ReactorSoundModel.humPitch(0.1));
        for (double f = -1; f <= 2; f += 0.1) {
            inRange(ReactorSoundModel.humVolume(f), ReactorSoundModel.humPitch(f));
        }
        assertEquals(1.0, ReactorSoundModel.powerFraction(5000, 1000), 1e-9, "clamped at full power");
        assertEquals(0.5, ReactorSoundModel.powerFraction(500, 1000), 1e-9, "a share of the core's rated power");
        assertEquals(0.5, ReactorSoundModel.powerFraction(6000, 12000), 1e-9, "whatever its size");
        assertEquals(0.0, ReactorSoundModel.powerFraction(Double.NaN, 1000), 1e-9);
        assertEquals(0.0, ReactorSoundModel.powerFraction(500, 0), 1e-9, "no rating, no share");
    }

    @Test
    void klaxonUntilColdAlarmInTheWarningZone() {
        assertTrue(ReactorSoundModel.klaxonWanted(true, 600));
        assertFalse(ReactorSoundModel.klaxonWanted(true, 60), "a cold core is quiet");
        assertFalse(ReactorSoundModel.klaxonWanted(false, 600), "no SCRAM, no klaxon");
        assertFalse(ReactorSoundModel.alarmWanted(true, 1400, 1500));
        assertTrue(ReactorSoundModel.alarmWanted(true, 2500, 1500));
        assertTrue(ReactorSoundModel.alarmPitch(2700, 1500, 2800) > ReactorSoundModel.alarmPitch(1600, 1500, 2800), "more urgent near meltdown");
        for (double t = 0; t <= 6000; t += 250) {
            inRange(ReactorSoundModel.alarmVolume(t, 1500, 2800), ReactorSoundModel.alarmPitch(t, 1500, 2800));
        }
    }

    @Test
    void steamAndMachinesStayInTheEngineRange() {
        assertFalse(ReactorSoundModel.flowWanted(0));
        assertTrue(ReactorSoundModel.flowWanted(20));
        for (double flow = 0; flow <= 400; flow += 20) {
            inRange(ReactorSoundModel.hissVolume(flow), ReactorSoundModel.hissPitch(flow));
            inRange(ReactorSoundModel.bubblingVolume(flow), ReactorSoundModel.bubblingPitch(flow));
        }
        for (double rpm = -512; rpm <= 512; rpm += 32) {
            inRange(ReactorSoundModel.centrifugeVolume(rpm), ReactorSoundModel.centrifugePitch(rpm));
            inRange(ReactorSoundModel.electrolyzerVolume(rpm), ReactorSoundModel.electrolyzerPitch(rpm));
        }
        assertTrue(ReactorSoundModel.centrifugePitch(256) > ReactorSoundModel.centrifugePitch(64), "spins up");
        assertEquals(ReactorSoundModel.centrifugePitch(-128), ReactorSoundModel.centrifugePitch(128), "either direction");
        inRange(ReactorSoundModel.melterVolume(true), ReactorSoundModel.melterPitch(true));
        inRange(ReactorSoundModel.melterVolume(false), ReactorSoundModel.melterPitch(false));
        inRange(ReactorSoundModel.rtgVolume(1), ReactorSoundModel.rtgPitch(1));
        inRange(ReactorSoundModel.rtgVolume(0), ReactorSoundModel.rtgPitch(0));
        inRange(ReactorSoundModel.RELIEF_VOLUME, ReactorSoundModel.RELIEF_PITCH);
    }
}
