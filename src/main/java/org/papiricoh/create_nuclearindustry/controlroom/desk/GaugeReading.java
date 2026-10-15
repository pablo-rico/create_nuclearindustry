package org.papiricoh.create_nuclearindustry.controlroom.desk;

import org.papiricoh.create_nuclearindustry.controlroom.MonitorReading;
import org.papiricoh.create_nuclearindustry.controlroom.TargetKind;
import org.papiricoh.create_nuclearindustry.controlroom.TokamakScales;

import java.util.Locale;
import java.util.function.ToDoubleFunction;

import static org.papiricoh.create_nuclearindustry.controlroom.MonitorReading.*;

/** What a gauge's needle shows, as a 0-1 fraction of full scale, from a console sample. */
public enum GaugeReading {
    REACTOR_TEMPERATURE(TargetKind.REACTOR, r -> r.value(R_TEMPERATURE) / 4000.0),
    REACTOR_POWER(TargetKind.REACTOR, r -> r.value(R_POWER) / Math.max(1.0, r.value(MonitorReading.R_RATED_POWER))),
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
    CONTAINMENT_HYDROGEN(TargetKind.CONTAINMENT, r -> r.value(N_H2) / HYDROGEN_MAX),
    /** Airborne activity on a logarithmic scale, as radiation monitors read it. Saved by name; new readings go last. */
    CONTAINMENT_ACTIVITY(TargetKind.CONTAINMENT, r -> MonitorReading.activityFraction(r.value(N_ACTIVITY))),
    // Tokamak startup and plasma control. Saved by name; new readings go last anyway.
    /** Q up to {@link TokamakScales#GAIN_MAX}; an ignited plasma pins the needle. */
    FUSION_GAIN(TargetKind.TOKAMAK, r -> r.value(K_GAIN) / TokamakScales.GAIN_MAX),
    PLASMA_DENSITY(TargetKind.TOKAMAK, r -> r.value(K_DENSITY) / TokamakScales.DENSITY_MAX),
    HELIUM_ASH(TargetKind.TOKAMAK, r -> r.value(K_ASH) / TokamakScales.ASH_MAX),
    MAGNETIC_FIELD(TargetKind.TOKAMAK, r -> r.value(K_FIELD)),
    MAGNET_TEMPERATURE(TargetKind.TOKAMAK, r -> r.value(K_MAGNET_TEMPERATURE) / TokamakScales.MAGNET_TEMPERATURE_MAX),
    /** Logarithmic, as vacuum gauges are: high vacuum at the bottom, atmosphere at the top. */
    CHAMBER_PRESSURE(TargetKind.TOKAMAK, r -> (r.value(K_VACUUM) - TokamakScales.VACUUM_MIN)
            / (TokamakScales.VACUUM_MAX - TokamakScales.VACUUM_MIN)),
    HEATING_POWER(TargetKind.TOKAMAK, r -> r.value(K_HEATING) / TokamakScales.HEATING_MAX),
    /** The fuelling setpoint as a share of the most the tokamak takes. */
    FUEL_RATE(TargetKind.TOKAMAK, r -> r.value(K_FUEL_RATE_MAX) > 0 ? r.value(K_FUEL_RATE) / r.value(K_FUEL_RATE_MAX) : 0),
    LIQUID_HELIUM(TargetKind.TOKAMAK, r -> r.value(K_LIQUID_HELIUM)),
    TRITIUM(TargetKind.TOKAMAK, r -> r.value(K_TRITIUM)),
    // Penning traps and the annihilation generator. Saved by name; new readings go last anyway.
    TRAP_ANTIMATTER(TargetKind.ANTIMATTER_TRAP, r -> r.value(P_FILL)),
    TRAP_FIELD(TargetKind.ANTIMATTER_TRAP, r -> r.value(P_FIELD)),
    TRAP_BUFFER(TargetKind.ANTIMATTER_TRAP, r -> r.value(P_BUFFER)),
    GENERATOR_OUTPUT(TargetKind.ANNIHILATION_GENERATOR, r -> r.value(G_OUTPUT)),
    GENERATOR_THROTTLE(TargetKind.ANNIHILATION_GENERATOR, r -> r.value(G_THROTTLE)),
    GENERATOR_CHAMBER(TargetKind.ANNIHILATION_GENERATOR, r -> r.value(G_CHAMBER_FILL)),
    GENERATOR_ENERGY(TargetKind.ANNIHILATION_GENERATOR, r -> r.value(G_RATED_FE) > 0 ? r.value(G_FE) / r.value(G_RATED_FE) : 0);

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

    /**
     * The pens a chart recorder starts with when linked to a target of {@code kind}: what an operator
     * watches first. A reactor with a pressurized loop gets its pressure, hot leg and pressurizer level;
     * a tokamak its fusion power, plasma temperature and density, the three that tell a burn apart.
     */
    public static java.util.List<GaugeReading> defaultPens(TargetKind kind, boolean pressurized) {
        return switch (kind) {
            case REACTOR -> pressurized ? java.util.List.of(REACTOR_PRESSURE, REACTOR_HOT_LEG, PRESSURIZER_LEVEL)
                    : java.util.List.of(REACTOR_TEMPERATURE, REACTOR_POWER);
            case STEAM_GENERATOR -> java.util.List.of(SG_LEVEL, SG_STEAM, SG_PRIMARY_DT);
            case CONTAINMENT -> java.util.List.of(CONTAINMENT_PRESSURE, CONTAINMENT_HYDROGEN, CONTAINMENT_ACTIVITY);
            case TOKAMAK -> java.util.List.of(FUSION_POWER, PLASMA_TEMPERATURE, PLASMA_DENSITY);
            case ANTIMATTER_TRAP -> java.util.List.of(TRAP_ANTIMATTER, TRAP_FIELD, TRAP_BUFFER);
            case ANNIHILATION_GENERATOR -> java.util.List.of(GENERATOR_OUTPUT, GENERATOR_THROTTLE, GENERATOR_CHAMBER);
            default -> forKind(kind).stream().limit(2).toList();
        };
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
