package org.papiricoh.create_nuclearindustry.explosive.fx;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * What a detonation does to a player's senses: flash blindness from looking at the fireball, or deafness from the shock wave.
 * The effect does nothing on the server; the client reads it to wash out the screen or muffle the world, by its amplifier
 * (0 mild to 3 severe).
 */
public class BlastSenseEffect extends MobEffect {

    public BlastSenseEffect(int color) {
        super(MobEffectCategory.HARMFUL, color);
    }
}
