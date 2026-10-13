package org.papiricoh.create_nuclearindustry.controlroom.desk.client;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.resources.ResourceLocation;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ModuleType;

/**
 * Module models the desk renderer places on a desk, table or wall panel: a fixed base and a moving
 * part for each type, modelled flat around (8, 0, 8) px with the surface at y 0 and the operator
 * towards -z (below, on a wall). Must be created before
 * models load, from the mod constructor through {@link #init()}.
 */
public final class DeskPartials {
    public static final PartialModel BUTTON_BASE = of("push_button_base");
    public static final PartialModel BUTTON_CAP = of("push_button_cap");
    public static final PartialModel LEVER_BASE = of("lever_base");
    public static final PartialModel LEVER_HANDLE = of("lever_handle");
    public static final PartialModel DIAL_BASE = of("dial_base");
    public static final PartialModel DIAL_KNOB = of("dial_knob");
    public static final PartialModel LAMP_BASE = of("lamp_base");
    public static final PartialModel LAMP_LENS = of("lamp_lens");
    public static final PartialModel LAMP_LENS_LIT = of("lamp_lens_lit");
    public static final PartialModel GAUGE_BASE = of("gauge_base");
    public static final PartialModel GAUGE_NEEDLE = of("gauge_needle");
    /** A gauge on a wall panel: its dial lies in the panel, read straight on. */
    public static final PartialModel GAUGE_WALL_BASE = of("gauge_wall_base");
    public static final PartialModel GAUGE_WALL_NEEDLE = of("gauge_wall_needle");
    public static final PartialModel KEY_SWITCH_BASE = of("key_switch_base");
    public static final PartialModel KEY_SWITCH_BAT = of("key_switch_bat");
    public static final PartialModel CONTROLLER_BASE = of("controller_base");
    public static final PartialModel CONTROLLER_KNOB = of("controller_knob");
    public static final PartialModel CONTROLLER_LAMP = of("controller_lamp");
    public static final PartialModel CONTROLLER_LAMP_LIT = of("controller_lamp_lit");
    /** The housing of an LED display; the renderer draws its digits. */
    public static final PartialModel LED_DISPLAY_BASE = of("led_display_base");

    private DeskPartials() {}

    /** The fixed part of a module; {@code wall} for one standing on a wall panel. */
    public static PartialModel base(ModuleType type, boolean wall) {
        return switch (type) {
            case PUSH_BUTTON -> BUTTON_BASE;
            case LEVER -> LEVER_BASE;
            case DIAL -> DIAL_BASE;
            case LAMP -> LAMP_BASE;
            case GAUGE -> wall ? GAUGE_WALL_BASE : GAUGE_BASE;
            case KEY_SWITCH -> KEY_SWITCH_BASE;
            case CONTROLLER -> CONTROLLER_BASE;
            case LED_DISPLAY -> LED_DISPLAY_BASE;
        };
    }

    private static PartialModel of(String path) {
        return PartialModel.of(ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "block/desk_module/" + path));
    }

    public static void init() {}
}
