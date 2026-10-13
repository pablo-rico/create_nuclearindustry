package org.papiricoh.create_nuclearindustry.controlroom;

import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.simibubi.create.foundation.gui.AllIcons;
import org.papiricoh.create_nuclearindustry.fusion.api.TokamakPhase;
import org.papiricoh.create_nuclearindustry.reactor.pwr.accident.ReleasePath;

import java.util.Locale;

/**
 * What a Nuclear Alarm, an alarm lamp or an annunciator window watches. The threshold is a 0-100
 * setting, scaled to each metric's own units; {@link #tripped} says whether a reading sets the alarm
 * off. Readings come from a monitor sample. Saved by ordinal, so new metrics always go at the end.
 */
public enum AlarmMetric implements INamedIconOptions {
    TEMPERATURE(AllIcons.I_PRIORITY_VERY_HIGH, TargetKind.REACTOR) {
        @Override
        public float scaled(int setting) {
            return setting * 40f;
        }

        @Override
        public String format(int setting) {
            return String.format(Locale.ROOT, "%.0f°C", scaled(setting));
        }

        @Override
        float reading(MonitorReading reading) {
            return reading.value(MonitorReading.R_TEMPERATURE);
        }
    },
    /** Fission power as a share of the core's rated power, up to 150%. */
    POWER(AllIcons.I_PRIORITY_HIGH, TargetKind.REACTOR) {
        @Override
        public float scaled(int setting) {
            return setting * 1.5f;
        }

        @Override
        public String format(int setting) {
            return String.format(Locale.ROOT, "%.0f%%", scaled(setting));
        }

        @Override
        float reading(MonitorReading reading) {
            float rated = reading.value(MonitorReading.R_RATED_POWER);
            return rated > 0 ? reading.value(MonitorReading.R_POWER) / rated * 100f : 0f;
        }
    },
    XENON(AllIcons.I_PATTERN_CHANCE_50, TargetKind.REACTOR) {
        @Override
        float reading(MonitorReading reading) {
            return reading.value(MonitorReading.R_XENON) * 100f;
        }
    },
    COOLANT_LOW(AllIcons.I_PRIORITY_LOW, TargetKind.REACTOR) {
        @Override
        float reading(MonitorReading reading) {
            return reading.value(MonitorReading.R_COOLANT) * 100f;
        }

        @Override
        boolean below() {
            return true;
        }
    },
    SCRAM(AllIcons.I_STOP, TargetKind.REACTOR) {
        @Override
        public String format(int setting) {
            return "-";
        }

        @Override
        float reading(MonitorReading reading) {
            return reading.flag(MonitorReading.R_SCRAM) ? 1 : 0;
        }

        @Override
        boolean tripped(float reading, int setting) {
            return reading > 0;
        }
    },
    TURBINE_LOAD(AllIcons.I_ROTATE_CCW, TargetKind.TURBINE) {
        @Override
        float reading(MonitorReading reading) {
            return reading.seriesA() * 100f;
        }
    },
    TOWER_LOAD(AllIcons.I_FX_FIELD_ON, TargetKind.COOLING_TOWER) {
        @Override
        float reading(MonitorReading reading) {
            return reading.seriesA() * 100f;
        }
    },
    AREA_RADIATION(AllIcons.I_TARGET, TargetKind.AREA) {
        @Override
        public float scaled(int setting) {
            return setting * 0.1f;
        }

        @Override
        public String format(int setting) {
            return String.format(Locale.ROOT, "%.1f rad/s", scaled(setting));
        }

        @Override
        float reading(MonitorReading reading) {
            return reading.value(MonitorReading.A_EXPOSURE);
        }
    },
    PLASMA_TEMPERATURE(AllIcons.I_FX_SURFACE_ON, TargetKind.TOKAMAK) {
        /** Up to the hottest a plasma gets (60° a step). */
        @Override
        public float scaled(int setting) {
            return setting * TokamakScales.MAX_TEMPERATURE / 100f;
        }

        @Override
        public String format(int setting) {
            return String.format(Locale.ROOT, "%.0f°", scaled(setting));
        }

        @Override
        float reading(MonitorReading reading) {
            return reading.value(MonitorReading.K_TEMPERATURE);
        }
    },
    PLASMA_INSTABILITY(AllIcons.I_DICE, TargetKind.TOKAMAK) {
        @Override
        float reading(MonitorReading reading) {
            return reading.value(MonitorReading.K_INSTABILITY);
        }
    },
    // Pressurized water loop, steam generator and containment. Saved by ordinal: new metrics go last.
    PRESSURE_HIGH(AllIcons.I_PRIORITY_VERY_HIGH, TargetKind.REACTOR) {
        @Override
        public float scaled(int setting) {
            return setting * MonitorReading.PRESSURE_MAX / 100f;
        }

        @Override
        public String format(int setting) {
            return String.format(Locale.ROOT, "%.0f bar", scaled(setting));
        }

        @Override
        float reading(MonitorReading reading) {
            return reading.value(MonitorReading.R_PRESSURE);
        }

        @Override
        boolean applies(MonitorReading sample) {
            return pressurized(sample);
        }
    },
    PRESSURE_LOW(AllIcons.I_PRIORITY_VERY_LOW, TargetKind.REACTOR) {
        @Override
        public float scaled(int setting) {
            return PRESSURE_HIGH.scaled(setting);
        }

        @Override
        public String format(int setting) {
            return PRESSURE_HIGH.format(setting);
        }

        @Override
        float reading(MonitorReading reading) {
            return reading.value(MonitorReading.R_PRESSURE);
        }

        @Override
        boolean below() {
            return true;
        }

        @Override
        boolean applies(MonitorReading sample) {
            return pressurized(sample);
        }
    },
    SUBCOOLING_LOW(AllIcons.I_FX_BLEND, TargetKind.REACTOR) {
        @Override
        public float scaled(int setting) {
            return setting * MonitorReading.SUBCOOLING_MAX / 100f;
        }

        @Override
        public String format(int setting) {
            return String.format(Locale.ROOT, "%.1f°C", scaled(setting));
        }

        @Override
        float reading(MonitorReading reading) {
            return reading.value(MonitorReading.R_SUBCOOLING);
        }

        @Override
        boolean below() {
            return true;
        }

        @Override
        boolean applies(MonitorReading sample) {
            return pressurized(sample);
        }
    },
    PZR_LEVEL(AllIcons.I_FILL, TargetKind.REACTOR) {
        @Override
        float reading(MonitorReading reading) {
            return reading.value(MonitorReading.R_PZR_LEVEL) * 100f;
        }

        @Override
        boolean below() {
            return true;
        }

        @Override
        boolean applies(MonitorReading sample) {
            return pressurized(sample);
        }
    },
    LOSS_OF_FLOW(AllIcons.I_PAUSE, TargetKind.REACTOR) {
        @Override
        public float scaled(int setting) {
            return setting * MonitorReading.FLOW_MAX / 100f;
        }

        @Override
        public String format(int setting) {
            return String.format(Locale.ROOT, "%.0f mB/t", scaled(setting));
        }

        @Override
        float reading(MonitorReading reading) {
            return reading.value(MonitorReading.R_FLOW);
        }

        @Override
        boolean below() {
            return true;
        }

        @Override
        boolean applies(MonitorReading sample) {
            return pressurized(sample);
        }
    },
    SG_LEVEL_LOW(AllIcons.I_FLATTEN, TargetKind.STEAM_GENERATOR) {
        @Override
        float reading(MonitorReading reading) {
            return reading.value(MonitorReading.S_LEVEL) * 100f;
        }

        @Override
        boolean below() {
            return true;
        }

        @Override
        boolean applies(MonitorReading sample) {
            return sample.kind() == TargetKind.STEAM_GENERATOR && sample.flag(MonitorReading.S_FORMED);
        }
    },
    CONTAINMENT_PRESSURE(AllIcons.I_FX_FIELD_OFF, TargetKind.CONTAINMENT) {
        @Override
        public float scaled(int setting) {
            return setting * MonitorReading.CONTAINMENT_PRESSURE_MAX / 100f;
        }

        @Override
        public String format(int setting) {
            return String.format(Locale.ROOT, "%.1f bar", scaled(setting));
        }

        @Override
        float reading(MonitorReading reading) {
            return reading.kind() == TargetKind.REACTOR
                    ? reading.value(MonitorReading.R_CONT_PRESSURE)
                    : reading.value(MonitorReading.N_PRESSURE);
        }

        @Override
        public boolean watches(TargetKind kind) {
            return kind == TargetKind.CONTAINMENT || kind == TargetKind.REACTOR;
        }
    },
    HYDROGEN(AllIcons.I_PATTERN_CHANCE_25, TargetKind.CONTAINMENT) {
        @Override
        public float scaled(int setting) {
            return setting * 0.1f;
        }

        @Override
        public String format(int setting) {
            return String.format(Locale.ROOT, "%.1f%%", scaled(setting));
        }

        @Override
        float reading(MonitorReading reading) {
            return 100f * (reading.kind() == TargetKind.REACTOR
                    ? reading.value(MonitorReading.R_H2)
                    : reading.value(MonitorReading.N_H2));
        }

        @Override
        public boolean watches(TargetKind kind) {
            return kind == TargetKind.CONTAINMENT || kind == TargetKind.REACTOR;
        }
    },
    // Added with the annunciator templates; saved by ordinal, so new metrics go after these.
    RELIEF_OPEN(AllIcons.I_SEND_ONLY, TargetKind.REACTOR) {
        @Override
        public String format(int setting) {
            return "-";
        }

        @Override
        float reading(MonitorReading reading) {
            return reading.flag(MonitorReading.R_RELIEF) ? 1 : 0;
        }

        @Override
        boolean tripped(float reading, int setting) {
            return reading > 0;
        }

        @Override
        boolean applies(MonitorReading sample) {
            return pressurized(sample);
        }
    },
    SG_LEVEL_HIGH(AllIcons.I_OVERLAY, TargetKind.STEAM_GENERATOR) {
        @Override
        float reading(MonitorReading reading) {
            return reading.value(MonitorReading.S_LEVEL) * 100f;
        }

        @Override
        boolean applies(MonitorReading sample) {
            return steamGeneratorFormed(sample);
        }
    },
    SG_STEAM_LOW(AllIcons.I_PRIORITY_VERY_LOW, TargetKind.STEAM_GENERATOR) {
        @Override
        public float scaled(int setting) {
            return setting * MonitorReading.SG_STEAM_MAX / 100f;
        }

        @Override
        public String format(int setting) {
            return String.format(Locale.ROOT, "%.0f mB/t", scaled(setting));
        }

        @Override
        float reading(MonitorReading reading) {
            return reading.value(MonitorReading.S_STEAM);
        }

        @Override
        boolean below() {
            return true;
        }

        @Override
        boolean applies(MonitorReading sample) {
            return steamGeneratorFormed(sample);
        }
    },
    SG_PRIMARY_DT_LOW(AllIcons.I_FX_BLEND_OFF, TargetKind.STEAM_GENERATOR) {
        @Override
        public float scaled(int setting) {
            return setting * MonitorReading.SG_DT_MAX / 100f;
        }

        @Override
        public String format(int setting) {
            return String.format(Locale.ROOT, "%.1f°C", scaled(setting));
        }

        @Override
        float reading(MonitorReading reading) {
            return reading.value(MonitorReading.S_PRIMARY_DT);
        }

        @Override
        boolean below() {
            return true;
        }

        @Override
        boolean applies(MonitorReading sample) {
            return steamGeneratorFormed(sample);
        }
    },
    CONTAINMENT_OPEN(AllIcons.I_DISABLE, TargetKind.CONTAINMENT) {
        @Override
        public String format(int setting) {
            return "-";
        }

        @Override
        float reading(MonitorReading reading) {
            return reading.flag(MonitorReading.N_SEALED) ? 0 : 1;
        }

        @Override
        boolean tripped(float reading, int setting) {
            return reading > 0;
        }
    },
    CONTAINMENT_ACTIVITY(AllIcons.I_PATTERN_CHECKERED, TargetKind.CONTAINMENT) {
        /** Logarithmic, as the activity gauge: 0 is a hundred-thousandth of a whole core's release, 100 all of it. */
        @Override
        public float scaled(int setting) {
            return (float) (ReleasePath.CORE_REFERENCE * Math.pow(10.0, (setting / 100.0 - 1.0) * MonitorReading.ACTIVITY_DECADES));
        }

        @Override
        public String format(int setting) {
            return String.format(Locale.ROOT, "%.3g", scaled(setting));
        }

        @Override
        float reading(MonitorReading reading) {
            return reading.value(MonitorReading.N_ACTIVITY);
        }
    },
    // Tokamak startup and plasma control. Saved by ordinal: new metrics go after these.
    /** Chamber pressure above the threshold while the plasma needs vacuum. */
    VACUUM_POOR(AllIcons.I_PATTERN_CHANCE_75, TargetKind.TOKAMAK) {
        @Override
        public float scaled(int setting) {
            return TokamakScales.vacuumThreshold(setting);
        }

        @Override
        public String format(int setting) {
            return TokamakScales.formatPressure(scaled(setting));
        }

        @Override
        float reading(MonitorReading reading) {
            return reading.value(MonitorReading.K_VACUUM);
        }

        @Override
        boolean applies(MonitorReading sample) {
            return tokamakPhase(sample) != null && tokamakPhase(sample).hasPlasma();
        }
    },
    /** Coil temperature above the threshold while the magnets carry, or are about to carry, current. */
    MAGNET_TEMPERATURE(AllIcons.I_FX_SURFACE_OFF, TargetKind.TOKAMAK) {
        @Override
        public float scaled(int setting) {
            return TokamakScales.magnetThreshold(setting);
        }

        @Override
        public String format(int setting) {
            return String.format(Locale.ROOT, "%.1f K", scaled(setting));
        }

        @Override
        float reading(MonitorReading reading) {
            return reading.value(MonitorReading.K_MAGNET_TEMPERATURE);
        }

        @Override
        boolean applies(MonitorReading sample) {
            TokamakPhase phase = tokamakPhase(sample);
            return phase != null && TokamakScales.magnetsInUse(phase, sample.value(MonitorReading.K_FIELD));
        }
    },
    /** A burning plasma's gain Q at or below the threshold: the heating eats the profit. */
    FUSION_GAIN_LOW(AllIcons.I_MOVE_GAUGE, TargetKind.TOKAMAK) {
        @Override
        public float scaled(int setting) {
            return TokamakScales.gainThreshold(setting);
        }

        @Override
        public String format(int setting) {
            return TokamakScales.formatGain(scaled(setting));
        }

        @Override
        float reading(MonitorReading reading) {
            return reading.value(MonitorReading.K_GAIN);
        }

        @Override
        public boolean tripped(MonitorReading sample, int setting) {
            TokamakPhase phase = tokamakPhase(sample);
            return phase != null && TokamakScales.gainLow(phase, reading(sample), scaled(setting));
        }
    },
    /** Plasma density above the threshold, in percent of the Greenwald limit. */
    PLASMA_DENSITY(AllIcons.I_PATTERN_SOLID, TargetKind.TOKAMAK) {
        @Override
        float reading(MonitorReading reading) {
            return reading.value(MonitorReading.K_DENSITY) * 100f;
        }

        @Override
        boolean applies(MonitorReading sample) {
            return tokamakPhase(sample) != null && tokamakPhase(sample).hasPlasma();
        }
    },
    /** Tritium tank at or below the threshold. */
    TRITIUM_LOW(AllIcons.I_PRIORITY_VERY_LOW, TargetKind.TOKAMAK) {
        @Override
        float reading(MonitorReading reading) {
            return reading.value(MonitorReading.K_TRITIUM) * 100f;
        }

        @Override
        boolean below() {
            return true;
        }

        @Override
        boolean applies(MonitorReading sample) {
            return tokamakPhase(sample) != null;
        }
    },
    /** The plasma went out, or a start was refused, for a reason other than the operator's shutdown. */
    TOKAMAK_TRIP(AllIcons.I_CONFIG_DISCARD, TargetKind.TOKAMAK) {
        @Override
        public String format(int setting) {
            return "-";
        }

        @Override
        float reading(MonitorReading reading) {
            return reading.flag(MonitorReading.K_TRIPPED) ? 1 : 0;
        }

        @Override
        boolean tripped(float reading, int setting) {
            return reading > 0;
        }
    },
    /** Fuel damaged by overheating, %: it never heals, so this one stays lit until the core is refuelled. */
    CORE_DAMAGE(AllIcons.I_PRIORITY_VERY_HIGH, TargetKind.REACTOR) {
        @Override
        float reading(MonitorReading reading) {
            return reading.value(MonitorReading.R_CORE_DAMAGE) * 100f;
        }
    };

