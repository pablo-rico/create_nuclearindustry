package org.papiricoh.create_nuclearindustry.fusion.cryo.plant;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.create_nuclearindustry.fusion.cryo.CryoConfig;
import org.papiricoh.create_nuclearindustry.fusion.cryo.CryoRegistry;
import org.papiricoh.create_nuclearindustry.fusion.cryo.physics.CryoplantModel;
import org.papiricoh.create_nuclearindustry.multiblock.ControllerLink;

import java.util.List;
import java.util.Locale;

/**
 * A cryogenic plant compressor. The plant's stress impact ({@code compressorStressPerLayer} per
 * layer) is shared among its compressors, so each carries {@link #share} layers' worth; one
 * outside a formed plant carries a single layer. The controller reads {@link #speedFactor()}.
 */
public class CryoplantCompressorBlockEntity extends KineticBlockEntity {
    private final ControllerLink<CryoplantControllerBlockEntity> link = new ControllerLink<>(this,
            CryoplantControllerBlockEntity.class,
            (controller, pos) -> controller.getStructure().map(s -> s.ownsCompressor(pos)).orElse(false));
    /** Plant layers this compressor drives. */
    private double share = 1.0;
    private boolean linked;

    public CryoplantCompressorBlockEntity(BlockPos pos, BlockState state) {
        super(CryoRegistry.CRYOPLANT_COMPRESSOR_ENTITY.get(), pos, state);
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) {
            return;
        }
        if (link.tick() && link.linkedPos().isEmpty() && (share != 1.0 || linked)) {
            linked = false;
            setShare(1.0);
        }
    }

    /** Share of rated speed this compressor turns at, 0..1; nothing while the network is overstressed. */
    public double speedFactor() {
        if (isOverStressed()) {
            return 0.0;
        }
        return CryoplantModel.speedFactor(getSpeed(), CryoConfig.plantMinSpeed, CryoConfig.plantRatedSpeed);
    }

    void link(BlockPos controllerPos, double layers) {
        link.link(controllerPos);
        linked = true;
        setShare(layers);
    }

    void unlink(BlockPos controllerPos) {
        if (link.unlink(controllerPos)) {
            linked = false;
            setShare(1.0);
        }
    }

    private void setShare(double layers) {
        double next = Math.max(0.0, layers);
        boolean changed = next != share;
        share = next;
        if (changed && hasNetwork()) {
            getOrCreateNetwork().updateStressFor(this, calculateStressApplied());
        }
        setChanged();
        sendData();
    }

    @Override
    public float calculateStressApplied() {
        float impact = (float) (CryoConfig.compressorStressPerLayer * share);
        lastStressApplied = impact;
        return impact;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putDouble("Share", share);
        tag.putBoolean("Linked", linked);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        share = tag.contains("Share") ? tag.getDouble("Share") : 1.0;
        linked = tag.getBoolean("Linked");
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        String k = "create_nuclearindustry.cryoplant.compressor.";
        tooltip.add(Component.literal("    ").append(Component.translatable("block.create_nuclearindustry.cryoplant_compressor"))
                .withStyle(ChatFormatting.GOLD));
        double factor = speedFactor();
        if (factor <= 0) {
            tooltip.add(line(Component.translatable(k + "spin_up", Math.round(CryoConfig.plantMinSpeed)), ChatFormatting.RED));
        } else {
            tooltip.add(line(Component.translatable(k + "speed", Math.round(factor * 100)), ChatFormatting.GREEN));
        }
        tooltip.add(line(linked
                ? Component.translatable(k + "share", String.format(Locale.ROOT, "%.1f", share))
                : Component.translatable(k + "unlinked"), linked ? ChatFormatting.GRAY : ChatFormatting.RED));
        return true;
    }

    private static Component line(Component text, ChatFormatting style) {
        return Component.literal("     ").append(text).withStyle(style);
    }
}
