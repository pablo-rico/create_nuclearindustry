package org.papiricoh.create_nuclearindustry.controlroom.desk;

import java.util.List;
import java.util.Locale;

/**
 * The kinds of control module that fit a slot of a desk, table or wall panel, and the functions each
 * can be set to. Saved by name; new types go last, which is the order of the creative tab.
 */
public enum ModuleType {
    PUSH_BUTTON("push_button_module", List.of(ModuleFunction.SCRAM, ModuleFunction.PULSE, ModuleFunction.TOKAMAK_START,
            ModuleFunction.TOKAMAK_SHUTDOWN)),
    LEVER("lever_module", List.of(ModuleFunction.AUTO_SCRAM, ModuleFunction.TOKAMAK_FUEL, ModuleFunction.TOGGLE,
            ModuleFunction.PZR_HEATERS, ModuleFunction.PZR_SPRAY, ModuleFunction.TOKAMAK_START, ModuleFunction.TOKAMAK_SHUTDOWN,
            ModuleFunction.TOKAMAK_HEATING)),
    DIAL("dial_module", List.of(ModuleFunction.ROD_TARGET, ModuleFunction.SCRAM_THRESHOLD, ModuleFunction.SIGNAL_LEVEL)),
    LAMP("lamp_module", List.of(ModuleFunction.LAMP_SCRAM, ModuleFunction.LAMP_RUNNING, ModuleFunction.LAMP_NO_SIGNAL,
            ModuleFunction.LAMP_ALARM, ModuleFunction.LAMP_RELIEF_OPEN)),
    GAUGE("gauge_module", List.of(ModuleFunction.READING)),
    KEY_SWITCH("key_switch_module", List.of(ModuleFunction.TOGGLE)),
    /**
     * Automatic control: a PID loop holding power or temperature (with the rods), primary pressure (with
     * the pressurizer) or a tokamak's fusion power (with its fuelling and heating).
     */
    CONTROLLER("controller_module", List.of(ModuleFunction.AUTO_POWER, ModuleFunction.AUTO_TEMPERATURE,
            ModuleFunction.AUTO_PRESSURE, ModuleFunction.AUTO_FUSION_POWER)),
    /**
     * A reading in digits with its unit. LED displays side by side in a row join into one wider
     * display set up by its leftmost module (see {@link LedGroups}).
     */
    LED_DISPLAY("led_display_module", List.of(ModuleFunction.READING)),
    /** A button under a hinged cover: the first click lifts the cover, the second presses. */
    GUARDED_BUTTON("guarded_button_module", List.of(ModuleFunction.SCRAM, ModuleFunction.TOKAMAK_SHUTDOWN, ModuleFunction.PULSE)),
    /** A reading as a column of LED segments, green to amber to red up to an alarm level (see {@link BarGraph}). */
    BAR_GRAPH("bar_graph_module", List.of(ModuleFunction.READING)),
    /** A reading drawn as a line over the last minutes (see {@link TrendRing}). */
    TREND_RECORDER("trend_recorder_module", List.of(ModuleFunction.READING)),
    /** A fission reactor's columns, rods and fuel as a map; four in one block make one big map (see {@link CoreMap}). */
    CORE_MAP("core_map_module", List.of(ModuleFunction.CORE_VIEW)),
    /** A cut through a tokamak's torus with its plasma; four in one block make one big screen (see {@link PlasmaSection}). */
    PLASMA_SECTION("plasma_section_module", List.of(ModuleFunction.PLASMA_VIEW));

    private final String itemId;
    private final List<ModuleFunction> functions;

    ModuleType(String itemId, List<ModuleFunction> functions) {
        this.itemId = itemId;
        this.functions = functions;
    }

    /** The id of the module item that places this type. */
    public String itemId() {
        return itemId;
    }

    public List<ModuleFunction> functions() {
        return functions;
    }

    public ModuleFunction defaultFunction() {
        return functions.get(0);
    }

    public boolean allows(ModuleFunction function) {
        return functions.contains(function);
    }

    /** Whether a click with an empty hand does something (lamps, gauges and screens only show). */
    public boolean interactive() {
        return switch (this) {
            case LAMP, GAUGE, LED_DISPLAY, BAR_GRAPH, TREND_RECORDER, CORE_MAP, PLASMA_SECTION -> false;
            default -> true;
        };
    }

    /** Whether four of it filling one block show as one screen (see {@link BlockScreen}). */
    public boolean mergesBlock() {
        return this == CORE_MAP || this == PLASMA_SECTION;
    }

    /** Whether it is pressed rather than thrown: a push button, guarded or not. */
    public boolean isButton() {
        return this == PUSH_BUTTON || this == GUARDED_BUTTON;
    }

    public String translationKey() {
        return "create_nuclearindustry.control_module.type." + name().toLowerCase(Locale.ROOT);
    }

    public static ModuleType byName(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
