package org.papiricoh.create_nuclearindustry.accelerator.physics;

/**
 * The three Penning trap sizes, each with its own colour (the glowing band on its magnet heads) and
 * default {@link PenningTrapModel.Params}. The config may override the numbers; see
 * {@code AcceleratorConfig}. Saved by name.
 */
public enum TrapTier {
    SMALL("small", 0x3FD8E8, PenningTrapModel.Params.SMALL),
    MEDIUM("medium", 0x9B5CFF, PenningTrapModel.Params.MEDIUM),
    LARGE("large", 0xFFB23F, PenningTrapModel.Params.LARGE);

    private final String id;
    private final int colour;
    private final PenningTrapModel.Params defaults;

    TrapTier(String id, int colour, PenningTrapModel.Params defaults) {
        this.id = id;
        this.colour = colour;
        this.defaults = defaults;
    }

    /** Lower-case name for saving and lang keys. */
    public String id() {
        return id;
    }

    /** Tier colour as 0xRRGGBB. */
    public int colour() {
        return colour;
    }

    /** Built-in numbers, before the config. */
    public PenningTrapModel.Params defaults() {
        return defaults;
    }

    public static TrapTier byId(String id) {
        for (TrapTier tier : values()) {
            if (tier.id.equals(id)) {
                return tier;
            }
        }
        return SMALL;
    }
}
