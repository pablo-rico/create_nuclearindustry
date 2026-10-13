package org.papiricoh.create_nuclearindustry.fusion.cryo.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.create_nuclearindustry.fusion.cryo.well.HeliumWellBlock;
import org.papiricoh.create_nuclearindustry.fusion.cryo.well.HeliumWellBlockEntity;

/** The half shaft entering a helium well through its back. Drawn here whatever the Flywheel backend. */
public class HeliumWellRenderer extends KineticBlockEntityRenderer<HeliumWellBlockEntity> {

    public HeliumWellRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(HeliumWellBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffers,
                              int light, int overlay) {
        BlockState state = be.getBlockState();
        if (!state.hasProperty(HeliumWellBlock.FACING)) {
            return;
        }
        standardKineticRotationTransform(CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, state,
                HeliumWellBlock.shaftSide(state)), be, light).renderInto(ms, buffers.getBuffer(RenderType.solid()));
    }
}
