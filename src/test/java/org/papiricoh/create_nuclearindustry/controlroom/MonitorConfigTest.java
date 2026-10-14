package org.papiricoh.create_nuclearindustry.controlroom;

import org.junit.jupiter.api.Test;
import org.papiricoh.create_nuclearindustry.controlroom.desk.GaugeReading;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A screen's wrench setup made safe for what it watches: title, rows, series and style. */
class MonitorConfigTest {

    @Test
    void theTitleIsPrintableAndShort() {
        assertEquals("Unit 1", MonitorConfig.sanitizeTitle("  §cUnit\n 1\t"), "codes and control characters go, the rest is trimmed");
        assertEquals("", MonitorConfig.sanitizeTitle(null));
        String longTitle = "Reactor number one, main loop";
        String clean = MonitorConfig.sanitizeTitle(longTitle);
        assertTrue(clean.length() <= MonitorConfig.TITLE_MAX, "at most " + MonitorConfig.TITLE_MAX + " characters, got " + clean);
        assertTrue(longTitle.startsWith(clean), "cut from the end");
        String emoji = "a".repeat(MonitorConfig.TITLE_MAX - 1) + "😀";
        String cut = MonitorConfig.sanitizeTitle(emoji);
        assertFalse(Character.isHighSurrogate(cut.charAt(cut.length() - 1)), "a character is never split in two");
    }

    @Test
    void rowsAndSeriesOfAnotherKindAreDropped() {
        MonitorConfig sent = new MonitorConfig("§kCore", MonitorLayout.BOTH,
                List.of(MonitorRow.TURBINE_SPEED, MonitorRow.REACTOR_FUEL, MonitorRow.REACTOR_TEMPERATURE),
                Set.of(MonitorRow.TOKAMAK_ASH, MonitorRow.REACTOR_XENON),
                GaugeReading.TURBINE_SPEED, GaugeReading.REACTOR_XENON);
        MonitorConfig clean = sent.sanitize(TargetKind.REACTOR);
        assertEquals("Core", clean.title());
        assertSame(MonitorLayout.BOTH, clean.layout());
        assertEquals(MonitorRow.REACTOR_FUEL, clean.rows().get(0), "the reactor rows keep their order");
        assertEquals(MonitorRow.REACTOR_TEMPERATURE, clean.rows().get(1));
        assertTrue(clean.rows().stream().allMatch(row -> row.kind() == TargetKind.REACTOR), "no turbine row on a reactor screen");
        assertEquals(MonitorRow.forKind(TargetKind.REACTOR).size(), clean.rows().size(), "the other reactor rows follow");
        assertEquals(Set.of(MonitorRow.REACTOR_XENON), clean.hidden(), "only reactor rows are hidden");
        assertNull(clean.seriesA(), "a turbine reading cannot be plotted for a reactor");
        assertSame(GaugeReading.REACTOR_XENON, clean.seriesB());
    }

    @Test
    void theUsualOrderIsStoredAsTheDefault() {
        MonitorConfig usual = MonitorConfig.DEFAULT.withRows(MonitorRow.forKind(TargetKind.TURBINE), Set.of());
        assertTrue(usual.sanitize(TargetKind.TURBINE).isDefault(), "the usual order with nothing hidden is no setup at all");
        MonitorConfig hidden = usual.withRows(MonitorRow.forKind(TargetKind.TURBINE), Set.of(MonitorRow.TURBINE_EXHAUST));
        assertTrue(hidden.sanitize(TargetKind.TURBINE).customRows(), "but hiding one is");
    }

    /**
     * With the target unloaded or out of reach the type is unknown, so editing the title must not
     * throw the rows and series away: they are kept as sent (the renderer draws only the target's
     * own rows anyway).
     */
    @Test
    void anUnlinkedScreenKeepsItsRowsAndSeriesAsSent() {
        MonitorConfig sent = new MonitorConfig("Spare", MonitorLayout.CHART, List.of(MonitorRow.REACTOR_FUEL),
                Set.of(MonitorRow.REACTOR_XENON), GaugeReading.REACTOR_FUEL, GaugeReading.REACTOR_POWER);
        for (TargetKind kind : new TargetKind[]{TargetKind.NONE, TargetKind.NO_SIGNAL}) {
            MonitorConfig clean = sent.sanitize(kind);
            assertEquals("Spare", clean.title());
            assertSame(MonitorLayout.CHART, clean.layout());
            assertEquals(List.of(MonitorRow.REACTOR_FUEL), clean.rows(), "the order is kept for " + kind);
            assertEquals(Set.of(MonitorRow.REACTOR_XENON), clean.hidden(), "and the hidden rows");
            assertSame(GaugeReading.REACTOR_FUEL, clean.seriesA());
            assertSame(GaugeReading.REACTOR_POWER, clean.seriesB());
        }
    }

