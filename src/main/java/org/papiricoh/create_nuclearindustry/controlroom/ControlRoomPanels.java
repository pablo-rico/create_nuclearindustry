package org.papiricoh.create_nuclearindustry.controlroom;

import com.simibubi.create.infrastructure.config.AllConfigs;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ControlDeskBlockEntity;
import org.papiricoh.create_nuclearindustry.controlroom.desk.LedGroups;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ModuleSurfaceBlock;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ControlModule;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ModuleFunction;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ModuleSurface;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ModuleType;
import org.papiricoh.create_nuclearindustry.controlroom.desk.OutputMode;
import org.papiricoh.create_nuclearindustry.controlroom.desk.ReadingValue;
import org.papiricoh.create_nuclearindustry.controlroom.link.AdvancedRedstoneLinkBlockEntity;
import org.papiricoh.create_nuclearindustry.controlroom.link.LinkCode;
import org.papiricoh.create_nuclearindustry.panel.PanelCard;

import java.util.List;
import java.util.Locale;

import static org.papiricoh.create_nuclearindustry.panel.PanelCard.*;

/**
 * The control room's panel cards: a control module (what it is set to, what it shows and where its redstone
 * goes), the console of a desk, table or wall panel when the crosshair is on no module, and an
 * Advanced Redstone Link. Only reads what the client already has.
 */
public final class ControlRoomPanels {
    private static final String P = "create_nuclearindustry.panel.";
    private static final String M = "create_nuclearindustry.control_module.";

    private ControlRoomPanels() {}

    // ------------------------------------------------------------------ modules

    /**
     * The card for a crosshair at {@code hit} on a desk, table or wall panel: the module there (for an
     * LED display, the head whose setup the whole display shows), or the console.
     */
    public static PanelCard surface(ControlDeskBlockEntity desk, Vec3 hit) {
        var state = desk.getBlockState();
        ModuleSurface surface = ModuleSurface.of(state);
        int slot = ModuleSurfaceBlock.slotAt(state, desk.getBlockPos(), hit);
        ControlModule module = surface != null && surface.hasSlot(state, slot) ? desk.module(slot) : null;
        if (module == null) {
            return console(desk).key(-1);
        }
        if (module.type() == ModuleType.LED_DISPLAY && desk.getLevel() != null) {
            LedGroups.Cell head = LedGroups.headOf(desk.getLevel(), desk.getBlockPos(), slot);
            if (desk.getLevel().getBlockEntity(head.pos()) instanceof ControlDeskBlockEntity headDesk && headDesk.module(head.slot()) != null) {
                module = headDesk.module(head.slot());
            }
        }
        return module(desk, slot, module).key(slot);
    }

    /**
     * {@code module} is the one the crosshair is on, or for an LED display the head whose setup the
     * whole display shows.
     */
    public static PanelCard module(ControlDeskBlockEntity desk, int slot, ControlModule module) {
        boolean linked = desk.getTarget() != null;
        int accent = accent(module, linked);
        // Without a label a gauge or LED display is best named by what it reads.
        Component title = !module.label().isEmpty() ? Component.literal(module.label())
                : module.function() == ModuleFunction.READING ? Component.translatable(module.reading().translationKey())
                : Component.translatable(module.function().translationKey());
        PanelCard card = new PanelCard(kicker(desk, slot), title, Component.translatable(module.type().translationKey()), accent);
        if (!linked) {
            card.status(Component.translatable(P + "status.unlinked"), AMBER, true);
        } else if (!module.active()) {
            card.status(Component.translatable(P + "status.wrong_target"), RED, true);
        } else {
            card.status(Component.translatable(P + "status.online"), GREEN, false);
        }
        body(card, module, accent);
        output(card, module);
        card.footer(footer(desk));
        return card;
    }

    private static int accent(ControlModule module, boolean linked) {
        if (!linked || !module.active()) {
            return linked ? RED : AMBER;
        }
        if (module.type() == ModuleType.LED_DISPLAY) {
            return AMBER;
        }
        if (module.isController()) {
            return module.engaged() && module.demand() >= 0 ? GREEN : AMBER;
        }
        if (module.type() == ModuleType.LAMP && module.shown() > 0.5f) {
            return lampColor(module.function());
        }
        return CYAN;
    }

