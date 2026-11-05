package org.papiricoh.create_nuclearindustry.explosive.fx.model;

import org.junit.jupiter.api.Test;
import org.papiricoh.create_nuclearindustry.explosive.blast.ShockFront;
import org.papiricoh.create_nuclearindustry.explosive.conventional.ConventionalKind;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Sounds arrive with the shock front close by and at the speed of sound past it; late listeners hear only what is still coming. */
class ExplosionAudioTest {

    private static ExplosionAudio.Cue find(List<ExplosionAudio.Cue> cues, ExplosionAudio.Sound sound) {
        return cues.stream().filter(c -> c.sound() == sound).findFirst().orElse(null);
    }

    @Test
    void closeByTheCrackAndBoomRideTheShock() {
        List<ExplosionAudio.Cue> cues = ExplosionAudio.nuclear(300, 8, 768, 1e6, 60, true, null);
        ExplosionAudio.Cue crack = find(cues, ExplosionAudio.Sound.NUKE_CRACK);
        ExplosionAudio.Cue boom = find(cues, ExplosionAudio.Sound.NUKE_BOOM);
        ExplosionAudio.Cue seismic = find(cues, ExplosionAudio.Sound.SEISMIC_RUMBLE);
        assertTrue(crack != null && boom != null && seismic != null);
        assertEquals(ShockFront.arrivalTicks(300, 8, 768), boom.tick(), 1e-9);
        assertTrue(seismic.tick() < boom.tick(), "the ground first");
        assertTrue(find(cues, ExplosionAudio.Sound.NUKE_RUMBLE).tick() > boom.tick());
        assertTrue(find(cues, ExplosionAudio.Sound.NUKE_RETURN_WIND).tick() > boom.tick(), "then the wind rushing back");
        assertTrue(find(cues, ExplosionAudio.Sound.NUKE_RUMBLE_FAR) == null);
    }

    @Test
    void farOffOnlyTheRollAtTheSpeedOfSound() {
        List<ExplosionAudio.Cue> cues = ExplosionAudio.nuclear(3000, 8, 768, 1e6, 60, true, null);
        assertTrue(find(cues, ExplosionAudio.Sound.NUKE_CRACK) == null);
        ExplosionAudio.Cue roll = find(cues, ExplosionAudio.Sound.NUKE_RUMBLE_FAR);
        assertEquals(768 / 8.0 + (3000 - 768) / ShockFront.SOUND_SPEED, roll.tick(), 1e-6);
        ExplosionAudio.Cue near = find(ExplosionAudio.nuclear(1000, 8, 768, 1e6, 60, true, null), ExplosionAudio.Sound.NUKE_RUMBLE_FAR);
        assertTrue(near.volume() > roll.volume(), "louder nearer");
    }

    @Test
    void aLateListenerHearsNoBoomReplayed() {
        ExplosionAudio.Cue boom = find(ExplosionAudio.nuclear(300, 8, 768, 1e6, 60, false, null), ExplosionAudio.Sound.NUKE_BOOM);
        assertTrue(ExplosionAudio.due(boom, boom.tick() + 1));
        assertFalse(ExplosionAudio.due(boom, boom.tick() - 1), "not yet");
        assertTrue(ExplosionAudio.stale(boom, 2400), "two minutes on");
    }

    @Test
    void lightningIsHeardLate() {
        List<ExplosionAudio.Cue> cues = ExplosionAudio.nuclear(340, 8, 768, 1e6, 60, false, new double[]{40, 80});
        List<ExplosionAudio.Cue> bolts = cues.stream().filter(c -> c.sound() == ExplosionAudio.Sound.CLOUD_LIGHTNING).toList();
        assertEquals(2, bolts.size());
        assertEquals(40 + 340 / ShockFront.SOUND_SPEED, bolts.get(0).tick(), 1e-6);
        assertTrue(bolts.get(0).id() != bolts.get(1).id());
    }

    @Test
    void conventionalBySizeAndDistance() {
        assertEquals(ExplosionAudio.Sound.EXPLOSION_SMALL, ExplosionAudio.size(1));
        assertEquals(ExplosionAudio.Sound.EXPLOSION_MEDIUM, ExplosionAudio.size(24));
        assertEquals(ExplosionAudio.Sound.EXPLOSION_LARGE, ExplosionAudio.size(192));
        List<ExplosionAudio.Cue> near = ExplosionAudio.conventional(30, ConventionalKind.HIGH_EXPLOSIVE, 192, 8, 64, false);
        assertEquals(ExplosionAudio.Sound.EXPLOSION_LARGE, near.get(0).sound());
        assertTrue(find(near, ExplosionAudio.Sound.DEBRIS_RAIN) != null, "debris patters down after a ground burst");
        List<ExplosionAudio.Cue> far = ExplosionAudio.conventional(800, ConventionalKind.HIGH_EXPLOSIVE, 192, 8, 64, false);
        assertEquals(ExplosionAudio.Sound.EXPLOSION_DISTANT, far.get(0).sound());
        assertTrue(far.get(0).tick() > near.get(0).tick());
        assertEquals(ExplosionAudio.Sound.THERMOBARIC_BLAST,
                ExplosionAudio.conventional(30, ConventionalKind.THERMOBARIC, 16, 8, 64, true).get(0).sound());
        assertTrue(ExplosionAudio.conventional(800, ConventionalKind.BOMBLET, 1.5, 8, 10, true).isEmpty(), "far bomblets crackle together");
        assertEquals(ExplosionAudio.conventional(30, ConventionalKind.DIRTY, 24, 8, 30, false).get(0).sound(),
                ExplosionAudio.conventional(30, ConventionalKind.HIGH_EXPLOSIVE, 24, 8, 30, false).get(0).sound(), "a dirty bomb sounds like HE");
    }

    @Test
    void manyBombletsCrackleTogether() {
        ExplosionAudio.CrackleWindow window = new ExplosionAudio.CrackleWindow();
        assertFalse(window.hear(0));
        assertFalse(window.hear(200), "far apart in time");
        assertFalse(window.hear(210));
        assertFalse(window.hear(215));
        assertTrue(window.hear(220), "four within three seconds");
        assertFalse(window.hear(225), "not again so soon");
        assertFalse(window.hear(300));
        assertFalse(window.hear(302));
        assertFalse(window.hear(304), "the old ones fell out of the window");
        assertTrue(window.hear(306), "a second volley");
    }
}
