package org.papiricoh.create_nuclearindustry.missile.silo.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.create_nuclearindustry.missile.silo.LaunchBaseBlock;
import org.papiricoh.create_nuclearindustry.missile.silo.SiloControllerBlock;
import org.papiricoh.create_nuclearindustry.missile.silo.SiloControllerEntity;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Draws a launch controller's moving parts: the half shaft coming up into its plinth, the generator's flywheel spinning with it,
 * its lit status lamp, and the hold-down clamps of its formed base, closed on a rocket standing ready and swinging back when it
 * lifts off (or with none there). How open the clamps are is kept here per controller and eased by the game time that passed.
 */
public class SiloControllerRenderer extends KineticBlockEntityRenderer<SiloControllerEntity> {
    /** Ticks a clamp takes to swing fully open or shut. */
    private static final float SWING_TICKS = 12;
    private static final Map<SiloControllerEntity, Clamps> CLAMPS = new WeakHashMap<>();

    private static final class Clamps {
        float open;
        double time = Double.NaN;
    }

    public SiloControllerRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(SiloControllerEntity controller, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Level level = controller.getLevel();
        if (level == null) return;
        BlockState state = controller.getBlockState();
        standardKineticRotationTransform(CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, state, Direction.DOWN), controller, light)
                .renderInto(pose, buffers.getBuffer(RenderType.solid()));
        standardKineticRotationTransform(CachedBuffers.partial(SiloPartials.FLYWHEEL, state), controller, light)
                .renderInto(pose, buffers.getBuffer(RenderType.solid()));

        float turn = -yRotation(state.getValue(SiloControllerBlock.FACING));
        PartialModel lamp = lamp(controller, level.getGameTime());
        if (lamp != null) CachedBuffers.partial(lamp, state).center().rotateYDegrees(turn).uncenter()
                .light(LightTexture.FULL_BRIGHT).renderInto(pose, buffers.getBuffer(RenderType.solid()));

        float open = clampOpening(controller, level.getGameTime() + partial);
        BlockPos centre = controller.floor(), at = controller.getBlockPos();
        float[] hinge = SiloPartials.CLAMP_HINGE;
        for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockPos edge = centre.relative(side);
            BlockState pad = level.getBlockState(edge);
            if (!pad.hasProperty(LaunchBaseBlock.PART) || pad.getValue(LaunchBaseBlock.PART) == LaunchBaseBlock.Part.NONE) continue;
            // The clamp is modelled on the north edge, closing southwards: turn it to face the centre from this side.
            CachedBuffers.partial(SiloPartials.CLAMP, pad)
                    .translate(edge.getX() - at.getX(), edge.getY() - at.getY(), edge.getZ() - at.getZ())
                    .center().rotateYDegrees(-yRotation(side.getOpposite()) + 180).uncenter()
                    .translate(hinge[0], hinge[1], hinge[2]).rotateXDegrees(-open * SiloPartials.CLAMP_OPEN_ANGLE).translate(-hinge[0], -hinge[1], -hinge[2])
                    .light(LevelRenderer.getLightColor(level, edge.above()))
                    .renderInto(pose, buffers.getBuffer(RenderType.solid()));
        }
    }

    /** A blockstate's y rotation (clockwise from above) for a model made facing north and turned to {@code facing}. */
    private static float yRotation(Direction facing) {
        return switch (facing) {
            case EAST -> 90;
            case SOUTH -> 180;
            case WEST -> 270;
            default -> 0;
        };
    }

    /** The lit lamp: green ready, amber armed and blinking through the countdown, red when it cannot launch; none once launched. */
    private static PartialModel lamp(SiloControllerEntity controller, long time) {
        return switch (controller.state()) {
            case READY -> SiloPartials.LAMP_GREEN;
            case ARMED -> SiloPartials.LAMP_AMBER;
            case COUNTDOWN -> time / 5 % 2 == 0 ? SiloPartials.LAMP_AMBER : null;
            case INCOMPLETE, UNPREPARED -> SiloPartials.LAMP_RED;
            case LAUNCHED -> null;
        };
    }

    /** How open the clamps are, 0 shut on a standing rocket to 1 swung back, eased to {@code time} (game ticks). */
    private static float clampOpening(SiloControllerEntity controller, double time) {
        float target = controller.rocketHeight() > 0 ? 0 : 1;
        Clamps clamps = CLAMPS.computeIfAbsent(controller, c -> {
            Clamps fresh = new Clamps();
            fresh.open = target;
            return fresh;
        });
        double dt = Double.isNaN(clamps.time) ? 0 : Mth.clamp(time - clamps.time, 0, 20);
        clamps.time = time;
        clamps.open = Mth.approach(clamps.open, target, (float) (dt / SWING_TICKS));
        return clamps.open;
    }
}
