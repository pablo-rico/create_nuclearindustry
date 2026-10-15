package org.papiricoh.create_nuclearindustry.antimatter.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperRenderTypeBuffer;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.joml.Matrix3f;
import org.joml.Vector3f;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.accelerator.physics.TrapState;
import org.papiricoh.create_nuclearindustry.accelerator.physics.TrapTier;
import org.papiricoh.create_nuclearindustry.antimatter.trap.PenningTrapBlock;
import org.papiricoh.create_nuclearindustry.antimatter.trap.PenningTrapBlockEntity;

/**
 * Draws what a Penning trap's model cannot: the antimatter cloud floating in the glass, the status
 * LED, the medium trap's field gauge, the large trap's charge ring and, in a quench, the red flash of
 * the field bands.
 *
 * <p>The cloud is a few camera-facing layers: a coloured body blended over what is behind
 * ({@link AntimatterRenderTypes#CLOUD_BODY}, so it shows in daylight) under additive light
 * ({@link AntimatterRenderTypes#CLOUD_GLOW}): a halo in the tier colour, two wisps turning opposite
 * ways and a white core, all as big as {@link TrapLook} says. The layers face the viewer through the whole transform (the model-view
 * matrix and the pose), so they also face the camera in a Ponder scene. Only the master block has a
 * block entity, so a large trap is drawn once, from its bottom north-west block.
 */
public class PenningTrapRenderer extends SafeBlockEntityRenderer<PenningTrapBlockEntity> {
    private static final float PX = 1 / 16f;
    private static final ResourceLocation CLOUD = tex("antimatter_cloud");
    private static final ResourceLocation WISP = tex("antimatter_cloud_wisp");
    private static final ResourceLocation LIT = tex("penning_trap_led_lit");
    /** Status LED colours, by {@link TrapState} ordinal; OFFLINE leaves the dark lens. */
    private static final int STABLE_GREEN = 0x58E070, LOW_AMBER = 0xFFB020, QUENCH_RED = 0xFF3030;
    private static final int RING_OFF = -1;
    /** Height of the medium trap's gauge (and its clamp), pixels above the lower block. */
    private static final float GAUGE_Y = 11;

    public PenningTrapRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    protected void renderSafe(PenningTrapBlockEntity trap, float partialTicks, PoseStack ms, MultiBufferSource buffers, int light,
                              int overlay) {
        Level level = trap.getLevel();
        BlockState state = trap.getBlockState();
        if (level == null || !(state.getBlock() instanceof PenningTrapBlock block) || !block.isMaster(state)) {
            return;
        }
        // Create's render clock rather than the level's game time, which stands still in a Ponder scene.
        float time = AnimationTickHolder.getRenderTime(level);
        TrapTier tier = trap.tier();
        TrapShape shape = TrapShape.of(tier);
        TrapState trapState = trap.state();
        double capacity = trap.capacity();
        double fill = capacity > 0 ? trap.stored() / capacity : 0;
        int quenchTicks = Math.max(1, trap.params().quenchTicks());
        float progress = trapState == TrapState.QUENCHING ? 1 - trap.quenchTicksLeft() / (float) quenchTicks : 0;
        boolean blinkOn = ((int) (time / 2.5f)) % 2 == 0;
        float facingAngle = tier == TrapTier.LARGE ? 0 : facingAngle(state);

        // Lit model parts first, each with its own buffer; the additive layers come after.
        int ledColour = switch (trapState) {
            case OFFLINE -> RING_OFF;
            case STABLE -> STABLE_GREEN;
            case LOW_POWER -> LOW_AMBER;
            case QUENCHING -> blinkOn ? QUENCH_RED : RING_OFF;
        };
        switch (tier) {
            case SMALL -> lit(AntimatterPartials.SMALL_STATUS_LED, state, ms, buffers, facingAngle, 0, ledColour);
            case MEDIUM -> {
                lit(AntimatterPartials.MEDIUM_STATUS_LED, state, ms, buffers, facingAngle, 0, ledColour);
                needle(trap, state, ms, buffers, light, facingAngle, partialTicks, time);
            }
            case LARGE -> {
                lit(AntimatterPartials.LARGE_STATUS_LED, state, ms, buffers, 0, 2, ledColour);
                ring(state, ms, buffers, fill, trapState, time);
            }
        }
        if (trapState == TrapState.QUENCHING && blinkOn) {
            quenchBands(shape, ms, buffers);
        }

        TrapLook look = TrapLook.of(trapState, fill, trap.fieldStrength(), progress, tier.colour(), time);
        if (look.radius() > 0 && look.glow() > 0.01f) {
            cloud(shape, look, ms, buffers, time);
        }
    }

