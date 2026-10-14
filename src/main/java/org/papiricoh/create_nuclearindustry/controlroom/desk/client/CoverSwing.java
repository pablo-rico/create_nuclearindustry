package org.papiricoh.create_nuclearindustry.controlroom.desk.client;

import org.papiricoh.create_nuclearindustry.controlroom.desk.ControlDeskBlockEntity;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Client side: swings a guarded button's cover between shut and open over a few ticks instead of
 * snapping, remembering each cover's angle by block entity and slot.
 */
final class CoverSwing {
    static final float OPEN = 95;
    /** Degrees per tick: open or shut in about five ticks. */
    private static final float SPEED = 19;
    private static final Map<ControlDeskBlockEntity, float[]> STATE = new WeakHashMap<>();

    private CoverSwing() {}

    /** The cover's angle now, moving towards open or shut since the last frame; {@code time} in ticks. */
    static float angle(ControlDeskBlockEntity desk, int slot, boolean open, float time) {
        float[] state = STATE.computeIfAbsent(desk, key -> new float[ControlDeskBlockEntity.SLOTS * 2]);
        float target = open ? OPEN : 0;
        float last = state[slot * 2 + 1];
        float elapsed = last == 0 ? Float.MAX_VALUE : Math.max(0, time - last);
        state[slot * 2 + 1] = time;
        float angle = state[slot * 2];
        float step = Math.min(Math.abs(target - angle), elapsed * SPEED);
        angle += Math.signum(target - angle) * step;
        state[slot * 2] = angle;
        return angle;
    }
}
