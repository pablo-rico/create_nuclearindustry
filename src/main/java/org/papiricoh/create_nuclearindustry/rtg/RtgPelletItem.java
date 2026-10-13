package org.papiricoh.create_nuclearindustry.rtg;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.papiricoh.create_nuclearindustry.AllNuclearDataComponents;

import java.util.List;

/** A radioisotope heat source for the RTG. Its charge halves every half-life while loaded. */
public class RtgPelletItem extends Item {
    private final RtgFuel fuel;

    public RtgPelletItem(RtgFuel fuel, Properties properties) {
        super(properties.component(AllNuclearDataComponents.RTG_CHARGE.get(), 1.0f));
        this.fuel = fuel;
    }

    public RtgFuel fuel() {
        return fuel;
    }

    public static float charge(ItemStack stack) {
        Float charge = stack.get(AllNuclearDataComponents.RTG_CHARGE.get());
        return charge == null ? 1.0f : charge;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.create_nuclearindustry.rtg_pellet.charge", Math.round(charge(stack) * 100))
                .withStyle(ChatFormatting.GRAY));
    }
}
