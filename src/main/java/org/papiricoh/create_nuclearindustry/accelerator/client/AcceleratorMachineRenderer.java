package org.papiricoh.create_nuclearindustry.accelerator.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import org.papiricoh.create_nuclearindustry.accelerator.blockentity.AcceleratorMachineBlockEntity;

/** The turning half shaft in an accelerator machine's shaft face, drawn whatever the Flywheel backend. */
public class AcceleratorMachineRenderer<T extends AcceleratorMachineBlockEntity> extends KineticBlockEntityRenderer<T> {
    public AcceleratorMachineRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(T machine, float partialTicks, PoseStack ms, MultiBufferSource buffers, int light, int overlay) {
        var shaft = CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, machine.getBlockState());
        standardKineticRotationTransform(shaft, machine, light).renderInto(ms, buffers.getBuffer(RenderType.solid()));
    }
}
