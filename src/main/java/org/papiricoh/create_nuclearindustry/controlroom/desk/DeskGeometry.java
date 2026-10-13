package org.papiricoh.create_nuclearindustry.controlroom.desk;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Where the slots of a module surface are, shared by the renderer and click handling. Native
 * model space is the block model's: the operator stands on the north side (z = 0) and the
 * operator's left is +x. A desk's top rises towards the south at {@link #SLOPE_DEGREES} from
 * (z 1, y 9) px; a table's is flat; a wall panel's face looks north from against the block to the
 * south. The blockstate turns the model by {@link #modelYaw}.
 */
public final class DeskGeometry {
    public static final float SLOPE_DEGREES = 22.5f;

    private DeskGeometry() {}

    /** The blockstate's y rotation of the model, clockwise from above. */
    public static int modelYaw(BlockState state) {
        return ((int) state.getValue(BlockStateProperties.HORIZONTAL_FACING).toYRot() + 180) % 360;
    }

    /** The y rotation of the body model, which desk corners turn a further quarter for their right-hand forms. */
    public static int bodyYaw(BlockState state) {
        int extra = state.hasProperty(ControlDeskBlock.SHAPE) ? state.getValue(ControlDeskBlock.SHAPE).bodyExtraRotation() : 0;
        return (modelYaw(state) + extra) % 360;
    }

    /**
     * From block space to a slot of any module surface: origin on the surface at the slot centre,
     * y out of the surface, x to the operator's left and z away from the operator (up, on a wall),
     * still in blocks. Module partials are modelled around (8, 0, 8) px, so callers shift by
     * (-0.5, 0, -0.5) before drawing them.
     */
    public static void toSurfaceSlot(PoseStack ms, BlockState state, int slot) {
        ModuleSurface surface = ModuleSurface.of(state);
        if (surface == null || surface == ModuleSurface.DESK) {
            turn(ms, bodyYaw(state));
            toSlot(ms, state.getValue(ControlDeskBlock.SHAPE), slot);
            return;
        }
        turn(ms, modelYaw(state));
        float[] centre = (surface == ModuleSurface.TABLE ? SurfaceGeometry.TABLE_SLOTS : SurfaceGeometry.WALL_SLOTS)[slot];
        ms.translate(centre[0] / 16f, centre[1] / 16f, centre[2] / 16f);
        if (surface == ModuleSurface.WALL) {
            // Up out of the face towards the operator, and the slot's far side up the wall.
            ms.mulPose(Axis.XP.rotationDegrees(-90));
        }
    }

    /**
     * From a desk's native model space to a slot. Corner slots are straight-desk slots turned onto
     * their arm.
     */
    private static void toSlot(PoseStack ms, DeskShape shape, int slot) {
        turn(ms, shape.slotYaw(slot));
        float[] centre = SurfaceGeometry.DESK_SLOTS[slot];
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
