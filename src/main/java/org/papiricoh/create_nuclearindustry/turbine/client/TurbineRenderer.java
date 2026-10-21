package org.papiricoh.create_nuclearindustry.turbine.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.createmod.catnip.platform.NeoForgeCatnipServices;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.neoforge.fluids.FluidStack;
import org.papiricoh.create_nuclearindustry.turbine.AbstractTurbineControllerBlockEntity;
import org.papiricoh.create_nuclearindustry.turbine.TurbineDisplay;
import org.papiricoh.create_nuclearindustry.turbine.TurbineStructure;

/**
 * Draws a turbine from its controller: the half shaft turning in the controller's outer face,
 * as on the tokamak's heaters. Once formed it adds the 3x3 intake bezel with its fan on the
 * shaft, one long rotor whose blades reach almost to the thin walls, all spinning with the Create
 * network, and the working fluid filling the casing from the top as the tank fills.
 */
public class TurbineRenderer<T extends AbstractTurbineControllerBlockEntity> extends KineticBlockEntityRenderer<T> {
    private static final float BLADE_CLEARANCE = 0.06f;
    /** Formed walls are 2-3 px plates; keep the blades and the fluid just inside them. */
    private static final float WALL_INSET = 3 / 16f;

    public TurbineRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRenderOffScreen(T turbine) {
        return turbine.getDisplay().formed();
    }

    @Override
    protected void renderSafe(T turbine, float partialTicks, PoseStack ms, MultiBufferSource buffers, int light, int overlay) {
        BlockState state = turbine.getBlockState();
        // Drawn here whatever the Flywheel backend, like the heaters' shafts.
        SuperByteBuffer shaft = CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, state);
        standardKineticRotationTransform(shaft, turbine, light).renderInto(ms, buffers.getBuffer(RenderType.solid()));

        TurbineDisplay display = turbine.getDisplay();
        if (!display.formed() || turbine.getLevel() == null) {
            return;
        }
        Direction outward = state.getValue(BlockStateProperties.FACING);
        TurbineStructure shape = new TurbineStructure(turbine.tier(), turbine.getBlockPos(), outward,
                display.hu(), display.hv(), display.length());
        // The casing is sealed, so light the inside from the shaft side.
        int innerLight = LevelRenderer.getLightColor(turbine.getLevel(), turbine.getBlockPos().relative(outward));

        renderFront(turbine, shape, state, ms, buffers, light);
        renderRotor(turbine, shape, state, ms, buffers.getBuffer(RenderType.solid()), innerLight);
        renderSteam(turbine, shape, partialTicks, ms, buffers, innerLight);
    }

    private void renderFront(T turbine, TurbineStructure shape, BlockState state, PoseStack ms, MultiBufferSource buffers, int light) {
        SuperByteBuffer bezel = CachedBuffers.partial(NuclearPartialModels.bezel(turbine.tier()), state);
        fromNorth(bezel.center(), shape.outward).uncenter().light(light).renderInto(ms, buffers.getBuffer(RenderType.cutout()));

        float angle = getAngleForBe(turbine, turbine.getBlockPos(), shape.axis);
        Direction spin = Direction.fromAxisAndDirection(shape.axis, Direction.AxisDirection.POSITIVE);
        SuperByteBuffer fan = CachedBuffers.partial(NuclearPartialModels.fan(turbine.tier()), state);
        fromNorth(fan.center().rotate(angle, spin), shape.outward).uncenter().light(light)
                .renderInto(ms, buffers.getBuffer(RenderType.cutout()));
    }

    /** Front partials are built facing north; turn them to the controller's facing (centred buffer). */
    private static SuperByteBuffer fromNorth(SuperByteBuffer buffer, Direction outward) {
        return switch (outward) {
            case NORTH -> buffer;
            case SOUTH -> buffer.rotateYDegrees(180);
            case EAST -> buffer.rotateYDegrees(-90);
            case WEST -> buffer.rotateYDegrees(90);
            case UP -> buffer.rotateXDegrees(90);
            case DOWN -> buffer.rotateXDegrees(-90);
        };
    }

    private void renderRotor(T turbine, TurbineStructure shape, BlockState state, PoseStack ms, VertexConsumer buffer, int light) {
        Direction.Axis axis = shape.axis;
        float angle = getAngleForBe(turbine, turbine.getBlockPos(), axis);
        Direction spin = Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE);
        // The walls are thin plates on the far side of the wall blocks, so the blades reach into them.
        float bladeScale = (Math.min(shape.hu, shape.hv) + 0.5f - WALL_INSET - BLADE_CLEARANCE) / 0.5f;
        Direction inward = shape.inward;
        for (int d = 1; d < shape.length - 1; d++) {
            float dx = inward.getStepX() * d;
            float dy = inward.getStepY() * d;
            float dz = inward.getStepZ() * d;
            SuperByteBuffer hub = CachedBuffers.partial(NuclearPartialModels.hub(turbine.tier()), state);
            align(hub.translate(dx, dy, dz).center().rotate(angle, spin), axis).uncenter().light(light).renderInto(ms, buffer);
            for (int k = 0; k < 4; k++) {
                SuperByteBuffer blade = CachedBuffers.partial(NuclearPartialModels.blade(turbine.tier()), state);
                align(blade.translate(dx, dy, dz).center().rotate(angle, spin), axis)
                        .rotateYDegrees(k * 90 + (d % 2) * 45)
                        .scale(bladeScale, 1, 1)
                        .uncenter()
                        .light(light)
                        .renderInto(ms, buffer);
            }
        }
    }

    /** Partial models are built along +Y; turn them onto the turbine axis. */
    private static SuperByteBuffer align(SuperByteBuffer buffer, Direction.Axis axis) {
        return switch (axis) {
            case X -> buffer.rotateZDegrees(-90);
            case Z -> buffer.rotateXDegrees(90);
            case Y -> buffer;
        };
    }

    private void renderSteam(T turbine, TurbineStructure shape, float partialTicks, PoseStack ms, MultiBufferSource buffers, int light) {
        FluidStack fluid = turbine.getSteamTank().getFluid();
        float level = turbine.steamLevel.getValue(partialTicks);
        if (fluid.isEmpty() || level <= 0.001f) {
            return;
        }
        BlockPos origin = turbine.getBlockPos();
        // Formed walls are thin, so the gas fills the wall blocks too, up to the plates.
        BoundingBox interior = BoundingBox.fromCorners(shape.at(1, -shape.hu, -shape.hv),
                shape.at(shape.length - 1, shape.hu, shape.hv));
        float x0 = interior.minX() - origin.getX() + WALL_INSET;
        float y0 = interior.minY() - origin.getY() + WALL_INSET;
        float z0 = interior.minZ() - origin.getZ() + WALL_INSET;
        float x1 = interior.maxX() + 1 - origin.getX() - WALL_INSET;
        float y1 = interior.maxY() + 1 - origin.getY() - WALL_INSET;
        float z1 = interior.maxZ() + 1 - origin.getZ() - WALL_INSET;
        // A gas gathers under the roof first.
        float height = (y1 - y0) * Math.min(1, level);
        NeoForgeCatnipServices.FLUID_RENDERER.renderFluidBox(fluid, x0, y1 - height, z0, x1, y1, z1, buffers, ms, light, true, true);
    }
}
