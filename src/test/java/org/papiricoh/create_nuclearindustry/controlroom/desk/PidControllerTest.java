package org.papiricoh.create_nuclearindustry.controlroom.desk;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PidControllerTest {
    private static final PidController.Tuning TUNING = new PidController.Tuning(1.5, 0.3, 0.0, 0.005, 0.0);

    /** A core whose power lags behind (1 - insertion) with a time constant of {@code tau} seconds. */
    private static double plant(double power, double insertion, double tau, double dt) {
        return power + (1.0 - insertion - power) * dt / tau;
    }

    @Test
    void theFirstStepIsBumpless() {
        PidController pid = new PidController();
        assertEquals(0.7, pid.step(0.2, 0.9, 1.0, 0.7, 0, TUNING), 1e-9, "takes over where the rods are");
        assertTrue(pid.initialized());
    }

    @Test
    void settlesOnTheSetpoint() {
        PidController pid = new PidController();
        double power = 0.0;
        double insertion = 1.0;
        for (int i = 0; i < 600; i++) {
            insertion = pid.step(0.4, power, 1.0, insertion, 0, TUNING);
            power = plant(power, insertion, 5.0, 1.0);
        }
        assertEquals(0.4, power, 0.01, "power settles at the setpoint");
        assertEquals(0.6, insertion, 0.02, "with the rods 60% in");
    }

    @Test
    void tooHotInsertsAndTooColdWithdraws() {
        PidController hot = new PidController();
        hot.step(0.5, 0.8, 1.0, 0.5, 0, TUNING);
        assertTrue(hot.step(0.5, 0.8, 1.0, 0.5, 0, TUNING) > 0.5, "above the setpoint the rods go in");
        PidController cold = new PidController();
        cold.step(0.5, 0.1, 1.0, 0.5, 0, TUNING);
        assertTrue(cold.step(0.5, 0.1, 1.0, 0.5, 0, TUNING) < 0.5, "below it they come out");
    }

    @Test
    void insideTheDeadbandTheOutputHolds() {
        PidController pid = new PidController();
        pid.step(0.5, 0.5, 1.0, 0.42, 0, TUNING);
        for (int i = 0; i < 50; i++) {
            assertEquals(0.42, pid.step(0.5, 0.503, 1.0, 0.0, 0, TUNING), 1e-9);
        }
        assertEquals(0.42, pid.integral(), 1e-9, "and nothing integrates");
    }

    @Test
    void theRateLimitCapsEachStep() {
        PidController.Tuning slow = new PidController.Tuning(10.0, 1.0, 0.0, 0.0, 0.05);
        PidController pid = new PidController();
        double previous = pid.step(0.0, 1.0, 1.0, 0.0, 0, slow);
        for (int i = 0; i < 10; i++) {
            double next = pid.step(0.0, 1.0, 0.5, 0.0, 0, slow);
            assertTrue(next - previous <= 0.025 + 1e-9, "at most 5% per second, got " + (next - previous));
            assertTrue(next > previous, "but it keeps moving");
            previous = next;
        }
    }

    @Test
    void noWindupWhileSaturated() {
        PidController pid = new PidController();
        double insertion = pid.step(0.9, 0.0, 1.0, 0.5, 0, TUNING);
        // An unreachable setpoint: the rods come all the way out and stay there for a long time.
        for (int i = 0; i < 1000; i++) {
            insertion = pid.step(0.9, 0.0, 1.0, insertion, 0, TUNING);
        }
        assertEquals(0.0, insertion, 1e-9);
        assertTrue(pid.integral() > -0.5, "the integral tracks the clamped output instead of winding up, got " + pid.integral());
        // Once the measurement overshoots, the rods go back in on the very next step.
        double after = pid.step(0.2, 0.6, 1.0, insertion, 0, TUNING);
        assertTrue(after > 0.3, "no long wait for a wound-up integral to unwind, got " + after);
    }

    @Test
    void theOutputLandsOnWholeRodSegments() {
        PidController pid = new PidController();
        double insertion = 1.0;
        double power = 0.0;
        for (int i = 0; i < 100; i++) {
            insertion = pid.step(0.37, power, 1.0, insertion, 3, TUNING);
            double segments = insertion * 3;
            assertEquals(Math.round(segments), segments, 1e-9, "a multiple of 1/3, got " + insertion);
            power = plant(power, insertion, 5.0, 1.0);
        }
        assertEquals(0.5, PidController.quantize(0.49, 2), 1e-9);
        assertEquals(1.0, PidController.quantize(1.7, 4), 1e-9);
        assertEquals(0.123, PidController.quantize(0.123, 0), 1e-9);
    }

    @Test
    void resetStartsBumplessAgain() {
        PidController pid = new PidController();
        pid.step(0.2, 0.8, 1.0, 0.3, 0, TUNING);
        pid.step(0.2, 0.8, 1.0, 0.3, 0, TUNING);
        pid.reset();
        assertEquals(0.9, pid.step(0.2, 0.8, 1.0, 0.9, 0, TUNING), 1e-9);
        PidController copy = new PidController();
        copy.load(pid.initialized(), pid.integral(), pid.output(), pid.previousMeasurement());
        assertEquals(pid.step(0.2, 0.7, 1.0, 0, 0, TUNING), copy.step(0.2, 0.7, 1.0, 0, 0, TUNING), 1e-12,
                "a loaded controller carries on exactly");
    }
}
