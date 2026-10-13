package org.papiricoh.create_nuclearindustry.coolingtower.blockentity;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.create_nuclearindustry.AllNuclearEntities;
import org.papiricoh.create_nuclearindustry.Config;

import java.util.List;

/** Forced-draft fan: a stress consumer whose share of the tower's boost grows with speed. */
public class CoolingTowerFanBlockEntity extends KineticBlockEntity {
    public static final float MIN_SPEED = 16.0f;
    public static final float RATED_SPEED = 256.0f;

    public CoolingTowerFanBlockEntity(BlockPos pos, BlockState state) {
        super(AllNuclearEntities.COOLING_TOWER_FAN.get(), pos, state);
    }

    /** 0..1: nothing below 16 RPM, full at 256 RPM. */
    public double boostContribution() {
        float speed = Math.abs(getSpeed());
        if (speed < MIN_SPEED || isOverStressed()) {
            return 0.0;
        }
        return Math.min(1.0, speed / RATED_SPEED);
    }

    @Override
    public float calculateStressApplied() {
        float impact = (float) Config.coolingTowerFanStress;
        lastStressApplied = impact;
        return impact;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        tooltip.add(Component.literal("Cooling Tower Fan").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal(String.format("  Draft: %.0f%%", boostContribution() * 100))
                .withStyle(boostContribution() > 0 ? ChatFormatting.GREEN : ChatFormatting.RED));
        return true;
    }
}
