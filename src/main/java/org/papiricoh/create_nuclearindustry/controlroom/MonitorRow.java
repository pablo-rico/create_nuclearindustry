package org.papiricoh.create_nuclearindustry.controlroom;

import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.fusion.FusionConfig;
import org.papiricoh.create_nuclearindustry.reactor.physics.FissionConfig;
import org.papiricoh.create_nuclearindustry.fusion.api.TokamakPhase;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;

import static org.papiricoh.create_nuclearindustry.controlroom.MonitorReading.*;

/**
 * One line a control room screen can show: its label, how it formats the value, the gauge and the
 * colour that warns. Constants of a kind of target are declared in the order a screen lists them
 * when nobody set it up, most watched first, so a small screen keeps those. Saved by name: new rows
 * may go anywhere, a screen set up before simply shows them last.
 * <p>
 * No game classes: which rows a screen shows ({@link #visible}) is plain logic, tested on its own;
 * the renderer only draws the {@link Line}s.
 */
public enum MonitorRow {
    // ------------------------------------------------------------------ reactor
    REACTOR_TEMPERATURE(TargetKind.REACTOR, "temperature", r -> {
        float temperature = r.value(R_TEMPERATURE);
        return line(fmt("%.0f °C", temperature), temperature / 4000f,
                temperature >= FissionConfig.dangerTemperature() ? Tone.BAD
                        : temperature >= FissionConfig.warningTemperature() ? Tone.WARN : Tone.TEXT);
    }),
    REACTOR_POWER(TargetKind.REACTOR, "power", r -> {
        float rated = Math.max(1f, r.value(R_RATED_POWER));
        return line(fmt("%.0f MW", r.value(R_POWER)), r.value(R_POWER) / rated, r.value(R_POWER) > 1.05f * rated ? Tone.WARN : Tone.TEXT);
    }),
    /** Only once the fuel is damaged. */
    REACTOR_DAMAGE(TargetKind.REACTOR, "damage", r -> r.value(R_CORE_DAMAGE) > 0
            ? line(fmt("%.0f%%", r.value(R_CORE_DAMAGE) * 100), r.value(R_CORE_DAMAGE), Tone.BAD)
            : null),
    // A pressurized loop puts its own readings before the rods: they are the ones that go wrong.
    REACTOR_PRESSURE(TargetKind.REACTOR, "pressure", r -> pwr(r, () -> {
        float pressure = r.value(R_PRESSURE);
        return line(fmt("%.0f bar", pressure), pressure / PRESSURE_MAX, pressure >= 170 ? Tone.BAD : pressure < 100 ? Tone.WARN : Tone.TEXT);
    })),
    REACTOR_SUBCOOLING(TargetKind.REACTOR, "subcooling", r -> pwr(r, () -> {
        float subcooling = r.value(R_SUBCOOLING);
        return line(fmt("%.1f °C", subcooling), subcooling / SUBCOOLING_MAX,
                subcooling < 10 ? Tone.BAD : subcooling < 20 ? Tone.WARN : Tone.GOOD);
    })),
    REACTOR_PZR_LEVEL(TargetKind.REACTOR, "pzr_level", r -> pwr(r, () -> {
        float level = r.value(R_PZR_LEVEL);
        return line(fmt("%.0f%%", level * 100), level, level < 0.2f || level > 0.9f ? Tone.WARN : Tone.TEXT);
    })),
    REACTOR_FLOW(TargetKind.REACTOR, "flow", r -> pwr(r, () -> {
        float flow = r.value(R_FLOW);
        return line(fmt("%.0f mB/t", flow), flow / FLOW_MAX, flow <= 0 ? Tone.BAD : Tone.TEXT);
    })),
    REACTOR_RODS(TargetKind.REACTOR, "rods", r -> {
        float target = r.value(R_ROD_TARGET);
        String rods = fmt("%.0f%%", r.value(R_RODS) * 100) + (target >= 0 ? fmt(" > %.0f%%", target * 100) : "");
        return line(rods, r.value(R_RODS), Tone.TEXT);
    }),
    // The rest of the plant around a pressurized reactor: legs, containment and the core's water.
    REACTOR_LEGS(TargetKind.REACTOR, "legs", r -> pwr(r, () -> line(fmt("%.0f/%.0f °C", r.value(R_T_HOT), r.value(R_T_COLD)),
            r.value(R_T_HOT) / LEG_TEMPERATURE_MAX, Tone.TEXT))),
    REACTOR_RELIEF(TargetKind.REACTOR, "relief", r -> pwr(r, () -> translated(r.flag(R_RELIEF) ? "open" : "closed",
            r.flag(R_RELIEF) ? Tone.BAD : Tone.TEXT))),
    REACTOR_CONTAINMENT(TargetKind.REACTOR, "containment", r -> pwr(r, () -> {
        float containment = r.value(R_CONT_PRESSURE);
        return line(fmt("%.2f bar", containment), containment / CONTAINMENT_PRESSURE_MAX,
                containment >= 5 ? Tone.BAD : containment >= 2 ? Tone.WARN : Tone.TEXT);
    })),
    REACTOR_HYDROGEN(TargetKind.REACTOR, "hydrogen", r -> pwr(r, () -> hydrogen(r.value(R_H2)))),
    REACTOR_VESSEL(TargetKind.REACTOR, "vessel", r -> pwr(r, () -> {
        float vessel = r.value(R_VESSEL_LEVEL);
        return line(fmt("%.0f%%", vessel * 100), vessel, vessel < 0.9f ? Tone.BAD : Tone.TEXT);
    })),
    REACTOR_XENON(TargetKind.REACTOR, "xenon", r -> line(fmt("-%.0f%%", r.value(R_XENON) * 100), r.value(R_XENON) / 0.5f,
            r.value(R_XENON) >= 0.15f ? Tone.WARN : Tone.TEXT)),
    REACTOR_COOLANT(TargetKind.REACTOR, "coolant", r -> line(fmt("%.0f%%", r.value(R_COOLANT) * 100), r.value(R_COOLANT),
            r.value(R_COOLANT) < 0.2f ? Tone.BAD : Tone.TEXT)),
    REACTOR_REACTIVITY(TargetKind.REACTOR, "reactivity", r -> line(fmt("%+.0f pcm", r.value(R_NET_REACTIVITY)), -1, Tone.TEXT)),
    /** What a SCRAM (and the boron) would hold back once the core is cold: below the required margin it would restart. */
    REACTOR_SHUTDOWN_MARGIN(TargetKind.REACTOR, "shutdown_margin", r -> {
        float margin = r.value(R_COLD_MARGIN);
        double required = FissionConfig.requiredShutdownMargin;
        return line(fmt("%+.0f pcm", margin), margin / SHUTDOWN_MARGIN_MAX,
                margin < required ? Tone.BAD : margin < 2 * required ? Tone.WARN : Tone.GOOD);
    }),
    /** Only once boric acid put boron in the water. */
    REACTOR_BORON(TargetKind.REACTOR, "boron", r -> r.value(R_BORON_PPM) >= 0.5f
            ? line(fmt("%.0f ppm", r.value(R_BORON_PPM)), r.value(R_BORON_PPM) / BORON_MAX, Tone.TEXT)
            : null),
    REACTOR_COOLANT_TEMPERATURE(TargetKind.REACTOR, "coolant_temperature", r -> line(fmt("%.0f °C", r.value(R_COOLANT_TEMP)),
            r.value(R_COOLANT_TEMP) / 4000f, Tone.TEXT)),
    REACTOR_DECAY_HEAT(TargetKind.REACTOR, "decay_heat", r -> line(fmt("%.0f MW", r.value(R_DECAY_HEAT)), -1, Tone.TEXT)),
    REACTOR_STEAM(TargetKind.REACTOR, "steam", r -> line(fmt("%.0f%%", r.value(R_STEAM) * 100), r.value(R_STEAM), Tone.TEXT)),
    REACTOR_FUEL(TargetKind.REACTOR, "fuel", r -> line(fmt("%.0f%%", r.value(R_FUEL) * 100), r.value(R_FUEL), Tone.TEXT)),
    REACTOR_NEUTRONS(TargetKind.REACTOR, "neutrons", r -> line(fmt("%.0f", r.value(R_NEUTRONS)), r.value(R_NEUTRONS) / 1000f, Tone.TEXT)),
    REACTOR_STRESS(TargetKind.REACTOR, "stress", r -> line(fmt("%.0f%%", r.value(R_STRESS)), r.value(R_STRESS) / 100f,
            r.value(R_STRESS) >= 65 ? Tone.BAD : Tone.TEXT)),