    private static int lampColor(ModuleFunction function) {
        return switch (function) {
            case LAMP_RUNNING -> GREEN;
            case LAMP_NO_SIGNAL -> AMBER;
            default -> RED;
        };
    }

    private static void body(PanelCard card, ControlModule module, int accent) {
        ModuleFunction function = module.function();
        boolean on = module.shown() > 0.5f;
        if (function == ModuleFunction.READING) {
            if (!module.label().isEmpty()) {
                card.add(new Field(Component.translatable(P + "reading"), Component.translatable(module.reading().translationKey()), TEXT));
            }
            card.add(new BigValue(Component.literal(number(module)), ReadingValue.unit(module.reading()), accent));
            card.add(new Bar(Component.translatable(P + "scale"), module.shown(), percent(module.shown()), accent));
        } else if (module.isController()) {
            card.add(new Field(Component.translatable(P + "mode"),
                    Component.translatable(M + (module.engaged() ? "mode.auto" : "mode.standby")), module.engaged() ? GREEN : AMBER));
            card.add(new BigValue(Component.literal(module.formatSetpoint()), "", accent));
            card.add(new Field(Component.translatable(P + "demand"), module.describeDemand(), TEXT));
            card.add(new Bar(Component.translatable(P + "setpoint"), module.setting() / 100f, percent(module.setting() / 100f), accent));
        } else if (function == ModuleFunction.ROD_TARGET) {
            card.add(new BigValue(Component.literal(Integer.toString(Math.round(module.shown() * 100))), "%", accent));
            card.add(new Bar(Component.translatable(P + "rods_in"), module.shown(), percent(module.shown()), accent));
        } else if (function == ModuleFunction.SCRAM_THRESHOLD) {
            int threshold = Math.round(ControlModule.MIN_THRESHOLD + module.shown() * (ControlModule.MAX_THRESHOLD - ControlModule.MIN_THRESHOLD));
            card.add(new BigValue(Component.literal(Integer.toString(threshold)), "°C", accent));
            card.add(new Bar(Component.translatable(P + "threshold"), module.shown(), percent(module.shown()), accent));
        } else if (function == ModuleFunction.SIGNAL_LEVEL) {
            card.add(new BigValue(Component.literal(Integer.toString(module.setting())), "/15", accent));
            card.add(new Bar(Component.translatable(P + "level"), module.setting() / 15f, Component.literal(module.setting() + "/15"), accent));
        } else if (function == ModuleFunction.LAMP_ALARM) {
            card.add(new Field(Component.translatable(P + "alarm"), Component.translatable(module.metric().getTranslationKey()), TEXT));
            if (!module.metric().isFlag()) {
                card.add(new Field(Component.translatable(P + "threshold"), Component.literal(module.metric().format(module.setting())), TEXT));
            }
            card.add(new BigValue(onOff(on), "", on ? RED : DIM));
        } else {
            card.add(new BigValue(onOff(on), "", on ? accent : DIM));
        }
    }

    private static void output(PanelCard card, ControlModule module) {
        card.add(new Section(Component.translatable(P + "redstone")));
        OutputMode output = module.output();
        if (output == OutputMode.ADVANCED_LINK) {
            card.add(new Code(Component.translatable(M + "output.advanced_link"), module.linkCode(),
                    LinkCode.complete(module.linkCode()) ? AMBER : RED));
        } else if (output == OutputMode.LINK) {
            card.add(new Items(Component.translatable(M + "output.link"), List.of(module.frequencyFirst(), module.frequencyLast())));
        } else {
            card.add(new Field(Component.translatable(P + "output"), Component.translatable(output.translationKey()), TEXT));
        }
        card.add(new Signal(Component.translatable(P + "signal"), module.signal()));
    }

    // ------------------------------------------------------------------ consoles

