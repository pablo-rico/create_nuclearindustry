package org.papiricoh.create_nuclearindustry.enrichment.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.create_nuclearindustry.enrichment.block.CentrifugeBlock;
import org.papiricoh.create_nuclearindustry.enrichment.blockentity.CentrifugeBlockEntity;

public class CentrifugeRenderer extends KineticBlockEntityRenderer<CentrifugeBlockEntity> {
    public CentrifugeRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(CentrifugeBlockEntity centrifuge, float partialTicks, PoseStack poseStack,
                              MultiBufferSource bufferSource, int light, int overlay) {
        BlockState state = centrifuge.getBlockState();
        Direction shaftDirection = state.getValue(CentrifugeBlock.FACING).getOpposite();
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.solid());
        SuperByteBuffer shaft = CachedBuffers.partialFacingVertical(AllPartialModels.SHAFT_HALF, state, shaftDirection);

        renderRotatingBuffer(centrifuge, shaft, poseStack, buffer, light);
    }
}
