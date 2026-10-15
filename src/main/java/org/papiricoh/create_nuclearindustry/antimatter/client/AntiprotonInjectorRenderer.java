package org.papiricoh.create_nuclearindustry.antimatter.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.createmod.catnip.render.SuperRenderTypeBuffer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.accelerator.physics.InjectorModel;
import org.papiricoh.create_nuclearindustry.accelerator.physics.TrapState;
import org.papiricoh.create_nuclearindustry.antimatter.injector.AntiprotonInjectorBlockEntity;

/**
 * The antiproton injector's lights: the amber ARMED lamp (blinking while it cools down after a shot),
 * the green READY lamp, the cyan dose cloud in the little trap chamber on its outer face, and a shot:
 * the chamber flares white, the nozzle on its inner face lights up and a streak of light runs from it
 * into the plasma. Model north is the outer face; the blockstate turns it like a neutral beam
 * injector's (y for the sides, x 270 facing up, x 90 facing down).
 */
public class AntiprotonInjectorRenderer extends SafeBlockEntityRenderer<AntiprotonInjectorBlockEntity> {
    private static final float PX = 1 / 16f;
    private static final int ARMED_AMBER = 0xFFB020, READY_GREEN = 0x58E070, ALARM_RED = 0xFF3030;
    private static final int DOSE_CYAN = 0x3FD8E8, FLASH_HOT = 0xBFF6FF;
    /** How far the shot's streak reaches into the tokamak, blocks. */
    private static final float BEAM_LENGTH = 3.0f;
    private static final ResourceLocation CHAMBER = ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID,
            "block/antiproton_injector_chamber");

    public AntiprotonInjectorRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    protected void renderSafe(AntiprotonInjectorBlockEntity injector, float partialTicks, PoseStack ms, MultiBufferSource buffers, int light,
                              int overlay) {
        Level level = injector.getLevel();
        if (level == null) {
            return;
        }
        BlockState state = injector.getBlockState();
        Direction facing = injector.facing();
        float time = AnimationTickHolder.getRenderTime(level);
        float flash = InjectorClientEffects.flash(injector, partialTicks);
        boolean quenching = injector.state() == TrapState.QUENCHING;

        if (quenching) {
            if (((int) (time / 2.5f)) % 2 == 0) {
                lit(AntimatterPartials.INJECTOR_ARMED_LED, state, ms, buffers, facing, ALARM_RED);
                lit(AntimatterPartials.INJECTOR_READY_LED, state, ms, buffers, facing, ALARM_RED);
            }
        } else {
            if (injector.armed()) {
                boolean cooling = injector.verdict() == InjectorModel.Verdict.COOLDOWN;
                if (!cooling || ((int) (time / 5f)) % 2 == 0) {
                    lit(AntimatterPartials.INJECTOR_ARMED_LED, state, ms, buffers, facing, ARMED_AMBER);
                }
            }
            if (injector.armed() && injector.ready()) {
                lit(AntimatterPartials.INJECTOR_READY_LED, state, ms, buffers, facing, READY_GREEN);
            }
        }
        if (flash > 0.01f) {
            int colour = CloudLayers.scale(CloudLayers.lerp(DOSE_CYAN, FLASH_HOT, flash), Math.min(1, 0.25f + flash));
            lit(AntimatterPartials.INJECTOR_NOZZLE_FLASH, state, ms, buffers, facing, colour);
        }

        if (buffers instanceof SuperRenderTypeBuffer) {
            // A Ponder scene: the cloud's render types do not show there; the lamps and particles tell it.
            return;
        }
        ms.pushPose();
        orient(ms, facing);
        double capacity = injector.capacity();
        double fill = capacity > 0 ? Math.max(0, Math.min(1, injector.stored() / capacity)) : 0;
        if (fill > 1.0e-6 || flash > 0.01f) {
            backing(ms, buffers, light);
            dose(ms, buffers, injector, fill, flash, time, quenching);
        }
        if (flash > 0.01f) {
            beam(ms, buffers, flash);
        }
        ms.popPose();
    }

    /** The pose turned as the blockstate turns the model, so model space lines up with the block. */
    private static void orient(PoseStack ms, Direction facing) {
        ms.translate(0.5, 0.5, 0.5);
        ms.mulPose(Axis.YP.rotationDegrees(-yRotation(facing)));
        ms.mulPose(Axis.XP.rotationDegrees(-xRotation(facing)));
        ms.translate(-0.5, -0.5, -0.5);
    }

    private static float yRotation(Direction facing) {
        return facing.getAxis().isHorizontal() ? (facing.toYRot() + 180) % 360 : 0;
    }

    private static float xRotation(Direction facing) {
        return facing == Direction.UP ? 270 : facing == Direction.DOWN ? 90 : 0;
    }

    private static void lit(PartialModel partial, BlockState state, PoseStack ms, MultiBufferSource buffers, Direction facing, int colour) {
        SuperByteBuffer sbb = CachedBuffers.partial(partial, state);
        sbb.center().rotateYDegrees(-yRotation(facing)).rotateXDegrees(-xRotation(facing)).uncenter()
                .color(0xFF000000 | colour)
                .light(LightTexture.FULL_BRIGHT)
                .disableDiffuse()
                .renderInto(ms, buffers.getBuffer(RenderType.cutout()));
    }

    /**
     * The chamber's back plate again, a hair in front of the model's and written to the depth buffer
     * before the cloud. The whole model is in the translucent layer, drawn after block entities, and
     * the cloud writes no depth: without this the plate behind it would paint over the cloud. An
     * entity render type is drawn as soon as the cloud's buffer is asked for, so it lands first.
     */
    private static void backing(PoseStack ms, MultiBufferSource buffers, int light) {
        TextureAtlasSprite plate = CloudLayers.sprite(CHAMBER);
        VertexConsumer buffer = buffers.getBuffer(RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS));
        PoseStack.Pose pose = ms.last();
        float x0 = 4.5f * PX, x1 = 11.5f * PX, y0 = 7.5f * PX, y1 = 11 * PX, z = 1.88f * PX;
        // A north face: the texture's left edge on the east side, as the model maps it.
        vertex(buffer, pose, x1, y0, z, plate.getU0(), plate.getV1(), light);
        vertex(buffer, pose, x0, y0, z, plate.getU1(), plate.getV1(), light);
        vertex(buffer, pose, x0, y1, z, plate.getU1(), plate.getV0(), light);
        vertex(buffer, pose, x1, y1, z, plate.getU0(), plate.getV0(), light);
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, float z, float u, float v, int light) {
        buffer.addVertex(pose, x, y, z).setColor(255, 255, 255, 255).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                .setNormal(pose, 0, 0, -1);
    }

    /**
     * The dose waiting in the chamber behind the front glass: a small cyan cloud as big as the charge
     * (by the cube root, like a trap's), flaring white as a shot leaves.
     */
    private static void dose(PoseStack ms, MultiBufferSource buffers, AntiprotonInjectorBlockEntity injector, double fill, float flash,
                             float time, boolean quenching) {
        ms.pushPose();
        ms.translate(8 * PX, 9.25f * PX, 1.35f * PX);
        CloudLayers.View view = CloudLayers.View.of(ms);
        TextureAtlasSprite cloud = CloudLayers.sprite(CloudLayers.CLOUD);
        TextureAtlasSprite wisp = CloudLayers.sprite(CloudLayers.WISP);
        float size = fill > 1.0e-6 ? Math.max(0.25f, (float) Math.cbrt(fill)) : 0;
        float field = Math.max(0, Math.min(1, injector.fieldStrength()));
        float beat = 1 + 0.06f * (float) Math.sin(time * (0.15 + 0.25 * field));
        float radius = (1.6f * size * beat + 1.4f * flash) * PX;
        int colour = quenching ? TrapLook.QUENCH_RED : DOSE_CYAN;
        float r = CloudLayers.red(colour), g = CloudLayers.green(colour), b = CloudLayers.blue(colour);
        float glow = (0.55f + 0.45f * field) * (size > 0 ? 1 : 0) + flash;
        // A solid lit orb first, in the cutout layer: it shows whatever else is drawn around it (the
        // additive layers can be lost under a burning plasma's pass), then the soft glow over it.
        CloudLayers.orb(buffers.getBuffer(RenderType.cutout()), ms.last(), view, CloudLayers.sprite(CloudLayers.LIT),
                radius * 0.75f, colour, flash, time);
        if (size > 0) {
            VertexConsumer body = buffers.getBuffer(AntimatterRenderTypes.CLOUD_BODY);
            CloudLayers.layer(body, ms, view, cloud, radius * 1.3f, 0, r * 0.75f, g * 0.75f, b * 0.75f, 0.75f + 0.25f * field);
        }
        VertexConsumer light = buffers.getBuffer(AntimatterRenderTypes.CLOUD_GLOW);
        CloudLayers.layer(light, ms, view, cloud, radius * 1.6f, 0, r, g, b, Math.min(1, 0.7f * glow));
        CloudLayers.layer(light, ms, view, wisp, radius, time * 0.08f, CloudLayers.mix(r, 0.2f), CloudLayers.mix(g, 0.2f),
                CloudLayers.mix(b, 0.2f), Math.min(1, 0.7f * glow));
        CloudLayers.layer(light, ms, view, cloud, radius * 0.45f, 0, CloudLayers.mix(r, 0.6f + 0.4f * flash),
                CloudLayers.mix(g, 0.6f + 0.4f * flash), CloudLayers.mix(b, 0.6f + 0.4f * flash), Math.min(1, 0.8f * glow));
        ms.popPose();
    }

    /** The shot: a streak of light from the nozzle on the inner face, deep into the plasma, fading as the dose spreads. */
    private static void beam(PoseStack ms, MultiBufferSource buffers, float flash) {
        ms.pushPose();
        ms.translate(8 * PX, 8 * PX, 16 * PX);
        TextureAtlasSprite cloud = CloudLayers.sprite(CloudLayers.CLOUD);
        VertexConsumer light = buffers.getBuffer(AntimatterRenderTypes.CLOUD_GLOW);
        float length = BEAM_LENGTH * (0.4f + 0.6f * (float) Math.sqrt(1 - flash));
        float r = CloudLayers.red(FLASH_HOT), g = CloudLayers.green(FLASH_HOT), b = CloudLayers.blue(FLASH_HOT);
        CloudLayers.streak(light, ms, cloud, length, 0.22f, CloudLayers.red(DOSE_CYAN), CloudLayers.green(DOSE_CYAN),
                CloudLayers.blue(DOSE_CYAN), 0.8f * flash);
        CloudLayers.streak(light, ms, cloud, length, 0.08f, r, g, b, flash);
        CloudLayers.View view = CloudLayers.View.of(ms);
        CloudLayers.layer(light, ms, view, cloud, 0.35f + 0.2f * flash, 0, r, g, b, 0.9f * flash);
        ms.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(AntiprotonInjectorBlockEntity injector) {
        return new AABB(injector.getBlockPos()).inflate(BEAM_LENGTH);
    }
}
