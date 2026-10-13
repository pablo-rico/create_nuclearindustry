package org.papiricoh.create_nuclearindustry.reactor.pwr.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.create_nuclearindustry.reactor.pwr.containment.door.ContainmentDoorBlock;
import org.papiricoh.create_nuclearindustry.reactor.pwr.containment.door.ContainmentDoorBlockEntity;
import org.papiricoh.create_nuclearindustry.reactor.pwr.containment.door.ContainmentDoorMotion;

/**
 * Draws a moving containment door from its own shut block model, as Create draws its sliding doors:
 * pulled out of the frame towards the side it was placed from, then slid sideways
 * along the wall.
 */
public class ContainmentDoorRenderer extends SafeBlockEntityRenderer<ContainmentDoorBlockEntity> {

    public ContainmentDoorRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    protected void renderSafe(ContainmentDoorBlockEntity door, float partialTicks, PoseStack ms, MultiBufferSource buffer,
                              int light, int overlay) {
        BlockState state = door.getBlockState();
        if (!(state.getBlock() instanceof ContainmentDoorBlock) || !door.shouldRenderAnimated()) {
            return;
        }
        Direction facing = state.getValue(DoorBlock.FACING);
        Direction slide = ContainmentDoorBlock.slideDirection(facing, state.getValue(DoorBlock.HINGE));
        float progress = door.progress(partialTicks);
        Vec3 offset = Vec3.atLowerCornerOf(facing.getOpposite().getNormal()).scale(ContainmentDoorMotion.pull(progress))
                .add(Vec3.atLowerCornerOf(slide.getNormal()).scale(ContainmentDoorMotion.slide(progress)));
        // A hair smaller than its two blocks, shrunk towards the seam between the halves so that no
        // gap opens there: whatever slid into the wall beside it stays behind the wall's faces.
        float inset = ContainmentDoorMotion.INSET;
        BlockState shut = state.setValue(DoorBlock.OPEN, false).setValue(ContainmentDoorBlock.VISIBLE, true);
        for (DoubleBlockHalf half : DoubleBlockHalf.values()) {
            CachedBuffers.block(shut.setValue(DoorBlock.HALF, half))
                    .translate(offset)
                    .translate(0.5, 1, 0.5)
                    .scale(1 - 2 * inset, 1 - inset, 1 - 2 * inset)
                    .translate(-0.5, -1, -0.5)
                    .translate(0, half == DoubleBlockHalf.UPPER ? 1 : 0, 0)
                    .light(light)
                    .renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));
        }
    }
}
