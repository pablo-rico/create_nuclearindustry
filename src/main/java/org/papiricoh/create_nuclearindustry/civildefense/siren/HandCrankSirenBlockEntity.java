package org.papiricoh.create_nuclearindustry.civildefense.siren;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.create_nuclearindustry.civildefense.CivilDefenseConfig;

import java.util.List;

/** Sounds while turning fast enough and not overstressed; the pitch follows the speed. */
public class HandCrankSirenBlockEntity extends KineticBlockEntity implements SoundingSiren {

    public HandCrankSirenBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null) {
            return;
        }
        if (level.isClientSide) {
            SoundingSiren.ClientHooks.tick(this);
            return;
        }
        boolean sounding = fastEnough();
        BlockState state = getBlockState();
        if (state.getValue(AirRaidSirenBlock.SOUNDING) != sounding) {
            level.setBlock(worldPosition, state.setValue(AirRaidSirenBlock.SOUNDING, sounding), Block.UPDATE_ALL);
            if (sounding) org.papiricoh.create_nuclearindustry.advancement.NuclearAdvancements.awardNearby(level, worldPosition, org.papiricoh.create_nuclearindustry.advancement.NuclearAdvancements.WITNESS_RADIUS, org.papiricoh.create_nuclearindustry.advancement.NuclearEvent.SIREN_SOUNDED);
        }
    }

    /** Turning at {@code crankMinSpeed} RPM or more, either way, with enough stress capacity. */
    public boolean fastEnough() {
        return !isOverStressed() && Math.abs(getSpeed()) >= CivilDefenseConfig.crankSirenMinSpeed;
    }

    @Override
    public boolean isSounding() {
        return getBlockState().getValue(AirRaidSirenBlock.SOUNDING);
    }

    @Override
    public float pitch(double ticks) {
        return SirenTone.crank(getSpeed());
    }

    @Override
    public float calculateStressApplied() {
        float impact = (float) CivilDefenseConfig.crankSirenStress;
        lastStressApplied = impact;
        return impact;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        tooltip.add(Component.literal("    ").append(Component.translatable("block.create_nuclearindustry.hand_crank_siren"))
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(AirRaidSirenBlockEntity.indent(isSounding()
                ? Component.translatable("create_nuclearindustry.siren.alert").withStyle(ChatFormatting.YELLOW)
                : Component.translatable("create_nuclearindustry.siren.needs_speed", CivilDefenseConfig.crankSirenMinSpeed)
                        .withStyle(ChatFormatting.GRAY)));
        return true;
    }
}
