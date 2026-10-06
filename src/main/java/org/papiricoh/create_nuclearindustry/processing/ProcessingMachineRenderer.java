package org.papiricoh.create_nuclearindustry.processing;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/** Renders the half shaft entering a {@link KineticProcessingBlock} from behind. */
public class ProcessingMachineRenderer<T extends KineticProcessingBlockEntity> extends KineticBlockEntityRenderer<T> {

    public ProcessingMachineRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(T machine, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource,
                              int light, int overlay) {
        BlockState state = machine.getBlockState();
        Direction shaftDirection = state.getValue(KineticProcessingBlock.FACING).getOpposite();
        SuperByteBuffer shaft = CachedBuffers.partialFacingVertical(AllPartialModels.SHAFT_HALF, state, shaftDirection);
        renderRotatingBuffer(machine, shaft, poseStack, bufferSource.getBuffer(RenderType.solid()), light);
    }
}
