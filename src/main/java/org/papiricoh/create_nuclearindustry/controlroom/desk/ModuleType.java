package org.papiricoh.create_nuclearindustry.controlroom.desk;

import java.util.List;
import java.util.Locale;

/** The kinds of control module that fit a desk slot, and the functions each can be set to. */
public enum ModuleType {
    PUSH_BUTTON("push_button_module", List.of(ModuleFunction.SCRAM, ModuleFunction.PULSE)),
    LEVER("lever_module", List.of(ModuleFunction.AUTO_SCRAM, ModuleFunction.TOKAMAK_FUEL, ModuleFunction.TOGGLE)),
    DIAL("dial_module", List.of(ModuleFunction.ROD_TARGET, ModuleFunction.SCRAM_THRESHOLD, ModuleFunction.SIGNAL_LEVEL)),
    LAMP("lamp_module", List.of(ModuleFunction.LAMP_SCRAM, ModuleFunction.LAMP_RUNNING, ModuleFunction.LAMP_NO_SIGNAL,
            ModuleFunction.LAMP_ALARM)),
    GAUGE("gauge_module", List.of(ModuleFunction.READING)),
    KEY_SWITCH("key_switch_module", List.of(ModuleFunction.TOGGLE));

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

    /** Whether a click with an empty hand does something (lamps and gauges only show). */
    public boolean interactive() {
        return this != LAMP && this != GAUGE;
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