    // ------------------------------------------------------------------ turbine
    TURBINE_SPEED(TargetKind.TURBINE, "speed", r -> line(fmt("%.0f RPM", r.value(T_SPEED)), r.value(T_SPEED) / 256f, Tone.TEXT)),
    TURBINE_STEAM_USE(TargetKind.TURBINE, "steam_use", r -> line(fmt("%.0f/%.0f mB/t", r.value(T_USE), r.value(T_LIMIT)), r.seriesA(), Tone.TEXT)),
    TURBINE_CAPACITY(TargetKind.TURBINE, "capacity", r -> line(fmt("%.0f su", r.value(T_CAPACITY)), -1, Tone.TEXT)),
    TURBINE_STEAM_TANK(TargetKind.TURBINE, "steam_tank", r -> line(fmt("%.0f%%", r.value(T_STEAM) * 100), r.value(T_STEAM), Tone.TEXT)),
    TURBINE_EXHAUST(TargetKind.TURBINE, "exhaust", r -> translated(r.flag(T_EXHAUST) ? "yes" : "no", Tone.TEXT)),

    // ------------------------------------------------------------------ cooling tower
    TOWER_CONDENSING(TargetKind.COOLING_TOWER, "condensing", r -> line(fmt("%.0f/%.0f mB/t", r.value(C_USE), r.value(C_CAPACITY)),
            r.seriesA(), Tone.TEXT)),
    TOWER_WATER(TargetKind.COOLING_TOWER, "water", r -> line(fmt("%.0f%%", r.value(C_WATER) * 100), r.value(C_WATER), Tone.TEXT)),
    TOWER_STEAM_TANK(TargetKind.COOLING_TOWER, "steam_tank", r -> line(fmt("%.0f%%", r.value(C_STEAM) * 100), r.value(C_STEAM),
            r.value(C_STEAM) > 0.9f ? Tone.WARN : Tone.TEXT)),
    TOWER_FANS(TargetKind.COOLING_TOWER, "fans", r -> line(fmt("%.0f", r.value(C_FANS)), -1, Tone.TEXT)),

