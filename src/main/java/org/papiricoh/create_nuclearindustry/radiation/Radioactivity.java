package org.papiricoh.create_nuclearindustry.radiation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * How strongly one item irradiates whoever carries it, in dose per second.
 *
 * @param perSecond             dose rate of one item (of weapons-grade material, if scaled)
 * @param scalesWithEnrichment  multiply by {@code enrichment / 90}, so natural uranium barely registers
 * @param heatPerSecond         decay heat of one item; carried without protection it burns
 */
public record Radioactivity(float perSecond, boolean scalesWithEnrichment, float heatPerSecond) {
    public static final Codec<Radioactivity> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.fieldOf("per_second").forGetter(Radioactivity::perSecond),
            Codec.BOOL.optionalFieldOf("scales_with_enrichment", false).forGetter(Radioactivity::scalesWithEnrichment),
            Codec.FLOAT.optionalFieldOf("heat_per_second", 0.0f).forGetter(Radioactivity::heatPerSecond)
    ).apply(instance, Radioactivity::new));

    public Radioactivity(float perSecond, boolean scalesWithEnrichment) {
        this(perSecond, scalesWithEnrichment, 0.0f);
    }
}