    private final AllIcons icon;
    private final TargetKind watches;

    AlarmMetric(AllIcons icon, TargetKind watches) {
        this.icon = icon;
        this.watches = watches;
    }

    /** The threshold in this metric's units; percentages by default. */
    public float scaled(int setting) {
        return setting;
    }

    public String format(int setting) {
        return setting + "%";
    }

    /** The kind of target this metric is meant for. */
    public TargetKind watches() {
        return watches;
    }

    /** Whether a target of {@code kind} can set this metric off; containment metrics also read a reactor's containment. */
    public boolean watches(TargetKind kind) {
        return kind == watches;
    }

    abstract float reading(MonitorReading reading);

    /**
     * Whether the sample carries this metric at all. Pressurized water metrics ignore a reactor that
     * boils directly, which reports them as 0, so a low-pressure alarm does not sound on every such reactor.
     */
    boolean applies(MonitorReading sample) {
        return watches(sample.kind());
    }

    private static boolean pressurized(MonitorReading sample) {
        return sample.kind() == TargetKind.REACTOR && sample.flag(MonitorReading.R_PWR);
    }

    /** The phase of a formed tokamak's sample, or null for anything else. */
    @org.jetbrains.annotations.Nullable
    private static TokamakPhase tokamakPhase(MonitorReading sample) {
        return sample.kind() == TargetKind.TOKAMAK && sample.flag(MonitorReading.K_FORMED)
                ? TokamakScales.phase(sample.value(MonitorReading.K_PHASE)) : null;
    }