    // ------------------------------------------------------------------ tokamak
    // Why it tripped, the plasma and what it gives, then what holds it (field, magnets, vacuum) and what feeds it.
    /** Only while the tokamak is tripped. */
    TOKAMAK_TRIP(TargetKind.TOKAMAK, "trip", r -> r.flag(K_TRIPPED)
            ? new Line(null, TokamakScales.trip(r.value(K_TRIP)).langKey(), true, -1, Tone.BAD)
            : null),
    TOKAMAK_PLASMA(TargetKind.TOKAMAK, "plasma", r -> {
        float plasma = r.value(K_TEMPERATURE);
        return line(fmt("%.0f °", plasma), r.seriesA(), plasma >= TokamakScales.DANGER_TEMPERATURE ? Tone.BAD
                : plasma >= TokamakScales.IGNITION_TEMPERATURE ? Tone.GOOD : Tone.TEXT);
    }),
    TOKAMAK_POWER(TargetKind.TOKAMAK, "power", r -> line(fmt("%.0f MW", r.value(K_POWER)), r.seriesB(), Tone.TEXT)),
    TOKAMAK_GAIN(TargetKind.TOKAMAK, "gain", r -> {
        float gain = r.value(K_GAIN);
        TokamakPhase phase = TokamakScales.phase(r.value(K_PHASE));
        return line(TokamakScales.formatGain(gain), gain / TokamakScales.GAIN_MAX, phase != TokamakPhase.BURN ? Tone.TEXT
                : TokamakScales.ignited(gain) ? Tone.GOOD : gain < TokamakScales.NET_GAIN ? Tone.WARN : Tone.TEXT);
    }),
    TOKAMAK_DENSITY(TargetKind.TOKAMAK, "density", r -> {
        float density = r.value(K_DENSITY);
        return line(fmt("%.0f%%", density * 100), density / TokamakScales.DENSITY_MAX,
                density >= 1.0f ? Tone.BAD : density >= 0.9f ? Tone.WARN : Tone.TEXT);
    }),
    TOKAMAK_INSTABILITY(TargetKind.TOKAMAK, "instability", r -> {
        float instability = r.value(K_INSTABILITY);
        return line(fmt("%.0f%%", instability), instability / TokamakScales.DISRUPTION_INSTABILITY,
                instability >= TokamakScales.INSTABILITY_WARNING ? Tone.BAD : Tone.TEXT);
    }),
    TOKAMAK_FIELD(TargetKind.TOKAMAK, "field", r -> line(fmt("%.0f%%", r.value(K_FIELD) * 100), r.value(K_FIELD), Tone.TEXT)),
    TOKAMAK_MAGNETS(TargetKind.TOKAMAK, "magnets", r -> {
        float magnets = r.value(K_MAGNET_TEMPERATURE);
        boolean superconducting = magnets <= TokamakScales.SUPERCONDUCTING_LIMIT;
        return line(fmt(magnets < 100 ? "%.1f K" : "%.0f K", magnets), magnets / TokamakScales.MAGNET_TEMPERATURE_MAX,
                superconducting ? Tone.GOOD : r.value(K_FIELD) > 0.01f ? Tone.BAD : Tone.TEXT);
    }),
    TOKAMAK_VACUUM(TargetKind.TOKAMAK, "vacuum", r -> {
        float vacuum = r.value(K_VACUUM);
        return line(TokamakScales.formatPressure(vacuum),
                (vacuum - TokamakScales.VACUUM_MIN) / (TokamakScales.VACUUM_MAX - TokamakScales.VACUUM_MIN),
                vacuum <= FusionConfig.vacuumLimit ? Tone.GOOD : TokamakScales.phase(r.value(K_PHASE)).hasPlasma() ? Tone.BAD : Tone.TEXT);
    }),
    TOKAMAK_HEATING(TargetKind.TOKAMAK, "heating", r -> r.flag(K_HEATING_ON)
            ? line(fmt("%.1f MW", r.value(K_HEATING)), r.value(K_HEATING) / TokamakScales.HEATING_MAX, Tone.TEXT)
            : translated("off", Tone.DIM)),
    TOKAMAK_FUEL_RATE(TargetKind.TOKAMAK, "fuel_rate", r -> {
        float maxFuel = r.value(K_FUEL_RATE_MAX);
        return line(fmt("%.2f mB/t", r.value(K_FUEL_RATE)), maxFuel > 0 ? r.value(K_FUEL_RATE) / maxFuel : 0, Tone.TEXT);
    }),
    TOKAMAK_HELIUM(TargetKind.TOKAMAK, "helium", r -> {
        float helium = r.value(K_LIQUID_HELIUM);
        return line(fmt("%.0f%%", helium * 100), helium, helium < 0.1f ? Tone.WARN : Tone.TEXT);
    }),
    TOKAMAK_CONFINEMENT(TargetKind.TOKAMAK, "confinement", r -> line(fmt("%.0f%%", r.value(K_CONFINEMENT) * 100), r.value(K_CONFINEMENT),
            r.value(K_CONFINEMENT) < TokamakScales.MIN_BURN_CONFINEMENT ? Tone.WARN : Tone.TEXT)),
    TOKAMAK_ASH(TargetKind.TOKAMAK, "ash", r -> {
        float ash = r.value(K_ASH);
        return line(fmt("%.1f%%", ash * 100), ash / TokamakScales.ASH_MAX, ash >= 0.1f ? Tone.WARN : Tone.TEXT);
    }),
    TOKAMAK_DEUTERIUM(TargetKind.TOKAMAK, "deuterium", r -> line(fmt("%.0f%%", r.value(K_DEUTERIUM) * 100), r.value(K_DEUTERIUM), Tone.TEXT)),
    TOKAMAK_TRITIUM(TargetKind.TOKAMAK, "tritium", r -> line(fmt("%.0f%%", r.value(K_TRITIUM) * 100), r.value(K_TRITIUM), Tone.TEXT)),

