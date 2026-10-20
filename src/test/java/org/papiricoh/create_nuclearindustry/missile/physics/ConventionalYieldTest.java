package org.papiricoh.create_nuclearindustry.missile.physics;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConventionalYieldTest {
    @Test void singleTntAndEmptyLoad() {
        assertEquals(0, ConventionalYield.power(0, 16));
        assertEquals(4, ConventionalYield.power(1, 16));
        assertEquals(8, ConventionalYield.power(8, 16));
    }
    @Test void monotonicAndCapped() {
        double previous = 0;
        for (int n = 0; n < 1000; n++) {
            double power = ConventionalYield.power(n, 13);
            assertTrue(power >= previous && power <= 13);
            previous = power;
        }
        assertEquals(13, previous);
    }
}