    private static boolean steamGeneratorFormed(MonitorReading sample) {
        return sample.kind() == TargetKind.STEAM_GENERATOR && sample.flag(MonitorReading.S_FORMED);
    }

    /** True when the alarm sounds below the threshold rather than above it. */
    boolean below() {
        return false;
    }

    boolean tripped(float reading, int setting) {
        float threshold = scaled(setting);
        return below() ? reading <= threshold : reading >= threshold;
    }

    /** Whether this sample of the linked target sets the alarm off. Unrelated targets never do. */
    public boolean tripped(MonitorReading sample, int setting) {
        return applies(sample) && tripped(reading(sample), setting);
    }

    /**
     * The metric's value in a sample, in the units of its threshold, for setup screens to show next to
     * it; NaN when the sample does not carry it (another machine, a reactor without a pressurized loop).
     */
    public float liveValue(MonitorReading sample) {
        return applies(sample) ? reading(sample) : Float.NaN;
    }

    /** True for on/off metrics (SCRAM, relief valve, trip...), which take no threshold. */
    public boolean isFlag() {
        return "-".equals(format(0));
    }

    /**
     * A {@link #liveValue} printed in the metric's units, like {@link #format} prints a threshold. Flags
     * print as 1 or 0; screens word them themselves.
     */
    public String formatValue(float value) {
        return switch (this) {
            case TEMPERATURE -> formatted("%.0f°C", value);
            case PLASMA_TEMPERATURE -> formatted("%.0f°", value);
            case AREA_RADIATION -> formatted("%.2f rad/s", value);
            case PRESSURE_HIGH, PRESSURE_LOW -> formatted("%.1f bar", value);
            case CONTAINMENT_PRESSURE -> formatted("%.2f bar", value);
            case SUBCOOLING_LOW, SG_PRIMARY_DT_LOW -> formatted("%.1f°C", value);
            case LOSS_OF_FLOW, SG_STEAM_LOW -> formatted("%.0f mB/t", value);
            case HYDROGEN -> formatted("%.2f%%", value);
            case CONTAINMENT_ACTIVITY -> formatted("%.3g", value);
            case VACUUM_POOR -> TokamakScales.formatPressure(value);
            case MAGNET_TEMPERATURE -> formatted("%.1f K", value);
            case FUSION_GAIN_LOW -> TokamakScales.formatGain(value);
            case SCRAM, RELIEF_OPEN, CONTAINMENT_OPEN, TOKAMAK_TRIP -> value > 0 ? "1" : "0";
            default -> formatted("%.0f%%", value);
        };
    }

    private static String formatted(String pattern, float value) {
        return String.format(Locale.ROOT, pattern, value);
    }

    @Override
    public AllIcons getIcon() {
        return icon;
    }

    @Override
    public String getTranslationKey() {
        return "create_nuclearindustry.alarm.metric." + name().toLowerCase(Locale.ROOT);
    }
}
