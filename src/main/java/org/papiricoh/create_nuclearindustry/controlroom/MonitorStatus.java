package org.papiricoh.create_nuclearindustry.controlroom;

/** How bad the state a screen shows on its title line is, from the status a target reports. */
public final class MonitorStatus {
    private MonitorStatus() {}

    /** {@link MonitorRow.Tone#BAD} for a trip or danger, {@link MonitorRow.Tone#GOOD} for normal running, otherwise WARN. */
    public static MonitorRow.Tone tone(String status) {
        if (status.contains("SCRAM") || status.equals("MELTING") || status.equals("Critical") || status.endsWith(".radiation")
                || status.endsWith(".tripped")) {
            return MonitorRow.Tone.BAD;
        }
        if (status.equals("Running") || status.endsWith(".running") || status.endsWith(".clear") || status.endsWith(".burning")) {
            return MonitorRow.Tone.GOOD;
        }
        return MonitorRow.Tone.WARN;
    }
}
