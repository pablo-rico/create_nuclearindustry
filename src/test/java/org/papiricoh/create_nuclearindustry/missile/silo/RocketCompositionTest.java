package org.papiricoh.create_nuclearindustry.missile.silo;

import org.junit.jupiter.api.Test;
import org.papiricoh.create_nuclearindustry.missile.physics.MissileDesign;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.papiricoh.create_nuclearindustry.missile.silo.RocketComposition.Section.ENGINE;
import static org.papiricoh.create_nuclearindustry.missile.silo.RocketComposition.Section.GUIDANCE;
import static org.papiricoh.create_nuclearindustry.missile.silo.RocketComposition.Section.TANK;
import static org.papiricoh.create_nuclearindustry.missile.silo.RocketComposition.Section.WARHEAD;
import static org.papiricoh.create_nuclearindustry.missile.silo.RocketComposition.Section.KILL_VEHICLE;

/** The grammar of a silo column: engine and tanks per stage, then guidance and one warhead on top. */
class RocketCompositionTest {

    private static String reason(List<RocketComposition.Section> sections) {
        return assertThrows(RocketComposition.Invalid.class, () -> RocketComposition.parse(sections)).reason;
    }

    private static int index(List<RocketComposition.Section> sections) {
        return assertThrows(RocketComposition.Invalid.class, () -> RocketComposition.parse(sections)).index;
    }

    @Test
    void theSmallestRocketIsOneStageOfOneTank() {
        RocketComposition rocket = RocketComposition.parse(List.of(ENGINE, TANK, GUIDANCE, WARHEAD));
        assertEquals(List.of(1), rocket.tanksPerStage());
        assertEquals(4, rocket.height());
        assertEquals(List.of(0), rocket.stageStarts());
    }

    @Test
    void killVehicleIsAnAlternativePayloadAndLegacyConstructorMeansWarhead() {
        RocketComposition legacy = new RocketComposition(List.of(1));
        assertEquals(RocketComposition.Payload.WARHEAD, legacy.payload());
        RocketComposition interceptor = RocketComposition.parse(List.of(ENGINE, TANK, GUIDANCE, KILL_VEHICLE));
        assertEquals(RocketComposition.Payload.KILL_VEHICLE, interceptor.payload());
        assertTrue(interceptor.interceptor());
        assertEquals(4, interceptor.height());
        assertThrows(RocketComposition.Invalid.class,
                () -> RocketComposition.parse(List.of(ENGINE, TANK, GUIDANCE, WARHEAD, KILL_VEHICLE)));
    }

    @Test
    void stagesStartAtTheirEngines() {
        RocketComposition rocket = RocketComposition.parse(List.of(ENGINE, TANK, TANK, ENGINE, TANK, ENGINE, TANK, TANK, TANK, GUIDANCE, WARHEAD));
        assertEquals(List.of(2, 1, 3), rocket.tanksPerStage());
        assertEquals(List.of(0, 3, 5), rocket.stageStarts());
        assertEquals(11, rocket.height());
    }

    @Test
    void theTallestRocketIsThreeStagesOfFourTanks() {
        List<RocketComposition.Section> sections = new ArrayList<>();
        for (int stage = 0; stage < 3; stage++) {
            sections.add(ENGINE);
            for (int tank = 0; tank < 4; tank++) sections.add(TANK);
        }
        sections.add(GUIDANCE);
        sections.add(WARHEAD);
        assertEquals(17, RocketComposition.parse(sections).height());
    }

    @Test
    void malformedColumnsSayWhatAndWhere() {
        assertEquals("height", reason(List.of(ENGINE, TANK, WARHEAD)));
        assertEquals("engine", reason(List.of(TANK, TANK, GUIDANCE, WARHEAD)));
        assertEquals("tanks", reason(List.of(ENGINE, GUIDANCE, WARHEAD, WARHEAD)));
        assertEquals(1, index(List.of(ENGINE, GUIDANCE, WARHEAD, WARHEAD)), "the missing tank is right above the engine");
        assertEquals("tanks", reason(List.of(ENGINE, TANK, TANK, TANK, TANK, TANK, GUIDANCE, WARHEAD)));
        assertEquals("guidance", reason(List.of(ENGINE, TANK, WARHEAD, GUIDANCE)));
        assertEquals(2, index(List.of(ENGINE, TANK, WARHEAD, GUIDANCE)));
        assertEquals("warhead", reason(List.of(ENGINE, TANK, GUIDANCE, GUIDANCE)));
        assertEquals("extra", reason(List.of(ENGINE, TANK, GUIDANCE, WARHEAD, TANK)));
        assertEquals(4, index(List.of(ENGINE, TANK, GUIDANCE, WARHEAD, TANK)));
    }

    @Test
    void aFourthStageIsRefused() {
        List<RocketComposition.Section> sections = new ArrayList<>();
        for (int stage = 0; stage < 4; stage++) {
            sections.add(ENGINE);
            sections.add(TANK);
        }
        sections.add(GUIDANCE);
        sections.add(WARHEAD);
        assertEquals("stages", reason(sections));
        assertEquals(6, index(sections), "the fourth engine is the culprit");
    }

    @Test
    void moreTanksCarryMorePropellant() {
        MissileDesign small = new RocketComposition(List.of(1)).design(1);
        MissileDesign large = new RocketComposition(List.of(4)).design(1);
        assertTrue(large.stages().get(0).propellant() > small.stages().get(0).propellant());
        assertTrue(large.deltaV() > small.deltaV(), "more propellant gives more delta-v");
        assertEquals(1, small.payloadMass());
    }

    @Test
    void outOfBoundsStageListsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new RocketComposition(List.of()));
        assertThrows(IllegalArgumentException.class, () -> new RocketComposition(List.of(1, 1, 1, 1)));
        assertThrows(IllegalArgumentException.class, () -> new RocketComposition(List.of(5)));
    }
}
