package org.papiricoh.create_nuclearindustry.controlroom.desk;

import org.papiricoh.create_nuclearindustry.controlroom.MonitorReading;
import org.papiricoh.create_nuclearindustry.controlroom.TargetKind;

import java.util.Locale;
import java.util.function.ToDoubleFunction;

import static org.papiricoh.create_nuclearindustry.controlroom.MonitorReading.*;

/** What a gauge's needle shows, as a 0-1 fraction of full scale, from a console sample. */
public enum GaugeReading {
    REACTOR_TEMPERATURE(TargetKind.REACTOR, r -> r.value(R_TEMPERATURE) / 4000.0),
    REACTOR_POWER(TargetKind.REACTOR, r -> r.value(R_POWER) / 1000.0),
    REACTOR_RODS(TargetKind.REACTOR, r -> r.value(R_RODS)),
    REACTOR_XENON(TargetKind.REACTOR, r -> r.value(R_XENON) / 0.5),
    REACTOR_COOLANT(TargetKind.REACTOR, r -> r.value(R_COOLANT)),
    REACTOR_STEAM(TargetKind.REACTOR, r -> r.value(R_STEAM)),
    REACTOR_FUEL(TargetKind.REACTOR, r -> r.value(R_FUEL)),
    TURBINE_SPEED(TargetKind.TURBINE, r -> r.value(T_SPEED) / 256.0),
    TURBINE_LOAD(TargetKind.TURBINE, MonitorReading::seriesA),
    TOWER_LOAD(TargetKind.COOLING_TOWER, MonitorReading::seriesA),
    TOWER_WATER(TargetKind.COOLING_TOWER, r -> r.value(C_WATER)),
    PLASMA_TEMPERATURE(TargetKind.TOKAMAK, MonitorReading::seriesA),
    FUSION_POWER(TargetKind.TOKAMAK, MonitorReading::seriesB),
    CONFINEMENT(TargetKind.TOKAMAK, r -> r.value(K_CONFINEMENT)),
    INSTABILITY(TargetKind.TOKAMAK, r -> r.value(K_INSTABILITY) / 100.0),
    AREA_EXPOSURE(TargetKind.AREA, MonitorReading::seriesA),
    AREA_GROUND(TargetKind.AREA, MonitorReading::seriesB);

    private final TargetKind kind;
    private final ToDoubleFunction<MonitorReading> fraction;

    GaugeReading(TargetKind kind, ToDoubleFunction<MonitorReading> fraction) {
        this.kind = kind;
        this.fraction = fraction;
    }

    public TargetKind kind() {
        return kind;
    }

    /** The needle position, 0-1, or 0 when the sample is of another kind of machine. */
    public float fraction(MonitorReading reading) {
        if (reading.kind() != kind) {
            return 0;
        }
        return (float) Math.max(0, Math.min(1, fraction.applyAsDouble(reading)));
    }

    public String translationKey() {
        return "create_nuclearindustry.control_module.reading." + name().toLowerCase(Locale.ROOT);
    }

    public static GaugeReading byName(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return REACTOR_TEMPERATURE;
        }
    }
}
