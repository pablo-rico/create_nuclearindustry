package org.papiricoh.create_nuclearindustry.controlroom.annunciator;

import org.papiricoh.create_nuclearindustry.controlroom.MonitorReading;
import org.papiricoh.create_nuclearindustry.controlroom.TokamakScales;
import org.papiricoh.create_nuclearindustry.reactor.control.AutoScram;
import org.papiricoh.create_nuclearindustry.reactor.pwr.containment.ContainmentModel;

import java.util.List;
import java.util.Locale;

/**
 * Ready-made setups for all six windows of an annunciator panel, one per kind of machine the whole
 * panel is linked to. Thresholds come from what the plant does on its own: the pressurizer's
 * setpoint and relief valve, the automatic SCRAM, natural circulation, the core's temperature rise,
 * the flammability of hydrogen, the strength of containment concrete and a tokamak's own trips
 * (see {@link TokamakScales}), so the windows light before the machine's own protections act.
 * <p>
 * Metrics are named, not {@link org.papiricoh.create_nuclearindustry.controlroom.AlarmMetric}
 * constants, and the full scales are compile-time constants, so the table has no game classes and
 * can be unit tested; the panel resolves the names when it applies a preset.
 */
public enum AnnunciatorPreset {
    REACTOR_CORE,
    PWR_PRIMARY,
    STEAM_GENERATOR,
    CONTAINMENT,
    TOKAMAK;

    /** Threshold of the containment pressure HI-HI window, as a share of containment concrete's 6 bar rating. */
    static final double CONCRETE_RATING = 6.0;

    /**
     * One window of a preset.
     *
     * @param metric  name of the alarm metric
     * @param setting its 0-100 threshold setting
     * @param label   short English label, used when the client sends none
     */
    public record Window(String metric, int setting, String label) {}

    /**
     * The plant settings the thresholds follow (the {@code pwr.*} config).
     *
     * @param pressureSetpoint   pressure the pressurizer holds, bar
     * @param reliefOpenPressure pressure the relief valve opens at, bar
     * @param naturalCirculation flow through the core with the pumps stopped, mB/t
     * @param coreTemperatureRise how much the primary warms crossing the core, °C
     */
    public record Plant(double pressureSetpoint, double reliefOpenPressure, double naturalCirculation, double coreTemperatureRise) {
        /** The config's defaults. */
        public static final Plant DEFAULTS = new Plant(155.0, 170.0, 8.0, 35.0);
    }

