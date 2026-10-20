package org.papiricoh.create_nuclearindustry.missile.link;

import net.minecraft.core.GlobalPos;

import java.util.List;

/**
 * A missile device a Tactical Linker links to radars and launch controllers: the command table (radars and silos) and the
 * radar scope (one radar). The Control Room Linker refuses these, so the two tools never get mixed up.
 */
public interface TacticalDevice {
    /** What linking did; sent to the player. Append only. */
    enum Result { ADDED, REMOVED, FULL, UNSUPPORTED, OUT_OF_RANGE }

    /** Links {@code target}, or unlinks it if it was linked already. */
    Result toggleLink(GlobalPos target);

    /** Unlinks {@code target}; false if it was not linked. */
    boolean unlink(GlobalPos target);

    /** Drops every link. */
    void clearLinks();

    /** Everything linked, radars and launch controllers alike. */
    List<GlobalPos> links();
}