    // ------------------------------------------------------------------ area
    AREA_EXPOSURE(TargetKind.AREA, "exposure", r -> {
        float exposure = r.value(A_EXPOSURE);
        return line(fmt("%.2f rad/s", exposure), r.seriesA(), exposure >= 1 ? Tone.BAD : exposure >= 0.05f ? Tone.WARN : Tone.GOOD);
    }),
    AREA_GROUND(TargetKind.AREA, "ground", r -> line(fmt("%.2f rad/s", r.value(A_GROUND)), r.seriesB(),
            r.value(A_GROUND) >= 0.05f ? Tone.WARN : Tone.GOOD)),

    // ------------------------------------------------------------------ steam generator
    SG_LEVEL(TargetKind.STEAM_GENERATOR, "level", r -> {
        float level = r.value(S_LEVEL);
        return line(fmt("%.0f%%", level * 100), level, level < 0.2f ? Tone.BAD : level < 0.35f ? Tone.WARN : Tone.TEXT);
    }),
    SG_STEAM_MADE(TargetKind.STEAM_GENERATOR, "steam_made", r -> line(fmt("%.0f mB/t", r.value(S_STEAM)), r.value(S_STEAM) / SG_STEAM_MAX, Tone.TEXT)),
    SG_HEAT(TargetKind.STEAM_GENERATOR, "heat", r -> line(fmt("%.0f MW", r.value(S_HEAT)), r.value(S_HEAT) / SG_HEAT_MAX, Tone.TEXT)),
    SG_PRIMARY_DT(TargetKind.STEAM_GENERATOR, "primary_dt", r -> line(fmt("%.0f °C", r.value(S_PRIMARY_DT)),
            r.value(S_PRIMARY_DT) / SG_DT_MAX, Tone.TEXT)),
    SG_FORMED(TargetKind.STEAM_GENERATOR, "formed", r -> translated(r.flag(S_FORMED) ? "yes" : "no", r.flag(S_FORMED) ? Tone.GOOD : Tone.BAD)),

