package org.papiricoh.create_nuclearindustry.controlroom;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Which rows a control room screen draws, in which order, from a reading made by hand. */
class MonitorRowTest {

    /** A reactor reading: 300 °C, 50 MW of 100, rods half in; a pressurized loop and damaged fuel if asked. */
    private static MonitorReading reactor(boolean pwr, float damage) {
        float[] v = new float[35];
        v[MonitorReading.R_TEMPERATURE] = 300;
        v[MonitorReading.R_POWER] = 50;
        v[MonitorReading.R_RATED_POWER] = 100;
        v[MonitorReading.R_RODS] = 0.5f;
        v[MonitorReading.R_ROD_TARGET] = -1;
        v[MonitorReading.R_PWR] = pwr ? 1 : 0;
        v[MonitorReading.R_PRESSURE] = pwr ? 155 : 0;
        v[MonitorReading.R_CORE_DAMAGE] = damage;
        return new MonitorReading(TargetKind.REACTOR, "Running", v, 300 / 4000f, 0.5f);
    }

    private static List<MonitorRow> rows(List<MonitorRow.Line> lines) {
        return lines.stream().map(MonitorRow.Line::row).toList();
    }

    @Test
    void anUnsetScreenListsTheUsualRows() {
        List<MonitorRow> shown = rows(MonitorRow.visible(reactor(false, 0), MonitorConfig.DEFAULT));
        assertEquals(MonitorRow.REACTOR_TEMPERATURE, shown.get(0));
        assertEquals(MonitorRow.REACTOR_POWER, shown.get(1));
        assertEquals(MonitorRow.REACTOR_RODS, shown.get(2), "no damage and no pressurized loop: rods come third");
        assertFalse(shown.contains(MonitorRow.REACTOR_PRESSURE), "a reactor boiling directly has no loop pressure");
        assertFalse(shown.contains(MonitorRow.REACTOR_DAMAGE), "sound fuel shows no damage");
        assertEquals(MonitorRow.forKind(TargetKind.REACTOR).size() - 10, shown.size(), "nine loop rows and the damage row left out");
    }

    @Test
    void conditionalRowsAppearWhenTheyApply() {
        List<MonitorRow> shown = rows(MonitorRow.visible(reactor(true, 0.25f), MonitorConfig.DEFAULT));
        assertEquals(List.of(MonitorRow.REACTOR_TEMPERATURE, MonitorRow.REACTOR_POWER, MonitorRow.REACTOR_DAMAGE,
                MonitorRow.REACTOR_PRESSURE), shown.subList(0, 4), "damage, then the loop before the rods");
        assertEquals(MonitorRow.forKind(TargetKind.REACTOR), shown, "every reactor row applies");
        MonitorRow.Line damage = MonitorRow.REACTOR_DAMAGE.line(reactor(true, 0.25f));
        assertEquals("25%", damage.value());
        assertEquals(MonitorRow.Tone.BAD, damage.tone());
        assertEquals("create_nuclearindustry.monitor.damage", damage.labelKey());
    }

    @Test
    void hiddenRowsAreNotDrawnAndTheOrderIsKept() {
        MonitorConfig config = MonitorConfig.DEFAULT.withRows(
                List.of(MonitorRow.REACTOR_FUEL, MonitorRow.REACTOR_DAMAGE, MonitorRow.REACTOR_PRESSURE, MonitorRow.REACTOR_POWER),
                EnumSet.of(MonitorRow.REACTOR_TEMPERATURE, MonitorRow.REACTOR_XENON));
        List<MonitorRow> shown = rows(MonitorRow.visible(reactor(false, 0), config));
        assertEquals(List.of(MonitorRow.REACTOR_FUEL, MonitorRow.REACTOR_POWER, MonitorRow.REACTOR_RODS), shown.subList(0, 3),
                "the chosen order first, without the rows that do not apply, then the rest in the usual order");
        assertFalse(shown.contains(MonitorRow.REACTOR_TEMPERATURE), "hidden");
        assertFalse(shown.contains(MonitorRow.REACTOR_XENON), "hidden");
        assertEquals(1, shown.stream().filter(row -> row == MonitorRow.REACTOR_FUEL).count(), "each row once");
    }

    @Test
    void rowsOfAnotherKindAreNeverDrawn() {
        MonitorConfig config = MonitorConfig.DEFAULT.withRows(List.of(MonitorRow.TURBINE_SPEED, MonitorRow.AREA_GROUND), Set.of());
        List<MonitorRow> shown = rows(MonitorRow.visible(reactor(false, 0), config));
        assertTrue(shown.stream().allMatch(row -> row.kind() == TargetKind.REACTOR));
        assertNull(MonitorRow.TURBINE_SPEED.line(reactor(false, 0)));
        assertTrue(MonitorRow.visible(MonitorReading.NONE, config).isEmpty(), "an unlinked screen draws no rows");
    }

    @Test
    void everyKindHasRowsAndOneLabelPerRow() {
        for (TargetKind kind : TargetKind.values()) {
            List<MonitorRow> rows = MonitorRow.forKind(kind);
            boolean real = kind != TargetKind.NONE && kind != TargetKind.NO_SIGNAL;
            assertEquals(real, !rows.isEmpty(), kind + " rows");
            assertEquals(rows.size(), rows.stream().map(MonitorRow::labelKey).distinct().count(), kind + " labels are unique");
        }
    }
}
