package org.papiricoh.create_nuclearindustry.antimatter.trap;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.papiricoh.create_nuclearindustry.accelerator.physics.PenningTrapModel;
import org.papiricoh.create_nuclearindustry.accelerator.physics.TrapState;
import org.papiricoh.create_nuclearindustry.antimatter.vial.AntimatterVialItem;

import java.util.List;
import java.util.Locale;

/** What goggles show of a Penning trap: charge, field, energy, state, time left and the vial port. */
final class PenningTrapGoggles {
    private static final String K = "create_nuclearindustry.penning_trap.goggles.";

    private PenningTrapGoggles() {}

    static void add(PenningTrapBlockEntity trap, List<Component> tooltip) {
        PenningTrapModel.Params p = trap.params();
        TrapState state = trap.state();
        tooltip.add(Component.literal("    ").append(trap.getBlockState().getBlock().getName()).withStyle(ChatFormatting.GOLD));
        tooltip.add(line(Component.translatable(K + "stored", fmt2(trap.stored()), fmt0(p.capacity()),
                fmt0(PenningTrapModel.fill(p, trap.stored()) * 100)), ChatFormatting.AQUA));
        tooltip.add(line(Component.translatable(K + "state", Component.translatable(PenningTrapBlockEntity.stateKey(state))),
                switch (state) {
                    case STABLE -> ChatFormatting.GREEN;
                    case LOW_POWER -> ChatFormatting.YELLOW;
                    case QUENCHING -> ChatFormatting.RED;
                    case OFFLINE -> ChatFormatting.GRAY;
                }));
        tooltip.add(line(Component.translatable(K + "field", fmt0(trap.fieldStrength() * 100), fmt1(trap.demand())), ChatFormatting.GRAY));
        tooltip.add(line(Component.translatable(K + "energy", trap.buffer().getEnergyStored(), trap.buffer().getMaxEnergyStored()),
                ChatFormatting.GRAY));
        if (state == TrapState.QUENCHING) {
            tooltip.add(line(Component.translatable(K + "quench", fmt1(trap.quenchTicksLeft() / 20.0)), ChatFormatting.RED));
        } else if (PenningTrapModel.loaded(trap.stored())) {
            int ticks = trap.ticksToAnnihilation();
            if (ticks != Integer.MAX_VALUE) {
                tooltip.add(line(Component.translatable(K + "time_left", fmt1(ticks / 20.0)),
                        ticks < 400 ? ChatFormatting.RED : ChatFormatting.YELLOW));
            }
        }
        ItemStack vial = trap.vial();
        Component mode = Component.translatable(trap.mode().langKey());
        if (vial.isEmpty()) {
            tooltip.add(line(Component.translatable(K + "port_empty", mode), ChatFormatting.DARK_GRAY));
        } else {
            tooltip.add(line(Component.translatable(K + "port", fmt2(AntimatterVialItem.antimatter(vial)), mode), ChatFormatting.GREEN));
        }
    }

    private static Component line(Component text, ChatFormatting style) {
        return Component.literal("     ").append(text).withStyle(style);
    }

    private static String fmt0(double value) {
        return String.format(Locale.ROOT, "%.0f", value);
    }

    private static String fmt1(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static String fmt2(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
