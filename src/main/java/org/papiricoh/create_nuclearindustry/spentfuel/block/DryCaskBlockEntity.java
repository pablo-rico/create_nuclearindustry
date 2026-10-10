package org.papiricoh.create_nuclearindustry.spentfuel.block;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.create_nuclearindustry.spentfuel.CoolingGrade;
import org.papiricoh.create_nuclearindustry.spentfuel.SpentFuel;
import org.papiricoh.create_nuclearindustry.spentfuel.SpentFuelConfig;
import org.papiricoh.create_nuclearindustry.spentfuel.SpentFuelRegistry;
import org.papiricoh.create_nuclearindustry.spentfuel.SpentFuelText;

import java.util.List;

/**
 * Dry storage cask: steel and concrete around nine assemblies that have already cooled in a pool.
 * Air cooling cannot take fresh fuel, so Hot assemblies are refused. It lets almost nothing out,
 * never leaks, and the fuel inside keeps cooling.
 */
public class DryCaskBlockEntity extends SpentFuelStorageBlockEntity {

    public DryCaskBlockEntity(BlockPos pos, BlockState state) {
        super(SpentFuelRegistry.DRY_CASK_ENTITY.get(), pos, state);
    }

    @Override
    public boolean accepts(ItemStack stack) {
        if (!super.accepts(stack)) {
            return false;
        }
        long now = level != null ? level.getGameTime() : 0;
        return SpentFuel.currentGrade(stack, now) != CoolingGrade.HOT;
    }

    @Override
    protected double containment() {
        return SpentFuelConfig.caskTransmission;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        addContentsTooltip(tooltip);
        tooltip.add(Component.literal("     ").append(Component.translatable(SpentFuelText.PREFIX + "goggles.cask_rule",
                SpentFuelText.grade(CoolingGrade.WARM))).withStyle(ChatFormatting.GRAY));
        return true;
    }
}
