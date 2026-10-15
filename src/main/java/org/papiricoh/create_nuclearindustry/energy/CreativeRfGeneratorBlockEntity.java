package org.papiricoh.create_nuclearindustry.energy;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.CenteredSideValueBoxTransform;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;

import java.util.List;

/**
 * Pushes its set rate of Forge Energy (0 to 1,000,000 FE/t, 100,000 by default) into every block
 * around it each tick, and lets cables pull as much as they like: an endless store. The rate is set
 * on Create's value panel on any side.
 */
public class CreativeRfGeneratorBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    public static final int DEFAULT_RATE = 100_000;

    private FeRateScrollBehaviour rate;
    /** FE pushed into the neighbours on the last tick. */
    private int lastPushed;
    private final IEnergyStorage endless = new Endless();

    public CreativeRfGeneratorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        rate = new FeRateScrollBehaviour(Component.translatable("create_nuclearindustry.creative_rf_generator.rate"), this,
                new CenteredSideValueBoxTransform((state, direction) -> true));
        rate.setValue(DEFAULT_RATE);
        behaviours.add(rate);
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) {
            return;
        }
        int perSide = rate();
        int pushed = 0;
        for (Direction side : Direction.values()) {
            pushed += FeSource.push(level, worldPosition, side, perSide);
        }
        lastPushed = pushed;
    }

    /** FE per tick offered to each neighbour. */
    public int rate() {
        return rate == null ? DEFAULT_RATE : rate.getValue();
    }

    /** Sets the rate (game tests). */
    public void setRate(int value) {
        rate.setValue(Math.max(0, Math.min(FeRateScrollBehaviour.MAX, value)));
    }

    /** FE the neighbours took on the last tick. */
    public int lastPushed() {
        return lastPushed;
    }

    public IEnergyStorage energy() {
        return endless;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        String k = "create_nuclearindustry.creative_rf_generator.goggles.";
        tooltip.add(Component.literal("    ").append(getBlockState().getBlock().getName()).withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.literal("     ").append(Component.translatable(k + "rate", FeRateScrollBehaviour.longText(rate())))
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("     ").append(Component.translatable(k + "hint")).withStyle(ChatFormatting.DARK_GRAY));
        return true;
    }

    /** As much as anyone asks for; takes nothing. */
    private static final class Endless implements IEnergyStorage {
        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            return 0;
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            return Math.max(0, toExtract);
        }

        @Override
        public int getEnergyStored() {
            return Integer.MAX_VALUE;
        }

        @Override
        public int getMaxEnergyStored() {
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean canExtract() {
            return true;
        }

        @Override
        public boolean canReceive() {
            return false;
        }
    }
}
