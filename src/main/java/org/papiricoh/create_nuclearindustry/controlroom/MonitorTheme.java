package org.papiricoh.create_nuclearindustry.controlroom;

import java.util.Locale;

/**
 * How a control room screen looks, set with a wrench. Each style lays the same readings and graph
 * out its own way, with its own frames and panels; how many readings a page holds follows it
 * ({@link MonitorPaging}). Saved by name.
 * <ul>
 *   <li>{@link #STANDARD}: dark green glass and mint text, what every screen showed before.</li>
 *   <li>{@link #PROCESS_COMPUTER}: a 1980s plant computer's colour CRT page: a header band with the
 *   machine's tag and the time, boxed panels, a point-log table with segmented bar graphs and alarm
 *   flags, a trend with axes and a footer with the state, the alarms and the page.</li>
 *   <li>{@link #MIMIC_BOARD}: a 1970s hard-wired board: enamel, an engraved nameplate, the state in an
 *   annunciator window, a grid of instruments (a nameplate that lights up, a digital meter and an
 *   edgewise meter) and a strip-chart recorder.</li>
 * </ul>
 */
public enum MonitorTheme {
    STANDARD,
    PROCESS_COMPUTER,
    MIMIC_BOARD;

    public String translationKey() {
        return "create_nuclearindustry.monitor.setup.theme." + name().toLowerCase(Locale.ROOT);
    }

    /** The style called {@code name}, or {@link #STANDARD} for anything else. */
    public static MonitorTheme byName(String name) {
        for (MonitorTheme theme : values()) {
            if (theme.name().equals(name)) {
                return theme;
            }
        }
        return STANDARD;
    }
}
