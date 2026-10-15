package org.papiricoh.create_nuclearindustry.antimatter.generator;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.papiricoh.create_nuclearindustry.accelerator.physics.AnnihilationModel;
import org.papiricoh.create_nuclearindustry.accelerator.physics.TrapState;
import org.papiricoh.create_nuclearindustry.antimatter.vial.AntimatterVialItem;

import java.util.List;
import java.util.Locale;

/**
 * What goggles show of an annihilation generator: what it is doing, its mode, the throttle and the
 * output it delivers, the chamber and its field, the energy it makes and the vial port. Create adds
 * the stress capacity underneath.
 */
final class AnnihilationGeneratorGoggles {
    private static final String K = "create_nuclearindustry.annihilation_generator.goggles.";

    private AnnihilationGeneratorGoggles() {}

    static void add(AnnihilationGeneratorBlockEntity generator, List<Component> tooltip) {
        tooltip.add(Component.literal("    ").append(generator.getBlockState().getBlock().getName()).withStyle(ChatFormatting.GOLD));
        String status = generator.statusId();
        tooltip.add(line(Component.translatable(K + "status", Component.translatable(generator.statusKey())), switch (status) {
            case "running" -> ChatFormatting.GREEN;
            case "standby", "short" -> ChatFormatting.YELLOW;
            case "no_field", "quenching" -> ChatFormatting.RED;
            default -> ChatFormatting.GRAY;
        }));
        AnnihilationModel.Mode mode = generator.effectiveMode();
        tooltip.add(line(Component.translatable(K + "mode", Component.translatable(GeneratorMode.langKey(mode))), ChatFormatting.AQUA));
        if (mode != generator.mode()) {
            tooltip.add(line(Component.translatable(K + "fe_disabled"), ChatFormatting.DARK_GRAY));
        }
        tooltip.add(line(Component.translatable(K + "throttle", fmt(generator.throttle() * 100, 0),
                fmt(generator.outputFraction() * 100, 0)), ChatFormatting.GRAY));
        tooltip.add(line(Component.translatable(K + "chamber", fmt(generator.stored(), 3), fmt(generator.capacity(), 0)),
                ChatFormatting.AQUA));
        if (generator.burning()) {
            tooltip.add(line(Component.translatable(generator.lit() ? K + "field_lit" : K + "field",
                    fmt(generator.fieldStrength() * 100, 0)), ChatFormatting.GRAY));
        } else {
            tooltip.add(line(Component.translatable(K + "field", fmt(generator.fieldStrength() * 100, 0)),
                    generator.fieldFailing() ? ChatFormatting.RED : ChatFormatting.GRAY));
        }
        tooltip.add(line(Component.translatable(K + "buffer", generator.fieldBuffer().getEnergyStored(), generator.fieldBufferMax()),
                ChatFormatting.GRAY));
        if (generator.chamberState() == TrapState.QUENCHING) {
            tooltip.add(line(Component.translatable(K + "quench", fmt(generator.quenchTicksLeft() / 20.0, 1)), ChatFormatting.RED));
        }
        if (mode.makesEnergy()) {
            tooltip.add(line(Component.translatable(K + "energy", generator.feMade()), ChatFormatting.AQUA));
        }
        if (generator.shortSupply()) {
            tooltip.add(line(Component.translatable(K + "short"), ChatFormatting.YELLOW));
        }
        ItemStack vial = generator.vial();
        if (vial.isEmpty()) {
            tooltip.add(line(Component.translatable(K + "port_empty"), ChatFormatting.DARK_GRAY));
        } else {
            tooltip.add(line(Component.translatable(K + "port", fmt(AntimatterVialItem.antimatter(vial), 2)), ChatFormatting.GREEN));
        }
        tooltip.add(Component.empty());
    }

    private static Component line(Component text, ChatFormatting style) {
        return Component.literal("     ").append(text).withStyle(style);
    }

    private static String fmt(double value, int decimals) {
        return String.format(Locale.ROOT, "%." + decimals + "f", value);
    }
}