    // ------------------------------------------------------------------ containment
    CONTAINMENT_PRESSURE(TargetKind.CONTAINMENT, "pressure", r -> {
        float pressure = r.value(N_PRESSURE);
        return line(fmt("%.2f bar", pressure), pressure / CONTAINMENT_PRESSURE_MAX,
                pressure >= 5 ? Tone.BAD : pressure >= 2 ? Tone.WARN : Tone.TEXT);
    }),
    CONTAINMENT_HYDROGEN(TargetKind.CONTAINMENT, "hydrogen", r -> hydrogen(r.value(N_H2))),
    CONTAINMENT_SEALED(TargetKind.CONTAINMENT, "sealed", r -> translated(r.flag(N_SEALED) ? "yes" : "no",
            r.flag(N_SEALED) ? Tone.GOOD : Tone.BAD)),
    CONTAINMENT_VENT(TargetKind.CONTAINMENT, "vent", r -> translated(r.flag(N_VENT) ? "open" : "closed",
            r.flag(N_VENT) ? Tone.WARN : Tone.TEXT)),
    CONTAINMENT_ACTIVITY(TargetKind.CONTAINMENT, "activity", r -> line(fmt("%.2f", r.value(N_ACTIVITY)),
            activityFraction(r.value(N_ACTIVITY)), r.value(N_ACTIVITY) > 0 ? Tone.WARN : Tone.TEXT)),
    CONTAINMENT_VOLUME(TargetKind.CONTAINMENT, "volume", r -> line(fmt("%.0f m³", r.value(N_VOLUME)), -1, Tone.TEXT)),

    // ------------------------------------------------------------------ Penning trap
    TRAP_STORED(TargetKind.ANTIMATTER_TRAP, "antimatter", r -> line(fmt("%.2f ng", r.value(P_STORED)), r.value(P_FILL),
            r.value(P_FILL) >= 0.95f ? Tone.WARN : Tone.TEXT)),
    TRAP_STATE(TargetKind.ANTIMATTER_TRAP, "trap_state", r -> {
        int state = Math.round(r.value(P_STATE));
        // TrapState by ordinal: OFFLINE, STABLE, LOW_POWER, QUENCHING
        return switch (state) {
            case 1 -> translated("trap_stable", Tone.GOOD);
            case 2 -> translated("trap_low_power", Tone.WARN);
            case 3 -> translated("trap_quenching", Tone.BAD);
            default -> translated("trap_offline", Tone.DIM);
        };
    }),
    TRAP_FIELD(TargetKind.ANTIMATTER_TRAP, "field", r -> line(fmt("%.0f%%", r.value(P_FIELD) * 100), r.value(P_FIELD),
            r.value(P_FIELD) < 0.999f && r.value(P_STORED) > 0 ? Tone.BAD : Tone.TEXT)),
    TRAP_BUFFER(TargetKind.ANTIMATTER_TRAP, "trap_buffer", r -> line(fmt("%.0f%%", r.value(P_BUFFER) * 100), r.value(P_BUFFER),
            r.value(P_BUFFER) < 0.25f ? Tone.WARN : Tone.TEXT)),
    TRAP_DEMAND(TargetKind.ANTIMATTER_TRAP, "trap_demand", r -> line(fmt("%.0f FE/t", r.value(P_DEMAND)), -1, Tone.TEXT)),

