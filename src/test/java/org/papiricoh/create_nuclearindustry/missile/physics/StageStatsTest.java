package org.papiricoh.create_nuclearindustry.missile.physics;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class StageStatsTest {
    @Test void singleStageMatchesDesign() {
        MissileDesign design = MissileDesign.LEGACY_PAD;
        StageStats stats = StageStats.of(design, 0.01).getFirst();
        assertEquals(design.deltaV(), stats.deltaV(), 1e-12);
        assertEquals(design.liftOffThrustToWeight(0.01), stats.twr(), 1e-12);
        assertEquals(design.totalBurnTicks(), stats.burnTicks(), 1e-12);
    }
    @Test void upperStagesAreCarriedAtIgnition() {
        var first = new MissileDesign.Stage(1, 8, 0.24, 4);
        var upper = new MissileDesign.Stage(0.5, 3, 0.12, 5);
        var design = new MissileDesign(List.of(first, upper), 2, 0);
        var stats = StageStats.of(design, 0.01);
        assertEquals(0.24 / (14.5 * 0.01), stats.get(0).twr(), 1e-12);
        assertEquals(4 * Math.log(14.5 / 6.5), stats.get(0).deltaV(), 1e-12);
        assertEquals(0.12 / (5.5 * 0.01), stats.get(1).twr(), 1e-12);
        assertEquals(design.deltaV(), stats.stream().mapToDouble(StageStats::deltaV).sum(), 1e-12);
        assertEquals(design.totalBurnTicks(), stats.stream().mapToDouble(StageStats::burnTicks).sum(), 1e-12);
    }
    @Test void emptyAndZeroGravityAreFinite() {
        assertTrue(StageStats.of(new MissileDesign(List.of(), 1, 0), 0).isEmpty());
        assertEquals(0, StageStats.of(MissileDesign.LEGACY_PAD, 0).getFirst().twr());
    }
}
