package org.papiricoh.create_nuclearindustry.explosive.fx.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.papiricoh.create_nuclearindustry.ClientConfig;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.explosive.fx.model.QualityPreset;
import org.slf4j.Logger;

/**
 * Hot air rippling the view: over a fireball and the air above it while it burns, and along the ground where a nuclear shock
 * front has just passed. A post-processing pass over the finished world ({@code shaders/post/nuke_heat_haze.json} declares its
 * spare target; the pass itself, {@code shaders/program/nuke_heat_haze}, is added here so its uniforms can be set each frame):
 * it rebuilds each pixel's place from the depth buffer and bends the image by a scrolling noise where the air is hot. One source
 * at a time, the nearest and strongest. Never under a shader pack (which has its own pipeline), with
 * {@code explosion.heatHaze} off, or at LOW quality.
 */
public final class HeatHaze {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation CHAIN = ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "shaders/post/nuke_heat_haze.json");
    private static final ResourceLocation NOISE = ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "textures/fx/heat_haze_noise.png");

    @Nullable
    private static PostChain chain;
    @Nullable
    private static EffectInstance effect;
    private static boolean failed;
    private static int width, height;
    private static final Vector4f TMP = new Vector4f();

    private HeatHaze() {}

    /** Drops the pass (resources reloaded); it is built again when next needed. */
    static void reload() {
        close();
        failed = false;
    }

    static void close() {
        if (chain != null) chain.close();
        chain = null;
        effect = null;
    }

    static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        if (!ClientConfig.explosionHeatHaze || NukeRenderTypes.pack() || failed || !QualityPreset.of(ClientConfig.explosionQuality).heatHaze()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 eye = event.getCamera().getPosition();
        // The hottest source near the player: a nuclear fireball or shock front, or a conventional fireball.
        double fireX = 0, fireY = 0, fireZ = 0, fireRadius = 0, fireStrength = 0;
        double groundX = 0, groundY = 0, groundZ = 0, shockRadius = 0, shockWidth = 1, shockStrength = 0;
        for (NuclearFx.Nuke nuke : NuclearFx.nukes()) {
            double age = nuke.age(partial);
            double seconds = nuke.cloud().fireballSeconds();
            Vec3 ball = nuke.fireball(age);
            double distance = ball.distanceTo(eye);
            double r = nuke.cloud().fireballRadius(age);
            double hot = Math.max(0, 1 - age / (seconds * 2.5)) * Math.min(1, 6 * r / Math.max(1, distance));
            if (hot > fireStrength) {
                fireStrength = hot;
                fireX = ball.x;
                fireY = ball.y;
                fireZ = ball.z;
                fireRadius = r;
            }
            double front = nuke.cloud().shockRadius(age);
            double wave = nuke.payload().waveRadius();
            Vec3 zero = nuke.groundZero();
            double flat = Math.hypot(eye.x - zero.x, eye.z - zero.z);
            if (front < wave && front > 0) {
                double shock = 0.8 * (1 - front / wave) * Math.max(0, 1 - Math.abs(front - flat) / 400);
                if (shock > shockStrength) {
                    shockStrength = shock;
                    shockRadius = front;
                    shockWidth = 4 + 0.03 * front;
                    groundX = zero.x;
                    groundY = zero.y;
                    groundZ = zero.z;
                }
            }
        }
        double[] spot = ConventionalFx.hotSpot(eye, partial);
        if (spot != null && spot[4] > fireStrength) {
            fireX = spot[0];
            fireY = spot[1];
            fireZ = spot[2];
            fireRadius = spot[3];
            fireStrength = spot[4];
        }
        if (fireStrength < 0.01 && shockStrength < 0.01) return;
        if (!ensure(mc)) return;
        EffectInstance pass = effect;
        if (pass == null || chain == null) return;
        Matrix4f view = event.getModelViewMatrix();
        Matrix4f projection = event.getProjectionMatrix();
        Vector3f fire = viewSpace(view, fireX - eye.x, fireY - eye.y, fireZ - eye.z, 1);
        Vector3f ground = viewSpace(view, groundX - eye.x, groundY - eye.y, groundZ - eye.z, 1);
        Vector3f up = viewSpace(view, 0, 1, 0, 0);
        pass.safeGetUniform("ProjScale").set(1 / projection.m00(), 1 / projection.m11());
        pass.safeGetUniform("DepthRange").set(0.05f, mc.gameRenderer.getDepthFar());
        pass.safeGetUniform("FireCentre").set(fire.x, fire.y, fire.z);
        pass.safeGetUniform("GroundCentre").set(ground.x, ground.y, ground.z);
        pass.safeGetUniform("Up").set(up.x, up.y, up.z);
        pass.safeGetUniform("Fire").set((float) fireRadius, (float) fireStrength);
        pass.safeGetUniform("Shock").set((float) shockRadius, (float) shockWidth, (float) shockStrength);
        pass.safeGetUniform("HazeTime").set((float) (((mc.level.getGameTime() % 24000) + partial) / 20.0));
        chain.process(partial);
        mc.getMainRenderTarget().bindWrite(false);
    }

    private static Vector3f viewSpace(Matrix4f view, double x, double y, double z, float w) {
        TMP.set((float) x, (float) y, (float) z, w);
        view.transform(TMP);
        return new Vector3f(TMP.x, TMP.y, TMP.z);
    }

    /** Builds the pass if it is not there and keeps it the screen's size; false if it cannot be had. */
    private static boolean ensure(Minecraft mc) {
        RenderTarget main = mc.getMainRenderTarget();
        if (chain == null) {
            try {
                PostChain built = new PostChain(mc.getTextureManager(), mc.getResourceManager(), main, CHAIN);
                built.resize(main.width, main.height);
                RenderTarget swap = built.getTempTarget("swap");
                PostPass haze = built.addPass(Create_NuclearIndustry.MODID + ":nuke_heat_haze", main, swap, true);
                haze.addAuxAsset("DepthSampler", main::getDepthTextureId, main.width, main.height);
                AbstractTexture noise = mc.getTextureManager().getTexture(NOISE);
                haze.addAuxAsset("NoiseSampler", noise::getId, 128, 128);
                built.addPass("blit", swap, main, false);
                built.resize(main.width, main.height);
                chain = built;
                effect = haze.getEffect();
                width = main.width;
                height = main.height;
            } catch (Exception e) {
                failed = true;
                LOGGER.warn("Could not load the explosion heat haze; it stays off", e);
                close();
                return false;
            }
        }
        if (main.width != width || main.height != height) {
            chain.resize(main.width, main.height);
            width = main.width;
            height = main.height;
        }
        return true;
    }
}
