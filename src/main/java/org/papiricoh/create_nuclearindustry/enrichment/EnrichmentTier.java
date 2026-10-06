package org.papiricoh.create_nuclearindustry.enrichment;

import net.minecraft.world.level.material.FlowingFluid;
import org.papiricoh.create_nuclearindustry.AllNuclearFluids;

import java.util.Locale;

/**
 * Discrete enrichment levels. Items keep a precise {@code enrichment} component, but
 * fluids are one fluid per tier: Create pipes and tanks never merge stacks with different
 * components, so a continuous value would fragment every tank in a cascade.
 */
public enum EnrichmentTier {
    DEPLETED(0.3f, 0.0f),
    NATURAL(0.7f, 0.7f),
    LOW(5.0f, 3.0f),
    HIGH(20.0f, 15.0f),
    WEAPONS(90.0f, 90.0f);

    private final float nominal;
    private final float min;

    EnrichmentTier(float nominal, float min) {
        this.nominal = nominal;
        this.min = min;
    }

    /** The enrichment written on items produced at this tier. */
    public float nominal() {
        return nominal;
    }

    /** Lowest enrichment (inclusive) that still belongs to this tier. */
    public float min() {
        return min;
    }

    /** Lowest enrichment (exclusive) of the next tier, or above 100 for the top tier. */
    public float max() {
        EnrichmentTier next = next();
        return next == this ? 100.01f : next.min;
    }

    public EnrichmentTier next() {
        EnrichmentTier[] tiers = values();
        return ordinal() + 1 < tiers.length ? tiers[ordinal() + 1] : this;
    }

    public EnrichmentTier previous() {
        return ordinal() > 0 ? values()[ordinal() - 1] : this;
    }

    public boolean contains(float enrichment) {
        return enrichment >= min && enrichment < max();
    }

    public static EnrichmentTier of(float enrichment) {
        EnrichmentTier[] tiers = values();
        for (int i = tiers.length - 1; i >= 0; i--) {
            if (enrichment >= tiers[i].min) {
                return tiers[i];
            }
        }
        return DEPLETED;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** The uranium hexafluoride fluid carrying this tier. */
    public FlowingFluid hexafluoride() {
        return switch (this) {
            case DEPLETED -> AllNuclearFluids.DEPLETED_URANIUM_HEXAFLUORIDE.get();
            case NATURAL -> AllNuclearFluids.URANIUM_HEXAFLUORIDE.get();
            case LOW -> AllNuclearFluids.LOW_ENRICHED_URANIUM_HEXAFLUORIDE.get();
            case HIGH -> AllNuclearFluids.HIGH_ENRICHED_URANIUM_HEXAFLUORIDE.get();
            case WEAPONS -> AllNuclearFluids.WEAPONS_GRADE_URANIUM_HEXAFLUORIDE.get();
        };
    }
}
