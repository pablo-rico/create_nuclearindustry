package org.papiricoh.create_nuclearindustry.fusion.cryo.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * The half shaft entering a compressor or vacuum pump through its outward face, and the rotor it
 * turns behind the grille. Drawn here whatever the Flywheel backend.
 */
public class CryoRotorRenderer<T extends KineticBlockEntity> extends KineticBlockEntityRenderer<T> {
    private final PartialModel rotor;

    public CryoRotorRenderer(BlockEntityRendererProvider.Context context, PartialModel rotor) {
        super(context);
        this.rotor = rotor;
    }

    @Override
    protected void renderSafe(T be, float partialTicks, PoseStack ms, MultiBufferSource buffers, int light, int overlay) {
        BlockState state = be.getBlockState();
        if (!state.hasProperty(BlockStateProperties.FACING)) {
            return;
        }
        Direction facing = state.getValue(BlockStateProperties.FACING);
        // Ask for the buffer right before each draw: a shared buffer may have been closed in between.
        standardKineticRotationTransform(CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, state, facing), be, light)
                .renderInto(ms, buffers.getBuffer(RenderType.solid()));
        standardKineticRotationTransform(CachedBuffers.partialFacing(rotor, state, facing), be, light)
                .renderInto(ms, buffers.getBuffer(RenderType.solid()));
    }
}
