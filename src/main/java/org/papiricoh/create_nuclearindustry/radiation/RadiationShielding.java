package org.papiricoh.create_nuclearindustry.radiation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Fraction of radiation one worn armour piece stops; a player's pieces add up, capped by config. */
public record RadiationShielding(float fraction) {
    public static final Codec<RadiationShielding> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.floatRange(0.0f, 1.0f).fieldOf("fraction").forGetter(RadiationShielding::fraction)
    ).apply(instance, RadiationShielding::new));
}
