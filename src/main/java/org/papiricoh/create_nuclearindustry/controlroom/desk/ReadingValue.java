package org.papiricoh.create_nuclearindustry.controlroom.desk;

import org.papiricoh.create_nuclearindustry.controlroom.MonitorReading;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.ToDoubleFunction;

import static org.papiricoh.create_nuclearindustry.controlroom.MonitorReading.*;

/**
 * A reading in the units an LED display shows it, with the same units and decimals as the
 * monitors' rows, where a gauge only needs a 0-1 needle position. Shares are shown in percent,
 * the chamber's vacuum as the log10 of its pressure in pascals. Every {@link GaugeReading} must be
 * listed here, which the switch enforces.
 */
public final class ReadingValue {
    private record Spec(ToDoubleFunction<MonitorReading> value, String unit, int decimals) {}

    private static final Map<GaugeReading, Spec> SPECS = new EnumMap<>(GaugeReading.class);

    static {
        for (GaugeReading reading : GaugeReading.values()) {
            SPECS.put(reading, spec(reading));
        }
    }

    private ReadingValue() {}

    private static Spec spec(GaugeReading reading) {
        return switch (reading) {
            case REACTOR_TEMPERATURE -> of(R_TEMPERATURE, "°C", 0);
            case REACTOR_POWER -> of(R_POWER, "MW", 0);
            case REACTOR_RODS -> percent(R_RODS, 0);
            case REACTOR_XENON -> percent(R_XENON, 0);
            case REACTOR_COOLANT -> percent(R_COOLANT, 0);
            case REACTOR_STEAM -> percent(R_STEAM, 0);
            case REACTOR_FUEL -> percent(R_FUEL, 0);
            case TURBINE_SPEED -> of(T_SPEED, "RPM", 0);
            case TURBINE_LOAD, TOWER_LOAD -> new Spec(r -> r.seriesA() * 100.0, "%", 0);
            case TOWER_WATER -> percent(C_WATER, 0);
            case PLASMA_TEMPERATURE -> of(K_TEMPERATURE, "°", 0);
            case FUSION_POWER -> of(K_POWER, "MW", 0);
            case CONFINEMENT -> percent(K_CONFINEMENT, 0);
            case INSTABILITY -> of(K_INSTABILITY, "%", 0);
            case AREA_EXPOSURE -> of(A_EXPOSURE, "rad/s", 2);
            case AREA_GROUND -> of(A_GROUND, "rad/s", 2);
            case REACTOR_PRESSURE -> of(R_PRESSURE, "bar", 1);
            case REACTOR_HOT_LEG -> of(R_T_HOT, "°C", 0);
            case REACTOR_COLD_LEG -> of(R_T_COLD, "°C", 0);
            case REACTOR_SUBCOOLING -> of(R_SUBCOOLING, "°C", 1);
            case PRESSURIZER_LEVEL -> percent(R_PZR_LEVEL, 0);
            case PRIMARY_FLOW -> of(R_FLOW, "mB/t", 0);
            case VESSEL_LEVEL -> percent(R_VESSEL_LEVEL, 0);
            case REACTOR_CONTAINMENT_PRESSURE -> of(R_CONT_PRESSURE, "bar", 2);
            case SG_LEVEL -> percent(S_LEVEL, 0);
            case SG_STEAM -> of(S_STEAM, "mB/t", 0);
            case SG_HEAT -> of(S_HEAT, "MW", 0);
            case SG_PRIMARY_DT -> of(S_PRIMARY_DT, "°C", 0);
            case CONTAINMENT_PRESSURE -> of(N_PRESSURE, "bar", 2);
            case CONTAINMENT_HYDROGEN -> percent(N_H2, 1);
            case CONTAINMENT_ACTIVITY -> of(N_ACTIVITY, "", 2);
            // An ignited plasma's gain is stored as a huge number: it reads OL.
            case FUSION_GAIN -> of(K_GAIN, "Q", 1);
            case PLASMA_DENSITY -> percent(K_DENSITY, 0);
            case HELIUM_ASH -> percent(K_ASH, 1);
            case MAGNETIC_FIELD -> percent(K_FIELD, 0);
            case MAGNET_TEMPERATURE -> of(K_MAGNET_TEMPERATURE, "K", 1);
            case CHAMBER_PRESSURE -> of(K_VACUUM, "log Pa", 1);
            case HEATING_POWER -> of(K_HEATING, "MW", 1);
            case FUEL_RATE -> of(K_FUEL_RATE, "mB/t", 2);
            case LIQUID_HELIUM -> percent(K_LIQUID_HELIUM, 0);
            case TRITIUM -> percent(K_TRITIUM, 0);
            case TRAP_ANTIMATTER -> of(P_STORED, "ng", 2);
            case TRAP_FIELD -> percent(P_FIELD, 0);
            case TRAP_BUFFER -> percent(P_BUFFER, 0);
            case GENERATOR_OUTPUT -> percent(G_OUTPUT, 0);
            case GENERATOR_THROTTLE -> percent(G_THROTTLE, 0);
            case GENERATOR_CHAMBER -> of(G_STORED, "ng", 2);
            case GENERATOR_ENERGY -> of(G_FE, "FE/t", 0);
            case THERMAL_THROTTLE, ANTIMATTER_THROTTLE -> percent(E_THROTTLE, 0);
            case THERMAL_NOZZLE, ANTIMATTER_COIL -> of(E_TEMPERATURE, "°C", 0);
            case THERMAL_PROPELLANT -> percent(E_FUEL, 0);
            case ANTIMATTER_CHAMBER -> of(E_STORED, "ng", 2);
            case REACTOR_SHUTDOWN_MARGIN -> of(R_COLD_MARGIN, "pcm", 0);
            case REACTOR_BORON -> of(R_BORON_PPM, "ppm", 0);
            case RADAR_TRACKS -> of(MonitorReading.D_TRACKS, "trk", 0);
            case RADAR_THREATS -> of(MonitorReading.D_THREATS, "trk", 0);
        };
    }

    private static Spec of(int index, String unit, int decimals) {
        return new Spec(r -> r.value(index), unit, decimals);
    }

    private static Spec percent(int index, int decimals) {
        return new Spec(r -> r.value(index) * 100.0, "%", decimals);
    }

    /** The reading in its units, or NaN when the sample is of another kind of machine. */
    public static double value(GaugeReading reading, MonitorReading sample) {
        return sample.kind() != reading.kind() ? Double.NaN : SPECS.get(reading).value().applyAsDouble(sample);
    }

    public static String unit(GaugeReading reading) {
        return SPECS.get(reading).unit();
    }

    /** The most decimals the reading is shown with, fewer when the digits run out. */
    public static int decimals(GaugeReading reading) {
        return SPECS.get(reading).decimals();
    }
}
