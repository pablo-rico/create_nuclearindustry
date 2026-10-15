package org.papiricoh.create_nuclearindustry.antimatter.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.antimatter.conduit.AntimatterConduitBlock;
import org.papiricoh.create_nuclearindustry.antimatter.conduit.AntimatterConduitBlockEntity;
import org.papiricoh.create_nuclearindustry.fusion.heating.client.PlasmaRenderTypes;

import java.util.ArrayList;
import java.util.List;

/**
 * The glow of antimatter moving through a conduit: while its network moves some, green pulses run
 * down the channel of every arm, and the channel itself glows faintly. Every block runs its pulses
 * in step, so on a straight run they pass from block to block without a jump. Each pulse is a solid
 * full-bright core (seen in daylight) in additive light (the plasma's render type).
 */
public class AntimatterConduitRenderer extends SafeBlockEntityRenderer<AntimatterConduitBlockEntity> {
    private static final float PX = 1 / 16f;
    /** The vial colour, #6BFF8A. */
    private static final float R = 0x6B / 255f, G = 1.0f, B = 0x8A / 255f;
    /** Blocks a pulse travels per tick, and pulses per block. */
    private static final float SPEED = 0.06f;
    private static final int PULSES = 3;
    private static final ResourceLocation LIT = ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID,
            "block/penning_trap_led_lit");
    /** The opaque node in the middle, where the arms start. */
    private static final float NODE_LOW = 5.5f * PX, NODE_HIGH = 10.5f * PX;

    public AntimatterConduitRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    protected void renderSafe(AntimatterConduitBlockEntity conduit, float partialTicks, PoseStack ms, MultiBufferSource buffers,
                              int light, int overlay) {
        if (conduit.getLevel() == null || !conduit.flowing()) {
            return;
        }
        BlockState state = conduit.getBlockState();
        if (!(state.getBlock() instanceof AntimatterConduitBlock)) {
            return;
        }
        float time = AnimationTickHolder.getRenderTime(conduit.getLevel());
        // The pulses keep the same phase in every block, so they run on across block faces.
        List<float[]> pulses = new ArrayList<>();
        for (Direction side : Direction.values()) {
            if (!state.getValue(AntimatterConduitBlock.SIDES.get(side))) {
                continue;
            }
            int axis = side.getAxis().ordinal();
            boolean positive = side.getAxisDirection() == Direction.AxisDirection.POSITIVE;
            for (int k = 0; k < PULSES; k++) {
                float u = frac(time * SPEED + k / (float) PULSES);
                if (positive ? u < NODE_HIGH : u > NODE_LOW) {
                    continue;
                }
                // Dimmer where it slips under the node, so it seems to pass through it.
                float fromNode = positive ? u - NODE_HIGH : NODE_LOW - u;
                pulses.add(new float[]{axis, u, Math.min(1, 0.35f + fromNode * 10)});
            }
        }
        // A solid bright core first, which reads in daylight...
        TextureAtlasSprite lit = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(LIT);
        VertexConsumer cutout = buffers.getBuffer(RenderType.cutout());
        PoseStack.Pose pose = ms.last();
        for (float[] p : pulses) {
            float[] lo = span((int) p[0], p[1], 1.2f * PX), hi = spanHigh((int) p[0], p[1], 1.2f * PX);
            LitBoxes.box(cutout, pose, lit, 0x6BFF8A, lo[0], lo[1], lo[2], hi[0], hi[1], hi[2]);
        }
        // ...then the glow round it and down the channel.
        VertexConsumer glow = buffers.getBuffer(PlasmaRenderTypes.PLASMA);
        for (Direction side : Direction.values()) {
            if (!state.getValue(AntimatterConduitBlock.SIDES.get(side))) {
                continue;
            }
            float[] lo = new float[]{0.5f - 0.6f * PX, 0.5f - 0.6f * PX, 0.5f - 0.6f * PX};
            float[] hi = new float[]{0.5f + 0.6f * PX, 0.5f + 0.6f * PX, 0.5f + 0.6f * PX};
            int axis = side.getAxis().ordinal();
            if (side.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
                lo[axis] = NODE_HIGH;
                hi[axis] = 1;
            } else {
                lo[axis] = 0;
                hi[axis] = NODE_LOW;
            }
            box(glow, pose, lo, hi, 0.45f);
        }
        for (float[] p : pulses) {
            pulse(glow, pose, (int) p[0], p[1], 1.1f * PX, 0.8f * p[2]);
            pulse(glow, pose, (int) p[0], p[1], 1.9f * PX, 0.35f * p[2]);
        }
    }

    /** The low corner of a pulse core of half-size {@code half} at {@code u} along {@code axis}. */
    private static float[] span(int axis, float u, float half) {
        float[] lo = {0.5f - half, 0.5f - half, 0.5f - half};
        lo[axis] = u - half * 2.2f;
        return lo;
    }

    private static float[] spanHigh(int axis, float u, float half) {
        float[] hi = {0.5f + half, 0.5f + half, 0.5f + half};
        hi[axis] = u + half * 2.2f;
        return hi;
    }

    private static void pulse(VertexConsumer buffer, PoseStack.Pose pose, int axis, float u, float half, float alpha) {
        float[] lo = {0.5f - half, 0.5f - half, 0.5f - half};
        float[] hi = {0.5f + half, 0.5f + half, 0.5f + half};
        lo[axis] = u - half * 1.6f;
        hi[axis] = u + half * 1.6f;
        box(buffer, pose, lo, hi, alpha);
    }

    private static float frac(float v) {
        return v - (float) Math.floor(v);
    }

    private static void box(VertexConsumer b, PoseStack.Pose pose, float[] lo, float[] hi, float alpha) {
        float x0 = lo[0], y0 = lo[1], z0 = lo[2], x1 = hi[0], y1 = hi[1], z1 = hi[2];
        quad(b, pose, alpha, x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0);
        quad(b, pose, alpha, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
        quad(b, pose, alpha, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
        quad(b, pose, alpha, x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0);
        quad(b, pose, alpha, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
        quad(b, pose, alpha, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1);
    }

    private static void quad(VertexConsumer b, PoseStack.Pose pose, float alpha, float... v) {
        for (int c = 0; c < 4; c++) {
            b.addVertex(pose, v[c * 3], v[c * 3 + 1], v[c * 3 + 2]).setColor(R, G, B, alpha);
        }
    }
}
