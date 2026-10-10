package org.papiricoh.create_nuclearindustry.controlroom.desk;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Where the sloping top of a desk and its slots are, shared by the renderer and click handling. Native
 * model space is the block model's: the operator stands on the north side (z = 0), the top rises
 * towards the south at {@link #SLOPE_DEGREES} from (z 1, y 9) px, and the operator's left is +x.
 * The blockstate turns the model by {@link #modelYaw}.
 */
public final class DeskGeometry {
    public static final float SLOPE_DEGREES = 22.5f;
    /** Slot centres in native px: front-left, front-right, back-left, back-right as the operator sees them. */
    private static final float[][] SLOTS = {
            {12, 10.5307f, 4.6955f},
            {4, 10.5307f, 4.6955f},
            {12, 13.2095f, 11.1627f},
            {4, 13.2095f, 11.1627f},
    };
    /** Clicks below this height land on the cabinet rather than on the top. */
    public static final double TOP_MIN_Y = 9 / 16.0;

    private DeskGeometry() {}

    /** The blockstate's y rotation of the model, clockwise from above. */
    public static int modelYaw(BlockState state) {
        return ((int) state.getValue(BlockStateProperties.HORIZONTAL_FACING).toYRot() + 180) % 360;
    }

    /** The y rotation of the body model, which corners turn a further quarter for their right-hand forms. */
    public static int bodyYaw(BlockState state) {
        return (modelYaw(state) + state.getValue(ControlDeskBlock.SHAPE).bodyExtraRotation()) % 360;
    }

    /** From block space to native model space of the body. */
    public static void toModel(PoseStack ms, BlockState state) {
        turn(ms, bodyYaw(state));
    }

    /**
     * From native model space to a slot: origin on the surface at the slot centre, y along the
     * surface normal, still in blocks. Module partials are modelled around (8, 0, 8) px, so callers
     * shift by (-0.5, 0, -0.5) before drawing them. Corner slots are straight-desk slots turned onto
     * their arm.
     */
    public static void toSlot(PoseStack ms, DeskShape shape, int slot) {
        turn(ms, shape.slotYaw(slot));
        float[] centre = SLOTS[slot];
        ms.translate(centre[0] / 16f, centre[1] / 16f, centre[2] / 16f);
        ms.mulPose(Axis.XP.rotationDegrees(-SLOPE_DEGREES));
    }

    /** A clockwise turn seen from above about the block's centre, as a blockstate y rotation. */
    private static void turn(PoseStack ms, int degrees) {
        if (degrees != 0) {
            ms.translate(0.5, 0, 0.5);
            ms.mulPose(Axis.YP.rotationDegrees(-degrees));
            ms.translate(-0.5, 0, -0.5);
        }
    }
}
