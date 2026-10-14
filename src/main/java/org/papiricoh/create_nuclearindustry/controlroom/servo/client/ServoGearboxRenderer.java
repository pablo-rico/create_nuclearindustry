package org.papiricoh.create_nuclearindustry.controlroom.servo.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.papiricoh.create_nuclearindustry.controlroom.servo.ServoGearboxBlock;
import org.papiricoh.create_nuclearindustry.controlroom.servo.ServoGearboxBlockEntity;

/**
 * Draws a Servo Gearbox's two half shafts, the output one turning only while the servo drives, as
 * Create draws a gearshift's. Always drawn here: the mod registers no Flywheel visual for it.
 */
public class ServoGearboxRenderer extends KineticBlockEntityRenderer<ServoGearboxBlockEntity> {

    public ServoGearboxRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(ServoGearboxBlockEntity servo, float partialTicks, PoseStack ms, MultiBufferSource buffer,
                              int light, int overlay) {
        Direction.Axis axis = servo.getBlockState().getValue(ServoGearboxBlock.AXIS);
        BlockPos pos = servo.getBlockPos();
        float time = AnimationTickHolder.getRenderTime(servo.getLevel());
        for (Direction direction : Direction.values()) {
            if (direction.getAxis() != axis) {
                continue;
            }
            float angle = time * servo.getSpeed() * 3.0f / 10.0f % 360.0f;
            angle *= servo.getRotationSpeedModifier(direction);
            angle += getRotationOffsetForPosition(servo, pos, axis);
            angle = angle / 180.0f * (float) Math.PI;
            SuperByteBuffer shaft = CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, servo.getBlockState(), direction);
            kineticRotationTransform(shaft, servo, axis, angle, light);
            shaft.renderInto(ms, buffer.getBuffer(RenderType.solid()));
        }
    }
}