    // ------------------------------------------------------------------ lit parts

    /** Model rotation of a horizontal FACING, as the blockstate turns it (front = model north). */
    private static float facingAngle(BlockState state) {
        if (!state.hasProperty(HorizontalDirectionalBlock.FACING)) {
            return 0;
        }
        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        // The blockstate turns y = toYRot + 180 clockwise from above; flywheel turns anticlockwise.
        return -((facing.toYRot() + 180) % 360);
    }

    /** A white lit partial tinted {@code colour}, turned with the trap and raised {@code up} blocks. */
    private static void lit(PartialModel partial, BlockState state, PoseStack ms, MultiBufferSource buffers, float angle, int up,
                            int colour) {
        if (colour == RING_OFF) {
            return;
        }
        SuperByteBuffer sbb = CachedBuffers.partial(partial, state);
        sbb.translate(0, up, 0).center().rotateYDegrees(angle).uncenter()
                .color(0xFF000000 | colour)
                .light(LightTexture.FULL_BRIGHT)
                .disableDiffuse()
                .renderInto(ms, buffers.getBuffer(RenderType.cutout()));
    }

    /** The large trap's charge ring: one lens per twelfth of the charge, chasing red in a quench. */
    private static void ring(BlockState state, PoseStack ms, MultiBufferSource buffers, double fill, TrapState trapState, float time) {
        PartialModel[] leds = AntimatterPartials.LARGE_RING;
        if (trapState == TrapState.QUENCHING) {
            int step = (int) (time / 1.5f);
            for (int i = 0; i < leds.length; i++) {
                if ((i + step) % 3 == 0) {
                    lit(leds[i], state, ms, buffers, 0, 2, QUENCH_RED);
                }
            }
            return;
        }
        int lit = fill > 1.0e-6 ? Math.max(1, (int) Math.round(fill * leds.length)) : 0;
        int colour = trapState == TrapState.LOW_POWER ? LOW_AMBER : TrapTier.LARGE.colour();
        for (int i = 0; i < Math.min(lit, leds.length); i++) {
            lit(leds[i], state, ms, buffers, 0, 2, colour);
        }
    }

    /**
     * The medium trap's field gauge: the needle sweeps clockwise from lower left (no field) to lower
     * right (full field) on the dial turned 45 degrees off the front, and trembles in a quench.
     */
    private static void needle(PenningTrapBlockEntity trap, BlockState state, PoseStack ms, MultiBufferSource buffers, int light,
                               float facingAngle, float partialTicks, float time) {
        float field = TrapClientEffects.needle(trap, partialTicks);
        float angle = -135 + 270 * Math.max(0, Math.min(1, field));
        if (trap.state() == TrapState.QUENCHING) {
            angle += 6 * (TrapLook.noise((long) (time * 4)) - 0.5f);
        }
        SuperByteBuffer sbb = CachedBuffers.partial(AntimatterPartials.GAUGE_NEEDLE, state);
        sbb.center().rotateYDegrees(facingAngle).uncenter()
                .translate(2.52f * PX, GAUGE_Y * PX, 2.52f * PX)
                .rotateYDegrees(45)
                .translate(0, 0, -0.02f * PX)
                .rotateZDegrees(angle)
                .translate(-0.5f, -0.5f, -0.5f)
                .color(0xFF7A1A1A)
                .light(light)
                .renderInto(ms, buffers.getBuffer(RenderType.cutout()));
    }

    /** Both field bands lit red, a little proud of the model's, while the quench alarm flashes. */
    private static void quenchBands(TrapShape shape, PoseStack ms, MultiBufferSource buffers) {
        TextureAtlasSprite sprite = sprite(LIT);
        VertexConsumer buffer = buffers.getBuffer(RenderType.cutout());
        PoseStack.Pose pose = ms.last();
        float h = shape.bandHalf() + 0.06f;
        for (float[] band : new float[][]{shape.bandLow(), shape.bandHigh()}) {
            LitBoxes.box(buffer, pose, sprite, QUENCH_RED, (shape.cx() - h) * PX, (band[0] - 0.04f) * PX, (shape.cz() - h) * PX,
                    (shape.cx() + h) * PX, (band[1] + 0.04f) * PX, (shape.cz() + h) * PX);
        }
    }

    // ------------------------------------------------------------------ the cloud

