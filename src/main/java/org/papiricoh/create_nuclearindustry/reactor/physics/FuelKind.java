package org.papiricoh.create_nuclearindustry.reactor.physics;

/**
 * What a fuel assembly is made of. MOX (plutonium mixed into depleted uranium) behaves like
 * 12% uranium, a little more reactive, hotter, and with less control rod margin. Thorium is
 * a breeder fuel: see {@link #THORIUM}.
 */
public enum FuelKind {
    URANIUM("uranium", 1.0, 1.0, 1.0),
    MOX("mox", 1.10, 1.15, 0.90),
    /**
     * Thorium dioxide seeded with a little fissile uranium: weak on its own, it breeds uranium-233
     * as it burns. Provisional numbers until the thorium cycle is modelled.
     */
    THORIUM("thorium", 0.85, 1.0, 1.05);

    /** Enrichment MOX counts as for fuel life and reactivity. */
    public static final float MOX_EQUIVALENT_ENRICHMENT = 12.0f;
    /** Enrichment thorium fuel counts as for fuel life and reactivity (provisional). */
    public static final float THORIUM_EQUIVALENT_ENRICHMENT = 5.0f;

    private final String id;
    private final double reactivityBonus;
    private final double heatFactor;
    private final double controlWorth;

    FuelKind(String id, double reactivityBonus, double heatFactor, double controlWorth) {
        this.id = id;
        this.reactivityBonus = reactivityBonus;
        this.heatFactor = heatFactor;
        this.controlWorth = controlWorth;
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
