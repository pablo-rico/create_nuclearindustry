package org.papiricoh.create_nuclearindustry.energy;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A Forge Energy rate from 0 to 1,000,000 FE/t on Create's value board, which only has room for a
 * hundred steps: the first row sets it in thousands (up to 100,000), the second in ten thousands.
 */
public class FeRateScrollBehaviour extends ScrollValueBehaviour {
    public static final int MAX = 1_000_000;
    private static final int STEPS = 100;
    private static final int FINE = 1_000;
    private static final int COARSE = 10_000;

    public FeRateScrollBehaviour(Component label, SmartBlockEntity be, ValueBoxTransform slot) {
        super(label, be, slot);
        between(0, MAX);
        withFormatter(FeRateScrollBehaviour::shortText);
    }

    @Override
    public ValueSettingsBoard createBoard(Player player, BlockHitResult hitResult) {
        return new ValueSettingsBoard(label, STEPS, 10,
                ImmutableList.of(Component.translatable("create_nuclearindustry.creative_rf_generator.row.fine"),
                        Component.translatable("create_nuclearindustry.creative_rf_generator.row.coarse")),
                new ValueSettingsFormatter(this::formatSettings));
    }

    @Override
    public void setValueSettings(Player player, ValueSettings settings, boolean ctrlDown) {
        if (!settings.equals(getValueSettings())) {
            playFeedbackSound(this);
        }
        setValue(toValue(settings.row(), settings.value()));
    }

    @Override
    public ValueSettings getValueSettings() {
        int v = getValue();
        if (v <= STEPS * FINE && v % FINE == 0) {
            return new ValueSettings(0, v / FINE);
        }
        return new ValueSettings(1, Math.round(v / (float) COARSE));
    }

    public MutableComponent formatSettings(ValueSettings settings) {
        return Component.literal(longText(toValue(settings.row(), settings.value())) + " FE/t");
    }

    /** The rate a board setting stands for. */
    public static int toValue(int row, int value) {
        int steps = Math.max(0, Math.min(STEPS, value));
        return Math.min(MAX, steps * (row == 0 ? FINE : COARSE));
    }

    static String shortText(int value) {
        if (value >= 1_000_000) {
            return value / 1_000_000 + "M";
        }
        if (value >= 1_000) {
            return value / 1_000 + "k";
        }
        return Integer.toString(value);
    }

    static String longText(int value) {
        return String.format(java.util.Locale.ROOT, "%,d", value);
    }

    @Override
    public String getClipboardKey() {
        return "FE";
    }
}
