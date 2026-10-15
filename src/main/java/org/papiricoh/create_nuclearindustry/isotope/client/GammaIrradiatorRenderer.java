package org.papiricoh.create_nuclearindustry.isotope.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.render.RenderTypes;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.papiricoh.create_nuclearindustry.isotope.irradiator.GammaIrradiatorBlockEntity;

/**
 * The Gamma Irradiator in use: the two shutter leaves slide apart while food is held under it, the
 * aperture glows Cherenkov blue and a faint cone of light reaches down to the belt, weaker as the
 * cobalt decays. The lamps say what the head holds: amber open, green loaded and closed, blinking
 * red when the sources are nearly spent, dark when empty.
 */
public class GammaIrradiatorRenderer extends SafeBlockEntityRenderer<GammaIrradiatorBlockEntity> {
    private static final float PX = 1 / 16f;
    private static final int CHERENKOV = 0x4F9BFF, AMBER = 0xFFB23F, GREEN = 0x6BFF8A, RED = 0xFF4A3A;
    /** The beam partial reaches 16 px down; the depot or belt surface is about 19 px below the block. */
    private static final float BEAM_STRETCH = 19f / 16f;
    private static final float LEAF_TRAVEL = 3 * PX;

    public GammaIrradiatorRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    protected void renderSafe(GammaIrradiatorBlockEntity irradiator, float partialTicks, PoseStack ms, MultiBufferSource buffers, int light,
                              int overlay) {
        Level level = irradiator.getLevel();
        if (level == null) {
            return;
        }
        BlockState state = irradiator.getBlockState();
        float open = Mth.clamp(irradiator.shutter.getValue(partialTicks), 0, 1);
        float time = AnimationTickHolder.getRenderTime(level);

        CachedBuffers.partial(IsotopePartials.SHUTTER_WEST, state).translate(-open * LEAF_TRAVEL, 0, 0).light(light)
                .renderInto(ms, buffers.getBuffer(RenderType.solid()));
        CachedBuffers.partial(IsotopePartials.SHUTTER_EAST, state).translate(open * LEAF_TRAVEL, 0, 0).light(light)
                .renderInto(ms, buffers.getBuffer(RenderType.solid()));

        boolean loaded = irradiator.sourceCount() > 0;
        double strength = irradiator.sourceStrength();
        if (open > 0.02f && loaded) {
            float glow = open * (float) Math.max(0.25, strength);
            float flicker = 0.9f + 0.1f * Mth.sin(time * 0.9f) * Mth.sin(time * 0.37f + 1.3f);
            CachedBuffers.partial(IsotopePartials.GLOW, state).color(0xFF000000 | scale(CHERENKOV, glow)).light(LightTexture.FULL_BRIGHT)
                    .disableDiffuse().renderInto(ms, buffers.getBuffer(RenderTypes.additive()));
            // Additive: the black-backed texture adds only its blue, so the cone reads as light, not glass.
            SuperByteBuffer beam = CachedBuffers.partial(IsotopePartials.BEAM, state);
            beam.scale(1, BEAM_STRETCH, 1);
            beam.color(0xFF000000 | scale(CHERENKOV, glow * flicker * 0.8f)).light(LightTexture.FULL_BRIGHT).disableDiffuse()
                    .renderInto(ms, buffers.getBuffer(RenderTypes.additive()));
        }
        if (!loaded) {
            return;
        }
        if (strength < 0.1) {
            if (((int) (time / 6f)) % 2 == 0) {
                lit(IsotopePartials.LAMP, state, ms, buffers, RED);
            }
        } else {
            lit(IsotopePartials.LAMP, state, ms, buffers, open > 0.5f ? AMBER : GREEN);
        }
    }

    private static void lit(PartialModel partial, BlockState state, PoseStack ms, MultiBufferSource buffers, int colour) {
        CachedBuffers.partial(partial, state).color(0xFF000000 | colour).light(LightTexture.FULL_BRIGHT).disableDiffuse()
                .renderInto(ms, buffers.getBuffer(RenderType.cutout()));
    }

    private static int scale(int rgb, float factor) {
        float f = Mth.clamp(factor, 0, 1);
        int r = (int) (((rgb >> 16) & 0xFF) * f), g = (int) (((rgb >> 8) & 0xFF) * f), b = (int) ((rgb & 0xFF) * f);
        return (r << 16) | (g << 8) | b;
    }

    @Override
    public boolean shouldRenderOffScreen(GammaIrradiatorBlockEntity irradiator) {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(GammaIrradiatorBlockEntity irradiator) {
        return new AABB(irradiator.getBlockPos()).expandTowards(0, -2, 0);
    }
}
