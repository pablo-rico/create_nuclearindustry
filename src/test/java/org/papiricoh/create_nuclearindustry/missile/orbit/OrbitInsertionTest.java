package org.papiricoh.create_nuclearindustry.missile.orbit;

import org.junit.jupiter.api.Test;
import org.papiricoh.create_nuclearindustry.missile.physics.MissileDesign;
import org.papiricoh.create_nuclearindustry.missile.physics.PropellantModel;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The default thresholds against the rockets players build: three full solid stages or two liquid ones reach low orbit, three
 * stages with liquid geostationary.
 */
class OrbitInsertionTest {
    private static final double LEO = 9.5, GEO = 11.5, SATELLITE = 1.0;

    private static MissileDesign rocket(int stages, int tanks, boolean liquid) {
        List<MissileDesign.Stage> list = new ArrayList<>();
        for (int s = 0; s < stages; s++) {
            List<PropellantModel.Tank> fill = new ArrayList<>();
            for (int t = 0; t < tanks; t++) fill.add(liquid ? new PropellantModel.Tank(false, 1000, 1000) : new PropellantModel.Tank(true, 0, 0));
            list.add(PropellantModel.stage(fill).design());
        }
        return new MissileDesign(list, SATELLITE, 0);
    }

    @Test
    void oneStageNeverReachesOrbit() {
        assertNull(OrbitInsertion.reach(rocket(1, 4, true), LEO, GEO));
    }

    @Test
    void twoFullLiquidStagesReachLowOrbit() {
        assertEquals(OrbitType.LEO, OrbitInsertion.reach(rocket(2, 4, true), LEO, GEO));
        assertTrue(OrbitInsertion.reaches(rocket(2, 4, true), OrbitType.LEO, LEO, GEO));
        assertFalse(OrbitInsertion.reaches(rocket(2, 4, true), OrbitType.GEO, LEO, GEO));
    }

    @Test
    void threeFullLiquidStagesReachGeostationaryOrbit() {
        assertEquals(OrbitType.GEO, OrbitInsertion.reach(rocket(3, 4, true), LEO, GEO));
        assertTrue(OrbitInsertion.reaches(rocket(3, 4, true), OrbitType.LEO, LEO, GEO));
    }

    @Test
    void threeFullSolidStagesReachLowOrbitOnly() {
        assertEquals(OrbitType.LEO, OrbitInsertion.reach(rocket(3, 4, false), LEO, GEO));
    }

    @Test
    void twoSolidStagesFallShortOfOrbit() {
        assertNull(OrbitInsertion.reach(rocket(2, 4, false), LEO, GEO));
    }

    @Test
    void halfFilledLiquidStagesFallShort() {
        assertNull(OrbitInsertion.reach(rocket(2, 2, true), LEO, GEO));
    }
}
