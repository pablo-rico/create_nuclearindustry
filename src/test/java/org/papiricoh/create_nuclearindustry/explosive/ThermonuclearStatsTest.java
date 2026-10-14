package org.papiricoh.create_nuclearindustry.explosive;

import org.junit.jupiter.api.Test;
import org.papiricoh.create_nuclearindustry.explosive.ThermonuclearStats.Caps;
import org.papiricoh.create_nuclearindustry.explosive.ThermonuclearStats.Primary;
import org.papiricoh.create_nuclearindustry.explosive.ThermonuclearStats.Result;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A Teller-Ulam warhead's yield from its primary, its secondary and its tamper. */
class ThermonuclearStatsTest {
    private static final Caps CAPS = Caps.DEFAULT;

    private static Result pu(int secondary, int tamper) {
        return ThermonuclearStats.of(Primary.PLUTONIUM, secondary, tamper, CAPS);
    }

    @Test
    void withoutAPrimaryNothingGoesOff() {
        assertSame(Result.NONE, ThermonuclearStats.of(Primary.NONE, 16, 4, CAPS));
        assertFalse(Result.NONE.live());
    }

    @Test
    void aPrimaryAloneFizzlesAsOneCore() {
        Result fizzle = pu(0, 4);
        assertTrue(fizzle.fizzle() && fizzle.live());
        assertEquals(WarheadStats.horizontalRadius(8, 90), fizzle.radius());
        assertEquals(WarheadStats.power(8, 90), fizzle.power(), 1e-6);
        assertEquals(0, fizzle.hotSpot(), "the tamper needs fusion neutrons to split");
    }

    @Test
    void moreDeuterideMeansABiggerBlast() {
        int last = 0;
        for (int d = 1; d <= ThermonuclearStats.SECONDARY_FULL; d++) {
            Result r = pu(d, 0);
            assertTrue(r.radius() > last, "D=" + d + " gives " + r.radius());
            last = r.radius();
        }
        assertTrue(pu(1, 0).radius() > WarheadStats.horizontalRadius(48, 90), "even one deuteride beats a full atomic bomb");
        assertEquals(pu(16, 0).radius(), pu(30, 0).radius(), "past a full secondary there is no room for more");
    }

    @Test
    void theTamperAddsYieldAndMuchMoreFallout() {
        Result bare = pu(8, 0);
        Result tamped = pu(8, 4);
        assertTrue(tamped.radius() > bare.radius());
        assertTrue(tamped.power() > bare.power());
        assertTrue(tamped.falloutRadius() > bare.falloutRadius() * 1.3, bare.falloutRadius() + " -> " + tamped.falloutRadius());
        assertEquals(0, bare.hotSpot());
        assertEquals(CAPS.tamperHotSpot(), tamped.hotSpot(), 1e-6);
        assertTrue(tamped.intensity() > bare.intensity());
    }

    @Test
    void aFullBombReachesThreeTimesTheAtomicRadius() {
        Result full = pu(16, 4);
        assertEquals(750, full.radius());
        assertEquals(4320, full.power(), 1e-3);
        assertEquals(3 * WarheadStats.horizontalRadius(48, 90), full.radius());
        assertEquals(CAPS.maxCrater(), full.maxCrater());
    }

    @Test
    void plutoniumCompressesBetterThanUranium() {
        assertTrue(pu(16, 4).power() > ThermonuclearStats.of(Primary.URANIUM, 16, 4, CAPS).power());
        assertTrue(pu(8, 0).radius() > ThermonuclearStats.of(Primary.URANIUM, 8, 0, CAPS).radius());
    }

    @Test
    void theRadiusFollowsTheCubeRootOfTheYield() {
        Caps open = new Caps(100_000, 1.0e9, 110, 0.5, 150);
        for (int d : new int[]{1, 4, 9, 16}) {
            Result r = ThermonuclearStats.of(Primary.PLUTONIUM, d, 2, open);
            double expected = 160 * Math.pow(r.radius() / 250.0, 3);
            assertEquals(expected, r.power(), expected * 0.01, "D=" + d);
        }
    }

    @Test
    void theBiggestShellStaysUnderAFullBomb() {
        assertTrue(pu(4, 2).radius() < pu(16, 4).radius());
        assertTrue(pu(4, 2).radius() > 400, "but still dwarfs any atomic warhead: " + pu(4, 2).radius());
    }

    @Test
    void theServersCapsHold() {
        Caps small = new Caps(300, 500, 20, 0, 10);
        Result r = ThermonuclearStats.of(Primary.PLUTONIUM, 16, 4, small);
        assertEquals(300, r.radius());
        assertEquals(500, r.power(), 1e-6);
        assertEquals(Math.round(300 * 1.5f), r.falloutRadius(), "no tamper reach, no wider fallout");
        assertEquals(10, r.hotSpot(), 1e-6);
    }
}
