package org.papiricoh.create_nuclearindustry.spentfuel;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** Translated, coloured names for what players read about spent fuel. */
public final class SpentFuelText {
    public static final String PREFIX = "create_nuclearindustry.spent_fuel.";

    private SpentFuelText() {}

    public static MutableComponent grade(CoolingGrade grade) {
        return Component.translatable(PREFIX + "grade." + grade.id()).withStyle(color(grade));
    }

    public static ChatFormatting color(CoolingGrade grade) {
        return switch (grade) {
            case HOT -> ChatFormatting.RED;
            case WARM -> ChatFormatting.GOLD;
            case COOLED -> ChatFormatting.AQUA;
            case AGED -> ChatFormatting.GREEN;
        };
    }
}
