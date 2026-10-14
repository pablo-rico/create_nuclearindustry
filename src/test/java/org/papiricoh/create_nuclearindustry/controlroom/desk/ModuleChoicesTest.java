package org.papiricoh.create_nuclearindustry.controlroom.desk;

import org.junit.jupiter.api.Test;
import org.papiricoh.create_nuclearindustry.controlroom.TargetKind;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModuleChoicesTest {

    @Test
    void anUnlinkedConsoleOffersEveryFunction() {
        for (ModuleType type : ModuleType.values()) {
            assertEquals(type.functions(), ModuleChoices.usable(type, TargetKind.NONE, false), type.name());
            assertEquals(type.functions(), ModuleChoices.usable(type, TargetKind.NO_SIGNAL, false), type.name());
        }
    }

    @Test
    void aReactorConsoleGreysOutTokamakFunctions() {
        assertEquals(ModuleChoices.Fit.WRONG_MACHINE, ModuleChoices.fit(ModuleFunction.TOKAMAK_HEATING, TargetKind.REACTOR, true));
        assertEquals(ModuleChoices.Fit.WRONG_MACHINE, ModuleChoices.fit(ModuleFunction.AUTO_FUSION_POWER, TargetKind.REACTOR, true));
        assertEquals(TargetKind.TOKAMAK, ModuleChoices.needs(ModuleFunction.AUTO_FUSION_POWER));
        assertEquals(List.of(ModuleFunction.AUTO_POWER, ModuleFunction.AUTO_TEMPERATURE, ModuleFunction.AUTO_PRESSURE),
                ModuleChoices.usable(ModuleType.CONTROLLER, TargetKind.REACTOR, true));
        assertEquals(List.of(ModuleFunction.AUTO_FUSION_POWER), ModuleChoices.usable(ModuleType.CONTROLLER, TargetKind.TOKAMAK, false));
    }

    @Test
    void pressurizerFunctionsNeedAPressurizer() {
        assertEquals(ModuleChoices.Fit.NO_PRESSURIZER, ModuleChoices.fit(ModuleFunction.PZR_SPRAY, TargetKind.REACTOR, false));
        assertEquals(ModuleChoices.Fit.OK, ModuleChoices.fit(ModuleFunction.PZR_SPRAY, TargetKind.REACTOR, true));
        assertEquals(ModuleChoices.Fit.WRONG_MACHINE, ModuleChoices.fit(ModuleFunction.PZR_SPRAY, TargetKind.TURBINE, false));
        assertFalse(ModuleChoices.usable(ModuleType.CONTROLLER, TargetKind.REACTOR, false).contains(ModuleFunction.AUTO_PRESSURE));
    }

    @Test
    void redstoneFunctionsWorkAnywhere() {
        for (TargetKind kind : TargetKind.values()) {
            assertEquals(ModuleChoices.Fit.OK, ModuleChoices.fit(ModuleFunction.TOGGLE, kind, false), kind.name());
            assertEquals(ModuleChoices.Fit.OK, ModuleChoices.fit(ModuleFunction.SIGNAL_LEVEL, kind, false), kind.name());
            assertEquals(ModuleChoices.Fit.OK, ModuleChoices.fit(ModuleFunction.LAMP_ALARM, kind, false), kind.name());
        }
    }

    @Test
    void theRunningLampNeedsAMachineThatRuns() {
        assertEquals(ModuleChoices.Fit.OK, ModuleChoices.fit(ModuleFunction.LAMP_RUNNING, TargetKind.TURBINE, false));
        assertEquals(ModuleChoices.Fit.OK, ModuleChoices.fit(ModuleFunction.LAMP_RUNNING, TargetKind.TOKAMAK, false));
        assertEquals(ModuleChoices.Fit.WRONG_MACHINE, ModuleChoices.fit(ModuleFunction.LAMP_RUNNING, TargetKind.AREA, false));
        assertNull(ModuleChoices.needs(ModuleFunction.LAMP_RUNNING), "it names no single machine");
    }

    @Test
    void settingsAreCoarseExceptTheRedstoneLevel() {
        assertTrue(ModuleChoices.hasSetting(ModuleFunction.LAMP_ALARM));
        assertTrue(ModuleChoices.hasSetting(ModuleFunction.AUTO_PRESSURE));
        assertTrue(ModuleChoices.hasSetting(ModuleFunction.SIGNAL_LEVEL));
        assertFalse(ModuleChoices.hasSetting(ModuleFunction.SCRAM));
        assertFalse(ModuleChoices.coarseSetting(ModuleFunction.SIGNAL_LEVEL));
        assertTrue(ModuleChoices.coarseSetting(ModuleFunction.AUTO_POWER));
    }

    @Test
    void readingScreensHaveTheirOwnSetting() {
        assertTrue(ModuleChoices.hasSetting(ModuleType.BAR_GRAPH, ModuleFunction.READING), "the alarm level");
        assertTrue(ModuleChoices.hasSetting(ModuleType.TREND_RECORDER, ModuleFunction.READING), "the speed");
        assertFalse(ModuleChoices.hasSetting(ModuleType.GAUGE, ModuleFunction.READING));
        assertTrue(ModuleChoices.coarseSetting(ModuleType.BAR_GRAPH, ModuleFunction.READING));
        assertFalse(ModuleChoices.coarseSetting(ModuleType.TREND_RECORDER, ModuleFunction.READING), "three speeds, one at a time");
    }
}
