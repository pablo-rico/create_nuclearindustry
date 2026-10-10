package org.papiricoh.create_nuclearindustry.fusion.heating.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import org.papiricoh.create_nuclearindustry.fusion.heating.PlasmaHeaterBlockEntity;

/** The turning half shaft in a heater's outer face. Drawn here whatever the Flywheel backend, like the RTG's. */
public class PlasmaHeaterRenderer extends KineticBlockEntityRenderer<PlasmaHeaterBlockEntity> {
    public PlasmaHeaterRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(PlasmaHeaterBlockEntity heater, float partialTicks, PoseStack ms, MultiBufferSource buffers,
                              int light, int overlay) {
        var shaft = CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, heater.getBlockState());
        standardKineticRotationTransform(shaft, heater, light).renderInto(ms, buffers.getBuffer(RenderType.solid()));
    }
}
