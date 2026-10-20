package org.papiricoh.create_nuclearindustry.missile.defense;

/** Who ordered an engagement. Add new ones at the end. */
public enum EngagementSource {
    /** A battery on its own, from the radars on its link code. */
    AUTO_BATTERY,
    /** A command table's automatic defence, from its linked radars. */
    AUTO_TABLE,
    /** An operator at a command table. */
    MANUAL_TABLE
}