    private void cloud(TrapShape shape, TrapLook look, PoseStack ms, MultiBufferSource buffers, float time) {
        float radius = look.radius() * shape.maxRadius() * PX;
        float jitter = look.shake() * radius;
        float dx = jitter * (TrapLook.noise((long) (time * 3)) - 0.5f) * 2;
        float dy = jitter * (TrapLook.noise((long) (time * 3) + 101) - 0.5f) * 2;
        float dz = jitter * (TrapLook.noise((long) (time * 3) + 202) - 0.5f) * 2;
        ms.pushPose();
        ms.translate(shape.cx() * PX + dx, (shape.cloudY() + look.bob()) * PX + dy, shape.cz() * PX + dz);
        // The view's right and up axes in this local space: the layers lie in their plane and so face
        // the viewer, in the world and in Ponder alike. The transpose rather than the inverse: it points
        // the same way for a rotation times any scale, and still works when a GUI transform flips y or
        // flattens depth (as Ponder's does), where an inverse goes wrong.
        Matrix3f toLocal = new Matrix3f();
        new Matrix4f(RenderSystem.getModelViewMatrix()).mul(ms.last().pose()).get3x3(toLocal);
        toLocal.transpose();
        Vector3f right = toLocal.transform(new Vector3f(1, 0, 0)).normalize();
        Vector3f up = toLocal.transform(new Vector3f(0, 1, 0)).normalize();
        Billboard view = new Billboard(right, up);

        float r = look.red(), g = look.green(), b = look.blue(), glow = look.glow();
        if (buffers instanceof SuperRenderTypeBuffer) {
            ponderCloud(buffers, ms, view, radius, look);
            ms.popPose();
            return;
        }
        TextureAtlasSprite cloud = sprite(CLOUD);
        TextureAtlasSprite wisp = sprite(WISP);
        // The body: coloured haze over what is behind, so the cloud keeps its colour in daylight.
        VertexConsumer body = buffers.getBuffer(AntimatterRenderTypes.CLOUD_BODY);
        layer(body, ms, view, cloud, radius * 1.35f, 0, r * 0.7f, g * 0.7f, b * 0.7f, 0.6f + 0.3f * glow);
        layer(body, ms, view, wisp, radius * 1.1f, look.spin() * 0.6f + 0.8f, r, g, b, 0.4f + 0.35f * glow);
        layer(body, ms, view, cloud, radius * 0.8f, 0, mix(r, 0.25f), mix(g, 0.25f), mix(b, 0.25f), 0.65f + 0.3f * glow);
        // The glow on top: a halo in the tier colour, two wisps turning opposite ways, a white-hot core.
        VertexConsumer light = buffers.getBuffer(AntimatterRenderTypes.CLOUD_GLOW);
        layer(light, ms, view, cloud, radius * 1.5f, 0, r, g, b, 0.55f * glow);
        layer(light, ms, view, wisp, radius * 1.15f, look.spin(), mix(r, 0.15f), mix(g, 0.15f), mix(b, 0.15f), 0.8f * glow);
        layer(light, ms, view, wisp, radius * 0.95f, -look.spin() * 1.4f + 1.9f, r, g, b, 0.6f * glow);
        layer(light, ms, view, cloud, radius * 0.42f, 0, mix(r, 0.6f), mix(g, 0.6f), mix(b, 0.6f), 0.8f * glow);
        ms.popPose();
    }

    /**
     * The cloud in a Ponder scene, where the cloud's own render types do not show: soft discs of
     * the translucent layer, stacked so the middle is densest, the tier colour outside and white
     * towards the core.
     */
    private static void ponderCloud(MultiBufferSource buffers, PoseStack ms, Billboard view, float radius, TrapLook look) {
        TextureAtlasSprite white = sprite(LIT);
        VertexConsumer buffer = buffers.getBuffer(RenderType.translucent());
        PoseStack.Pose pose = ms.last();
        int discs = 6;
        for (int i = 0; i < discs; i++) {
            float t = i / (float) (discs - 1);
            float size = radius * (1.35f - 1.0f * t);
            float whiten = 0.85f * t * t;
            float alpha = (0.16f + 0.12f * t) * look.glow();
            disc(buffer, pose, view, white, size, look.spin() * (i % 2 == 0 ? 1 : -1), mix(look.red(), whiten), mix(look.green(), whiten),
                    mix(look.blue(), whiten), alpha);
        }
    }

