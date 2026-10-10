package org.papiricoh.create_nuclearindustry.controlroom;

import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.simibubi.create.foundation.gui.AllIcons;

import java.util.Locale;

/**
 * What a Nuclear Alarm watches. The threshold is a 0-100 setting, scaled to each metric's own units;
 * {@link #tripped} says whether a reading sets the alarm off. Readings come from a monitor sample.
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
    POWER(AllIcons.I_PRIORITY_HIGH, TargetKind.REACTOR) {
        @Override
        public float scaled(int setting) {
            return setting * 10f;
        }

        @Override
        public String format(int setting) {
            return String.format(Locale.ROOT, "%.0f MW", scaled(setting));
        }

        @Override
        float reading(MonitorReading reading) {
            return reading.value(MonitorReading.R_POWER);
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
        @Override
        public float scaled(int setting) {
            return setting * 60f;
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

    public TargetKind watches() {
        return watches;
    }

    abstract float reading(MonitorReading reading);

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
        return sample.kind() == watches && tripped(reading(sample), setting);
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
