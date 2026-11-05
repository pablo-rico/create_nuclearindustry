package org.papiricoh.create_nuclearindustry.mixin.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.create_nuclearindustry.explosive.fx.client.SkyTint;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Tints the sky and the clouds after a nuclear detonation ({@link SkyTint}): the fireball's orange afterglow, then the dust it
 * raised. Optional: if a later game or another mod changes these methods, the sky simply stays as it is.
 */
@Mixin(ClientLevel.class)
abstract class ClientLevelSkyTintMixin {
    @Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true, require = 0)
    private void create_nuclearindustry$tintSky(Vec3 pos, float partialTick, CallbackInfoReturnable<Vec3> cir) {
        Vec3 tinted = SkyTint.sky(cir.getReturnValue());
        if (tinted != null) cir.setReturnValue(tinted);
    }

    @Inject(method = "getCloudColor", at = @At("RETURN"), cancellable = true, require = 0)
    private void create_nuclearindustry$tintClouds(float partialTick, CallbackInfoReturnable<Vec3> cir) {
        Vec3 tinted = SkyTint.clouds(cir.getReturnValue());
        if (tinted != null) cir.setReturnValue(tinted);
    }
}
