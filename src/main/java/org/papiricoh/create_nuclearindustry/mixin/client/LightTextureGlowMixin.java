package org.papiricoh.create_nuclearindustry.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.LightTexture;
import org.joml.Vector3f;
import org.papiricoh.create_nuclearindustry.explosive.fx.client.TerrainGlow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Lights the world in a detonation's flash and its fireball's glow ({@link TerrainGlow}): each lightmap texel, once the
 * dimension has had its say, gets the glow added by how much sky light reaches it. Wraps the call rather than replacing it, so
 * other mods adjusting the lightmap still do. Optional: if the call moves, the world is simply not lit.
 */
@Mixin(LightTexture.class)
abstract class LightTextureGlowMixin {
    @WrapOperation(method = "updateLightTexture", require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/DimensionSpecialEffects;adjustLightmapColors(Lnet/minecraft/client/multiplayer/ClientLevel;FFFFIILorg/joml/Vector3f;)V"))
    private void create_nuclearindustry$glow(DimensionSpecialEffects effects, ClientLevel level, float partialTicks, float skyDarken,
                                             float blockLightRedFlicker, float skyLight, int pixelX, int pixelY, Vector3f colors,
                                             Operation<Void> original) {
        original.call(effects, level, partialTicks, skyDarken, blockLightRedFlicker, skyLight, pixelX, pixelY, colors);
        TerrainGlow.apply(pixelY, pixelX, colors);
    }
}
