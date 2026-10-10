package org.papiricoh.create_nuclearindustry.fusion.cryo.plant;

import net.minecraft.util.StringRepresentable;

/** What a Cryogenic Plant Port carries; the wrench cycles through them. */
public enum CryoplantPortMode implements StringRepresentable {
    /** Warm helium gas in, from the tokamak's cryogenic outlet. */
    HELIUM_IN("helium_in", true),
    /** Liquid helium out, to the tokamak's cryogenic inlet. */
    LIQUID_HELIUM_OUT("liquid_helium_out", false),
    /** Liquid nitrogen in, to pre-cool the plant. */
    NITROGEN_IN("nitrogen_in", true),
    /** An air intake: the plant liquefies air and this port gives the liquid air out. */
    LIQUID_AIR_OUT("liquid_air_out", false);

    private final String serializedName;
    private final boolean input;

    CryoplantPortMode(String serializedName, boolean input) {
        this.serializedName = serializedName;
        this.input = input;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }

    /** Takes fluid in (pulls from tanks next to it) rather than giving it out. */
    public boolean input() {
        return input;
    }

    public CryoplantPortMode next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public String translationKey() {
        return "create_nuclearindustry.cryoplant.port." + serializedName;
    }
}
