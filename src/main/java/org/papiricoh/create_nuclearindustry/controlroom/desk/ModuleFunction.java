package org.papiricoh.create_nuclearindustry.controlroom.desk;

import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.controlroom.TargetKind;

import java.util.Locale;

/**
 * What a control module does. Each needs the console to be linked to a given kind of machine, or
 * works on any console ({@link #needs} null): those drive redstone only. A module whose function
 * does not match the console's target shows dark and does nothing.
 */
public enum ModuleFunction {
    /** Button: SCRAM, or reset once a SCRAM is on. */
    SCRAM(TargetKind.REACTOR),
    /** Button: a short redstone pulse. */
    PULSE(null),
    /** Lever: the reactor's automatic SCRAM on or off. */
    AUTO_SCRAM(TargetKind.REACTOR),
    /** Lever: the tokamak's fuel injection on or off. */
    TOKAMAK_FUEL(TargetKind.TOKAMAK),
    /** Lever or key switch: a redstone output on or off. */
    TOGGLE(null),
    /** Dial: the control rod target, 0-100%. */
    ROD_TARGET(TargetKind.REACTOR),
    /** Dial: the automatic SCRAM threshold. */
    SCRAM_THRESHOLD(TargetKind.REACTOR),
    /** Dial: a redstone level, 0-15. */
    SIGNAL_LEVEL(null),
    /** Lamp: lit while the reactor's SCRAM signal is on. */
    LAMP_SCRAM(TargetKind.REACTOR),
    /** Lamp: lit while the machine runs (reactor chain reaction, turbine or tower working, tokamak burning). */
    LAMP_RUNNING(null),
    /** Lamp: lit while the console has no signal from its target. */
    LAMP_NO_SIGNAL(null),
    /** Lamp: lit while an alarm metric passes its threshold. */
    LAMP_ALARM(null),
    /** Gauge: a needle showing one reading. */
    READING(null);

    @Nullable
    private final TargetKind needs;

    ModuleFunction(@Nullable TargetKind needs) {
        this.needs = needs;
    }

    @Nullable
    public TargetKind needs() {
        return needs;
    }

    /** Whether it can act on a console linked to {@code kind}. */
    public boolean worksOn(TargetKind kind) {
        return needs == null || needs == kind;
    }

    public String translationKey() {
        return "create_nuclearindustry.control_module.function." + name().toLowerCase(Locale.ROOT);
    }

    public static ModuleFunction byName(String name, ModuleFunction fallback) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }
}