    // ------------------------------------------------------------------ annihilation generator
    GENERATOR_OUTPUT(TargetKind.ANNIHILATION_GENERATOR, "gen_output", r -> line(fmt("%.0f%%", r.value(G_OUTPUT) * 100), r.value(G_OUTPUT),
            r.value(G_OUTPUT) + 0.01f < r.value(G_THROTTLE) ? Tone.WARN : r.value(G_OUTPUT) > 0 ? Tone.GOOD : Tone.DIM)),
    GENERATOR_STATUS(TargetKind.ANNIHILATION_GENERATOR, "gen_status", r -> {
        // TrapState by ordinal: OFFLINE, STABLE, LOW_POWER, QUENCHING
        if (Math.round(r.value(G_STATE)) == 3) {
            return translated("gen_quenching", Tone.BAD);
        }
        boolean lit = r.flag(G_LIT);
        if (!lit && r.value(G_STORED) > 0) {
            return translated("gen_no_field", Tone.BAD);
        }
        if (!lit) {
            return translated("gen_cold", Tone.DIM);
        }
        return r.value(G_OUTPUT) > 0 ? translated("gen_running", Tone.GOOD) : translated("gen_standby", Tone.WARN);
    }),
    GENERATOR_MODE(TargetKind.ANNIHILATION_GENERATOR, "gen_mode", r -> switch (Math.round(r.value(G_MODE))) {
        case 1 -> translated("gen_mode_electric", Tone.TEXT);
        case 2 -> translated("gen_mode_split", Tone.TEXT);
        default -> translated("gen_mode_rotation", Tone.TEXT);
    }),
    GENERATOR_THROTTLE(TargetKind.ANNIHILATION_GENERATOR, "gen_throttle", r -> line(fmt("%.0f%%", r.value(G_THROTTLE) * 100),
            r.value(G_THROTTLE), Tone.TEXT)),
    GENERATOR_CHAMBER(TargetKind.ANNIHILATION_GENERATOR, "antimatter", r -> line(fmt("%.3f ng", r.value(G_STORED)),
            r.value(G_CHAMBER_FILL), r.value(G_STORED) <= 0 ? Tone.WARN : Tone.TEXT)),
    GENERATOR_ENERGY(TargetKind.ANNIHILATION_GENERATOR, "gen_energy", r -> line(fmt("%.0f FE/t", r.value(G_FE)),
            r.value(G_RATED_FE) > 0 ? r.value(G_FE) / r.value(G_RATED_FE) : -1, Tone.TEXT)),
    GENERATOR_STRESS(TargetKind.ANNIHILATION_GENERATOR, "gen_stress", r -> line(fmt("%.0f SU", r.value(G_STRESS)), -1, Tone.TEXT)),
    GENERATOR_BURN(TargetKind.ANNIHILATION_GENERATOR, "gen_burn", r -> line(fmt("%.2f ng/min", r.value(G_BURN)), -1, Tone.TEXT)),

