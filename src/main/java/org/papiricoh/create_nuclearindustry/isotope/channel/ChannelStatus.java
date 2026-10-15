package org.papiricoh.create_nuclearindustry.isotope.channel;

import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/** What the irradiation channel's tube mouth shows. */
public enum ChannelStatus implements StringRepresentable {
    EMPTY,
    /** A target is in, but the reactor gives no neutrons. */
    LOADED,
    IRRADIATING,
    /** Activated targets wait to be taken out. */
    READY;

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
