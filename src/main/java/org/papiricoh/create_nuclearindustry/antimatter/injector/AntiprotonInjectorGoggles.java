package org.papiricoh.create_nuclearindustry.antimatter.injector;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.papiricoh.create_nuclearindustry.accelerator.physics.InjectorModel;
import org.papiricoh.create_nuclearindustry.accelerator.physics.TrapState;
import org.papiricoh.create_nuclearindustry.antimatter.trap.PenningTrapBlockEntity;
import org.papiricoh.create_nuclearindustry.antimatter.vial.AntimatterVialItem;

import java.util.List;
import java.util.Locale;

/** What goggles show of an antiproton injector: arming, why it waits, the doses, its energy and field, and the port. */
final class AntiprotonInjectorGoggles {
    private static final String K = AntiprotonInjectorBlockEntity.K + "goggles.";

    private AntiprotonInjectorGoggles() {}

    static void add(AntiprotonInjectorBlockEntity injector, List<Component> tooltip) {
        tooltip.add(Component.literal("    ").append(injector.getBlockState().getBlock().getName()).withStyle(ChatFormatting.GOLD));
        tooltip.add(line(Component.translatable(K + "arming", Component.translatable(AntiprotonInjectorBlockEntity.armingKey(injector.arming())),
                Component.translatable(injector.armed() ? K + "armed" : K + "disarmed")),
                injector.armed() ? ChatFormatting.GOLD : ChatFormatting.GRAY));
        InjectorModel.Verdict verdict = injector.verdict();
        tooltip.add(line(Component.translatable(AntiprotonInjectorBlockEntity.verdictKey(verdict)), switch (verdict) {
            case READY -> ChatFormatting.GREEN;
            case SAFE, WAITING, IGNITED -> ChatFormatting.GRAY;
            case COOLDOWN, LOW_CONFINEMENT, LOW_DENSITY -> ChatFormatting.YELLOW;
            case NO_DOSE, NO_POWER -> ChatFormatting.RED;
        }));
        int doses = injector.doses();
        tooltip.add(line(Component.translatable(K + "dose", fmt(injector.stored(), 2), fmt(injector.capacity(), 0),
                doses == Integer.MAX_VALUE ? "∞" : Integer.toString(doses), fmt(injector.dose(), 2)),
                doses > 0 ? ChatFormatting.AQUA : ChatFormatting.RED));
        tooltip.add(line(Component.translatable(K + "energy", injector.buffer().getEnergyStored(), injector.bufferMax(), injector.shotFe()),
                injector.buffer().getEnergyStored() >= injector.shotFe() ? ChatFormatting.GRAY : ChatFormatting.YELLOW));
        TrapState state = injector.state();
        tooltip.add(line(Component.translatable(K + "field", Component.translatable(PenningTrapBlockEntity.stateKey(state))), switch (state) {
            case STABLE -> ChatFormatting.GREEN;
            case LOW_POWER -> ChatFormatting.YELLOW;
            case QUENCHING -> ChatFormatting.RED;
            case OFFLINE -> ChatFormatting.GRAY;
        }));
        if (state == TrapState.QUENCHING) {
            tooltip.add(line(Component.translatable(K + "quench", fmt(injector.quenchTicksLeft() / 20.0, 1)), ChatFormatting.RED));
        }
        if (injector.coolingDown()) {
            tooltip.add(line(Component.translatable(K + "cooldown", fmt(injector.cooldownTicks() / 20.0, 1)), ChatFormatting.YELLOW));
        }
        if (!injector.inTokamak()) {
            tooltip.add(line(Component.translatable(K + "no_tokamak"), ChatFormatting.RED));
        }
        ItemStack vial = injector.vial();
        if (!vial.isEmpty()) {
            tooltip.add(line(Component.translatable(K + "port", fmt(AntimatterVialItem.antimatter(vial), 2)), ChatFormatting.GREEN));
        }
    }

    private static Component line(Component text, ChatFormatting style) {
        return Component.literal("     ").append(text).withStyle(style);
    }

    private static String fmt(double value, int decimals) {
        return String.format(Locale.ROOT, "%." + decimals + "f", value);
    }
}