    // ------------------------------------------------------------------ nuclear engines
    THERMAL_THRUST(TargetKind.THERMAL_ENGINE, "eng_thrust", MonitorRow::engineThrust),
    THERMAL_THROTTLE(TargetKind.THERMAL_ENGINE, "eng_throttle", MonitorRow::engineThrottle),
    THERMAL_NOZZLE(TargetKind.THERMAL_ENGINE, "eng_nozzle", MonitorRow::engineTemperature),
    THERMAL_WEAR(TargetKind.THERMAL_ENGINE, "eng_wear", MonitorRow::engineWear),
    THERMAL_PROPELLANT(TargetKind.THERMAL_ENGINE, "eng_propellant", r -> line(fmt("%.0f%%", r.value(E_FUEL) * 100), r.value(E_FUEL),
            r.value(E_FUEL) < 0.1f ? Tone.WARN : Tone.TEXT)),
    THERMAL_FLOW(TargetKind.THERMAL_ENGINE, "eng_flow", r -> line(fmt("%.0f mB/t", r.value(E_FLOW)), -1, Tone.TEXT)),
    ANTIMATTER_THRUST(TargetKind.ANTIMATTER_ENGINE, "eng_thrust", MonitorRow::engineThrust),
    ANTIMATTER_THROTTLE(TargetKind.ANTIMATTER_ENGINE, "eng_throttle", MonitorRow::engineThrottle),
    ANTIMATTER_COIL(TargetKind.ANTIMATTER_ENGINE, "eng_coil", MonitorRow::engineTemperature),
    ANTIMATTER_WEAR(TargetKind.ANTIMATTER_ENGINE, "eng_wear", MonitorRow::engineWear),
    ANTIMATTER_CHAMBER(TargetKind.ANTIMATTER_ENGINE, "antimatter", r -> line(fmt("%.3f ng", r.value(E_STORED)), r.value(E_FUEL),
            r.value(E_STORED) <= 0 ? Tone.WARN : Tone.TEXT)),
    ANTIMATTER_BURN(TargetKind.ANTIMATTER_ENGINE, "eng_burn", r -> line(fmt("%.2f ng/min", r.value(E_FLOW)), -1, Tone.TEXT)),
    ANTIMATTER_FIELD(TargetKind.ANTIMATTER_ENGINE, "eng_field", r -> line(fmt("%.0f%%", r.value(E_ENERGY) * 100), r.value(E_ENERGY),
            r.value(E_ENERGY) < 0.1f ? Tone.BAD : r.value(E_ENERGY) < 0.3f ? Tone.WARN : Tone.TEXT)),

    // ------------------------------------------------------------------ missile radars
    RADAR_TRACKS(TargetKind.RADAR, "radar_tracks", r -> !r.flag(MonitorReading.D_ONLINE) ? translated("radar_offline", Tone.DIM)
            : line(fmt("%.0f", r.value(MonitorReading.D_TRACKS)), Math.min(1, r.value(MonitorReading.D_TRACKS) / 8f),
            r.value(MonitorReading.D_TRACKS) > 0 ? Tone.WARN : Tone.GOOD)),
    RADAR_THREATS(TargetKind.RADAR, "radar_threats", r -> line(fmt("%.0f", r.value(MonitorReading.D_THREATS)), -1,
            r.value(MonitorReading.D_THREATS) > 0 ? Tone.BAD : Tone.GOOD)),
    RADAR_ETA(TargetKind.RADAR, "radar_eta", r -> r.value(MonitorReading.D_TRACKS) <= 0 ? null
            : line(fmt("%.0f s", r.value(MonitorReading.D_ETA)), -1, r.value(MonitorReading.D_THREATS) > 0 ? Tone.BAD : Tone.TEXT)),
    RADAR_RANGE(TargetKind.RADAR, "radar_range", r -> r.value(MonitorReading.D_RANGE) <= 0 ? translated("radar_all", Tone.TEXT)
            : line(fmt("%.0f m", r.value(MonitorReading.D_RANGE)), -1, Tone.TEXT));

    private static Line engineThrust(MonitorReading r) {
        if (r.flag(E_FAILED)) {
            return translated("eng_failed", Tone.BAD);
        }
        return line(fmt("%.0f", r.value(E_THRUST)), -1, r.value(E_THRUST) > 0 ? Tone.GOOD : Tone.DIM);
    }

    private static Line engineThrottle(MonitorReading r) {
        return line(fmt("%.0f%%", r.value(E_THROTTLE) * 100), r.value(E_THROTTLE), Tone.TEXT);
    }

    private static Line engineTemperature(MonitorReading r) {
        float limit = Math.max(1, r.value(E_LIMIT));
        float share = r.value(E_TEMPERATURE) / limit;
        return line(fmt("%.0f °C", r.value(E_TEMPERATURE)), Math.min(1, share), share > 1 ? Tone.BAD : share > 0.85f ? Tone.WARN : Tone.TEXT);
    }

    private static Line engineWear(MonitorReading r) {
        return line(fmt("%.0f%%", r.value(E_WEAR) * 100), r.value(E_WEAR), r.value(E_WEAR) > 0.5f ? Tone.BAD : r.value(E_WEAR) > 0 ? Tone.WARN : Tone.GOOD);
    }

    /** Prefix of the row labels and of the translated values (yes, no, open, closed, off). */
    public static final String KEY = "create_nuclearindustry.monitor.";

