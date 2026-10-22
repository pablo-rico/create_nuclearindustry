package org.papiricoh.create_nuclearindustry.radiation.decontamination.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.create_nuclearindustry.radiation.decontamination.DecontaminationSprinklerBlockEntity;

/** The half shaft entering the sprinkler from below and the spray head turning with it. Drawn here whatever the Flywheel backend. */
public class SprinklerRenderer extends KineticBlockEntityRenderer<DecontaminationSprinklerBlockEntity> {

    public SprinklerRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(DecontaminationSprinklerBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffers,
                              int light, int overlay) {
        BlockState state = be.getBlockState();
        standardKineticRotationTransform(CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, state, Direction.DOWN), be, light)
                .renderInto(ms, buffers.getBuffer(RenderType.solid()));
        standardKineticRotationTransform(CachedBuffers.partial(DecontaminationClient.SPRINKLER_HEAD, state), be, light)
                .renderInto(ms, buffers.getBuffer(RenderType.solid()));
    }
}
