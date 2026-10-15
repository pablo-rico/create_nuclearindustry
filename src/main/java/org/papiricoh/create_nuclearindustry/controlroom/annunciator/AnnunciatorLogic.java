package org.papiricoh.create_nuclearindustry.controlroom.annunciator;

import java.util.Arrays;

/**
 * The alarm sequence of an annunciator panel, after ISA-18.1 sequence R (manual reset with ringback):
 * <ul>
 *     <li>NORMAL: the window is dark.</li>
 *     <li>ALERT: the condition tripped. The window flashes fast and the horn sounds. It stays in
 *     ALERT even if the condition clears (lock-in), so a short upset is never missed.</li>
 *     <li>ACKNOWLEDGED: the operator acknowledged while the condition persists. Steady light, horn off.</li>
 *     <li>RINGBACK: the condition cleared after it was acknowledged (or was acknowledged once already
 *     clear). Slow flash until the operator resets it.</li>
 * </ul>
 * A RINGBACK window that trips again goes back to ALERT; RESET only clears windows in RINGBACK.
 * The first window to trip while no first-out is marked becomes the first-out, the likely cause of a
 * burst of alarms, until it is back to NORMAL.
 * <p>
 * A window that does not latch (a status window, see {@link AnnunciatorSeverity}) skips the sequence:
 * it is ACKNOWLEDGED (steady) while its condition holds and NORMAL once it clears, never sounds and is
 * never the first-out. No game classes, so it can be tested on its own.
 */
public final class AnnunciatorLogic {
    /** {@link #update} flags. */
    public static final int CHANGED = 1, NEW_ALERT = 2, RINGBACK = 4;
    /** Ticks per half-period of the fast and slow flashes. */
    public static final int FAST_FLASH = 4, SLOW_FLASH = 16;

    public enum State {
        NORMAL, ALERT, ACKNOWLEDGED, RINGBACK;

        static State byOrdinal(int ordinal) {
            State[] values = values();
            return ordinal >= 0 && ordinal < values.length ? values[ordinal] : NORMAL;
        }
    }

    private final State[] states;
    private final boolean[] tripped;
    private int firstOut = -1;
    private int lastNewAlert = -1;

    public AnnunciatorLogic(int windows) {
        states = new State[windows];
        tripped = new boolean[windows];
        Arrays.fill(states, State.NORMAL);
    }

    public int windows() {
        return states.length;
    }

    public State state(int window) {
        return states[window];
    }

    /** Whether the condition of {@code window} was tripped at the last update. */
    public boolean tripped(int window) {
        return tripped[window];
    }

    /** The first-out window, or -1. */
    public int firstOut() {
        return firstOut;
    }

    /** The first window that went into ALERT at the last update, or -1. */
    public int lastNewAlert() {
        return lastNewAlert;
    }

    /** Whether any window waits for an acknowledgement: the horn sounds and the panel's output is on. */
    public boolean unacknowledged() {
        for (State state : states) {
            if (state == State.ALERT) {
                return true;
            }
        }
        return false;
    }

    /** Windows that are not NORMAL. */
    public int active() {
        int count = 0;
        for (State state : states) {
            if (state != State.NORMAL) {
                count++;
            }
        }
        return count;
    }

    /**
     * Feeds the conditions now: {@code now[i]} is whether window i's condition is tripped. Returns
     * {@link #CHANGED}, {@link #NEW_ALERT} and {@link #RINGBACK} flags for what happened.
     */
    public int update(boolean[] now) {
        return update(now, null);
    }

    /**
     * As {@link #update(boolean[])}, with {@code latching[i]} false for a window that only shows its
     * condition; null means every window latches.
     */
    public int update(boolean[] now, boolean[] latching) {
        int result = 0;
        int firstNew = -1;
        for (int i = 0; i < states.length; i++) {
            boolean trip = i < now.length && now[i];
            tripped[i] = trip;
            State before = states[i];
            State after = before;
            if (latching != null && i < latching.length && !latching[i]) {
                after = trip ? State.ACKNOWLEDGED : State.NORMAL;
                if (firstOut == i) {
                    // A window that was the first-out before it stopped latching.
                    firstOut = -1;
                    result |= CHANGED;
                }
            } else if (trip && (before == State.NORMAL || before == State.RINGBACK)) {
                after = State.ALERT;
                if (firstNew < 0) {
                    firstNew = i;
                }
                result |= NEW_ALERT;
            } else if (!trip && before == State.ACKNOWLEDGED) {
                after = State.RINGBACK;
                result |= RINGBACK;
            }
            if (after != before) {
                states[i] = after;
                result |= CHANGED;
            }
        }
        lastNewAlert = firstNew;
        if (firstOut < 0 && firstNew >= 0) {
            firstOut = firstNew;
            result |= CHANGED;
        }
        return result;
    }

    /** The operator acknowledges: alerts go steady, or to ringback if already clear. Returns whether anything changed. */
    public boolean acknowledge() {
        boolean changed = false;
        for (int i = 0; i < states.length; i++) {
            if (states[i] == State.ALERT) {
                states[i] = tripped[i] ? State.ACKNOWLEDGED : State.RINGBACK;
                changed = true;
            }
        }
        return changed;
    }

    /** The operator resets: windows in ringback go dark. Returns whether anything changed. */
    public boolean reset() {
        boolean changed = false;
        for (int i = 0; i < states.length; i++) {
            if (states[i] == State.RINGBACK) {
                states[i] = State.NORMAL;
                changed = true;
                if (firstOut == i) {
                    firstOut = -1;
                }
            }
        }
        return changed;
    }

    /** Whether a window in {@code state} shows lit at {@code time} ticks: fast flash, steady or slow flash. */
    public static boolean lit(State state, long time) {
        return switch (state) {
            case NORMAL -> false;
            case ALERT -> (time / FAST_FLASH) % 2 == 0;
            case ACKNOWLEDGED -> true;
            case RINGBACK -> (time / SLOW_FLASH) % 2 == 0;
        };
    }

    // ------------------------------------------------------------------ persistence

    public int[] saveStates() {
        int[] out = new int[states.length];
        for (int i = 0; i < states.length; i++) {
            out[i] = states[i].ordinal() | (tripped[i] ? 0x100 : 0);
        }
        return out;
    }

    /** Restores what {@link #saveStates} wrote; missing windows are NORMAL. */
    public void load(int[] saved, int savedFirstOut) {
        for (int i = 0; i < states.length; i++) {
            int value = i < saved.length ? saved[i] : 0;
            states[i] = State.byOrdinal(value & 0xFF);
            tripped[i] = (value & 0x100) != 0;
        }
        firstOut = savedFirstOut >= 0 && savedFirstOut < states.length && states[savedFirstOut] != State.NORMAL ? savedFirstOut : -1;
    }
}
