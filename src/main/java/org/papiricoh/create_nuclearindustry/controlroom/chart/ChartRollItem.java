package org.papiricoh.create_nuclearindustry.controlroom.chart;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.fml.loading.FMLEnvironment;
import org.papiricoh.create_nuclearindustry.controlroom.ControlRoom2Registry;
import org.papiricoh.create_nuclearindustry.controlroom.TargetKind;
import org.papiricoh.create_nuclearindustry.controlroom.desk.GaugeReading;

import java.util.List;

/** A chart torn off a Chart Recorder. Use it to read the chart; it changes nothing in the world. */
public class ChartRollItem extends Item {
    private static final String KEY = "item.create_nuclearindustry.chart_roll.";

    public ChartRollItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        ChartData data = stack.get(ControlRoom2Registry.CHART_DATA.get());
        if (data == null) {
            return InteractionResultHolder.pass(stack);
        }
        if (level.isClientSide && FMLEnvironment.dist.isClient()) {
            org.papiricoh.create_nuclearindustry.controlroom.client.ChartRollScreen.open(data);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        ChartData data = stack.get(ControlRoom2Registry.CHART_DATA.get());
        if (data == null) {
            tooltip.add(Component.translatable(KEY + "blank").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        tooltip.add(Component.translatable(KEY + "source", Component.translatable(TargetKind.byName(data.kind()).translationKey()),
                String.format(java.util.Locale.ROOT, "%.0f", data.spanTicks() / 20.0)).withStyle(ChatFormatting.GRAY));
        for (int pen = 0; pen < data.pens().size(); pen++) {
            GaugeReading reading = data.pen(pen);
            if (reading != null) {
                tooltip.add(Component.literal("  ").append(Component.translatable(reading.translationKey()))
                        .withStyle(pen == 0 ? ChatFormatting.RED : pen == 1 ? ChatFormatting.BLUE : ChatFormatting.DARK_GREEN));
            }
        }
        tooltip.add(Component.translatable(KEY + "hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
