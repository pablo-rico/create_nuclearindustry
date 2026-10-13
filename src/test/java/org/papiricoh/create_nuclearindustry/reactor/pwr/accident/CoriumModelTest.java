package org.papiricoh.create_nuclearindustry.reactor.pwr.accident;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoriumModelTest {

    @Test
    void stateRoundTrips() {
        assertEquals(31, CoriumModel.MAX_ENERGY);
        for (int e = 0; e <= CoriumModel.MAX_ENERGY; e++) {
            assertEquals(e, CoriumModel.energy(CoriumModel.heat(e), CoriumModel.step(e)));
        }
        assertEquals(CoriumModel.MAX_ENERGY, CoriumModel.energy(7, 3));
    }

    /** Runs a block of corium to the end; returns how far down it went. */
    private static int run(java.util.function.IntFunction<CoriumModel.Below> belowAtDepth, boolean water, boolean destruction) {
        int energy = CoriumModel.MAX_ENERGY;
        int depth = 0;
        for (int tick = 0; tick < 1_000; tick++) {
            CoriumModel.Decision d = CoriumModel.decide(energy, belowAtDepth.apply(depth), water, false, destruction);
            if (d.action() == CoriumModel.Action.FALL || d.action() == CoriumModel.Action.MELT) {
                depth++;
            }
            if (d.solidifies()) {
                return depth;
            }
            assertTrue(d.energy() < energy, "every tick spends heat");
            energy = d.energy();
        }
        throw new AssertionError("corium never solidified");
    }

    @Test
    void fallsAtMost32BlocksThroughAir() {
        assertEquals(32, run(depth -> CoriumModel.Below.OPEN, false, true));
    }

    @Test
    void meltsThroughSolidGroundMoreSlowly() {
        assertEquals(16, run(depth -> CoriumModel.Below.MELTABLE, false, true));
    }

    @Test
    void neverDescendsIntoResistantBlocks() {
        assertEquals(0, run(depth -> CoriumModel.Below.RESISTANT, false, true));
        assertEquals(3, run(depth -> depth < 3 ? CoriumModel.Below.MELTABLE : CoriumModel.Below.RESISTANT, false, true));
    }

    @Test
    void withoutBlockDestructionItOnlyRests() {
        assertEquals(0, run(depth -> CoriumModel.Below.MELTABLE, false, false));
    }

    @ParameterizedTest
    @EnumSource(CoriumModel.Below.class)
    void alwaysSolidifiesWithinTheBudget(CoriumModel.Below below) {
        int depth = run(d -> below, false, true);
        assertTrue(depth <= CoriumModel.MAX_ENERGY + 1);
    }

    @Test
    void randomGroundStillEnds() {
        Random random = new Random(42);
        CoriumModel.Below[] kinds = CoriumModel.Below.values();
        for (int trial = 0; trial < 200; trial++) {
            int energy = CoriumModel.MAX_ENERGY;
            int ticks = 0;
            int depth = 0;
            while (true) {
                CoriumModel.Decision d = CoriumModel.decide(energy, kinds[random.nextInt(kinds.length)],
                        random.nextInt(10) == 0, random.nextBoolean(), random.nextBoolean());
                ticks++;
                if (d.action() == CoriumModel.Action.FALL || d.action() == CoriumModel.Action.MELT) {
                    depth++;
                }
                if (d.solidifies()) {
                    break;
                }
                energy = d.energy();
            }
            assertTrue(ticks <= CoriumModel.MAX_ENERGY + 1, "ended in " + ticks + " ticks");
            assertTrue(depth <= 32);
        }
    }

    @Test
    void waterQuenchesFirst() {
        CoriumModel.Decision d = CoriumModel.decide(CoriumModel.MAX_ENERGY, CoriumModel.Below.OPEN, true, false, true);
        assertEquals(CoriumModel.Action.QUENCH, d.action());
        assertEquals(CoriumModel.MAX_ENERGY - CoriumModel.QUENCH_COST, d.energy());
        assertFalse(d.solidifies());
    }

    @Test
    void spreadsOverACoreCatcher() {
        assertEquals(CoriumModel.Action.SPREAD,
                CoriumModel.decide(20, CoriumModel.Below.CATCHER, false, true, true).action());
        CoriumModel.Decision stuck = CoriumModel.decide(20, CoriumModel.Below.CATCHER, false, false, true);
        assertEquals(CoriumModel.Action.REST, stuck.action());
        assertEquals(20 - CoriumModel.CATCHER_COST, stuck.energy());
    }

    @Test
    void lastBitOfHeatFreezesIt() {
        CoriumModel.Decision d = CoriumModel.decide(0, CoriumModel.Below.OPEN, false, false, true);
        assertEquals(CoriumModel.Action.FALL, d.action());
        assertTrue(d.solidifies());
    }
}
