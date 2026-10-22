package org.papiricoh.create_nuclearindustry.processing;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.create_nuclearindustry.processing.client.ProcessingPartials.MachineParts;

/**
 * The moving parts of a {@link KineticProcessingBlock}: the shaft stub entering its gearbox from
 * behind, the rotor it drives about the vertical axis between the plinth and the head, and the gauge needle
 * on the front showing how far the batch is. Drawn here whatever the Flywheel backend.
 */
public class ProcessingMachineRenderer<T extends KineticProcessingBlockEntity> extends KineticBlockEntityRenderer<T> {
    /** The needle's pivot on the north-facing model, in blocks. */
    private static final float PIVOT_X = 11.5f / 16, PIVOT_Y = 13 / 16f, PIVOT_Z = 0;
    /** The needle's angle at 0 % and at 100 %, clockwise as seen from the front. */
    private static final float NEEDLE_EMPTY = (float) Math.toRadians(-60), NEEDLE_FULL = (float) Math.toRadians(60);

    private final MachineParts parts;

    public ProcessingMachineRenderer(BlockEntityRendererProvider.Context context, MachineParts parts) {
        super(context);
        this.parts = parts;
    }

    @Override
    protected void renderSafe(T machine, float partialTicks, PoseStack ms, MultiBufferSource buffers, int light, int overlay) {
        BlockState state = machine.getBlockState();
        Direction facing = state.getValue(KineticProcessingBlock.FACING);
        float yaw = -(facing.toYRot() + 180);

        Direction.Axis shaftAxis = facing.getAxis();
        float shaftAngle = getAngleForBe(machine, machine.getBlockPos(), shaftAxis);
        // Ask for the buffer right before each draw: a shared buffer may have been closed in between.
        fromNorth(CachedBuffers.partial(parts.shaftStub(), state).center()
                .rotate(shaftAngle, Direction.fromAxisAndDirection(shaftAxis, Direction.AxisDirection.POSITIVE)), yaw)
                .uncenter().light(light).renderInto(ms, buffers.getBuffer(RenderType.solid()));

        float rotorAngle = getAngleForBe(machine, machine.getBlockPos(), Direction.Axis.Y) * parts.rotorRatio();
        fromNorth(CachedBuffers.partial(parts.rotor(), state).center().rotate(rotorAngle, Direction.UP), yaw)
                .uncenter().light(light).renderInto(ms, buffers.getBuffer(RenderType.solid()));

        float reading = machine.gauge(partialTicks);
        float needleAngle = NEEDLE_EMPTY + (NEEDLE_FULL - NEEDLE_EMPTY) * reading;
        fromNorth(CachedBuffers.partial(parts.needle(), state).center(), yaw).uncenter()
                .translate(PIVOT_X, PIVOT_Y, PIVOT_Z).rotate(needleAngle, Direction.SOUTH).translate(-PIVOT_X, -PIVOT_Y, -PIVOT_Z)
                .light(light).renderInto(ms, buffers.getBuffer(RenderType.solid()));
    }

    /** Partials are built facing north; turn one to the machine's facing (centred buffer). */
    private static SuperByteBuffer fromNorth(SuperByteBuffer buffer, float yaw) {
        return buffer.rotateYDegrees(yaw);
    }
}
