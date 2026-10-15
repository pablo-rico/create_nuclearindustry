package org.papiricoh.create_nuclearindustry.maintenance.incident;

import org.papiricoh.create_nuclearindustry.maintenance.physics.InesScale;

import java.util.Locale;

/** What the incident log records, with the INES level each kind rates at least. */
public enum IncidentType {
    SCRAM(0),
    SEISMIC_SCRAM(0),
    TURBINE_TRIP(0),
    STUCK_ROD(1),
    RELIEF_VALVE_STUCK(1),
    SG_TUBE_RUPTURE(2),
    CORE_DAMAGE(4),
    VESSEL_BREACH(4),
    MELTDOWN(5),
    EARTHQUAKE(InesScale.NONE),
    DETONATION(InesScale.NONE),
    DRILL(InesScale.NONE);

    private final int ines;

    IncidentType(int ines) {
        this.ines = ines;
    }

    /** The lowest INES level this kind rates, or {@link InesScale#NONE} for events off the scale. */
    public int ines() {
        return ines;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static IncidentType byId(String id) {
        for (IncidentType type : values()) {
            if (type.id().equals(id)) {
                return type;
            }
        }
        return DRILL;
    }
}