    /** How a value is coloured: the renderer picks the colours. */
    public enum Tone { TEXT, DIM, GOOD, WARN, BAD }

    /**
     * One drawn line. {@code value} is a translation key when {@code translated}, otherwise the text
     * itself; {@code gauge} is the bar's 0-1 fill, or negative for no bar.
     */
    public record Line(MonitorRow row, String value, boolean translated, float gauge, Tone tone) {
        public String labelKey() {
            return row.labelKey();
        }

        private Line of(MonitorRow owner) {
            return new Line(owner, value, translated, gauge, tone);
        }
    }

    private static final MonitorRow[] VALUES = values();

    private final TargetKind kind;
    private final String label;
    private final Function<MonitorReading, Line> line;

    MonitorRow(TargetKind kind, String label, Function<MonitorReading, Line> line) {
        this.kind = kind;
        this.label = label;
        this.line = line;
    }

    public TargetKind kind() {
        return kind;
    }

    /** The label's translation key, one of the screen's own {@code monitor.*} keys. */
    public String labelKey() {
        return KEY + label;
    }

    /**
     * This row for {@code reading}, or null when it does not apply: a reading of another kind,
     * fuel that is not damaged, a reactor without a pressurized loop, a tokamak that has not tripped.
     */
    @Nullable
    public Line line(MonitorReading reading) {
        if (reading.kind() != kind) {
            return null;
        }
        Line made = line.apply(reading);
        return made == null ? null : made.of(this);
    }

    /** The rows of a target of {@code kind}, in the order a screen nobody set up lists them. */
    public static List<MonitorRow> forKind(TargetKind kind) {
        List<MonitorRow> rows = new ArrayList<>();
        for (MonitorRow row : VALUES) {
            if (row.kind == kind) {
                rows.add(row);
            }
        }
        return rows;
    }

    /**
     * Every row of {@code kind} in the order {@code config} puts them: the rows it lists first, then
     * those it does not (new ones, or every one for a screen nobody set up) in their usual order.
     * Hidden rows are included; this is what a setup screen lists.
     */
    public static List<MonitorRow> order(TargetKind kind, MonitorConfig config) {
        List<MonitorRow> order = new ArrayList<>();
        for (MonitorRow row : config.rows()) {
            if (row.kind == kind && !order.contains(row)) {
                order.add(row);
            }
        }
        for (MonitorRow row : forKind(kind)) {
            if (!order.contains(row)) {
                order.add(row);
            }
        }
        return order;
    }

    /**
     * What a screen set up as {@code config} draws for {@code reading}, in order: the rows of the
     * reading's kind as {@link #order} arranges them, without the hidden ones and without those that
     * do not apply now. Empty for an unlinked screen or one without signal.
     */
    public static List<Line> visible(MonitorReading reading, MonitorConfig config) {
        List<Line> lines = new ArrayList<>();
        Set<MonitorRow> hidden = config.hidden().isEmpty() ? Set.of() : EnumSet.copyOf(config.hidden());
        for (MonitorRow row : order(reading.kind(), config)) {
            if (hidden.contains(row)) {
                continue;
            }
            Line made = row.line(reading);
            if (made != null) {
                lines.add(made);
            }
        }
        return lines;
    }

    /** The row called {@code name}, or null for anything else. */
    @Nullable
    public static MonitorRow parse(String name) {
        for (MonitorRow row : VALUES) {
            if (row.name().equals(name)) {
                return row;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ building lines

    private static Line line(String value, float gauge, Tone tone) {
        return new Line(null, value, false, gauge, tone);
    }

    private static Line translated(String key, Tone tone) {
        return new Line(null, KEY + key, true, -1, tone);
    }

    private static Line hydrogen(float hydrogen) {
        return line(fmt("%.1f%%", hydrogen * 100), hydrogen / HYDROGEN_MAX,
                hydrogen >= 0.04f ? Tone.BAD : hydrogen >= 0.02f ? Tone.WARN : Tone.TEXT);
    }

    /** Rows of the pressurized loop: none on a reactor that boils its water directly. */
    @Nullable
    private static Line pwr(MonitorReading reading, java.util.function.Supplier<Line> line) {
        return reading.flag(R_PWR) ? line.get() : null;
    }

    private static String fmt(String format, Object... args) {
        return String.format(Locale.ROOT, format, args);
    }
}