    /** A 16-sided disc facing the viewer, as quads from the middle, full-bright. */
    private static void disc(VertexConsumer b, PoseStack.Pose pose, Billboard view, TextureAtlasSprite s, float half, float spin, float r,
                             float g, float bl, float a) {
        Vector3f ax = view.right(), ay = view.up();
        float u = (s.getU0() + s.getU1()) / 2, v = (s.getV0() + s.getV1()) / 2;
        int segments = 16;
        for (int k = 0; k < segments; k += 2) {
            float[] p = new float[9];
            for (int j = 0; j < 3; j++) {
                double angle = spin + 2 * Math.PI * (k + j) / segments;
                float c = (float) Math.cos(angle) * half, sn = (float) Math.sin(angle) * half;
                p[j * 3] = ax.x() * c + ay.x() * sn;
                p[j * 3 + 1] = ax.y() * c + ay.y() * sn;
                p[j * 3 + 2] = ax.z() * c + ay.z() * sn;
            }
            // Both windings: Ponder's view may mirror the plane, and this layer culls back faces.
            vertex(b, pose, 0, 0, 0, u, v, r, g, bl, a);
            vertex(b, pose, p[0], p[1], p[2], u, v, r, g, bl, a);
            vertex(b, pose, p[3], p[4], p[5], u, v, r, g, bl, a);
            vertex(b, pose, p[6], p[7], p[8], u, v, r, g, bl, a);
            vertex(b, pose, 0, 0, 0, u, v, r, g, bl, a);
            vertex(b, pose, p[6], p[7], p[8], u, v, r, g, bl, a);
            vertex(b, pose, p[3], p[4], p[5], u, v, r, g, bl, a);
            vertex(b, pose, p[0], p[1], p[2], u, v, r, g, bl, a);
        }
    }

    private static void vertex(VertexConsumer b, PoseStack.Pose pose, float x, float y, float z, float u, float v, float r, float g, float bl,
                               float a) {
        b.addVertex(pose, x, y, z).setColor(r, g, bl, a).setUv(u, v).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0, 1, 0);
    }

    /** Blends a channel towards white. */
    private static float mix(float channel, float white) {
        return channel + (1 - channel) * white;
    }

    /** The plane facing the viewer, as two unit axes in the pose's space. */
    private record Billboard(Vector3f right, Vector3f up) {}

    /** One square layer of half-size {@code half}, turned {@code spin} radians in the view plane. */
    private static void layer(VertexConsumer buffer, PoseStack ms, Billboard view, TextureAtlasSprite sprite, float half, float spin,
                              float r, float g, float b, float alpha) {
        if (alpha <= 0.004f || half <= 0) {
            return;
        }
        float cos = (float) Math.cos(spin) * half, sin = (float) Math.sin(spin) * half;
        Vector3f ax = view.right(), ay = view.up();
        // The turned axes, scaled to the half-size.
        float ux = ax.x() * cos + ay.x() * sin, uy = ax.y() * cos + ay.y() * sin, uz = ax.z() * cos + ay.z() * sin;
        float vx = ay.x() * cos - ax.x() * sin, vy = ay.y() * cos - ax.y() * sin, vz = ay.z() * cos - ax.z() * sin;
        PoseStack.Pose pose = ms.last();
        // Half a texel in, so the smooth filter never samples the atlas neighbours.
        float du = (sprite.getU1() - sprite.getU0()) / 32f, dv = (sprite.getV1() - sprite.getV0()) / 32f;
        float u0 = sprite.getU0() + du, u1 = sprite.getU1() - du, v0 = sprite.getV0() + dv, v1 = sprite.getV1() - dv;
        buffer.addVertex(pose, -ux - vx, -uy - vy, -uz - vz).setUv(u0, v1).setColor(r, g, b, alpha);
        buffer.addVertex(pose, ux - vx, uy - vy, uz - vz).setUv(u1, v1).setColor(r, g, b, alpha);
        buffer.addVertex(pose, ux + vx, uy + vy, uz + vz).setUv(u1, v0).setColor(r, g, b, alpha);
        buffer.addVertex(pose, -ux + vx, -uy + vy, -uz + vz).setUv(u0, v0).setColor(r, g, b, alpha);
    }

    private static TextureAtlasSprite sprite(ResourceLocation texture) {
        return Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(texture);
    }

    private static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "block/" + name);
    }

    // ------------------------------------------------------------------ culling

    @Override
    public boolean shouldRenderOffScreen(PenningTrapBlockEntity trap) {
        return trap.tier() == TrapTier.LARGE;
    }

    /** The whole trap, so a tall or wide one is not culled when its master block leaves the screen. */
    @Override
    public AABB getRenderBoundingBox(PenningTrapBlockEntity trap) {
        BlockPos pos = trap.getBlockPos();
        return switch (trap.tier()) {
            case SMALL -> new AABB(pos);
            case MEDIUM -> new AABB(pos).expandTowards(0, 1, 0);
            case LARGE -> new AABB(pos).expandTowards(1, 2, 1);
        };
    }
}