    @Test
    void theSetupSurvivesNbt() {
        MonitorConfig config = new MonitorConfig("Unit 2", MonitorLayout.LIST,
                List.of(MonitorRow.SG_FORMED, MonitorRow.SG_LEVEL), Set.of(MonitorRow.SG_HEAT, MonitorRow.SG_PRIMARY_DT),
                GaugeReading.SG_STEAM, null);
        assertEquals(config, MonitorConfig.read(config.write()));
        assertEquals(MonitorConfig.DEFAULT, MonitorConfig.read(new net.minecraft.nbt.CompoundTag()), "an empty tag is the default");
    }

    @Test
    void theThirdLineAndStyleSurviveNbt() {
        MonitorConfig config = MonitorConfig.DEFAULT.withSeries(GaugeReading.REACTOR_FUEL, null)
                .withSeriesC(GaugeReading.REACTOR_XENON).withTheme(MonitorTheme.MIMIC_BOARD);
        MonitorConfig read = MonitorConfig.read(config.write());
        assertEquals(config, read);
        assertSame(GaugeReading.REACTOR_XENON, read.seriesC());
        assertSame(MonitorTheme.MIMIC_BOARD, read.theme());
    }

    /** Screens saved before the third line and the styles have neither key; nonsense reads as nothing. */
    @Test
    void oldSetupsLoadWithTheDefaults() {
        MonitorConfig old = new MonitorConfig("Unit 3", MonitorLayout.LIST, List.of(), Set.of(), GaugeReading.REACTOR_FUEL, null);
        net.minecraft.nbt.CompoundTag tag = old.write();
        tag.remove("SeriesC");
        tag.remove("Theme");
        MonitorConfig read = MonitorConfig.read(tag);
        assertEquals(old, read);
        assertNull(read.seriesC(), "no third line");
        assertSame(MonitorTheme.STANDARD, read.theme(), "the look they always had");
        tag.putString("SeriesC", "NOPE");
        tag.putString("Theme", "BOGUS");
        assertEquals(old, MonitorConfig.read(tag), "an unknown reading or style is none and standard");
    }

    @Test
    void aThirdLineOfAnotherKindIsDropped() {
        MonitorConfig sent = MonitorConfig.DEFAULT.withSeriesC(GaugeReading.TURBINE_SPEED).withTheme(MonitorTheme.PROCESS_COMPUTER);
        MonitorConfig clean = sent.sanitize(TargetKind.REACTOR);
        assertNull(clean.seriesC(), "a turbine reading cannot be plotted for a reactor");
        assertSame(MonitorTheme.PROCESS_COMPUTER, clean.theme(), "the style suits any target");
        assertSame(GaugeReading.TURBINE_SPEED, sent.sanitize(TargetKind.TURBINE).seriesC());
        MonitorConfig unlinked = sent.sanitize(TargetKind.NONE);
        assertSame(GaugeReading.TURBINE_SPEED, unlinked.seriesC(), "kept as sent while the type is unknown");
        assertSame(MonitorTheme.PROCESS_COMPUTER, unlinked.theme());
    }

    @Test
    void theStyleAloneIsASetupAndTheFirstLinesKeepTheThird() {
        assertFalse(MonitorConfig.DEFAULT.withTheme(MonitorTheme.MIMIC_BOARD).isDefault(), "a style is saved");
        assertFalse(MonitorConfig.DEFAULT.withSeriesC(GaugeReading.REACTOR_FUEL).isDefault(), "and so is a third line");
        MonitorConfig withC = MonitorConfig.DEFAULT.withSeriesC(GaugeReading.REACTOR_FUEL);
        assertSame(GaugeReading.REACTOR_FUEL, withC.withSeries(GaugeReading.REACTOR_POWER, null).seriesC(),
                "choosing the first two lines leaves the third");
        assertSame(MonitorTheme.STANDARD, new MonitorConfig("", null, null, null, null, null, null, null).theme(),
                "no style is the standard one");
    }
}