    /** The six windows, in panel order (left to right, top to bottom). */
    public List<Window> windows(Plant plant) {
        return switch (this) {
            case REACTOR_CORE -> List.of(
                    new Window("TEMPERATURE", celsius(AutoScram.DEFAULT_THRESHOLD - 800), "CORE TEMP HI"),
                    new Window("TEMPERATURE", celsius(AutoScram.DEFAULT_THRESHOLD - 400), "CORE TEMP HI-HI"),
                    new Window("POWER", 90, "POWER HI"),
                    new Window("XENON", 15, "XENON HI"),
                    new Window("COOLANT_LOW", 20, "COOLANT LO"),
                    new Window("SCRAM", 0, "SCRAM"));
            case PWR_PRIMARY -> List.of(
                    // Halfway between the setpoint and the relief valve, so it sounds before the valve lifts.
                    new Window("PRESSURE_HIGH", bar((plant.pressureSetpoint() + plant.reliefOpenPressure()) / 2.0), "PRESS HI"),
                    new Window("PRESSURE_LOW", bar(plant.pressureSetpoint() - 10.0), "PRESS LO"),
                    new Window("RELIEF_OPEN", 0, "RELIEF OPEN"),
                    new Window("SUBCOOLING_LOW", scaled(10.0, MonitorReading.SUBCOOLING_MAX), "SUBCOOL LO"),
                    // The heaters trip at 10%.
                    new Window("PZR_LEVEL", 15, "PZR LEVEL LO"),
                    // Little more than natural circulation left: the pumps have stopped.
                    new Window("LOSS_OF_FLOW", scaled(2.0 * plant.naturalCirculation(), MonitorReading.FLOW_MAX), "LOSS OF FLOW"));
            case STEAM_GENERATOR -> List.of(
                    new Window("SG_LEVEL_LOW", 35, "SG LEVEL LO"),
                    new Window("SG_LEVEL_LOW", 20, "SG LEVEL LO-LO"),
                    new Window("SG_LEVEL_LOW", 5, "SG DRYOUT"),
                    new Window("SG_LEVEL_HIGH", 90, "SG LEVEL HI"),
                    new Window("SG_STEAM_LOW", scaled(5.0, MonitorReading.SG_STEAM_MAX), "STEAM FLOW LO"),
                    // Half the core's rise: the tubes pass on only half the heat.
                    new Window("SG_PRIMARY_DT_LOW", scaled(plant.coreTemperatureRise() / 2.0, MonitorReading.SG_DT_MAX), "PRIMARY dT LO"));
            case CONTAINMENT -> List.of(
                    new Window("CONTAINMENT_PRESSURE", scaled(2.0, MonitorReading.CONTAINMENT_PRESSURE_MAX), "CONT PRESS HI"),
                    new Window("CONTAINMENT_PRESSURE", scaled(0.8 * CONCRETE_RATING, MonitorReading.CONTAINMENT_PRESSURE_MAX),
                            "CONT PRESS HI-HI"),
                    new Window("HYDROGEN", scaled(100.0 * ContainmentModel.LOWER_FLAMMABILITY / 2.0, 10.0), "H2 HI"),
                    new Window("HYDROGEN", scaled(100.0 * ContainmentModel.LOWER_FLAMMABILITY, 10.0), "H2 HI-HI"),
                    // A thousandth of a whole core's release in the air: the fuel is failing.
                    new Window("CONTAINMENT_ACTIVITY", activity(1.0e-3), "ACTIVITY HI"),
                    new Window("CONTAINMENT_OPEN", 0, "CONT OPEN"));
            // Each window lights before the tokamak's own protection acts: the disruption, the
            // vacuum and magnet trips, the density limit; and Q where the heating stops paying.
            case TOKAMAK -> List.of(
                    new Window("PLASMA_INSTABILITY", Math.round(TokamakScales.INSTABILITY_WARNING), "INSTABILITY HI"),
                    new Window("PLASMA_DENSITY", 90, "DENSITY HI"),
                    new Window("VACUUM_POOR", TokamakScales.vacuumSetting(TokamakScales.PLASMA_VACUUM_LIMIT - 0.5), "VACUUM POOR"),
                    new Window("MAGNET_TEMPERATURE", TokamakScales.magnetSetting(TokamakScales.SUPERCONDUCTING_LIMIT / 2.0),
                            "MAGNET TEMP HI"),
                    new Window("FUSION_GAIN_LOW", TokamakScales.gainSetting(TokamakScales.NET_GAIN), "Q LOW"),
                    new Window("TOKAMAK_TRIP", 0, "TOKAMAK TRIP"));
        };
    }

    /** Translation key of the preset's name. */
    public String translationKey() {
        return "create_nuclearindustry.annunciator.preset." + name().toLowerCase(Locale.ROOT);
    }

    /** Translation key of one window's label. */
    public String labelKey(int window) {
        return translationKey() + "." + (window + 1);
    }

    public AnnunciatorPreset next(int direction) {
        AnnunciatorPreset[] all = values();
        return all[Math.floorMod(ordinal() + direction, all.length)];
    }

    /** The preset called {@code name}, or null. */
    public static AnnunciatorPreset byName(String name) {
        for (AnnunciatorPreset preset : values()) {
            if (preset.name().equals(name)) {
                return preset;
            }
        }
        return null;
    }

    /** Setting of a °C threshold of the core temperature metric (40 °C a step). */
    private static int celsius(double celsius) {
        return clamp(Math.round(celsius / 40.0));
    }

    /** Setting of a primary pressure threshold. */
    private static int bar(double bar) {
        return scaled(bar, MonitorReading.PRESSURE_MAX);
    }

    /** Setting of a threshold on a metric whose setting 100 is {@code fullScale}. */
    static int scaled(double value, double fullScale) {
        return clamp(Math.round(value / fullScale * 100.0));
    }

    /** Setting of the logarithmic activity metric for a share of a whole core's release. */
    private static int activity(double shareOfCore) {
        return clamp(Math.round((Math.log10(shareOfCore) + MonitorReading.ACTIVITY_DECADES) / MonitorReading.ACTIVITY_DECADES * 100.0));
    }

    private static int clamp(long setting) {
        return (int) Math.max(0, Math.min(100, setting));
    }
}
