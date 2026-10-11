package org.papiricoh.create_nuclearindustry.network;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PayloadVerdictTest {
    private static final BooleanSupplier YES = () -> true;
    private static final BooleanSupplier NO = () -> false;

    @Test
    void aPlayerWhoMayUseTheBlockIsLetThrough() {
        assertEquals(PayloadVerdict.OK, PayloadVerdict.decide(true, false, true, true, YES, YES, YES));
    }

    @Test
    void eachCheckTurnsTheRequestAway() {
        assertEquals(PayloadVerdict.NOT_PLAYING, PayloadVerdict.decide(false, false, true, true, YES, YES, YES), "dead");
        assertEquals(PayloadVerdict.NOT_PLAYING, PayloadVerdict.decide(true, true, true, true, YES, YES, YES), "spectator");
        assertEquals(PayloadVerdict.UNKNOWN_BLOCK, PayloadVerdict.decide(true, false, false, true, YES, YES, YES), "another level");
        assertEquals(PayloadVerdict.UNKNOWN_BLOCK, PayloadVerdict.decide(true, false, true, false, YES, YES, YES), "unloaded");
        assertEquals(PayloadVerdict.OUT_OF_REACH, PayloadVerdict.decide(true, false, true, true, NO, YES, YES), "too far");
        assertEquals(PayloadVerdict.PROTECTED, PayloadVerdict.decide(true, false, true, true, YES, NO, YES), "spawn protection");
        assertEquals(PayloadVerdict.MAY_NOT_BUILD, PayloadVerdict.decide(true, false, true, true, YES, YES, NO), "adventure mode");
    }

    @Test
    void theCostlierChecksAreOnlyAskedOnceTheCheapOnesPass() {
        AtomicInteger asked = new AtomicInteger();
        BooleanSupplier counted = () -> {
            asked.incrementAndGet();
            return true;
        };
        PayloadVerdict.decide(true, true, true, true, counted, counted, counted);
        PayloadVerdict.decide(true, false, true, false, counted, counted, counted);
        assertEquals(0, asked.get(), "a spectator or an unloaded block needs no distance or protection check");
        PayloadVerdict.decide(true, false, true, true, NO, counted, counted);
        assertEquals(0, asked.get(), "out of reach needs no protection check");
    }
}
