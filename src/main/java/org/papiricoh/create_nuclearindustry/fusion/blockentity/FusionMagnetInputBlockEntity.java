package org.papiricoh.create_nuclearindustry.fusion.blockentity;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.create_nuclearindustry.AllNuclearEntities;

import java.util.List;

/**
 * Consumes a large amount of Create stress to energize the confinement field. The contribution it
 * reports (0..1) scales with shaft speed once above the minimum, and the controller reads it each
 * tick. While the plasma is ignited the confinement cost tapers off (self-sustaining field).
 */
public class FusionMagnetInputBlockEntity extends KineticBlockEntity {
    private static final float MIN_CONFINEMENT_SPEED = 64.0f;
    private static final float RATED_SPEED = 192.0f;
    private static final float CONFINEMENT_STRESS = 1024.0f;

    public FusionMagnetInputBlockEntity(BlockPos pos, BlockState state) {
        super(AllNuclearEntities.FUSION_MAGNET_INPUT.get(), pos, state);
    }

    public void tick() {
        super.tick();
    }

    /** 0..1 confinement contribution from current shaft speed. */
    public double getConfinementContribution() {
        float speed = Math.abs(getSpeed());
        if (speed < MIN_CONFINEMENT_SPEED) {
            return 0.0;
        }
        return Math.min(1.0, speed / RATED_SPEED);
    }

    @Override
    public float calculateStressApplied() {
        return CONFINEMENT_STRESS;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        tooltip.add(Component.literal("Confinement Magnet Drive").withStyle(ChatFormatting.AQUA));
        float speed = Math.abs(getSpeed());
        if (speed < MIN_CONFINEMENT_SPEED) {
            tooltip.add(Component.literal("  Status: Spin up (need " + (int) MIN_CONFINEMENT_SPEED + " RPM)")
                    .withStyle(ChatFormatting.RED));
        } else {
            tooltip.add(Component.literal(String.format("  Confinement: %.0f%%", getConfinementContribution() * 100.0))
                    .withStyle(ChatFormatting.GREEN));
        }
        tooltip.add(Component.literal(String.format("  Speed: %.1f RPM", speed)).withStyle(ChatFormatting.GRAY));
        return true;
    }
}
