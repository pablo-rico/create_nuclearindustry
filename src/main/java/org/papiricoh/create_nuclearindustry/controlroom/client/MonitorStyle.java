package org.papiricoh.create_nuclearindustry.controlroom.client;

import org.papiricoh.create_nuclearindustry.controlroom.MonitorTheme;

/**
 * How a screen of one {@link MonitorTheme} is laid out and drawn: each style places the title,
 * readings, gauges and graph its own way over the same data ({@link MonitorFrame}).
 */
public interface MonitorStyle {
    /** Draws the screen of a linked target that has a signal; the backdrop is already there. */
    void draw(MonitorFrame frame);

    static MonitorStyle of(MonitorTheme theme) {
        return switch (theme) {
            case PROCESS_COMPUTER -> ProcessComputerStyle.INSTANCE;
            case MIMIC_BOARD -> MimicBoardStyle.INSTANCE;
            default -> StandardStyle.INSTANCE;
        };
    }
}
