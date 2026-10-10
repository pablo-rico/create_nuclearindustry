package org.papiricoh.create_nuclearindustry.reactor.pwr.steamgen;

import net.minecraft.util.StringRepresentable;

/** What a Steam Generator Port carries; the wrench cycles through them. */
public enum SteamGeneratorPortRole implements StringRepresentable {
    /** Hot primary coolant in, from the reactor's hot leg. */
    PRIMARY_IN("primary_in"),
    /** Cooled primary coolant out, back to the reactor's cold leg. */
    PRIMARY_OUT("primary_out"),
    /** Feedwater in. */
    FEED_IN("feed_in"),
    /** Steam out, for the turbines. */
    STEAM_OUT("steam_out");

    private final String serializedName;

    SteamGeneratorPortRole(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }

    public SteamGeneratorPortRole next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public String translationKey() {
        return "create_nuclearindustry.steam_generator.role." + serializedName;
    }
}
