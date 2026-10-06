package org.papiricoh.create_nuclearindustry.radiation;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/** Acute radiation syndrome: periodic damage that armor does not stop. */
public class RadiationSicknessEffect extends MobEffect {

    public RadiationSicknessEffect() {
        super(MobEffectCategory.HARMFUL, 0x7FCB3A);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        entity.hurt(entity.damageSources().source(AllNuclearRadiation.RADIATION_DAMAGE), 1.0f + amplifier);
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        int interval = Math.max(10, 40 >> amplifier);
        return duration % interval == 0;
    }
}
