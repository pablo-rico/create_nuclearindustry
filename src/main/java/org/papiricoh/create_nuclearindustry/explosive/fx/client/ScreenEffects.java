package org.papiricoh.create_nuclearindustry.explosive.fx.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.papiricoh.create_nuclearindustry.ClientConfig;
import org.papiricoh.create_nuclearindustry.Create_NuclearIndustry;
import org.papiricoh.create_nuclearindustry.explosive.blast.ExplosionPalette;
import org.papiricoh.create_nuclearindustry.explosive.fx.ExplosionFxRegistry;

/**
 * What a detonation does to the screen. The flash: white and brief, brighter facing the fireball and nearer it, and the orange
 * glow after it. Flash blindness ({@link ExplosionFxRegistry#FLASH_BLINDED}, applied by the server to whoever looked): the view
 * washes out white, then a veil lingers and an afterimage of the fireball stays where it was seen on the retina, first bright,
 * then a dark blot, fading with the effect. Both are drawn under the chat and the menus, never over them, and scale with the
 * player's {@code explosion.flash} and {@code explosion.flashBlindness}.
 */
public final class ScreenEffects {
    private static final ResourceLocation FLASH = ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "nuclear_flash");
    private static final ResourceLocation BLINDNESS = ResourceLocation.fromNamespaceAndPath(Create_NuclearIndustry.MODID, "flash_blindness");

    /** The last frame's view, to place the afterimage where the fireball was seen. */
    private static final Matrix4f VIEW = new Matrix4f();
    private static final Matrix4f PROJECTION = new Matrix4f();
    private static Vec3 viewEye = Vec3.ZERO;
    private static boolean haveView;

    /** The blindness being shown: how long it ran when it began, its strength, and where its afterimage sits (0-1 of the screen). */
    private static int startDuration;
    private static int amplifier = -1;
    private static float afterX = 0.5f, afterY = 0.5f;

    private ScreenEffects() {}

    static void registerLayers(RegisterGuiLayersEvent event) {
        event.registerBelow(VanillaGuiLayers.CHAT, BLINDNESS, ScreenEffects::renderBlindness);
        event.registerBelow(VanillaGuiLayers.CHAT, FLASH, ScreenEffects::renderFlash);
    }

    /** Keeps the frame's view for placing the afterimage. */
    static void capture(RenderLevelStageEvent event) {
        VIEW.set(event.getModelViewMatrix());
        PROJECTION.set(event.getProjectionMatrix());
        viewEye = event.getCamera().getPosition();
        haveView = true;
    }

    static void reset() {
        amplifier = -1;
        startDuration = 0;
    }

    /** The flash, white and brief, and the orange glow after it, over the whole screen. */
    static void renderFlash(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || (NuclearFx.nukes().isEmpty() && ConventionalFx.active() == 0)) return;
        float partial = delta.getGameTimeDeltaPartialTick(false);
        Vec3 eye = mc.player.getEyePosition(partial);
        Vec3 look = mc.player.getViewVector(partial);
        double flash = 0;
        double glow = 0;
        ExplosionPalette.Rgb flashColour = ExplosionPalette.FLASH;
        ExplosionPalette.Rgb glowColour = ExplosionPalette.AFTERGLOW;
        for (NuclearFx.Nuke nuke : NuclearFx.nukes()) {
            double age = nuke.age(partial);
            flash = Math.max(flash, NuclearFx.flash(nuke, age, eye, look, mc));
            glow = Math.max(glow, Math.min(1, 1.5 * NuclearFx.glow(nuke, age, eye)) * nuke.palette().glowAlpha());
        }
        flash = Math.max(flash, ConventionalFx.flash(eye, look, partial));
        flash *= ClientConfig.explosionFlash;
        glow *= ClientConfig.explosionFlash;
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        if (glow > 0.003) {
            graphics.fill(0, 0, width, height, PuffList.argb((float) glow, glowColour.red(), glowColour.green(), glowColour.blue()));
        }
        if (flash > 0.01) {
            graphics.fill(0, 0, width, height, PuffList.argb((float) Math.min(1, flash), flashColour.red(), flashColour.green(), flashColour.blue()));
        }
    }

    /** Flash blindness: a white wash, a lingering veil and the fireball's afterimage, by the effect's strength and time left. */
    static void renderBlindness(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        MobEffectInstance effect = mc.player.getEffect(ExplosionFxRegistry.FLASH_BLINDED);
        if (effect == null || ClientConfig.explosionFlashBlindness <= 0) {
            reset();
            return;
        }
        int remaining = effect.getDuration();
        if (effect.getAmplifier() != amplifier || remaining > startDuration) {
            amplifier = effect.getAmplifier();
            startDuration = Math.max(1, remaining);
            locateAfterimage(mc);
        }
        float partial = delta.getGameTimeDeltaPartialTick(false);
        double elapsed = startDuration - remaining + partial;
        double left = Math.max(0, Math.min(1, remaining / (double) startDuration));
        double strength = ClientConfig.explosionFlashBlindness * (0.35 + 0.65 * Math.min(3, amplifier) / 3.0);
        double envelope = Math.pow(left, 0.7);
        double whiteout = Math.exp(-elapsed / 16);
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        float wash = (float) Math.min(1, strength * (0.9 * whiteout + 0.25 * envelope));
        if (wash > 0.004) graphics.fill(0, 0, width, height, PuffList.argb(wash, 1.0f, 0.97f, 0.9f));
        // The afterimage: bright for a few seconds, then a dark blot where the fireball burned into the retina.
        float size = (float) (height * (0.16 + 0.05 * Math.min(3, amplifier)));
        int x = Math.round(afterX * width - size / 2), y = Math.round(afterY * height - size / 2);
        double bright = Math.exp(-elapsed / 50);
        float a = (float) Math.min(1, strength * envelope);
        // Smooth, not the texture's 64 pixels blown up into blocks.
        mc.getTextureManager().getTexture(NukeRenderTypes.AFTERIMAGE).setFilter(true, false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        if (bright > 0.05) {
            RenderSystem.setShaderColor(1.0f, 0.95f, 0.8f, (float) (a * bright));
            graphics.blit(NukeRenderTypes.AFTERIMAGE, x, y, Math.round(size), Math.round(size), 0, 0, 64, 64, 64, 64);
        }
        float dark = (float) (a * 0.65 * (1 - bright));
        if (dark > 0.01) {
            RenderSystem.setShaderColor(0.22f, 0.18f, 0.3f, dark);
            graphics.blit(NukeRenderTypes.AFTERIMAGE, x, y, Math.round(size), Math.round(size), 0, 0, 64, 64, 64, 64);
        }
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.disableBlend();
    }

    /**
     * Puts the afterimage where the brightest fireball was on screen when the blindness began (seen from where the player looks
     * now), or in the middle if none was in view. It then stays there on the screen: an afterimage is burnt into the retina, so it
     * moves with the eye.
     */
    private static void locateAfterimage(Minecraft mc) {
        afterX = 0.5f;
        afterY = 0.5f;
        if (!haveView || mc.player == null) return;
        Vec3 look = mc.player.getViewVector(1);
        Vec3 best = null;
        double bestScore = 0;
        for (NuclearFx.Nuke nuke : NuclearFx.nukes()) {
            double age = nuke.age(0);
            Vec3 at = nuke.fireball(age);
            double score = NuclearFx.flash(nuke, age, viewEye, look, mc) + 1e-4 * NuclearFx.apparent(nuke, age, viewEye);
            if (score > bestScore) {
                bestScore = score;
                best = at;
            }
        }
        for (ConventionalFx.Blast blast : ConventionalFx.blasts()) {
            double d = Math.max(1, blast.centre().distanceTo(viewEye));
            double score = 1e-5 * blast.payload.fireballRadius() / d;
            if (score > bestScore) {
                bestScore = score;
                best = blast.centre();
            }
        }
        if (best == null) return;
        Vector4f clip = new Vector4f((float) (best.x - viewEye.x), (float) (best.y - viewEye.y), (float) (best.z - viewEye.z), 1);
        VIEW.transform(clip);
        PROJECTION.transform(clip);
        if (clip.w <= 0) return;
        float sx = clip.x / clip.w, sy = clip.y / clip.w;
        if (Math.abs(sx) > 1.1f || Math.abs(sy) > 1.1f) return;
        afterX = Math.max(0.05f, Math.min(0.95f, sx * 0.5f + 0.5f));
        afterY = Math.max(0.05f, Math.min(0.95f, 0.5f - sy * 0.5f));
    }
}
