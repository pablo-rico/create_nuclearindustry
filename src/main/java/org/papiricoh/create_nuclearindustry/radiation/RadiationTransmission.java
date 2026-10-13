package org.papiricoh.create_nuclearindustry.radiation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.create_nuclearindustry.Config;

/**
 * Fraction of radiation that gets through one block. Blocks without an entry pass everything
 * if they are not solid, and {@code radiation.solidTransmission} if they are.
 */
public record RadiationTransmission(float transmission) {
    public static final Codec<RadiationTransmission> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.floatRange(0.0f, 1.0f).fieldOf("transmission").forGetter(RadiationTransmission::transmission)
    ).apply(instance, RadiationTransmission::new));

    public static double of(BlockState state) {
        if (state.isAir()) {
            return 1.0;
        }
        RadiationTransmission entry = state.getBlockHolder().getData(AllNuclearRadiation.TRANSMISSION);
        if (entry != null) {
            return entry.transmission();
        }
        return state.canOcclude() ? Config.radiationSolidTransmission : 1.0;
    }
}
