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
    AREA_GROUND(TargetKind.AREA, MonitorReading::seriesB),
    // Pressurized water loop, read through the reactor; 0 on a reactor that boils directly.
    REACTOR_PRESSURE(TargetKind.REACTOR, r -> r.value(R_PRESSURE) / PRESSURE_MAX),
    REACTOR_HOT_LEG(TargetKind.REACTOR, r -> r.value(R_T_HOT) / LEG_TEMPERATURE_MAX),
    REACTOR_COLD_LEG(TargetKind.REACTOR, r -> r.value(R_T_COLD) / LEG_TEMPERATURE_MAX),
    REACTOR_SUBCOOLING(TargetKind.REACTOR, r -> r.value(R_SUBCOOLING) / SUBCOOLING_MAX),
    PRESSURIZER_LEVEL(TargetKind.REACTOR, r -> r.value(R_PZR_LEVEL)),
    PRIMARY_FLOW(TargetKind.REACTOR, r -> r.value(R_FLOW) / FLOW_MAX),
    VESSEL_LEVEL(TargetKind.REACTOR, r -> r.value(R_VESSEL_LEVEL)),
    REACTOR_CONTAINMENT_PRESSURE(TargetKind.REACTOR, r -> r.value(R_CONT_PRESSURE) / CONTAINMENT_PRESSURE_MAX),
    SG_LEVEL(TargetKind.STEAM_GENERATOR, r -> r.value(S_LEVEL)),
    SG_STEAM(TargetKind.STEAM_GENERATOR, r -> r.value(S_STEAM) / SG_STEAM_MAX),
    SG_HEAT(TargetKind.STEAM_GENERATOR, r -> r.value(S_HEAT) / SG_HEAT_MAX),
    SG_PRIMARY_DT(TargetKind.STEAM_GENERATOR, r -> r.value(S_PRIMARY_DT) / SG_DT_MAX),
    CONTAINMENT_PRESSURE(TargetKind.CONTAINMENT, r -> r.value(N_PRESSURE) / CONTAINMENT_PRESSURE_MAX),
    CONTAINMENT_HYDROGEN(TargetKind.CONTAINMENT, r -> r.value(N_H2) / HYDROGEN_MAX);

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

    /** The readings that work on a target of {@code kind}, in order. */
    public static java.util.List<GaugeReading> forKind(TargetKind kind) {
        return java.util.Arrays.stream(values()).filter(reading -> reading.kind == kind).toList();
    }

    public String translationKey() {
        return "create_nuclearindustry.control_module.reading." + name().toLowerCase(Locale.ROOT);
    }

    /** The reading called {@code name}, or null for anything else (an empty name means none). */
    @org.jetbrains.annotations.Nullable
    public static GaugeReading parse(String name) {
        for (GaugeReading reading : values()) {
            if (reading.name().equals(name)) {
                return reading;
            }
        }
        return null;
    }

    public static GaugeReading byName(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return REACTOR_TEMPERATURE;
        }
    }
}
