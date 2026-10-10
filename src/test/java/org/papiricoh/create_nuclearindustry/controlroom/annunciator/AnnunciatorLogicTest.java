package org.papiricoh.create_nuclearindustry.controlroom.annunciator;

import org.junit.jupiter.api.Test;
import org.papiricoh.create_nuclearindustry.controlroom.annunciator.AnnunciatorLogic.State;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnnunciatorLogicTest {

    private static boolean[] trips(int windows, int... tripped) {
        boolean[] now = new boolean[windows];
        for (int window : tripped) {
            now[window] = true;
        }
        return now;
    }

    @Test
    void fullSequenceAlertAcknowledgeRingbackReset() {
        AnnunciatorLogic logic = new AnnunciatorLogic(6);
        int flags = logic.update(trips(6, 2));
        assertTrue((flags & AnnunciatorLogic.NEW_ALERT) != 0);
        assertEquals(State.ALERT, logic.state(2));
        assertTrue(logic.unacknowledged(), "the horn sounds");

        assertTrue(logic.acknowledge());
        assertEquals(State.ACKNOWLEDGED, logic.state(2), "acknowledged while still tripped: steady");
        assertFalse(logic.unacknowledged(), "horn off");

        assertFalse(logic.reset(), "reset does nothing while the condition persists");
        assertEquals(State.ACKNOWLEDGED, logic.state(2));

        flags = logic.update(trips(6));
        assertTrue((flags & AnnunciatorLogic.RINGBACK) != 0);
        assertEquals(State.RINGBACK, logic.state(2), "cleared after acknowledging: ringback");

        assertTrue(logic.reset());
        assertEquals(State.NORMAL, logic.state(2));
        assertEquals(-1, logic.firstOut());
        assertEquals(0, logic.active());
    }

    @Test
    void anAlertLocksInUntilAcknowledged() {
        AnnunciatorLogic logic = new AnnunciatorLogic(6);
        logic.update(trips(6, 0));
        logic.update(trips(6));
        assertEquals(State.ALERT, logic.state(0), "a short upset keeps flashing until acknowledged");
        assertTrue(logic.acknowledge());
        assertEquals(State.RINGBACK, logic.state(0), "acknowledged once clear: straight to ringback");
    }

    @Test
    void ringbackThatTripsAgainAlertsAgain() {
        AnnunciatorLogic logic = new AnnunciatorLogic(6);
        logic.update(trips(6, 4));
        logic.acknowledge();
        logic.update(trips(6));
        assertEquals(State.RINGBACK, logic.state(4));
        int flags = logic.update(trips(6, 4));
        assertTrue((flags & AnnunciatorLogic.NEW_ALERT) != 0);
        assertEquals(State.ALERT, logic.state(4));
    }

    @Test
    void theFirstWindowToTripIsTheFirstOut() {
        AnnunciatorLogic logic = new AnnunciatorLogic(6);
        logic.update(trips(6, 3));
        logic.update(trips(6, 1, 3, 5));
        assertEquals(3, logic.firstOut(), "later alarms of the same burst are not first-out");
        logic.acknowledge();
        logic.update(trips(6));
        logic.reset();
        assertEquals(-1, logic.firstOut(), "cleared once its window is back to normal");
        logic.update(trips(6, 5, 1));
        assertEquals(1, logic.firstOut(), "windows tripping together: the lowest is first-out");
    }

    @Test
    void steadyConditionsChangeNothing() {
        AnnunciatorLogic logic = new AnnunciatorLogic(2);
        assertEquals(0, logic.update(trips(2)));
        logic.update(trips(2, 0));
        assertEquals(0, logic.update(trips(2, 0)), "a window already alerting stays put");
        assertFalse(logic.reset(), "nothing to reset");
    }

    @Test
    void flashesFastSteadyAndSlow() {
        assertFalse(AnnunciatorLogic.lit(State.NORMAL, 0));
        assertTrue(AnnunciatorLogic.lit(State.ACKNOWLEDGED, 5));
        assertTrue(AnnunciatorLogic.lit(State.ALERT, 0));
        assertFalse(AnnunciatorLogic.lit(State.ALERT, AnnunciatorLogic.FAST_FLASH));
        assertTrue(AnnunciatorLogic.lit(State.RINGBACK, AnnunciatorLogic.FAST_FLASH), "the slow flash is still on");
        assertFalse(AnnunciatorLogic.lit(State.RINGBACK, AnnunciatorLogic.SLOW_FLASH));
    }

    @Test
    void saveAndLoadKeepStatesAndFirstOut() {
        AnnunciatorLogic logic = new AnnunciatorLogic(6);
        logic.update(trips(6, 2, 4));
        logic.acknowledge();
        logic.update(trips(6, 2));
        AnnunciatorLogic copy = new AnnunciatorLogic(6);
        copy.load(logic.saveStates(), logic.firstOut());
        for (int i = 0; i < 6; i++) {
            assertEquals(logic.state(i), copy.state(i));
            assertEquals(logic.tripped(i), copy.tripped(i));
        }
        assertEquals(2, copy.firstOut());
        AnnunciatorLogic shortSave = new AnnunciatorLogic(6);
        shortSave.load(new int[]{1}, 5);
        assertEquals(State.ALERT, shortSave.state(0));
        assertEquals(State.NORMAL, shortSave.state(5));
        assertEquals(-1, shortSave.firstOut(), "a first-out on a normal window is dropped");
    }
}
