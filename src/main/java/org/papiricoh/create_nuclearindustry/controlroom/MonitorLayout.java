package org.papiricoh.create_nuclearindustry.controlroom;

import java.util.Locale;

/**
 * What a control room screen shows, set with a wrench. Saved by name.
 * <ul>
 *   <li>{@link #AUTO}: a big screen switches between the readings and the graph (with both side by
 *   side when it is at least three blocks wide and two high); a single block steps through its
 *   readings. What every screen did before it could be set up.</li>
 *   <li>{@link #LIST}: the readings only.</li>
 *   <li>{@link #CHART}: the history graph only.</li>
 *   <li>{@link #BOTH}: readings and graph together on any screen of two blocks or more; a single
 *   block shows the graph after its readings.</li>
 * </ul>
 */
public enum MonitorLayout {
    AUTO,
    LIST,
    CHART,
    BOTH;

    public String translationKey() {
        return "create_nuclearindustry.monitor.setup.layout." + name().toLowerCase(Locale.ROOT);
    }

    /** The layout called {@code name}, or {@link #AUTO} for anything else. */
    public static MonitorLayout byName(String name) {
        for (MonitorLayout layout : values()) {
            if (layout.name().equals(name)) {
                return layout;
            }
        }
        return AUTO;
    }
}
