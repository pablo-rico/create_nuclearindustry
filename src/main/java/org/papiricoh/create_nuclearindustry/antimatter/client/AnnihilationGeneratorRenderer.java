package org.papiricoh.create_nuclearindustry.antimatter.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.createmod.catnip.render.SuperRenderTypeBuffer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.create_nuclearindustry.accelerator.physics.AnnihilationModel;
import org.papiricoh.create_nuclearindustry.antimatter.generator.AnnihilationGeneratorBlockEntity;

/**
 * What the annihilation generator's model cannot show: the half shaft turning in its front collar,
 * the lamp of the mode it runs in, and the core behind the porthole on its back, dim green on the
 * standby burn and white-green flat out, flickering when its supply cannot keep up. A soft glow of
 * the cloud layers hangs in front of the core while it burns. Model north is the shaft side.
 */
public class AnnihilationGeneratorRenderer extends KineticBlockEntityRenderer<AnnihilationGeneratorBlockEntity> {
    private static final float PX = 1 / 16f;
    /** Mode lamp colours, by {@link AnnihilationModel.Mode} ordinal. */
    private static final int[] MODE_COLOURS = {0x7EC8FF, 0xFFD84A, 0x6BFF8A};
    private static final int ALARM_RED = 0xFF3030;
    private static final int CORE_DIM = 0x2E7A44, CORE_HOT = 0xD8FFE0, GLOW = 0x6BFF8A;

    public AnnihilationGeneratorRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(AnnihilationGeneratorBlockEntity generator, float partialTicks, PoseStack ms, MultiBufferSource buffers,
                              int light, int overlay) {
        Level level = generator.getLevel();
        BlockState state = generator.getBlockState();
        if (level == null || !state.hasProperty(HorizontalKineticBlock.HORIZONTAL_FACING)) {
            return;
        }
        Direction facing = state.getValue(HorizontalKineticBlock.HORIZONTAL_FACING);
        // The shaft, drawn here whatever the Flywheel backend (no visual is registered for it).
        SuperByteBuffer shaft = CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, state, facing);
        standardKineticRotationTransform(shaft, generator, light).renderInto(ms, buffers.getBuffer(RenderType.solid()));

        float time = AnimationTickHolder.getRenderTime(level);
        float angle = -((facing.toYRot() + 180) % 360);
        boolean blink = ((int) (time / 4f)) % 2 == 0;
        GeneratorClientEffects.Look look = GeneratorClientEffects.look(generator, partialTicks, time);

        // Lamps: all three blinking red when the cold chamber holds antimatter without its field.
        if (generator.fieldFailing()) {
            if (((int) (time / 2.5f)) % 2 == 0) {
                for (PartialModel led : AntimatterPartials.GENERATOR_MODE_LED) {
                    lit(led, state, ms, buffers, angle, ALARM_RED);
                }
            }
        } else if (generator.burning() || look.heat() > 0.02f) {
            int mode = generator.effectiveMode().ordinal();
            // On the standby burn the lamp of the mode blinks: fuelled, but nothing asked of it.
            if (!generator.standby() || blink) {
                lit(AntimatterPartials.GENERATOR_MODE_LED[mode], state, ms, buffers, angle, MODE_COLOURS[mode]);
            }
        }

        if (look.heat() <= 0.01f) {
            return;
        }
        int core = CloudLayers.scale(CloudLayers.lerp(CORE_DIM, CORE_HOT, look.output()), Math.min(1, look.heat() * look.flicker()));
        lit(AntimatterPartials.GENERATOR_CORE, state, ms, buffers, angle, core);
        if (buffers instanceof SuperRenderTypeBuffer) {
            // In a Ponder scene the cloud's render types do not show; the lit core says it all.
            return;
        }
        glow(ms, buffers, angle, look, time);
    }

    /** A lit partial tinted {@code colour}, turned like the blockstate turns the model. */
    private static void lit(PartialModel partial, BlockState state, PoseStack ms, MultiBufferSource buffers, float angle, int colour) {
        SuperByteBuffer sbb = CachedBuffers.partial(partial, state);
        sbb.center().rotateYDegrees(angle).uncenter()
                .color(0xFF000000 | colour)
                .light(LightTexture.FULL_BRIGHT)
                .disableDiffuse()
                .renderInto(ms, buffers.getBuffer(RenderType.cutout()));
    }

    /** Additive light in front of the core, inside the porthole's glass: a halo, a turning wisp and a white heart. */
    private static void glow(PoseStack ms, MultiBufferSource buffers, float angle, GeneratorClientEffects.Look look, float time) {
        ms.pushPose();
        ms.translate(0.5, 0.5, 0.5);
        ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(angle));
        ms.translate(-0.5, -0.5, -0.5);
        ms.translate(8 * PX, 8.5f * PX, 14.0f * PX);
        CloudLayers.View view = CloudLayers.View.of(ms);
        TextureAtlasSprite cloud = CloudLayers.sprite(CloudLayers.CLOUD);
        TextureAtlasSprite wisp = CloudLayers.sprite(CloudLayers.WISP);
        float strength = Math.min(1, look.heat() * look.flicker());
        float out = look.output();
        float radius = (1.6f + 1.4f * out) * PX;
        float beat = 1 + 0.06f * (float) Math.sin(time * (0.2 + 0.4 * out));
        float r = CloudLayers.red(GLOW), g = CloudLayers.green(GLOW), b = CloudLayers.blue(GLOW);
        VertexConsumer light = buffers.getBuffer(AntimatterRenderTypes.CLOUD_GLOW);
        // Green light, white only at the heart: stacked additive layers saturate to white quickly.
        CloudLayers.layer(light, ms, view, cloud, radius * 1.5f * beat, 0, r, g, b, (0.2f + 0.25f * out) * strength);
        CloudLayers.layer(light, ms, view, wisp, radius * beat, time * (0.03f + 0.1f * out), r, g, b, (0.15f + 0.25f * out) * strength);
        CloudLayers.layer(light, ms, view, cloud, radius * 0.35f, 0, CloudLayers.mix(r, 0.6f), CloudLayers.mix(g, 0.6f),
                CloudLayers.mix(b, 0.6f), (0.25f + 0.35f * out) * strength);
        ms.popPose();
    }
}
