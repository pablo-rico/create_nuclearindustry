package org.papiricoh.create_nuclearindustry.radiation;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Anti-radiation treatment. The effects themselves do nothing each tick: {@link RadiationHandler}
 * checks for them once a second, cutting the dose taken (radioprotection) or flushing the dose
 * already held (radiation purge).
 */
public class RadiationMedicineEffect extends MobEffect {

    public RadiationMedicineEffect(int color) {
        super(MobEffectCategory.BENEFICIAL, color);
    }
}
