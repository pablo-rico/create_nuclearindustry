package org.papiricoh.create_nuclearindustry.missile.orbit;

import org.junit.jupiter.api.Test;
import org.papiricoh.create_nuclearindustry.missile.physics.MissileDesign;
import org.papiricoh.create_nuclearindustry.missile.physics.PropellantModel;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The default thresholds against the rockets players build: two full liquid stages reach low orbit, three geostationary. */
class OrbitInsertionTest {
    private static final double LEO = 10.5, GEO = 12.5, SATELLITE = 1.0;

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
    void solidStagesFallShortOfOrbit() {
        assertNull(OrbitInsertion.reach(rocket(3, 4, false), LEO, GEO));
    }
}
