package org.papiricoh.create_nuclearindustry.civildefense.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.papiricoh.create_nuclearindustry.civildefense.blastdoor.BlastDoorBlock;
import org.papiricoh.create_nuclearindustry.civildefense.blastdoor.BlastDoorControllerBlockEntity;
import org.papiricoh.create_nuclearindustry.civildefense.blastdoor.BlastDoorLayout;
import org.papiricoh.create_nuclearindustry.civildefense.blastdoor.LeafState;

/**
 * Draws a blast door's panel sliding into the wall: every leaf's shut model, offset along the slide,
 * a little thinner than a block so the part inside the frame stays hidden. Only while the leaves
 * themselves are hidden, that is from the moment the door starts to open until it is shut again.
 */
public class BlastDoorRenderer extends SafeBlockEntityRenderer<BlastDoorControllerBlockEntity> {

    public BlastDoorRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    protected void renderSafe(BlastDoorControllerBlockEntity controller, float partialTicks, PoseStack ms,
                              MultiBufferSource buffers, int light, int overlay) {
        Level level = controller.getLevel();
        BlastDoorLayout layout = controller.getLayout();
        if (level == null || layout == null) {
            return;
        }
        BlockState farLeaf = level.getBlockState(layout.far());
        if (!(farLeaf.getBlock() instanceof BlastDoorBlock) || farLeaf.getValue(BlastDoorBlock.LEAF) == LeafState.CLOSED) {
            return;
        }
        double offset = controller.getRenderedOffset(partialTicks);
        BlockPos origin = controller.getBlockPos();
        Direction slide = layout.slide();
        Direction.Axis shaft = controller.shaftAxis();
        float thickness = BlastDoorControllerBlockEntity.PANEL_THICKNESS;
        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        for (int column = 0; column < layout.width(); column++) {
            for (int row = 0; row < layout.height(); row++) {
                BlockPos cell = layout.cell(column, row);
                BlockState leaf = level.getBlockState(cell);
                if (!(leaf.getBlock() instanceof BlastDoorBlock)) {
                    continue;
                }
                ms.pushPose();
                ms.translate(cell.getX() - origin.getX() + slide.getStepX() * offset, cell.getY() - origin.getY(),
                        cell.getZ() - origin.getZ() + slide.getStepZ() * offset);
                ms.translate(0.5, 0.5, 0.5);
                ms.scale(shaft == Direction.Axis.X ? thickness : 1, 1, shaft == Direction.Axis.Z ? thickness : 1);
                ms.translate(-0.5, -0.5, -0.5);
                dispatcher.renderSingleBlock(leaf.setValue(BlastDoorBlock.LEAF, LeafState.CLOSED), ms, buffers,
                        LevelRenderer.getLightColor(level, cell), OverlayTexture.NO_OVERLAY, ModelData.EMPTY, RenderType.solid());
                ms.popPose();
            }
        }
    }

    @Override
    public boolean shouldRenderOffScreen(BlastDoorControllerBlockEntity controller) {
        return true;
    }
}
