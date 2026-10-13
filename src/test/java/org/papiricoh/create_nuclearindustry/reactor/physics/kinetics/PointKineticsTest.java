package org.papiricoh.create_nuclearindustry.reactor.physics.kinetics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Point kinetics: equilibrium, the stable period, the prompt drop and stability above prompt critical. */
class PointKineticsTest {
    private static final PointKinetics.Params P = new PointKinetics.Params(1.0e-3, 0.0, 10.0, 4);
    private static final DelayedNeutrons SIX = DelayedNeutrons.sixGroups(650, 1.0);

    private static double run(PointKinetics k, DelayedNeutrons groups, double pcm, double seconds) {
        for (int i = 0; i < seconds / 0.05; i++) {
            k.step(P, groups, pcm * 1.0e-5, 0.05);
        }
        return k.power();
    }

    @Test
    void anEquilibriumStaysPut() {
        PointKinetics k = new PointKinetics();
        k.atEquilibrium(SIX, 0.5);
        assertEquals(0.5, run(k, SIX, 0, 100), 1e-9);
    }

    @Test
    void oneHundredPcmGivesTheInhourPeriod() {
        // The inhour equation for +100 pcm with these groups: the stable period is about 79 s.
        PointKinetics k = new PointKinetics();
        k.atEquilibrium(SIX, 0.01);
        run(k, SIX, 100, 300);
        double a = k.power();
        run(k, SIX, 100, 60);
        double period = 60 / Math.log(k.power() / a);
        assertEquals(inhourPeriod(SIX, 100e-5, P.generationTime()), period, 0.1 * period);
    }

    /** Solves the inhour equation rho = omega (Lambda + sum beta_i / (omega + lambda_i)) for 1/omega. */
    private static double inhourPeriod(DelayedNeutrons g, double rho, double lambdaGen) {
        double lo = 1.0e-6, hi = 10;
        for (int i = 0; i < 200; i++) {
            double w = (lo + hi) / 2;
            double r = w * lambdaGen;
            for (int j = 0; j < g.groups(); j++) {
                r += w * g.beta()[j] / (w + g.lambda()[j]);
            }
            if (r > rho) {
                hi = w;
            } else {
                lo = w;
            }
        }
        return 1 / ((lo + hi) / 2);
    }

    @Test
    void aBigNegativeStepDropsPromptlyToTheDelayedLevel() {
        PointKinetics k = new PointKinetics();
        k.atEquilibrium(SIX, 1.0);
        double rho = -5000e-5;
        for (int i = 0; i < 4; i++) {
            k.step(P, SIX, rho, 0.05);
        }
        // The prompt jump: within a fraction of a second n drops to about beta / (beta - rho) of its
        // level, then decays with the precursors.
        double expected = SIX.total() / (SIX.total() - rho);
        assertTrue(k.power() > 0.6 * expected && k.power() < 1.2 * expected, "prompt drop to " + k.power() + ", about " + expected);
        run(k, SIX, -5000, 120);
        assertTrue(k.power() < 0.01, "the delayed tail dies away: " + k.power());
    }

    @Test
    void stableAbovePromptCriticalWithinTheCap() {
        PointKinetics k = new PointKinetics();
        k.atEquilibrium(SIX, 0.01);
        double n = run(k, SIX, 650 + 2000, 2);
        assertFalse(Double.isNaN(n));
        assertEquals(P.maxPower(), n, 1e-9, "prompt critical runs to the cap within moments");
    }

    @Test
    void oneAndSixGroupsAgreeRoughlyOnASmallStep() {
        PointKinetics six = new PointKinetics();
        PointKinetics one = new PointKinetics();
        DelayedNeutrons single = DelayedNeutrons.oneGroup(650, 0.08);
        six.atEquilibrium(SIX, 0.01);
        one.atEquilibrium(single, 0.01);
        run(six, SIX, 50, 200);
        run(one, single, 50, 200);
        assertEquals(Math.log(six.power()), Math.log(one.power()), 0.3 * Math.log(six.power() / 0.01) + 0.2);
    }

    @Test
    void theSourceKeepsAShutDownCoreFaintlyAlive() {
        PointKinetics k = new PointKinetics();
        k.atEquilibrium(SIX, 0);
        for (int i = 0; i < 4000; i++) {
            k.step(PointKinetics.Params.DEFAULT, SIX, -3000e-5, 0.05);
        }
        assertTrue(k.power() > 0 && k.power() < 1.0e-3, "subcritical multiplication of the source: " + k.power());
    }
}
