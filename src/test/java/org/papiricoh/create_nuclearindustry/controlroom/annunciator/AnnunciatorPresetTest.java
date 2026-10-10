package org.papiricoh.create_nuclearindustry.controlroom.annunciator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The annunciator preset table: six sane windows each, with thresholds that follow the plant config. */
class AnnunciatorPresetTest {
    private static final AnnunciatorPreset.Plant DEFAULTS = AnnunciatorPreset.Plant.DEFAULTS;

    private static AnnunciatorPreset.Window window(AnnunciatorPreset preset, int index) {
        return preset.windows(DEFAULTS).get(index);
    }

    @ParameterizedTest
    @EnumSource(AnnunciatorPreset.class)
    void everyPresetFillsTheSixWindows(AnnunciatorPreset preset) {
        List<AnnunciatorPreset.Window> windows = preset.windows(DEFAULTS);
        // A compile-time constant: the block entity class itself is never loaded.
        assertEquals(AnnunciatorPanelBlockEntity.WINDOWS, windows.size());
        for (AnnunciatorPreset.Window window : windows) {
            assertTrue(window.setting() >= 0 && window.setting() <= 100, window + " has a 0-100 setting");
            assertTrue(!window.label().isBlank() && window.label().length() <= 16, window + " has a label that fits a window");
            assertTrue(window.metric().matches("[A-Z_0-9]+"), window + " names a metric");
        }
        assertEquals(windows.size(), windows.stream().map(AnnunciatorPreset.Window::label).distinct().count(), "labels tell windows apart");
    }

    @Test
    void thePrimaryAlarmsSitBetweenTheSetpointAndTheReliefValve() {
        // Settings are 2.5 bar steps of the 250 bar full scale.
        assertEquals(65, window(AnnunciatorPreset.PWR_PRIMARY, 0).setting(), "162.5 bar, between 155 and 170");
        assertEquals("PRESSURE_HIGH", window(AnnunciatorPreset.PWR_PRIMARY, 0).metric());
        assertEquals(58, window(AnnunciatorPreset.PWR_PRIMARY, 1).setting(), "145 bar, 10 below the setpoint");
        assertEquals("RELIEF_OPEN", window(AnnunciatorPreset.PWR_PRIMARY, 2).metric());
        assertEquals(20, window(AnnunciatorPreset.PWR_PRIMARY, 3).setting(), "10 °C of subcooling on a 50 °C scale");
        assertEquals(6, window(AnnunciatorPreset.PWR_PRIMARY, 5).setting(), "twice natural circulation, 16 of 256 mB/t");
    }

    @Test
    void thresholdsFollowThePlantConfig() {
        AnnunciatorPreset.Plant lower = new AnnunciatorPreset.Plant(120.0, 140.0, 20.0, 50.0);
        List<AnnunciatorPreset.Window> primary = AnnunciatorPreset.PWR_PRIMARY.windows(lower);
        assertEquals(52, primary.get(0).setting(), "130 bar");
        assertEquals(44, primary.get(1).setting(), "110 bar");
        assertEquals(16, primary.get(5).setting(), "40 of 256 mB/t");
        assertEquals(50, AnnunciatorPreset.STEAM_GENERATOR.windows(lower).get(5).setting(), "half a 50 °C rise on a 50 °C scale");
    }

    @Test
    void containmentAlarmsFollowHydrogenAndConcrete() {
        assertEquals(20, window(AnnunciatorPreset.CONTAINMENT, 0).setting(), "2 bar");
        assertEquals(48, window(AnnunciatorPreset.CONTAINMENT, 1).setting(), "80% of concrete's 6 bar");
        assertEquals(20, window(AnnunciatorPreset.CONTAINMENT, 2).setting(), "2% hydrogen, half the flammability limit");
        assertEquals(40, window(AnnunciatorPreset.CONTAINMENT, 3).setting(), "4% hydrogen, where it can burn");
        assertEquals(40, window(AnnunciatorPreset.CONTAINMENT, 4).setting(), "a thousandth of a core, two decades of five up");
        assertEquals("CONTAINMENT_OPEN", window(AnnunciatorPreset.CONTAINMENT, 5).metric());
    }

    @Test
    void theCoreAlarmsLightBeforeTheAutomaticScram() {
        assertEquals(50, window(AnnunciatorPreset.REACTOR_CORE, 0).setting(), "2000 °C");
        assertEquals(60, window(AnnunciatorPreset.REACTOR_CORE, 1).setting(), "2400 °C, under the 2800 °C automatic SCRAM");
    }

    @Test
    void presetsAreFoundByNameAndCycle() {
        assertSame(AnnunciatorPreset.CONTAINMENT, AnnunciatorPreset.byName("CONTAINMENT"));
        assertNull(AnnunciatorPreset.byName("NOT_A_PRESET"));
        assertSame(AnnunciatorPreset.PWR_PRIMARY, AnnunciatorPreset.REACTOR_CORE.next(1));
        assertSame(AnnunciatorPreset.CONTAINMENT, AnnunciatorPreset.REACTOR_CORE.next(-1));
        assertEquals("create_nuclearindustry.annunciator.preset.steam_generator.3", AnnunciatorPreset.STEAM_GENERATOR.labelKey(2));
    }
}
