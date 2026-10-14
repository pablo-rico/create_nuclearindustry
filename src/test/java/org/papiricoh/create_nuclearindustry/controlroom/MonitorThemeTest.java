package org.papiricoh.create_nuclearindustry.controlroom;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Screen styles are saved by name, so a name must always read back and nonsense must fall back. */
class MonitorThemeTest {

    @Test
    void everyStyleReadsBackByName() {
        Set<String> keys = new HashSet<>();
        for (MonitorTheme theme : MonitorTheme.values()) {
            assertSame(theme, MonitorTheme.byName(theme.name()));
            assertTrue(keys.add(theme.translationKey()), "one text per style: " + theme);
        }
    }

    @Test
    void anUnknownStyleIsTheStandardOne() {
        assertSame(MonitorTheme.STANDARD, MonitorTheme.byName(""));
        assertSame(MonitorTheme.STANDARD, MonitorTheme.byName("process_computer"), "names are exact");
        assertSame(MonitorTheme.STANDARD, MonitorTheme.byName("NEON"));
    }
}
