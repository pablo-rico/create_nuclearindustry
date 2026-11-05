package org.papiricoh.create_nuclearindustry.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.create_nuclearindustry.dyson.client.fx.SunDimming;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Dims the sun's disc as the Dyson swarm fills ({@link SunDimming}). {@code renderSky} draws the sun right after binding its
 * texture and the moon right after binding its own, both under the same shader colour (alpha {@code 1 - rain}); the sun's alpha is
 * scaled just before the first and put back just before the second, so the moon is left alone. Optional: if a later game or another
 * mod moves these calls, the sun simply stays as it is.
 */
@Mixin(LevelRenderer.class)
abstract class LevelRendererSunDimMixin {
    @Shadow
    @Nullable
    private ClientLevel level;

    @Unique
    private float create_nuclearindustry$sunAlpha = Float.NaN;

    @Inject(method = "renderSky", require = 0, at = @At(value = "INVOKE", ordinal = 0,
            target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderTexture(ILnet/minecraft/resources/ResourceLocation;)V"))
    private void create_nuclearindustry$dimSun(CallbackInfo ci) {
        create_nuclearindustry$sunAlpha = Float.NaN;
        float factor = SunDimming.factor(level);
        if (factor >= 1) return;
        float[] colour = RenderSystem.getShaderColor();
        float r = colour[0], g = colour[1], b = colour[2], a = colour[3];
        create_nuclearindustry$sunAlpha = a;
        RenderSystem.setShaderColor(r, g, b, a * factor);
    }

    @Inject(method = "renderSky", require = 0, at = @At(value = "INVOKE", ordinal = 1,
            target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderTexture(ILnet/minecraft/resources/ResourceLocation;)V"))
    private void create_nuclearindustry$restoreForMoon(CallbackInfo ci) {
        if (Float.isNaN(create_nuclearindustry$sunAlpha)) return;
        float[] colour = RenderSystem.getShaderColor();
        RenderSystem.setShaderColor(colour[0], colour[1], colour[2], create_nuclearindustry$sunAlpha);
        create_nuclearindustry$sunAlpha = Float.NaN;
    }
}
