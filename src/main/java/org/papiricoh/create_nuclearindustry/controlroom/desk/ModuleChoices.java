package org.papiricoh.create_nuclearindustry.controlroom.desk;

import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.controlroom.TargetKind;

import java.util.List;

/**
 * Which module functions can work on a console, for the setup screen: it lists every function of the
 * module's type, greys out the ones the linked machine cannot use and says what they need instead.
 * Mirrors what makes a module light up in {@link ControlModule} (minus the alarm metric or gauge reading
 * picked, which the screen filters on its own). An unlinked or unreachable console allows everything,
 * since the player may be setting the desk up before linking it.
 */
public final class ModuleChoices {
    /** Machines a "running" lamp knows how to read. */
    private static final List<TargetKind> RUNNING_KINDS =
            List.of(TargetKind.REACTOR, TargetKind.TURBINE, TargetKind.COOLING_TOWER, TargetKind.TOKAMAK);

    private ModuleChoices() {}

    /** Whether a function suits the console, and if not, why not. */
    public enum Fit {
        /** Works, or the console is not linked to anything to judge by. */
        OK,
        /** Needs another kind of machine: {@link #needs} says which. */
        WRONG_MACHINE,
        /** Needs a reactor with a pressurizer on its lid, and this one has none. */
        NO_PRESSURIZER
    }

    /**
     * How {@code function} fits a console linked to {@code kind}. {@code pressurizer} is whether that
     * machine is a reactor with a pressurizer.
     */
    public static Fit fit(ModuleFunction function, TargetKind kind, boolean pressurizer) {
        if (kind == TargetKind.NONE || kind == TargetKind.NO_SIGNAL) {
            return Fit.OK;
        }
        if (function == ModuleFunction.LAMP_RUNNING) {
            return RUNNING_KINDS.contains(kind) ? Fit.OK : Fit.WRONG_MACHINE;
        }
        if (!function.worksOn(kind)) {
            return Fit.WRONG_MACHINE;
        }
        return function.needsPressurizer() && !pressurizer ? Fit.NO_PRESSURIZER : Fit.OK;
    }

    /** The machine a function needs, for the "needs a ..." hint, or null when it works on any console. */
    @Nullable
    public static TargetKind needs(ModuleFunction function) {
        return function == ModuleFunction.LAMP_RUNNING ? null : function.needs();
    }

    /** The functions of {@code type} that work on a console linked to {@code kind}, in the type's order. */
    public static List<ModuleFunction> usable(ModuleType type, TargetKind kind, boolean pressurizer) {
        return type.functions().stream().filter(function -> fit(function, kind, pressurizer) == Fit.OK).toList();
    }

    /** Whether the function's setting is a 0-100 threshold or setpoint, stepped 5 at a time (1 with shift). */
    public static boolean coarseSetting(ModuleFunction function) {
        return function != ModuleFunction.SIGNAL_LEVEL;
    }

    /** Whether the screen shows a setting input for a module of {@code type} set to the function. */
    public static boolean hasSetting(ModuleType type, ModuleFunction function) {
        if (function == ModuleFunction.READING) {
            return type == ModuleType.BAR_GRAPH || type == ModuleType.TREND_RECORDER;
        }
        return hasSetting(function);
    }

    /** As {@link #coarseSetting(ModuleFunction)}, for a module of {@code type}: a trend's speed steps one at a time. */
    public static boolean coarseSetting(ModuleType type, ModuleFunction function) {
        return type != ModuleType.TREND_RECORDER && coarseSetting(function);
    }

    /** Whether the screen shows a setting input for the function. */
    public static boolean hasSetting(ModuleFunction function) {
        return switch (function) {
            case LAMP_ALARM, SIGNAL_LEVEL, AUTO_POWER, AUTO_TEMPERATURE, AUTO_PRESSURE, AUTO_FUSION_POWER -> true;
            default -> false;
        };
    }
}
