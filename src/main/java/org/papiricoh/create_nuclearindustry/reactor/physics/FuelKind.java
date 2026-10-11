package org.papiricoh.create_nuclearindustry.reactor.physics;

/**
 * What a fuel assembly is made of. MOX (plutonium mixed into depleted uranium) behaves like
 * 12% uranium, a little more reactive, hotter, and with less control rod margin. Thorium is
 * a breeder fuel: see {@link #THORIUM}.
 */
public enum FuelKind {
    URANIUM("uranium", 1.0, 1.0, 1.0, 650),
    MOX("mox", 1.10, 1.15, 0.90, 400),
    /**
     * Thorium dioxide seeded with a little fissile uranium: weak on its own, it breeds uranium-233
     * as it burns ({@link ThoriumBreeding} shapes its reactivity and stretches its life). Thoria
     * conducts heat better than urania, so it runs a little cooler; uranium-233 has fewer delayed
     * neutrons, so the rods hold it slightly better but it answers them faster.
     */
    THORIUM("thorium", 1.0, 0.95, 1.05, 450);

    /** Enrichment MOX counts as for fuel life and reactivity. */
    public static final float MOX_EQUIVALENT_ENRICHMENT = 12.0f;
    /** Enrichment thorium fuel is rated at for fuel life and reactivity, before breeding. */
    public static final float THORIUM_EQUIVALENT_ENRICHMENT = 5.0f;

    private final String id;
    private final double reactivityBonus;
    private final double heatFactor;
    private final double controlWorth;
    private final double delayedFraction;

    FuelKind(String id, double reactivityBonus, double heatFactor, double controlWorth, double delayedFraction) {
        this.id = id;
        this.reactivityBonus = reactivityBonus;
        this.heatFactor = heatFactor;
        this.controlWorth = controlWorth;
        this.delayedFraction = delayedFraction;
    }

    /**
     * Delayed neutron fraction, pcm. Plutonium and uranium-233 give fewer delayed neutrons than
     * uranium-235, so MOX and thorium cores answer their rods faster and sit closer to prompt critical.
     */
    public double delayedFraction() {
        return delayedFraction;
    }

    public String id() {
        return id;
    }

    public double reactivityBonus() {
        return reactivityBonus;
    }

    /** Multiplier on the heat each neutron makes. */
    public double heatFactor() {
        return heatFactor;
    }

    /** Multiplier on how much the control rods can absorb. */
    public double controlWorth() {
        return controlWorth;
    }

    /** Missing or unknown ids are uranium: saves from before MOX. */
    public static FuelKind fromId(String id) {
        for (FuelKind kind : values()) {
            if (kind.id.equals(id)) {
                return kind;
            }
        }
        return URANIUM;
    }
}