    /** The console, for a crosshair on a desk, table or wall panel but on no module. */
    public static PanelCard console(ControlDeskBlockEntity desk) {
        GlobalPos target = desk.getTarget();
        int fitted = 0;
        int slots = 0;
        ModuleSurface surface = ModuleSurface.of(desk.getBlockState());
        for (int slot = 0; slot < ControlDeskBlockEntity.SLOTS; slot++) {
            if (surface != null && surface.hasSlot(desk.getBlockState(), slot)) {
                slots++;
                if (desk.module(slot) != null) {
                    fitted++;
                }
            }
        }
        PanelCard card = new PanelCard(desk.getBlockState().getBlock().getName(), Component.translatable(P + "console"),
                Component.literal(fitted + "/" + slots), target == null ? AMBER : CYAN);
        if (target == null) {
            card.status(Component.translatable(P + "status.unlinked"), AMBER, true);
        } else {
            card.status(Component.translatable(P + "status.linked"), GREEN, false);
        }
        card.add(new Field(Component.translatable(P + "target"), target == null ? Component.literal("-")
                : Component.literal(target.pos().getX() + " " + target.pos().getY() + " " + target.pos().getZ()), TEXT));
        card.add(new Field(Component.translatable(P + "console_blocks"), Component.literal(Integer.toString(desk.consoleSize())), TEXT));
        card.add(new Field(Component.translatable(P + "modules"), Component.literal(fitted + " / " + slots), TEXT));
        card.add(new Bar(Component.translatable(P + "slots_used"), slots == 0 ? 0 : fitted / (float) slots,
                Component.literal(fitted + "/" + slots), target == null ? AMBER : CYAN));
        card.add(new Note(Component.translatable(target == null ? P + "hint.link" : P + "hint.module")));
        return card;
    }

    // ------------------------------------------------------------------ links

    public static PanelCard link(AdvancedRedstoneLinkBlockEntity link) {
        boolean receiver = link.isReceiver();
        boolean complete = LinkCode.complete(link.getCode());
        int accent = receiver ? CYAN : AMBER;
        PanelCard card = new PanelCard(link.getBlockState().getBlock().getName(),
                Component.translatable(P + (receiver ? "receiver" : "transmitter")), Component.literal(receiver ? "RX" : "TX"), accent);
        if (!complete) {
            card.status(Component.translatable(P + "status.no_code"), RED, true);
        } else if (link.signal() > 0) {
            card.status(Component.translatable(P + (receiver ? "status.receiving" : "status.transmitting")), GREEN, false);
        } else {
            card.status(Component.translatable(P + "status.idle"), DIM, false);
        }
        card.add(new Code(Component.translatable(P + "code"), link.getCode(), complete ? accent : RED));
        card.add(new Signal(Component.translatable(P + "signal"), link.signal()));
        card.add(new Field(Component.translatable(P + "range"), Component.translatable(P + "blocks", linkRange()), TEXT));
        card.add(new Note(Component.translatable(P + "hint.code")));
        card.add(new Note(Component.translatable(P + (receiver ? "hint.to_transmitter" : "hint.to_receiver"))));
        return card;
    }

    /** Create's link range, from its server config when the client has it. */
    private static String linkRange() {
        try {
            return Integer.toString(AllConfigs.server().logistics.linkRange.get());
        } catch (RuntimeException notLoaded) {
            return "?";
        }
    }

    // ------------------------------------------------------------------ text

    private static Component kicker(ControlDeskBlockEntity desk, int slot) {
        return desk.getBlockState().getBlock().getName().copy().append(" · ").append(Component.translatable(P + "slot", slot + 1));
    }

    private static Component footer(ControlDeskBlockEntity desk) {
        GlobalPos target = desk.getTarget();
        MutableComponent link = target == null ? Component.translatable(P + "footer.unlinked")
                : Component.translatable(P + "footer.linked", target.pos().getX(), target.pos().getY(), target.pos().getZ());
        return link.append(" · ").append(Component.translatable(P + "footer.console", desk.consoleSize()));
    }

    /** A reading's value as the monitors write it, or dashes without one. */
    private static String number(ControlModule module) {
        float value = module.value();
        return Float.isNaN(value) ? "---"
                : String.format(Locale.ROOT, "%." + ReadingValue.decimals(module.reading()) + "f", value);
    }

    private static Component percent(float fraction) {
        return Component.literal(Math.round(Math.max(0, Math.min(1, fraction)) * 100) + "%");
    }

    private static Component onOff(boolean on) {
        return Component.translatable(P + (on ? "on" : "off"));
    }
}
