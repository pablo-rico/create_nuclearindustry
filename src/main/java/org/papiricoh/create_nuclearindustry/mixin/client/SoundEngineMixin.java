package org.papiricoh.create_nuclearindustry.mixin.client;

import dev.ryanhcode.sable.sound.MovingSoundInstanceDelegate;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin {
    @Shadow
    @Final
    private Map<SoundInstance, Integer> soundDeleteTime;
    @Shadow
    private boolean loaded;

    @Shadow
    private int tickCount;

    @Shadow
    @Final
    private Map<SoundInstance, ChannelAccess.ChannelHandle> instanceToChannel;

    @Inject(method = "isActive", at = @At("HEAD"), cancellable = true)
    public void create_nuclearindustry$isActive(SoundInstance soundInstance, CallbackInfoReturnable<Boolean> cir) {
        //if (!(soundInstance instanceof MachineLoopSound)) return;
        if (!this.loaded) cir.setReturnValue(false);
        else {
            if (containsMovingSoundKey(this.soundDeleteTime, soundInstance) && getSoundDeleteTime(this.soundDeleteTime, soundInstance) <= this.tickCount) cir.setReturnValue(true);
            else {
                cir.setReturnValue(containsMovingSoundKey(this.instanceToChannel, soundInstance));
            }
        }
    }

    @Unique
    private static boolean containsMovingSoundKey(Map<SoundInstance, ?> map, SoundInstance soundInstance) {
        for (Object key: map.keySet()) {
            if (key instanceof MovingSoundInstanceDelegate movingSoundInstance) {
                if (soundInstance == movingSoundInstance.instance) return true;
            }
        }
        return map.containsKey(soundInstance);
    }

    @Unique
    private static Integer getSoundDeleteTime(Map<SoundInstance, Integer> map, SoundInstance soundInstance) {
        for (SoundInstance key: map.keySet()) {
            if (key instanceof MovingSoundInstanceDelegate movingSoundInstance) {
                if (soundInstance == movingSoundInstance.instance) return map.get(movingSoundInstance);
            }
        }
        return map.get(soundInstance);
    }
}
