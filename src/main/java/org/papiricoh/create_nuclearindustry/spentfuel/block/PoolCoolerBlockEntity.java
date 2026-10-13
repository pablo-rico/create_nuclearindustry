package org.papiricoh.create_nuclearindustry.spentfuel.block;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.create_nuclearindustry.spentfuel.SpentFuelConfig;
import org.papiricoh.create_nuclearindustry.spentfuel.SpentFuelPool;
import org.papiricoh.create_nuclearindustry.spentfuel.SpentFuelRegistry;
import org.papiricoh.create_nuclearindustry.spentfuel.SpentFuelText;

import java.util.List;

/**
 * A pump and heat exchanger for a spent fuel pool, driven by a shaft from above. Racks in the
 * same body of water count its capacity with the pool's own; it only works submerged.
 */
public class PoolCoolerBlockEntity extends KineticBlockEntity {
    /** Below this it barely stirs the water. */
    public static final double MIN_SPEED = 8.0;

    public PoolCoolerBlockEntity(BlockPos pos, BlockState state) {
        super(SpentFuelRegistry.POOL_COOLER_ENTITY.get(), pos, state);
    }

    /** Heat it carries away right now, in kW. */
    public double coolingCapacity() {
        BlockState state = getBlockState();
        if (isOverStressed() || !state.hasProperty(PoolCoolerBlock.WATERLOGGED) || !state.getValue(PoolCoolerBlock.WATERLOGGED)) {
            return 0.0;
        }
        return SpentFuelPool.coolerCapacity(getSpeed(), MIN_SPEED, SpentFuelConfig.coolerRatedSpeed, SpentFuelConfig.coolerCapacity);
    }

    @Override
    public float calculateStressApplied() {
        float impact = (float) SpentFuelConfig.coolerStress;
        lastStressApplied = impact;
        return impact;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        tooltip.add(Component.literal("    ").append(getBlockState().getBlock().getName()).withStyle(ChatFormatting.AQUA));
        double capacity = coolingCapacity();
        tooltip.add(Component.literal("     ").append(Component.translatable(SpentFuelText.PREFIX + "goggles.cooler",
                String.format("%.1f", capacity))).withStyle(capacity > 0 ? ChatFormatting.GREEN : ChatFormatting.RED));
        if (!getBlockState().getValue(PoolCoolerBlock.WATERLOGGED)) {
            tooltip.add(Component.literal("     ").append(Component.translatable(SpentFuelText.PREFIX + "goggles.cooler_dry"))
                    .withStyle(ChatFormatting.RED));
        }
        return true;
    }
}
