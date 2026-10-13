package org.papiricoh.create_nuclearindustry.rtg.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import org.papiricoh.create_nuclearindustry.rtg.RtgBlockEntity;

/** The turning half shaft in the RTG's front opening. Drawn here whatever the Flywheel backend. */
public class RtgRenderer extends KineticBlockEntityRenderer<RtgBlockEntity> {
    public RtgRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(RtgBlockEntity rtg, float partialTicks, PoseStack ms, MultiBufferSource buffers, int light, int overlay) {
        var shaft = CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, rtg.getBlockState());
        standardKineticRotationTransform(shaft, rtg, light).renderInto(ms, buffers.getBuffer(RenderType.solid()));
    }
}
