package org.papiricoh.create_nuclearindustry.controlroom.desk;

import org.junit.jupiter.api.Test;
import org.papiricoh.create_nuclearindustry.fusion.api.TokamakPhase;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The pure parts of the screen modules: merged screens, bar graph zones, the trend ring, the core map and the plasma look. */
class ScreenModulesTest {

    @Test
    void fourOfAMergingTypeMakeOneScreen() {
        assertTrue(BlockScreen.merged(ModuleType.CORE_MAP, ModuleType.CORE_MAP, ModuleType.CORE_MAP, ModuleType.CORE_MAP));
        assertTrue(BlockScreen.merged(ModuleType.PLASMA_SECTION, ModuleType.PLASMA_SECTION, ModuleType.PLASMA_SECTION,
                ModuleType.PLASMA_SECTION));
        assertFalse(BlockScreen.merged(ModuleType.CORE_MAP, ModuleType.CORE_MAP, ModuleType.CORE_MAP, null), "a slot missing");
        assertFalse(BlockScreen.merged(ModuleType.CORE_MAP, ModuleType.CORE_MAP, ModuleType.CORE_MAP, ModuleType.PLASMA_SECTION),
                "two kinds");
        assertFalse(BlockScreen.merged(ModuleType.TREND_RECORDER, ModuleType.TREND_RECORDER, ModuleType.TREND_RECORDER,
                ModuleType.TREND_RECORDER), "trend recorders stay single");
        assertFalse(BlockScreen.merged(ModuleType.CORE_MAP, ModuleType.CORE_MAP), "not a whole block");
    }

    @Test
    void theBarTurnsRedFromTheAlarmLevel() {
        assertEquals(0, BarGraph.lit(0));
        assertEquals(10, BarGraph.lit(0.5f));
        assertEquals(BarGraph.SEGMENTS, BarGraph.lit(1.4f), "clamped at full scale");
        assertEquals(0, BarGraph.lit(Float.NaN));
        assertEquals(BarGraph.Zone.GREEN, BarGraph.zone(0, 90));
        assertEquals(BarGraph.Zone.AMBER, BarGraph.zone(16, 90), "82.5% is within 15 of 90");
        assertEquals(BarGraph.Zone.RED, BarGraph.zone(18, 90), "92.5% is past 90");
        assertEquals(BarGraph.Zone.RED, BarGraph.zone(0, 0), "an alarm at 0 makes it all red");
    }

    @Test
    void theTrendRingKeepsTheNewestInOrder() {
        TrendRing ring = new TrendRing();
        for (int i = 0; i < TrendRing.CAPACITY + 5; i++) {
            ring.push(i / (float) (TrendRing.CAPACITY + 5));
        }
        byte[] samples = ring.ordered();
        assertEquals(TrendRing.CAPACITY, samples.length);
        assertTrue(TrendRing.value(samples[0]) < TrendRing.value(samples[samples.length - 1]), "oldest first");
        ring.push(Float.NaN);
        assertTrue(Float.isNaN(TrendRing.value(ring.ordered()[TrendRing.CAPACITY - 1])), "a gap stays a gap");
        TrendRing copy = new TrendRing();
        copy.load(ring.ordered());
        assertArrayEquals(ring.ordered(), copy.ordered(), "saved and loaded");
        assertEquals(1.0f, TrendRing.value(TrendRing.quantize(2.0f)), 1.0e-6f);
        assertEquals(20, TrendRing.secondsFor(7), "speeds are clamped");
    }

    @Test
    void theCoreMapPacksKindsAndValues() {
        byte[] map = CoreMap.blank(3);
        CoreMap.set(map, 0, 0, CoreMap.Kind.FUEL, 0.4f);
        CoreMap.set(map, 1, 1, CoreMap.Kind.CONTROL, 1.0f);
        CoreMap.set(map, 2, 2, CoreMap.Kind.FUEL, Float.NaN);
        CoreMap.set(map, 5, 5, CoreMap.Kind.REFLECTOR, 0);
        assertEquals(3, CoreMap.side(map));
        assertEquals(CoreMap.Kind.FUEL, CoreMap.kind(map, 0, 0));
        assertEquals(0.4f, CoreMap.value(map, 0, 0), 0.01f);
        assertEquals(1.0f, CoreMap.value(map, 1, 1), 1.0e-6f);
        assertTrue(Float.isNaN(CoreMap.value(map, 2, 2)), "a fuel column with no assemblies has no value");
        assertEquals(CoreMap.Kind.EMPTY, CoreMap.kind(map, 2, 0));
        assertEquals(2, CoreMap.count(map, CoreMap.Kind.FUEL));
        assertEquals(0, CoreMap.side(new byte[0]));
        assertEquals(0, CoreMap.side(new byte[]{5, 1, 2}), "too short for its side");
        assertEquals(CoreMap.MAX_SIDE, CoreMap.side(CoreMap.blank(12)), "capped at a 7x7 interior");
    }

    @Test
    void thePlasmaLooksLikeWhatTheTokamakDoes() {
        PlasmaSection.Look off = PlasmaSection.decode(PlasmaSection.encode(TokamakPhase.OFFLINE, 0, 0, 0, 0, 0, false));
        assertNotNull(off);
        assertFalse(off.plasma());
        assertFalse(off.coils());
        PlasmaSection.Look cool = PlasmaSection.decode(PlasmaSection.encode(TokamakPhase.BURN, 0.2f, 0.3f, 0, 0.5f, 1, false));
        PlasmaSection.Look hot = PlasmaSection.decode(PlasmaSection.encode(TokamakPhase.BURN, 0.9f, 0.9f, 0.8f, 1, 1, true));
        assertTrue(cool.plasma() && hot.plasma());
        assertTrue(hot.brightness() > cool.brightness(), "hotter glows brighter");
        assertTrue(hot.radius() > cool.radius(), "denser fills more");
        assertTrue(hot.wobble() > cool.wobble(), "unstable shakes");
        assertEquals(PlasmaSection.MAX_FIELD_LINES, hot.fieldLines());
        assertTrue(hot.tripped());
        assertNull(PlasmaSection.decode(new byte[3]));
    }
}
